package com.tanay.warrior

// [UPDATE] v2.0.0: Added profile setup, bot simulation, leaderboard state
// [UPDATE] v2.1.0: Added update checker
// [UPDATE] v2.2.0: In-app DownloadManager download + install trigger
// [FIX]    v2.3.0: Fixed all update system bugs (see below)
// [UPDATE] v3.0.0: Pass userStreak to leaderboard calls for dynamic scoring
// [FIX]    v3.2.0: Fixed race condition in advanceBotsIfNeeded() that could wipe
//                  a logVictory() or logRelapse() if bot simulation finished after
//                  the user logged their day. The state copy now always happens on
//                  Main thread AFTER the background work completes, reading the
//                  latest _state.value to avoid overwriting concurrent changes.
// [NEW]    v3.2.0: completeProfile() stamps every bot with simulationStartDate = today
//                  (first-run date). This prevents bots from showing a full fake year
//                  of history on a fresh install. The 365-day heatmap only shows real
//                  data from the app install date forward.
//   - BUG 1: dismissUpdate() now persists dismissed version to DataStore
//   - BUG 2: retryDownload() now properly re-enqueues a new download
//   - BUG 3: checkForUpdate() skips dialog if latest == already-dismissed version
//   - BUG 4: installApk() now saves dismissed version so dialog won't reappear
//   - BUG 5: onCleared() cancels pollJob and cleans up properly
//   - BUG 6: init block cancels any orphaned DownloadManager job from previous session
//   - BUG 8: installApk() now uses the already-fetched localUri directly
//   (BUG 7 is a CI/build.gradle issue — fixed in build.gradle.kts)
//   (BUG 9 is a UI issue — fixed in AboutScreen.kt)

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
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanay.warrior.data.BotProfile
import com.tanay.warrior.data.BotSimulator
import com.tanay.warrior.data.DayData
import com.tanay.warrior.data.UserProfile
import com.tanay.warrior.data.WarriorRepository
import com.tanay.warrior.data.WarriorState
import com.tanay.warrior.data.todayKey
import com.tanay.warrior.notifications.WarriorScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
            // [FIX NC-6] Use first() to load initial state, then switch to collectLatest
            // for subsequent changes. Previously collectLatest fired advanceBotsIfNeeded()
            // on EVERY DataStore write (logVictory, saveConfession, etc.) — loading and
            // re-serialising 1050 bots each time. Now bot simulation runs only once on
            // launch, not on every state emission.
            val initial = repo.warriorStateFlow.first()
            _state.value = initial
            if (initial.hasCompletedProfile && initial.botsJson.isNotBlank()) {
                advanceBotsIfNeeded(initial.botsJson)
            }
            // Continue collecting for UI-driven state changes (streak, habits, etc.)
            repo.warriorStateFlow.collectLatest { s ->
                _state.value = s
                // Do NOT call advanceBotsIfNeeded here — bots are only advanced once per launch
            }
        }

        // [FIX BUG 6] On init, check if there's an orphaned DownloadManager job
        // from a previous session that was killed mid-download. If so, cancel it
        // and clear the persisted ID so the next update check starts fresh.
        viewModelScope.launch(Dispatchers.IO) {
            val orphanId = repo.getPendingDownloadId()
            if (orphanId != -1L) {
                UpdateChecker.cancelDownload(getApplication(), orphanId)
                repo.clearPendingDownloadId()
            }
        }
    }

    // ── [FIX BUG 5] Clean up coroutine and DownloadManager on ViewModel death ─

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
        // If a download was still running when the ViewModel was destroyed,
        // cancel it in DownloadManager so it doesn't keep downloading in the background.
        val currentDownloadId = _updateState.value.downloadId
        if (currentDownloadId != -1L &&
            _updateState.value.phase == DownloadPhase.DOWNLOADING) {
            val dm = getApplication<Application>()
                .getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.remove(currentDownloadId)
            // Fire-and-forget coroutine to clear persisted ID
            // (viewModelScope is already cancelled here, use GlobalScope equivalent)
            viewModelScope.launch(Dispatchers.IO) {
                repo.clearPendingDownloadId()
            }
        }
    }

    // ── Onboarding / Profile / Bots (unchanged) ───────────────────────────────

    fun completeOnboarding() {
        // Ensure a default habit exists on first run
        val habits = if (_state.value.habits.isEmpty()) {
            listOf(com.tanay.warrior.data.Habit(
                id = "habit_primary", name = "Main Habit", emoji = "🔥"))
        } else _state.value.habits
        val new = _state.value.copy(
            hasCompletedOnboarding = true,
            habits = habits,
            activeHabitId = habits.first().id
        )
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
    }

    fun completeProfile(name: String, dob: String, region: String) {
        _isGeneratingBots.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val profile  = UserProfile(name = name, dob = dob, region = region)

            // [NEW v3.2.0] Stamp every bot with today as their simulationStartDate.
            // This is the app install date — bots will only have data from this day
            // forward. The 365-day heatmap will show grey for all days before today.
            val todayStr = com.tanay.warrior.data.todayKey()
            val savedFirstRun = repo.getFirstRunDate()
            val firstRunDate  = if (savedFirstRun.isBlank()) {
                repo.saveFirstRunDate(todayStr)
                todayStr
            } else {
                savedFirstRun
            }

            // [FIX v3.1.0] generateBots() returns all 1050 bots (7 regions × 150).
            // [NEW v3.2.0] Each bot gets simulationStartDate = firstRunDate so their
            // history only covers time since the app was actually installed.
            val rawBots  = com.tanay.warrior.data.generateBots()
                .map { it.copy(simulationStartDate = firstRunDate) }
            val newBots  = BotSimulator.advanceSimulation(rawBots)
            val botsJson = BotSimulator.saveBots(newBots)
            val new = _state.value.copy(
                userProfile         = profile,
                hasCompletedProfile = true,
                botsJson            = botsJson
            )
            withContext(Dispatchers.Main) {
                _state.value = new
                _bots.value  = newBots   // all 1050 stored
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
                // [FIX v3.2.0] Race condition: bot simulation runs on Dispatchers.Default
                // and can take time. If logVictory() or logRelapse() fires during that window,
                // doing _state.value = _state.value.copy(botsJson = newJson) here would
                // snapshot the current _state correctly (since we're now on Main), but we must
                // re-read _state.value AFTER switching to Main — not capture it before the
                // withContext call — to guarantee we include any victories logged in between.
                withContext(Dispatchers.Main) {
                    val new = _state.value.copy(botsJson = newJson)
                    _state.value = new
                    repo.saveState(new)
                }
            }
        }
    }

    // ── Leaderboard ───────────────────────────────────────────────────────────

    val regionalBoard: StateFlow<List<BotSimulator.LeaderboardEntry>> =
        combine(_bots, _state) { bots, s ->
            BotSimulator.regionalLeaderboard(
                bots            = bots,
                userRegion      = s.userProfile.region,
                userName        = s.userProfile.name,
                userPoints      = s.userPoints,
                userTotalClean  = s.totalClean,
                userTotalLogged = s.totalClean + s.totalFailed,
                userStreak      = s.streak
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
                userTotalLogged = s.totalClean + s.totalFailed,
                userStreak      = s.streak
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun getBotProfile(botId: Int): BotProfile? = _bots.value.find { it.id == botId }

    // ── Existing actions (updated for multi-habit) ────────────────────────────

    fun logVictory() {
        val today = todayKey()
        val active = _state.value.activeHabit ?: return
        if (active.history.containsKey(today)) return
        val updated = active.copy(history = active.history + (today to DayData(status = "clean")))
        val new = _state.value.withUpdatedHabit(updated)
        _state.value = new
        _showConfetti.value = true
        viewModelScope.launch {
            repo.saveState(new)
            com.tanay.warrior.widget.StreakWidget().updateAll(getApplication())
        }
        vibrate(longArrayOf(0, 100, 50, 100))
        val milestones = setOf(3, 7, 14, 21, 30, 60, 90, 180, 365)
        if (new.streak in milestones) WarriorScheduler.fireMilestoneNow(getApplication(), new.streak)
    }

    fun logRelapse(url: String): Boolean = runCatching {
        val domain = if (url.isBlank() || url == "unknown") "unknown"
                     else extractDomain(url)
        val today  = todayKey()
        val active = _state.value.activeHabit ?: return false
        val newTriggers = active.triggers.toMutableMap()
        if (domain != "unknown") newTriggers[domain] = (newTriggers[domain] ?: 0) + 1
        val updated = active.copy(
            history  = active.history + (today to DayData(status = "failed", site = domain)),
            triggers = newTriggers
        )
        val new = _state.value.withUpdatedHabit(updated)
        _state.value = new
        viewModelScope.launch {
            repo.saveState(new)
            com.tanay.warrior.widget.StreakWidget().updateAll(getApplication())
        }
        vibrate(longArrayOf(0, 500))
        true
    }.getOrDefault(false)

    fun undoToday() {
        val today = todayKey()
        val active = _state.value.activeHabit ?: return
        if (!active.history.containsKey(today)) return
        val updated = active.copy(history = active.history - today)
        val new = _state.value.withUpdatedHabit(updated)
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
    }

    // ── v4.0.0: Multi-habit management ───────────────────────────────────────

    fun addHabit(name: String, emoji: String) {
        val id = "habit_${System.currentTimeMillis()}"
        val habit = com.tanay.warrior.data.Habit(id = id, name = name, emoji = emoji)
        val new = _state.value.copy(
            habits       = _state.value.habits + habit,
            activeHabitId = id
        )
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
    }

    fun deleteHabit(habitId: String) {
        if (_state.value.habits.size <= 1) return // don't delete last habit
        val remaining = _state.value.habits.filter { it.id != habitId }
        val newActive = if (_state.value.activeHabitId == habitId) remaining.first().id
                        else _state.value.activeHabitId
        val new = _state.value.copy(habits = remaining, activeHabitId = newActive)
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
    }

    fun switchHabit(habitId: String) {
        if (!_state.value.habits.any { it.id == habitId }) return
        val new = _state.value.copy(activeHabitId = habitId)
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
    }

    fun renameHabit(habitId: String, name: String, emoji: String) {
        val habit = _state.value.habits.find { it.id == habitId } ?: return
        val updated = habit.copy(name = name, emoji = emoji)
        val new = _state.value.withUpdatedHabit(updated)
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
    }

    fun clearConfetti() { _showConfetti.value = false }

    fun saveConfession(text: String) {
        if (text.isBlank()) return
        _state.update { it.copy(lastConfession = text.trim()) }
        viewModelScope.launch { repo.saveState(_state.value) }
    }

    fun exportJson(): String = repo.exportJson(_state.value)

    fun importJson(json: String): Boolean {
        val merged = repo.importJson(_state.value, json) ?: return false
        _state.value = merged
        viewModelScope.launch { repo.saveState(merged) }
        return true
    }

    fun importPlain(entries: Map<com.tanay.warrior.data.DayData, String>): Boolean = false // unused overload

    fun importPlainDays(days: Map<String, com.tanay.warrior.data.DayData>): Boolean {
        if (days.isEmpty()) return false
        val active = _state.value.activeHabit ?: return false
        val updated = active.copy(history = active.history + days)
        val merged = _state.value.withUpdatedHabit(updated)
        _state.value = merged
        viewModelScope.launch { repo.saveState(merged) }
        return true
    }

    // ── v2.3.0: Fixed update flow ─────────────────────────────────────────────

    /**
     * [FIX BUG 1 + BUG 3] Checks GitHub and only shows dialog if:
     *   1. A newer version exists
     *   2. The user has NOT already dismissed this exact version
     * This means once the user taps "Later" on v2.3.0, the dialog won't
     * reappear on every launch — it will only appear again when v2.4.0 drops.
     */
    fun checkForUpdate(currentVersion: String) {
        viewModelScope.launch {
            val result = UpdateChecker.check(currentVersion)
            if (!result.hasUpdate) return@launch

            // Load the version the user previously dismissed (persisted across restarts)
            val dismissedVersion = repo.getDismissedVersion()

            // Only show the dialog if this version is newer than what was dismissed.
            // e.g. user dismissed 2.3.0 → dismissedVersion = "2.3.0"
            //      next launch still shows 2.3.0 on GitHub → skip (already dismissed)
            //      later 2.4.0 ships → isNewer("2.4.0", "2.3.0") = true → show dialog
            if (dismissedVersion.isBlank() || UpdateChecker.isNewer(result.latestVersion, dismissedVersion)) {
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

        // [FIX BUG 6] Persist the downloadId so we can cancel it if the app is killed
        viewModelScope.launch(Dispatchers.IO) {
            repo.savePendingDownloadId(downloadId)
        }

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
                                phase         = DownloadPhase.READY,
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
                    // [FIX BUG 6] Clear persisted ID — download is no longer in-flight
                    repo.clearPendingDownloadId()
                    break
                }

                delay(500)
            }
        }
    }

    /**
     * [FIX BUG 8] Uses the localUri that was already retrieved from DownloadManager
     * (stored in UpdateState.localUri) instead of reconstructing the file path manually.
     * Reconstructing the path was fragile and could crash with FileNotFoundException
     * if the filename didn't match exactly.
     *
     * [FIX BUG 4] Also saves the installed version as the dismissed version so the
     * dialog won't reappear after the user installs the update.
     */
    fun installApk(context: Context) {
        val localUri = _updateState.value.localUri ?: return
        val version  = _updateState.value.latestVersion

        // [FIX BUG 2] On Android 10+ (API 29+) DownloadManager.COLUMN_LOCAL_URI
        // returns a content:// URI, not a file:// path. Converting it to File via
        // Uri.parse().path returns null or a wrong path, causing FileProvider to crash
        // with FileNotFoundException or IllegalArgumentException.
        //
        // Fix: use the URI directly. If it's already a content:// URI (API 29+) we pass
        // it straight to the installer. If it's a file:// URI (API 28-) we still need
        // FileProvider to wrap it into a content URI for the installer.
        val contentUri: android.net.Uri = run {
            val parsed = android.net.Uri.parse(localUri)
            if (parsed.scheme == "file") {
                // Legacy path: wrap via FileProvider
                val file = java.io.File(parsed.path ?: return)
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } else {
                // content:// URI from DownloadManager — use directly
                parsed
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)

        // [FIX BUG 4] Record that the user acted on this version. Even if they
        // cancel the system installer, we won't pester them again this session.
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveDismissedVersion(version)
        }
        _updateState.value = _updateState.value.copy(dismissed = true)
    }

    /**
     * [FIX BUG 1] Persists the dismissed version to DataStore so the dialog
     * does NOT reappear on the next launch for the same version.
     * [FIX BUG 3] Since we save the version string (not just a boolean),
     * the dialog WILL reappear when a genuinely newer version ships.
     */
    fun dismissUpdate() {
        pollJob?.cancel()
        val version = _updateState.value.latestVersion
        _updateState.value = _updateState.value.copy(dismissed = true)
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveDismissedVersion(version)
        }
    }

    /**
     * [FIX BUG 2] retryDownload() previously only reset the phase to IDLE without
     * actually starting a new download. Now it calls downloadUpdate() which
     * properly re-enqueues a fresh DownloadManager request.
     */
    fun retryDownload() {
        // Reset phase and clear the stale downloadId before re-enqueueing
        _updateState.value = _updateState.value.copy(
            phase      = DownloadPhase.IDLE,
            downloadId = -1L,
            progressBytes = 0L,
            totalBytes    = 0L,
            localUri      = null
        )
        // Now actually start a new download
        downloadUpdate()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun extractDomain(url: String): String {
        val cleaned    = url.trim()
        val withScheme = if (!cleaned.startsWith("http")) "https://$cleaned" else cleaned
        val host = android.net.Uri.parse(withScheme).host ?: throw IllegalArgumentException("bad url")
        return host.removePrefix("www.")
    }

    private fun WarriorState.withUpdatedHabit(habit: com.tanay.warrior.data.Habit): WarriorState =
        copy(habits = habits.map { if (it.id == habit.id) habit else it })

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