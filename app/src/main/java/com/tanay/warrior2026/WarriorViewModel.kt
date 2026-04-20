package com.tanay.warrior2026

// [UPDATE] v2.0.0: Added profile setup, bot simulation, leaderboard state
// [UPDATE] v2.1.0: Added update checker
// [UPDATE] v2.2.0: In-app DownloadManager download + install trigger

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanay.warrior2026.data.BotProfile
import com.tanay.warrior2026.data.BotSimulator
import com.tanay.warrior2026.data.DayData
import com.tanay.warrior2026.data.UserProfile
import com.tanay.warrior2026.data.WarriorRepository
import com.tanay.warrior2026.data.WarriorState
import com.tanay.warrior2026.data.todayKey
import com.tanay.warrior2026.notifications.WarriorScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WarriorViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = WarriorRepository(application)

    private val _state = MutableStateFlow(WarriorState())
    val state: StateFlow<WarriorState> = _state.asStateFlow()

    private val _showConfetti = MutableStateFlow(false)
    val showConfetti: StateFlow<Boolean> = _showConfetti.asStateFlow()

    private val _bots = MutableStateFlow<List<BotProfile>>(emptyList())
    val bots: StateFlow<List<BotProfile>> = _bots.asStateFlow()

    private val _isGeneratingBots = MutableStateFlow(false)
    val isGeneratingBots: StateFlow<Boolean> = _isGeneratingBots.asStateFlow()

    // ── v2.2.0: Extended UpdateState ─────────────────────────────────────────
    //
    //  DownloadPhase describes where the user is in the update flow:
    //
    //  IDLE          → update available, not yet tapped Download
    //  DOWNLOADING   → DownloadManager is running, show progress bar
    //  READY         → download complete, show "Install Now" button
    //  FAILED        → download failed, show retry option
    //
    enum class DownloadPhase { IDLE, DOWNLOADING, READY, FAILED }

    data class UpdateState(
        val hasUpdate: Boolean = false,
        val latestVersion: String = "",
        val downloadUrl: String = "",
        val dismissed: Boolean = false,
        // download tracking
        val phase: DownloadPhase = DownloadPhase.IDLE,
        val downloadId: Long = -1L,
        val progressBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val localUri: String? = null   // set when READY
    ) {
        /** 0f–1f progress fraction; -1f means indeterminate */
        val progressFraction: Float
            get() = if (totalBytes > 0) progressBytes.toFloat() / totalBytes else -1f
    }

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var pollJob: Job? = null   // cancellable polling coroutine

    val trollMessages = listOf(
        "EWW. Shame of humanity.",
        "You call yourself a warrior? Pathetic.",
        "Loser detected. Uninstall me.",
        "Disgusting. Go back to hell.",
        "Ancestors are ashamed of you."
    )

    init {
        viewModelScope.launch {
            repo.warriorStateFlow.collectLatest { s ->
                _state.value = s
                if (s.hasCompletedProfile && s.botsJson.isNotBlank()) {
                    advanceBotsIfNeeded(s.botsJson)
                }
            }
        }
    }

    // ── Onboarding / Profile / Bots (unchanged) ───────────────────────────────

    fun completeOnboarding() {
        val new = _state.value.copy(hasCompletedOnboarding = true)
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
    }

    fun completeProfile(name: String, dob: String, region: String) {
        _isGeneratingBots.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val profile  = UserProfile(name = name, dob = dob, region = region)
            val newBots  = BotSimulator.advanceSimulation(com.tanay.warrior2026.data.generateBots())
            val botsJson = BotSimulator.saveBots(newBots)
            val new = _state.value.copy(
                userProfile         = profile,
                hasCompletedProfile = true,
                botsJson            = botsJson
            )
            withContext(Dispatchers.Main) {
                _state.value = new
                _bots.value  = newBots
                _isGeneratingBots.value = false
            }
            repo.saveState(new)
        }
    }

    private fun advanceBotsIfNeeded(botsJson: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val loaded   = BotSimulator.loadBots(botsJson)
            val advanced = BotSimulator.advanceSimulation(loaded)
            val newJson  = BotSimulator.saveBots(advanced)
            withContext(Dispatchers.Main) { _bots.value = advanced }
            if (newJson != botsJson) {
                val new = _state.value.copy(botsJson = newJson)
                _state.value = new
                repo.saveState(new)
            }
        }
    }

    // ── Leaderboard — reactive StateFlows so UI auto-updates when bots load ──
    //
    // Previously these were plain functions that snapshotted _bots.value at call
    // time. If called before advanceBotsIfNeeded() finished (e.g. immediately after
    // app reopen), _bots was still emptyList() and only the user appeared.
    // Combining the two flows makes the leaderboard recompose as soon as bots arrive.

    val regionalBoard: StateFlow<List<BotSimulator.LeaderboardEntry>> =
        combine(_bots, _state) { bots, s ->
            BotSimulator.regionalLeaderboard(
                bots            = bots,
                userRegion      = s.userProfile.region,
                userName        = s.userProfile.name,
                userPoints      = s.userPoints,
                userTotalClean  = s.totalClean,
                userTotalLogged = s.totalClean + s.totalFailed
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val globalBoard: StateFlow<List<BotSimulator.LeaderboardEntry>> =
        combine(_bots, _state) { bots, s ->
            BotSimulator.globalLeaderboard(
                bots            = bots,
                userName        = s.userProfile.name,
                userPoints      = s.userPoints,
                userRegion      = s.userProfile.region,
                userTotalClean  = s.totalClean,
                userTotalLogged = s.totalClean + s.totalFailed
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun getBotProfile(botId: Int): BotProfile? = _bots.value.find { it.id == botId }

    // ── Existing actions (unchanged) ──────────────────────────────────────────

    fun logVictory() {
        val today = todayKey()
        if (_state.value.history.containsKey(today)) return
        val new = _state.value.copy(
            history = _state.value.history + (today to DayData(status = "clean"))
        )
        _state.value = new
        _showConfetti.value = true
        viewModelScope.launch { repo.saveState(new) }
        vibrate(longArrayOf(0, 100, 50, 100))
        val milestones = setOf(3, 7, 14, 21, 30, 60, 90, 180, 365)
        if (new.streak in milestones) WarriorScheduler.fireMilestoneNow(getApplication(), new.streak)
    }

    fun logRelapse(url: String): Boolean = runCatching {
        val domain = if (url.isBlank() || url == "unknown") "unknown"
                     else extractDomain(url)
        val today  = todayKey()
        val newTriggers = _state.value.triggers.toMutableMap()
        if (domain != "unknown") newTriggers[domain] = (newTriggers[domain] ?: 0) + 1
        val new = _state.value.copy(
            history  = _state.value.history + (today to DayData(status = "failed", site = domain)),
            triggers = newTriggers
        )
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
        vibrate(longArrayOf(0, 500))
        true
    }.getOrDefault(false)

    fun undoToday() {
        val today = todayKey()
        if (!_state.value.history.containsKey(today)) return
        val new = _state.value.copy(history = _state.value.history - today)
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
    }

    fun clearConfetti() { _showConfetti.value = false }

    fun exportJson(): String = repo.exportJson(_state.value)

    fun importJson(json: String): Boolean {
        val merged = repo.importJson(_state.value, json) ?: return false
        _state.value = merged
        viewModelScope.launch { repo.saveState(merged) }
        return true
    }

    // ── v2.2.0: Update flow ───────────────────────────────────────────────────

    fun checkForUpdate(currentVersion: String) {
        viewModelScope.launch {
            val result = UpdateChecker.check(currentVersion)
            if (result.hasUpdate) {
                _updateState.value = UpdateState(
                    hasUpdate     = true,
                    latestVersion = result.latestVersion,
                    downloadUrl   = result.downloadUrl
                )
            }
        }
    }

    /**
     * Called when the user taps "Download Update" in the dialog.
     * Enqueues the APK via DownloadManager and starts polling.
     */
    fun downloadUpdate() {
        val s = _updateState.value
        if (s.downloadUrl.isBlank()) return

        val ctx = getApplication<Application>()
        val downloadId = UpdateChecker.downloadApk(ctx, s.downloadUrl, s.latestVersion)

        _updateState.value = s.copy(
            phase      = DownloadPhase.DOWNLOADING,
            downloadId = downloadId
        )

        startPolling(downloadId)
    }

    /**
     * Polls DownloadManager every 500 ms until the download finishes or fails.
     */
    private fun startPolling(downloadId: Long) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val progress = UpdateChecker.queryProgress(getApplication(), downloadId)

                withContext(Dispatchers.Main) {
                    when (progress.status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            _updateState.value = _updateState.value.copy(
                                phase        = DownloadPhase.READY,
                                progressBytes = progress.bytesDownloaded,
                                totalBytes    = progress.bytesTotal,
                                localUri      = progress.localUri
                            )
                        }
                        DownloadManager.STATUS_FAILED -> {
                            _updateState.value = _updateState.value.copy(
                                phase = DownloadPhase.FAILED
                            )
                        }
                        else -> {
                            // STATUS_RUNNING or STATUS_PENDING — update progress
                            _updateState.value = _updateState.value.copy(
                                progressBytes = progress.bytesDownloaded,
                                totalBytes    = progress.bytesTotal
                            )
                        }
                    }
                }

                // Stop polling once terminal state is reached
                if (progress.status == DownloadManager.STATUS_SUCCESSFUL ||
                    progress.status == DownloadManager.STATUS_FAILED) {
                    break
                }

                delay(500)
            }
        }
    }

    /**
     * Called from MainActivity after READY state — fires the system package installer.
     * Uses FileProvider so it works on Android 7+ (API 24+).
     */
    fun installApk(context: Context) {
        val localUri = _updateState.value.localUri ?: return
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "warrior-${_updateState.value.latestVersion}.apk"
        )
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    fun dismissUpdate() {
        pollJob?.cancel()
        _updateState.value = _updateState.value.copy(dismissed = true)
    }

    fun retryDownload() {
        _updateState.value = _updateState.value.copy(phase = DownloadPhase.IDLE)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun extractDomain(url: String): String {
        val cleaned    = url.trim()
        val withScheme = if (!cleaned.startsWith("http")) "https://$cleaned" else cleaned
        val host = android.net.Uri.parse(withScheme).host ?: throw IllegalArgumentException("bad url")
        return host.removePrefix("www.")
    }

    private fun vibrate(pattern: LongArray) {
        try {
            val ctx = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ctx.getSystemService(VibratorManager::class.java)
                    ?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                else {
                    @Suppress("DEPRECATION") v?.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {}
    }
}
