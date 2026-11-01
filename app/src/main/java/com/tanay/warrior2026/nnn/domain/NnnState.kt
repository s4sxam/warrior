package com.tanay.warrior.nnn.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class NnnSettings(
    /** Keep the matched rule domain on events. OFF by default: events then carry no domain at all. */
    val storeDomainDetail: Boolean = false,
    /** After this many days, domain detail on stored events is redacted (outcomes/dates are kept). */
    val retentionDays: Int = DEFAULT_RETENTION_DAYS,
    val notificationsEnabled: Boolean = false,
    /** Minutes after local midnight; null = no quiet hours. */
    val quietHoursStartMinute: Int? = null,
    val quietHoursEndMinute: Int? = null,
    /** Shown on the Emergency Focus screen. Stays on this device. */
    val focusMessage: String = ""
) {
    companion object {
        const val MAX_FOCUS_MESSAGE = 140
        const val DEFAULT_RETENTION_DAYS = 90
        const val MIN_RETENTION_DAYS = 7
        const val MAX_RETENTION_DAYS = 365
    }
}

/**
 * Everything NNN persists. Deliberately has NO reference to ordinary habits, leaderboard, achievements or
 * theme state (pinned by a test) — an NNN failure cannot reach them.
 */
data class NnnState(
    val challenge: NnnChallenge? = null,
    val events: List<NnnEvent> = emptyList(),
    val selfChecks: List<SelfCheck> = emptyList(),
    val segments: List<MonitoringSegment> = emptyList(),
    val settings: NnnSettings = NnnSettings(),
    val allowlist: Set<String> = emptySet()
) {
    /** Current state of a source = its most recent segment; a source never seen is DISABLED. */
    fun monitorState(source: MonitorSource): MonitorState =
        segments.lastOrNull { it.source == source }?.takeIf { it.end == null }?.state ?: MonitorState.DISABLED
}

/**
 * All NNN state transitions as pure functions (state in → state out). The Android repository only
 * persists the result; services/ViewModels never edit state any other way.
 */
class NnnActions(private val domainRules: List<DomainRule> = BundledDomainRules.rules) {

    data class Applied(val state: NnnState, val result: NnnChallengeEngine.ProcessResult)
    data class CheckedIn(val state: NnnState, val result: NnnChallengeEngine.SelfCheckResult)
    data class Reviewed(val state: NnnState, val result: NnnChallengeEngine.ReviewResult)
    data class AllowlistChange(val state: NnnState, val result: AllowlistResult)

    fun engine(state: NnnState) = NnnChallengeEngine(DomainRuleEngine(domainRules, state.allowlist))
    fun navigationPolicy(state: NnnState) = SafeBrowserNavigationPolicy(DomainRuleEngine(domainRules, state.allowlist))

    fun snapshot(state: NnnState, now: Instant): NnnSnapshot? =
        state.challenge?.let { NnnScoring.snapshot(it, state.events, state.selfChecks, state.segments, now) }

    // ── lifecycle ────────────────────────────────────────────────────────────
    fun startDefaultChallenge(state: NnnState, zone: ZoneId, rules: NnnRuleSet, consentVersion: Int, now: Instant): NnnState {
        val existing = state.challenge
        if (existing != null && existing.status != ChallengeStatus.NOT_STARTED) return state
        val base = existing ?: NnnChallenge.default2026(zone, createdAt = now)
        return startChallenge(state, base, rules, consentVersion, now)
    }

    fun startChallenge(state: NnnState, challenge: NnnChallenge, rules: NnnRuleSet, consentVersion: Int, now: Instant): NnnState {
        val current = state.challenge
        if (current != null && current.status != ChallengeStatus.NOT_STARTED) return state
        return state.copy(challenge = challenge.started(now, sanitize(rules), consentVersion))
    }

    fun setChallengeStatus(state: NnnState, status: ChallengeStatus): NnnState =
        state.challenge?.let { state.copy(challenge = it.copy(status = status)) } ?: state

    /** Strict mode is meaningless without the device guard and invalid without an acknowledged warning. */
    private fun sanitize(r: NnnRuleSet): NnnRuleSet =
        if (!r.deviceGuardEnabled || !r.strictDomainGuard || r.strictAcknowledgedConsentVersion == null)
            r.copy(strictDomainGuard = false, strictAcknowledgedConsentVersion = null)
        else r

    fun tick(state: NnnState, now: Instant): NnnState {
        val c = state.challenge ?: return state
        val done = engine(state).completeIfEnded(c, now)
        return if (done === c) state else state.copy(challenge = done)
    }

    // ── signals / check-ins / review ─────────────────────────────────────────
    fun apply(state: NnnState, signal: NnnSignal, now: Instant): Applied {
        val c = state.challenge ?: return Applied(state, NnnChallengeEngine.ProcessResult.Ignored(IgnoreReason.CHALLENGE_NOT_ACTIVE))
        val r = engine(state).process(c, signal, state.events, state.settings.storeDomainDetail)
        return when (r) {
            is NnnChallengeEngine.ProcessResult.Recorded -> Applied(state.copy(events = state.events + r.event), r)
            is NnnChallengeEngine.ProcessResult.Ignored -> Applied(state, r)
        }
    }

