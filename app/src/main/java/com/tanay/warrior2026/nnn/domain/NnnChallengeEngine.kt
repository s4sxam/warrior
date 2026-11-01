package com.tanay.warrior.nnn.domain

import java.time.Instant
import java.time.LocalDate

/**
 * Central, pure event-processing policy. Android services, WebView callbacks and Compose screens
 * must call this — never re-implement classification or failure rules.
 *
 * Policy (see product spec 3.4):
 *  - Safe Browser main-frame navigation to a matched adult domain  -> CONFIRMED_FAIL (if the user enabled that rule)
 *  - Device Guard (VPN) domain signal                              -> POSSIBLE_EXPOSURE, or CONFIRMED_FAIL only in acknowledged strict mode
 *  - Manual report                                                 -> CONFIRMED_FAIL
 *  - everything else                                               -> Ignored(reason), never stored
 * A domain match never proves a physical act; it only applies the rule the user chose.
 */
class NnnChallengeEngine(
    private val domains: DomainRuleEngine,
    private val dedup: EventDeduplicator = EventDeduplicator()
) {
    sealed interface ProcessResult {
        data class Recorded(val event: NnnEvent) : ProcessResult
        data class Ignored(val reason: IgnoreReason) : ProcessResult
    }

    sealed interface ReviewResult {
        data class Updated(val events: List<NnnEvent>, val event: NnnEvent) : ReviewResult
        data object NotFound : ReviewResult
        data object InvalidReason : ReviewResult
    }

    sealed interface SelfCheckResult {
        data class Recorded(val check: SelfCheck) : SelfCheckResult
        data class AlreadyRecorded(val check: SelfCheck) : SelfCheckResult
        data class NotAllowed(val reason: SelfCheckDenied) : SelfCheckResult
    }

    private data class Candidate(val outcome: EventOutcome, val ruleDomain: String?, val category: String?, val confidence: Int?)

    fun process(
        challenge: NnnChallenge,
        signal: NnnSignal,
        existing: List<NnnEvent>,
        storeDomainDetail: Boolean = true
    ): ProcessResult {
        if (existing.any { it.id == signal.eventId }) return ignored(IgnoreReason.DUPLICATE)
        if (challenge.status != ChallengeStatus.ACTIVE) return ignored(IgnoreReason.CHALLENGE_NOT_ACTIVE)

        val date = challenge.localDate(signal.at)
        if (date.isBefore(challenge.startDate) || date.isAfter(challenge.endDate)) return ignored(IgnoreReason.OUT_OF_WINDOW)
        challenge.startedAt?.let { if (signal.at.isBefore(it)) return ignored(IgnoreReason.BEFORE_START) }

        val source: EventSource
        val candidate: Candidate = when (signal) {
            is ManualReport -> {
                source = EventSource.MANUAL
                Candidate(EventOutcome.CONFIRMED_FAIL, null, null, null)
            }
            is SafeBrowserNavigation -> {
                source = EventSource.SAFE_BROWSER
                if (!signal.isMainFrame) return ignored(IgnoreReason.NOT_MAIN_FRAME)
                val rule = when (val c = domains.classify(signal.host)) {
                    is DomainClassification.Match -> c.rule
                    is DomainClassification.Allowlisted -> return ignored(IgnoreReason.ALLOWLISTED)
                    is DomainClassification.Unknown -> return ignored(IgnoreReason.NOT_ADULT_DOMAIN)
                    DomainClassification.Invalid -> return ignored(IgnoreReason.INVALID_HOST)
                }
                if (!challenge.rules.safeBrowserNavigationFails) return ignored(IgnoreReason.RULE_DISABLED)
                Candidate(EventOutcome.CONFIRMED_FAIL, rule.domain, rule.category, rule.confidence)
            }
            is VpnDomainSignal -> {
                source = EventSource.LOCAL_VPN
                if (!challenge.rules.deviceGuardEnabled) return ignored(IgnoreReason.SOURCE_DISABLED)
                val rule = when (val c = domains.classify(signal.host)) {
                    is DomainClassification.Match -> c.rule
                    is DomainClassification.Allowlisted -> return ignored(IgnoreReason.ALLOWLISTED)
                    is DomainClassification.Unknown -> return ignored(IgnoreReason.NOT_ADULT_DOMAIN)
                    DomainClassification.Invalid -> return ignored(IgnoreReason.INVALID_HOST)
                }
                val outcome = if (challenge.rules.strictActive) EventOutcome.CONFIRMED_FAIL else EventOutcome.POSSIBLE_EXPOSURE
                Candidate(outcome, rule.domain, rule.category, rule.confidence)
            }
        }

        // With domain detail retention off, the stored record (including its dedup key) carries no domain.
        val keptDomain = if (storeDomainDetail) candidate.ruleDomain else null
        val key = dedup.keyFor(source, keptDomain)
        if (dedup.isDuplicate(key, source, signal.at, existing)) return ignored(IgnoreReason.DUPLICATE)

        return ProcessResult.Recorded(
            NnnEvent(
                id = signal.eventId, challengeId = challenge.id, at = signal.at, challengeDate = date,
                source = source, originalOutcome = candidate.outcome,
                matchedDomain = keptDomain,
                category = if (storeDomainDetail) candidate.category else null,
                confidence = if (storeDomainDetail) candidate.confidence else null,
                dedupKey = key
            )
        )
    }

    /** Appends a review entry; the original classification and every earlier review stay in the trail. */
    fun review(
        events: List<NnnEvent>, eventId: String, decision: ReviewDecision, reason: ReviewReason, at: Instant
    ): ReviewResult {
        val index = events.indexOfFirst { it.id == eventId }
        if (index < 0) return ReviewResult.NotFound
        if (!reason.accepts(decision)) return ReviewResult.InvalidReason
        val updated = events[index].let { it.copy(reviews = it.reviews + ReviewEntry(decision, reason, at)) }
        val list = events.toMutableList().also { it[index] = updated }.toList()
        return ReviewResult.Updated(list, updated)
    }

    /** A self check-in is a SELF REPORT, never technical verification. */
    fun recordSelfCheck(challenge: NnnChallenge, existing: Collection<SelfCheck>, date: LocalDate, now: Instant): SelfCheckResult {
        if (challenge.status != ChallengeStatus.ACTIVE) return SelfCheckResult.NotAllowed(SelfCheckDenied.CHALLENGE_NOT_ACTIVE)
        if (date.isBefore(challenge.startDate) || date.isAfter(challenge.endDate)) return SelfCheckResult.NotAllowed(SelfCheckDenied.OUTSIDE_WINDOW)
        if (date.isAfter(challenge.localDate(now))) return SelfCheckResult.NotAllowed(SelfCheckDenied.FUTURE_DAY)
        existing.firstOrNull { it.date == date }?.let { return SelfCheckResult.AlreadyRecorded(it) }
        return SelfCheckResult.Recorded(SelfCheck(date, now))
    }

    /** Completion only after the end date; paused/abandoned/not-started challenges are never auto-completed. */
    fun completeIfEnded(challenge: NnnChallenge, now: Instant): NnnChallenge =
        if (challenge.status == ChallengeStatus.ACTIVE && challenge.phaseAt(now) == ChallengePhase.ENDED)
            challenge.copy(status = ChallengeStatus.COMPLETED, completedAt = now)
        else challenge

    private fun ignored(reason: IgnoreReason) = ProcessResult.Ignored(reason)
}
