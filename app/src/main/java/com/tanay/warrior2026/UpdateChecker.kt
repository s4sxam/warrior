package com.tanay.warrior2026

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object UpdateChecker {

    // Must match your GitHub username/repo exactly
    private const val GITHUB_API =
        "https://api.github.com/repos/s4sxam/warrior/releases/latest"

    data class UpdateResult(
        val hasUpdate: Boolean,
        val latestVersion: String,
        val downloadUrl: String
    )

    suspend fun check(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val json = URL(GITHUB_API).readText()
            val obj = JSONObject(json)

            // tag_name is like "v2.1.0" — strip the "v" prefix
            val latestTag = obj.getString("tag_name").removePrefix("v")

            // Find the APK asset download URL
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

    // Compares semantic versions: "2.1.0" > "2.0.0" → true
    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }