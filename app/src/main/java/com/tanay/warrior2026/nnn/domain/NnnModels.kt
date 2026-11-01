package com.tanay.warrior.nnn.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Pure domain model. No Android, no serialization annotations (persistence DTOs live in nnn/data).

enum class ChallengeStatus { NOT_STARTED, ACTIVE, PAUSED, COMPLETED, ABANDONED }

/** Where "now" sits relative to the challenge dates, independent of the stored [ChallengeStatus]. */
enum class ChallengePhase { UPCOMING, IN_PROGRESS, ENDED }

enum class EventSource { SAFE_BROWSER, LOCAL_VPN, MANUAL }

/**
 * Stored events carry only CONFIRMED_FAIL or POSSIBLE_EXPOSURE as their ORIGINAL outcome.
 * REVIEWED_* are the EFFECTIVE outcomes after a user review. IGNORED signals are never stored.
 */
enum class EventOutcome { CONFIRMED_FAIL, POSSIBLE_EXPOSURE, IGNORED, REVIEWED_FALSE_POSITIVE, REVIEWED_CONFIRMED }

enum class IgnoreReason {
    DUPLICATE, CHALLENGE_NOT_ACTIVE, OUT_OF_WINDOW, BEFORE_START,
    SOURCE_DISABLED, RULE_DISABLED, NOT_MAIN_FRAME, ALLOWLISTED, NOT_ADULT_DOMAIN, INVALID_HOST
}

enum class ReviewDecision { FALSE_POSITIVE, CONFIRMED }

enum class ReviewReason(private val forFalsePositive: Boolean, private val forConfirmed: Boolean) {
    NOT_ME(true, false), BACKGROUND_TRAFFIC(true, false), ALLOWED_SITE(true, false),
    MISTAKEN_REPORT(true, false), CONFIRMED_VISIT(false, true), OTHER(true, true);

    fun accepts(decision: ReviewDecision) =
        if (decision == ReviewDecision.FALSE_POSITIVE) forFalsePositive else forConfirmed
}

enum class SelfCheckDenied { CHALLENGE_NOT_ACTIVE, OUTSIDE_WINDOW, FUTURE_DAY }

enum class MonitorSource { SAFE_BROWSER, DEVICE_GUARD }

enum class MonitorState { DISABLED, AWAITING_PERMISSION, ACTIVE, PAUSED, UNAVAILABLE, INTERRUPTED, CONFLICTED }

enum class DayStatus { FUTURE, UNLOGGED, SELF_REPORTED_CLEAN, MONITORED_CLEAN, POSSIBLE_EXPOSURE, FAILED, UNMONITORED, PARTIAL_COVERAGE }

/** The user's chosen rules. Defaults are the conservative ones from the product spec. */
data class NnnRuleSet(
    /** A main-frame Safe Browser navigation to a matched adult domain counts as a failure. */
    val safeBrowserNavigationFails: Boolean = true,
    /** Optional device-wide Domain Guard (local VPN). Off unless the user turns it on. */
    val deviceGuardEnabled: Boolean = false,
    /** Strict mode: a matched Domain Guard signal counts as a failure. Needs [strictAcknowledgedConsentVersion]. */
    val strictDomainGuard: Boolean = false,
    /** Consent version under which the user saw the false-positive warning. Null = never acknowledged. */
    val strictAcknowledgedConsentVersion: Int? = null
) {
    val strictActive: Boolean get() = deviceGuardEnabled && strictDomainGuard && strictAcknowledgedConsentVersion != null
}

