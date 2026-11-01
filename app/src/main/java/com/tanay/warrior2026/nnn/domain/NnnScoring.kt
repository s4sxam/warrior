package com.tanay.warrior.nnn.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

data class DayRecord(
    val date: LocalDate,
    val dayNumber: Int,
    val status: DayStatus,
    val confirmedFailureCount: Int,
    val possibleExposureCount: Int,
    val firstEventAt: Instant?,
    val lastEventAt: Instant?,
    /** Fraction of the day's monitorable time the Device Guard was ACTIVE; null when nothing could be monitored. */
    val deviceGuardCoverage: Double?,
    val selfCheckedAt: Instant?
)

data class NnnSnapshot(
    val challenge: NnnChallenge,
    val phase: ChallengePhase,
    val dayNumber: Int,
    val daysRemaining: Int,
    val days: List<DayRecord>,
    val currentStreak: Int,
    val bestStreak: Int,
    val daysSinceLastConfirmedFailure: Int?,
    val failedDays: Int,
    val confirmedIncidents: Int,
    val possibleExposureIncidents: Int,
    val pendingReviewCount: Int,
    val selfReportedDays: Int,
    val monitoredCleanDays: Int,
    val completedWithoutFailures: Boolean,
    val isChampion: Boolean
)

/**
 * Derives every number the UI shows from persisted facts (events, self check-ins, monitoring segments).
 * Nothing is stored pre-computed, so a review correction automatically recalculates days and streaks.
 *
 * Honesty rules baked in:
 *  - a day is clean only if the user said so (SELF_REPORTED_CLEAN) or Device Guard was ACTIVE for
 *    ≥ [SUFFICIENT_COVERAGE] of a COMPLETED day (MONITORED_CLEAN, i.e. "no events detected");
 *  - Safe Browser alone never yields MONITORED_CLEAN (it only sees Safe Browser);
 *  - paused / interrupted / unavailable / conflicted time is never coverage;
 *  - unreviewed possible exposures break the streak until reviewed (the UI says so).
 */
object NnnScoring {
    const val SUFFICIENT_COVERAGE = 0.95

    fun snapshot(
        challenge: NnnChallenge,
        events: List<NnnEvent>,
        selfChecks: Collection<SelfCheck>,
        segments: List<MonitoringSegment>,
        now: Instant
    ): NnnSnapshot {
        val today = challenge.localDate(now)
        val phase = challenge.phaseAt(now)
        val total = challenge.totalDays

        val eventsByDate = events
            .filter { it.challengeId == challenge.id && !it.challengeDate.isBefore(challenge.startDate) && !it.challengeDate.isAfter(challenge.endDate) }
            .groupBy { it.challengeDate }
        val checkByDate = selfChecks.associateBy { it.date }
        val guardSegments = segments.filter { it.source == MonitorSource.DEVICE_GUARD }
        val guardEverEnabled = guardSegments.any { it.state != MonitorState.DISABLED }
        val activeGuard = mergeIntervals(
            guardSegments.filter { it.state == MonitorState.ACTIVE }.map { it.start to (it.end ?: now) }
        )

        val days = (0 until total).map { i ->
            val date = challenge.startDate.plusDays(i.toLong())
            val dayEvents = eventsByDate[date].orEmpty()
            val confirmed = dayEvents.count { it.countsAsConfirmedFailure }
            val pending = dayEvents.count { it.isPendingReview }
            val counted = dayEvents.filter { it.countsAsConfirmedFailure || it.isPendingReview }
            val check = checkByDate[date]

            if (date.isAfter(today)) {
                DayRecord(date, i + 1, DayStatus.FUTURE, 0, 0, null, null, null, null)
            } else {
                val coverage = coverageFor(challenge, date, now, activeGuard)
                val sufficient = coverage != null && coverage >= SUFFICIENT_COVERAGE && date.isBefore(today)
                val status = when {
                    confirmed > 0 -> DayStatus.FAILED
                    pending > 0 -> DayStatus.POSSIBLE_EXPOSURE
                    sufficient -> DayStatus.MONITORED_CLEAN
                    check != null -> DayStatus.SELF_REPORTED_CLEAN
                    date == today -> DayStatus.UNLOGGED          // today is unresolved, not a gap
                    coverage == null -> DayStatus.UNLOGGED
                    coverage > 0.0 -> DayStatus.PARTIAL_COVERAGE
                    guardEverEnabled -> DayStatus.UNMONITORED
                    else -> DayStatus.UNLOGGED
                }
                DayRecord(
                    date, i + 1, status, confirmed, pending,
                    counted.minOfOrNull { it.at }, counted.maxOfOrNull { it.at }, coverage, check?.at
                )
            }
        }

        fun isClean(s: DayStatus) = s == DayStatus.SELF_REPORTED_CLEAN || s == DayStatus.MONITORED_CLEAN

        var last = when (phase) {
            ChallengePhase.UPCOMING -> -1
            ChallengePhase.IN_PROGRESS -> challenge.dayNumberAt(now) - 1
            ChallengePhase.ENDED -> total - 1
        }
        if (phase == ChallengePhase.IN_PROGRESS && last >= 0 && days[last].status == DayStatus.UNLOGGED) last--  // today not yet logged
        var streak = 0
        var i = last
        while (i >= 0 && isClean(days[i].status)) { streak++; i-- }

        var best = 0
        var run = 0
        for (d in days) { if (isClean(d.status)) { run++; if (run > best) best = run } else run = 0 }

        val reference = if (today.isAfter(challenge.endDate)) challenge.endDate else today
        val lastFailed = days.lastOrNull { it.status == DayStatus.FAILED }?.date
        val sinceFailure = lastFailed?.let { java.time.temporal.ChronoUnit.DAYS.between(it, reference).toInt() }

        val allEvents = eventsByDate.values.flatten()
        val failedDays = days.count { it.status == DayStatus.FAILED }
        val pendingTotal = allEvents.count { it.isPendingReview }
        val completedWithoutFailures = phase == ChallengePhase.ENDED && failedDays == 0 && pendingTotal == 0

        return NnnSnapshot(
            challenge = challenge, phase = phase,
            dayNumber = challenge.dayNumberAt(now), daysRemaining = challenge.daysRemainingAt(now),
            days = days, currentStreak = streak, bestStreak = best, daysSinceLastConfirmedFailure = sinceFailure,
            failedDays = failedDays,
            confirmedIncidents = allEvents.count { it.countsAsConfirmedFailure },
            possibleExposureIncidents = allEvents.count { it.originalOutcome == EventOutcome.POSSIBLE_EXPOSURE },
            pendingReviewCount = pendingTotal,
            selfReportedDays = days.count { it.status == DayStatus.SELF_REPORTED_CLEAN },
            monitoredCleanDays = days.count { it.status == DayStatus.MONITORED_CLEAN },
            completedWithoutFailures = completedWithoutFailures,
            isChampion = completedWithoutFailures && days.all { isClean(it.status) }
        )
    }

