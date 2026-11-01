package com.tanay.warrior.nnn.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanay.warrior.nnn.data.NnnRepository
import com.tanay.warrior.nnn.domain.*
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** Everything the NNN screens render. Derived from persisted state — never fake or demo values. */
data class NnnUiState(
    val loaded: Boolean = false,
    val state: NnnState = NnnState(),
    val snapshot: NnnSnapshot? = null,
    val insights: NnnInsights? = null,
    val now: Instant = Instant.now()
) {
    val challenge: NnnChallenge? get() = state.challenge
    val isRunning: Boolean get() = challenge?.status == ChallengeStatus.ACTIVE
    val needsSetup: Boolean get() = challenge?.status.let { it == null || it == ChallengeStatus.NOT_STARTED }
}

/** One-shot messages for the UI (toast/snackbar). */
sealed interface NnnMessage {
    data class Text(val title: String, val body: String = "") : NnnMessage
}

class NnnViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NnnRepository(app)
    private val actions = NnnActions()

    private val minuteTicker = flow { while (true) { emit(Instant.now()); delay(60_000) } }

    val ui: StateFlow<NnnUiState> = combine(repo.state, minuteTicker) { s, now ->
        val snap = actions.snapshot(s, now)
        NnnUiState(true, s, snap, snap?.let { NnnInsights.from(it, s.events) }, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NnnUiState())

    private val _messages = MutableSharedFlow<NnnMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<NnnMessage> = _messages

    init { viewModelScope.launch { repo.maintain() } }

    private fun say(title: String, body: String = "") { _messages.tryEmit(NnnMessage.Text(title, body)) }

    fun start(rules: NnnRuleSet, settings: NnnSettings, safeBrowserOn: Boolean, onDone: () -> Unit) = viewModelScope.launch {
        repo.startChallenge(rules, settings, NNN_CONSENT_VERSION)
        repo.setMonitorState(MonitorSource.SAFE_BROWSER, if (safeBrowserOn) MonitorState.ACTIVE else MonitorState.DISABLED)
        onDone()
    }

    fun checkInToday() = viewModelScope.launch {
        when (repo.checkIn(null)) {
            is NnnChallengeEngine.SelfCheckResult.Recorded -> say("Checked in", "Logged as self-reported clean for today.")
            is NnnChallengeEngine.SelfCheckResult.AlreadyRecorded -> say("Already checked in today")
            is NnnChallengeEngine.SelfCheckResult.NotAllowed -> say("Can't check in", "The challenge isn't running for this day.")
        }
    }

    fun reportSetback() = viewModelScope.launch {
        when (repo.reportSetback()) {
            is NnnChallengeEngine.ProcessResult.Recorded -> say("Setback recorded", "Your NNN streak restarts. The challenge continues.")
            is NnnChallengeEngine.ProcessResult.Ignored -> say("Already recorded", "A report from the last few seconds is already saved.")
        }
    }

    /** Safe Browser blocked a main-frame navigation. [onResult] tells the block panel what was recorded. */
    fun onSafeBrowserBlocked(host: String, onResult: (recorded: Boolean) -> Unit) = viewModelScope.launch {
        onResult(repo.safeBrowserNavigation(host) is NnnChallengeEngine.ProcessResult.Recorded)
    }

    fun review(eventId: String, decision: ReviewDecision, reason: ReviewReason) = viewModelScope.launch {
        when (repo.review(eventId, decision, reason)) {
            is NnnChallengeEngine.ReviewResult.Updated -> say("Review saved", "The original record is kept in the audit trail.")
            else -> say("Couldn't save review")
        }
    }

    fun setSafeBrowserMonitoring(on: Boolean) = viewModelScope.launch {
        repo.setMonitorState(MonitorSource.SAFE_BROWSER, if (on) MonitorState.ACTIVE else MonitorState.DISABLED)
    }
    fun updateRules(rules: NnnRuleSet) = viewModelScope.launch { repo.updateRules(rules) }
    fun updateSettings(settings: NnnSettings) = viewModelScope.launch { repo.updateSettings(settings) }
    fun stopAllMonitoring() = viewModelScope.launch { repo.stopAllMonitoring(); say("Monitoring stopped", "Stopped periods are recorded as gaps, never as clean days.") }
    fun endChallengeEarly() = viewModelScope.launch { repo.endChallengeEarly() }
    fun deleteActivity() = viewModelScope.launch { repo.deleteActivity(); say("Activity records deleted") }
    fun resetEverything() = viewModelScope.launch { repo.resetEverything(); say("NNN reset") }
    fun addAllowlist(entry: String) = viewModelScope.launch {
        when (val r = repo.addAllowlistEntry(entry)) {
            is AllowlistResult.Accepted -> say("Added", r.domain)
            is AllowlistResult.Rejected -> say("Not added", when (r.reason) {
                AllowlistRejection.INVALID_HOST -> "That isn't a valid domain."
                AllowlistRejection.TOO_BROAD -> "Too broad — enter a full domain like site.example."
                AllowlistRejection.SHADOWS_TOO_MANY_RULES -> "That would switch off too many rules."
            })
        }
    }
    fun removeAllowlist(entry: String) = viewModelScope.launch { repo.removeAllowlistEntry(entry) }

    /** User-initiated export: writes the NNN records as JSON to the file the user picked. */
    fun exportTo(uri: Uri) = viewModelScope.launch {
        val json = repo.exportJson()
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) } != null
            }.getOrDefault(false)
        }
        say(if (ok) "Exported" else "Export failed", if (ok) "Saved where you chose." else "Couldn't write the file.")
    }

    /** Rebuilds the Safe Browser policy from current state (allowlist changes apply immediately). */
    fun navigationPolicy(): SafeBrowserNavigationPolicy = actions.navigationPolicy(ui.value.state)
}

/** Bump when the rules/limitations text the user consents to changes materially. */
const val NNN_CONSENT_VERSION = 1