data class NnnChallenge(
    val id: String,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val zoneId: ZoneId,
    val status: ChallengeStatus,
    val createdAt: Instant,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val rules: NnnRuleSet,
    val consentVersion: Int
) {
    init {
        require(!endDate.isBefore(startDate)) { "endDate must not be before startDate" }
        require(totalDays <= MAX_DAYS) { "challenge longer than $MAX_DAYS days" }
    }

    /** Inclusive day count. */
    val totalDays: Int get() = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

    /** The ONLY place instants become dates. Always the challenge's own zone, never the device's current one. */
    fun localDate(instant: Instant): LocalDate = instant.atZone(zoneId).toLocalDate()

    fun phaseAt(now: Instant): ChallengePhase {
        val today = localDate(now)
        return when {
            today.isBefore(startDate) -> ChallengePhase.UPCOMING
            today.isAfter(endDate) -> ChallengePhase.ENDED
            else -> ChallengePhase.IN_PROGRESS
        }
    }

    /** 0 before the start, 1..totalDays during, totalDays after the end. */
    fun dayNumberAt(now: Instant): Int = when (phaseAt(now)) {
        ChallengePhase.UPCOMING -> 0
        ChallengePhase.ENDED -> totalDays
        ChallengePhase.IN_PROGRESS -> ChronoUnit.DAYS.between(startDate, localDate(now)).toInt() + 1
    }

    /** Days AFTER today until the end date (0 on the last day). */
    fun daysRemainingAt(now: Instant): Int = when (phaseAt(now)) {
        ChallengePhase.UPCOMING -> totalDays
        ChallengePhase.ENDED -> 0
        ChallengePhase.IN_PROGRESS -> totalDays - dayNumberAt(now)
    }

    /** Real length of a calendar day in the challenge zone (23h / 24h / 25h around DST changes). */
    fun dayLength(date: LocalDate): Duration =
        Duration.between(date.atStartOfDay(zoneId).toInstant(), date.plusDays(1).atStartOfDay(zoneId).toInstant())

    /** Starting never moves the dates and never pretends earlier days were monitored. */
    fun started(now: Instant, rules: NnnRuleSet, consentVersion: Int): NnnChallenge =
        copy(status = ChallengeStatus.ACTIVE, startedAt = now, rules = rules, consentVersion = consentVersion)

    companion object {
        const val MAX_DAYS = 366

        fun default2026(zone: ZoneId, createdAt: Instant) = NnnChallenge(
            id = "nnn-2026", title = "No Nut November 2026",
            startDate = LocalDate.of(2026, 11, 1), endDate = LocalDate.of(2026, 11, 30),
            zoneId = zone, status = ChallengeStatus.NOT_STARTED, createdAt = createdAt,
            startedAt = null, completedAt = null, rules = NnnRuleSet(), consentVersion = 0
        )
    }
}

// ── Signals (inputs to the engine) ──────────────────────────────────────────────
// toString() is redacted on purpose: hosts must never reach logs or crash text.

sealed interface NnnSignal {
    val eventId: String
    val at: Instant
}

data class SafeBrowserNavigation(
    override val eventId: String, override val at: Instant, val host: String, val isMainFrame: Boolean
) : NnnSignal { override fun toString() = "SafeBrowserNavigation(id=$eventId, mainFrame=$isMainFrame)" }

data class VpnDomainSignal(
    override val eventId: String, override val at: Instant, val host: String
) : NnnSignal { override fun toString() = "VpnDomainSignal(id=$eventId)" }

data class ManualReport(override val eventId: String, override val at: Instant) : NnnSignal

// ── Stored records ──────────────────────────────────────────────────────────────

data class ReviewEntry(val decision: ReviewDecision, val reason: ReviewReason, val at: Instant)

data class NnnEvent(
    val id: String,
    val challengeId: String,
    val at: Instant,
    val challengeDate: LocalDate,
    val source: EventSource,
    val originalOutcome: EventOutcome,
    /** The matched RULE domain (never the visited host/URL); null for manual events or when detail retention is off. */
    val matchedDomain: String?,
    val category: String?,
    val confidence: Int?,
    val dedupKey: String,
    /** Append-only audit trail; the last entry decides the effective outcome. */
    val reviews: List<ReviewEntry> = emptyList()
) {
    init {
        require(originalOutcome == EventOutcome.CONFIRMED_FAIL || originalOutcome == EventOutcome.POSSIBLE_EXPOSURE) {
            "stored events start as CONFIRMED_FAIL or POSSIBLE_EXPOSURE"
        }
    }

    val effectiveOutcome: EventOutcome
        get() = when (reviews.lastOrNull()?.decision) {
            null -> originalOutcome
            ReviewDecision.FALSE_POSITIVE -> EventOutcome.REVIEWED_FALSE_POSITIVE
            ReviewDecision.CONFIRMED -> EventOutcome.REVIEWED_CONFIRMED
        }

    val countsAsConfirmedFailure: Boolean
        get() = effectiveOutcome == EventOutcome.CONFIRMED_FAIL || effectiveOutcome == EventOutcome.REVIEWED_CONFIRMED

    val isPendingReview: Boolean get() = effectiveOutcome == EventOutcome.POSSIBLE_EXPOSURE

    override fun toString() = "NnnEvent(id=$id, source=$source, outcome=$effectiveOutcome)"
}

data class SelfCheck(val date: LocalDate, val at: Instant)

/** A period during which a source was in [state]. [end] == null means "still in this state". */
data class MonitoringSegment(val source: MonitorSource, val state: MonitorState, val start: Instant, val end: Instant?)