    /**
     * Share of the day's monitorable window (from the challenge start / day start until the day's end or
     * "now") covered by ACTIVE Device Guard time. Uses the real day length, so 23h/25h DST days are exact.
     */
    private fun coverageFor(challenge: NnnChallenge, date: LocalDate, now: Instant, active: List<Pair<Instant, Instant>>): Double? {
        val monitoringStart = challenge.startedAt ?: return null
        val dayStart = date.atStartOfDay(challenge.zoneId).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(challenge.zoneId).toInstant()
        val from = maxOf(dayStart, monitoringStart)
        val to = minOf(dayEnd, now)
        if (!to.isAfter(from)) return null
        val window = Duration.between(from, to).toMillis().toDouble()
        val covered = active.sumOf { (s, e) ->
            val a = maxOf(s, from); val b = minOf(e, to)
            if (b.isAfter(a)) Duration.between(a, b).toMillis() else 0L
        }
        return (covered / window).coerceIn(0.0, 1.0)
    }

    private fun mergeIntervals(intervals: List<Pair<Instant, Instant>>): List<Pair<Instant, Instant>> {
        val sorted = intervals.filter { it.second.isAfter(it.first) }.sortedBy { it.first }
        val out = ArrayList<Pair<Instant, Instant>>()
        for (cur in sorted) {
            val prev = out.lastOrNull()
            if (prev != null && !cur.first.isAfter(prev.second)) out[out.size - 1] = prev.first to maxOf(prev.second, cur.second)
            else out += cur
        }
        return out
    }
}

/** Human-readable monitoring status. Deliberately cannot say "verified", "protected" or "100%". */
object NnnStatusText {
    fun describe(safeBrowser: MonitorState, deviceGuard: MonitorState, deviceGuardCoverageToday: Double?): String {
        if (safeBrowser == MonitorState.DISABLED && deviceGuard == MonitorState.DISABLED)
            return "Manual reporting only. No automatic monitoring is on."
        val sb = when (safeBrowser) {
            MonitorState.ACTIVE -> "Safe Browser: on (sees Safe Browser only)"
            else -> "Safe Browser: ${phrase(safeBrowser)}"
        }
        val dg = when (deviceGuard) {
            MonitorState.ACTIVE -> {
                val c = deviceGuardCoverageToday
                when {
                    c == null -> "Domain Guard: on"
                    c >= 0.995 -> "Domain Guard: on for all of today so far"
                    else -> "Domain Guard: on for ${(c * 100).toInt()}% of today so far"
                }
            }
            MonitorState.INTERRUPTED, MonitorState.UNAVAILABLE, MonitorState.PAUSED, MonitorState.CONFLICTED ->
                "Domain Guard: ${phrase(deviceGuard)} — gaps are recorded and never counted as clean"
            else -> "Domain Guard: ${phrase(deviceGuard)}"
        }
        return "$sb · $dg"
    }

    private fun phrase(s: MonitorState) = when (s) {
        MonitorState.DISABLED -> "off"
        MonitorState.AWAITING_PERMISSION -> "waiting for permission"
        MonitorState.ACTIVE -> "on"
        MonitorState.PAUSED -> "paused"
        MonitorState.UNAVAILABLE -> "unavailable"
        MonitorState.INTERRUPTED -> "interrupted"
        MonitorState.CONFLICTED -> "blocked by a conflict"
    }
}