    fun checkIn(state: NnnState, date: LocalDate, now: Instant): CheckedIn {
        val c = state.challenge ?: return CheckedIn(state, NnnChallengeEngine.SelfCheckResult.NotAllowed(SelfCheckDenied.CHALLENGE_NOT_ACTIVE))
        val r = engine(state).recordSelfCheck(c, state.selfChecks, date, now)
        return if (r is NnnChallengeEngine.SelfCheckResult.Recorded) CheckedIn(state.copy(selfChecks = state.selfChecks + r.check), r)
        else CheckedIn(state, r)
    }

    fun review(state: NnnState, eventId: String, decision: ReviewDecision, reason: ReviewReason, now: Instant): Reviewed {
        val r = engine(state).review(state.events, eventId, decision, reason, now)
        return if (r is NnnChallengeEngine.ReviewResult.Updated) Reviewed(state.copy(events = r.events), r) else Reviewed(state, r)
    }

    // ── monitoring ───────────────────────────────────────────────────────────
    /** Records a source's state change. Gaps are history: the previous segment is closed, never rewritten. */
    fun setMonitorState(state: NnnState, source: MonitorSource, new: MonitorState, now: Instant): NnnState {
        val last = state.segments.lastOrNull { it.source == source }
        val currentlyOpen = last?.takeIf { it.end == null }
        if (currentlyOpen != null && currentlyOpen.state == new) return state
        if (currentlyOpen == null && new == MonitorState.DISABLED) return state
        val closed = state.segments.map { if (it === currentlyOpen) it.copy(end = now) else it }
        return state.copy(segments = closed + MonitoringSegment(source, new, now, null))
    }

    /** "Revoke / stop all monitoring": closes every open segment, marks sources DISABLED, turns the device guard off. */
    fun stopAllMonitoring(state: NnnState, now: Instant): NnnState {
        var s = state
        for (source in MonitorSource.values()) s = setMonitorState(s, source, MonitorState.DISABLED, now)
        val c = s.challenge ?: return s
        return s.copy(challenge = c.copy(rules = c.rules.copy(deviceGuardEnabled = false, strictDomainGuard = false, strictAcknowledgedConsentVersion = null)))
    }

    // ── allowlist ────────────────────────────────────────────────────────────
    fun addAllowlistEntry(state: NnnState, entry: String): AllowlistChange {
        val r = DomainRuleEngine.validateAllowlistEntry(entry, domainRules)
        return if (r is AllowlistResult.Accepted) AllowlistChange(state.copy(allowlist = state.allowlist + r.domain), r)
        else AllowlistChange(state, r)
    }

    fun removeAllowlistEntry(state: NnnState, entry: String): NnnState {
        val n = HostNormalizer.normalize(entry) ?: return state
        return state.copy(allowlist = state.allowlist - n)
    }

    // ── settings / retention / deletion ──────────────────────────────────────
    fun updateSettings(state: NnnState, settings: NnnSettings): NnnState = state.copy(
        settings = settings.copy(
            retentionDays = settings.retentionDays.coerceIn(NnnSettings.MIN_RETENTION_DAYS, NnnSettings.MAX_RETENTION_DAYS),
            focusMessage = settings.focusMessage.trim().take(NnnSettings.MAX_FOCUS_MESSAGE)
        )
    )

    /** Changes the user's rules mid-challenge (same sanitising as starting). No-op without a challenge. */
    fun updateRules(state: NnnState, rules: NnnRuleSet): NnnState =
        state.challenge?.let { state.copy(challenge = it.copy(rules = sanitize(rules))) } ?: state

    /**
     * Retention strips DOMAIN DETAIL from old events. Outcomes, dates and reviews stay so history can
     * never silently turn "failed" into "clean". Removing records entirely is [deleteActivity].
     */
    fun redactExpiredDetail(state: NnnState, now: Instant): NnnState {
        val cutoff = now.minusSeconds(state.settings.retentionDays * 86_400L)
        var changed = false
        val events = state.events.map {
            val redacted = it.copy(matchedDomain = null, category = null, confidence = null, dedupKey = "${it.source.name}|-")
            if (it.at.isBefore(cutoff) && redacted != it) { changed = true; redacted } else it
        }
        return if (changed) state.copy(events = events) else state
    }

    /** Deletes events, check-ins and monitoring history. Keeps the challenge setup, settings and allowlist. */
    fun deleteActivity(state: NnnState): NnnState = state.copy(events = emptyList(), selfChecks = emptyList(), segments = emptyList())

    fun resetEverything(@Suppress("UNUSED_PARAMETER") state: NnnState): NnnState = NnnState()
}
