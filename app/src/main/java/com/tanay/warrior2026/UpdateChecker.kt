package com.tanay.warrior2026

// [UPDATE] v2.2.0: Replaced browser-open with DownloadManager in-app download
// [FIX]    v2.3.0: Added cancelDownload() helper so orphaned DownloadManager jobs
//                  can be cleaned up when the app is killed mid-download and relaunches.

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object UpdateChecker {

    private const val GITHUB_API =
        "https://api.github.com/repos/s4sxam/warrior/releases/latest"

    data class UpdateResult(
        val hasUpdate: Boolean,
        val latestVersion: String,
        val downloadUrl: String
    )

    // ── Check GitHub for a newer release ─────────────────────────────────────

    suspend fun check(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val json = URL(GITHUB_API).readText()
            val obj = JSONObject(json)

            val latestTag = obj.getString("tag_name").removePrefix("v").trim()

            val assets = obj.getJSONArray("assets")
            var apkUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }

            val hasUpdate = isNewer(latestTag, currentVersion)
            UpdateResult(hasUpdate, latestTag, apkUrl)
        } catch (e: Exception) {
            UpdateResult(false, currentVersion, "")
        }
    }

    // ── Kick off a DownloadManager download, returns the download ID ──────────
    //
    // The returned Long (downloadId) is used by the ViewModel to poll progress
    // via DownloadManager.query(). When status == STATUS_SUCCESSFUL, the app
    // triggers the package installer with a content:// URI via FileProvider.
    //
    // Why DownloadManager instead of OkHttp/coroutine stream?
    //  • Survives app backgrounding / process death
    //  • Shows a system notification with progress bar automatically
    //  • Handles wifi/mobile resume on its own

    fun downloadApk(context: Context, url: String, versionName: String): Long {
        val fileName = "warrior-$versionName.apk"

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Warrior 2026 Update")
            setDescription("Downloading v$versionName...")
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            // Save to public Downloads so FileProvider can reach it
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
            // Allow download over both wifi and mobile data
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or
                DownloadManager.Request.NETWORK_MOBILE
            )
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    // ── Cancel and remove a DownloadManager job ───────────────────────────────
    // [FIX v2.3.0] Called on app relaunch if a downloadId was persisted but the
    // download never completed — prevents orphaned background downloads accumulating.

    fun cancelDownload(context: Context, downloadId: Long) {
        if (downloadId == -1L) return
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.remove(downloadId)
    }

    // ── Poll DownloadManager for current status/progress ─────────────────────

    data class DownloadProgress(
        val status: Int,       // DownloadManager.STATUS_* constants
        val bytesDownloaded: Long,
        val bytesTotal: Long,
        val localUri: String?  // non-null when STATUS_SUCCESSFUL
    )

    fun queryProgress(context: Context, downloadId: Long): DownloadProgress {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
        if (!cursor.moveToFirst()) {
            cursor.close()
            return DownloadProgress(DownloadManager.STATUS_FAILED, 0L, 0L, null)
        }
        val status = cursor.getInt(
            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
        )
        val downloaded = cursor.getLong(
            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        )
        val total = cursor.getLong(
            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
        )
        val localUri = if (status == DownloadManager.STATUS_SUCCESSFUL) {
            cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
        } else null
        cursor.close()
        return DownloadProgress(status, downloaded, total, localUri)
    }

    // ── Version comparison ────────────────────────────────────────────────────
    // [FIX v2.3.0] Added tag sanitisation — strips anything after a '-' so
    // "2.3.0-beta" and "2.3.0-rc1" don't break the numeric comparison.

    fun isNewer(latest: String, current: String): Boolean {
        val l = latest.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}
