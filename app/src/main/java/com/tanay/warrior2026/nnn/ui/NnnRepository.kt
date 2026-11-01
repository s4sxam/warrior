package com.tanay.warrior.nnn.data

import android.content.Context
import com.tanay.warrior.nnn.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Persists [NnnState]. Every operation is `store.update { NnnActions.x(state) }` — no business logic here,
 * all decisions live in the unit-tested [NnnActions]. Nothing in this class touches ordinary habit data.
 */
class NnnRepository(context: Context, private val actions: NnnActions = NnnActions()) {
    private val store = NnnDataStore(context)
    val state: Flow<NnnState> = store.state

    private suspend fun <R> mutate(block: (NnnState) -> Pair<NnnState, R>): R {
        var out: Any? = null
        store.update { current -> block(current).also { out = it.second }.first }
        @Suppress("UNCHECKED_CAST") return out as R
    }

    private fun newId() = UUID.randomUUID().toString()

    suspend fun startChallenge(rules: NnnRuleSet, settings: NnnSettings, consentVersion: Int) = mutate { s ->
        val now = Instant.now()
        actions.startDefaultChallenge(actions.updateSettings(s, settings), ZoneId.systemDefault(), rules, consentVersion, now) to Unit
    }

    suspend fun reportSetback(): NnnChallengeEngine.ProcessResult = mutate { s ->
        val now = Instant.now()
        actions.apply(s, ManualReport(newId(), now), now).let { it.state to it.result }
    }

    /** Called by Safe Browser with an in-memory host. The host is never stored unless the user opted into domain detail. */
    suspend fun safeBrowserNavigation(host: String): NnnChallengeEngine.ProcessResult = mutate { s ->
        val now = Instant.now()
        actions.apply(s, SafeBrowserNavigation(newId(), now, host, isMainFrame = true), now).let { it.state to it.result }
    }

    suspend fun checkIn(date: LocalDate?): NnnChallengeEngine.SelfCheckResult = mutate { s ->
        val now = Instant.now()
        val day = date ?: s.challenge?.localDate(now) ?: LocalDate.now()
        actions.checkIn(s, day, now).let { it.state to it.result }
    }

    suspend fun review(eventId: String, decision: ReviewDecision, reason: ReviewReason): NnnChallengeEngine.ReviewResult = mutate { s ->
        actions.review(s, eventId, decision, reason, Instant.now()).let { it.state to it.result }
    }

    suspend fun setMonitorState(source: MonitorSource, state: MonitorState) = mutate { s ->
        actions.setMonitorState(s, source, state, Instant.now()) to Unit
    }

    suspend fun updateRules(rules: NnnRuleSet) = mutate { s -> actions.updateRules(s, rules) to Unit }
    suspend fun updateSettings(settings: NnnSettings) = mutate { s -> actions.updateSettings(s, settings) to Unit }
    suspend fun stopAllMonitoring() = mutate { s -> actions.stopAllMonitoring(s, Instant.now()) to Unit }
    suspend fun endChallengeEarly() = mutate { s -> actions.setChallengeStatus(s, ChallengeStatus.ABANDONED) to Unit }
    suspend fun deleteActivity() = mutate { s -> actions.deleteActivity(s) to Unit }
    suspend fun resetEverything() = mutate { s -> actions.resetEverything(s) to Unit }
    suspend fun addAllowlistEntry(entry: String): AllowlistResult = mutate { s -> actions.addAllowlistEntry(s, entry).let { it.state to it.result } }
    suspend fun removeAllowlistEntry(entry: String) = mutate { s -> actions.removeAllowlistEntry(s, entry) to Unit }

    /** Housekeeping on app start: completes an ended challenge and redacts expired domain detail. */
    suspend fun maintain() = mutate { s ->
        val now = Instant.now()
        actions.redactExpiredDetail(actions.tick(s, now), now) to Unit
    }

    suspend fun exportJson(): String = store.encodeForExport(store.state.first())
}
