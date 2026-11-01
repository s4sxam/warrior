package com.tanay.warrior.nnn.domain

import com.tanay.warrior.nnn.domain.Fx.KOLKATA
import com.tanay.warrior.nnn.domain.Fx.NEW_YORK
import com.tanay.warrior.nnn.domain.Fx.at
import com.tanay.warrior.nnn.domain.Fx.checks
import com.tanay.warrior.nnn.domain.Fx.day
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NnnScoringTest {
    private val guardRules = NnnRuleSet(deviceGuardEnabled = true)
    private val c = Fx.challenge(rules = guardRules)
    private val now = at(KOLKATA, "2026-11-10T12:00:00")       // challenge day 10

    private fun snap(
        events: List<NnnEvent> = emptyList(),
        selfChecks: List<SelfCheck> = emptyList(),
        segments: List<MonitoringSegment> = emptyList(),
        ch: NnnChallenge = c,
        at: Instant = now
    ) = NnnScoring.snapshot(ch, events, selfChecks, segments, at)

    private fun NnnSnapshot.status(n: Int) = days[n - 1].status
    private fun guard(from: String, to: String?, state: MonitorState = MonitorState.ACTIVE, zone: java.time.ZoneId = KOLKATA) =
        MonitoringSegment(MonitorSource.DEVICE_GUARD, state, at(zone, from), to?.let { at(zone, it) })

    private fun manualOn(vararg dayTimes: String): List<NnnEvent> =
        Fx.record(c, *dayTimes.mapIndexed { i, t -> Fx.manual("m$i", KOLKATA, t) as NnnSignal }.toTypedArray())
    private fun exposureOn(local: String) = Fx.record(c, VpnDomainSignal("v", at(KOLKATA, local), "example.test"))

    // ── honesty: nothing is clean unless something says so ───────────────────
    @Test fun noInputsNeverFabricatesCleanDays() {
        val s = snap()
        assertEquals(30, s.days.size)
        (1..10).forEach { assertEquals(DayStatus.UNLOGGED, s.status(it)) }
        (11..30).forEach { assertEquals(DayStatus.FUTURE, s.status(it)) }
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.selfReportedDays + s.monitoredCleanDays)
        assertEquals(10, s.dayNumber)
        assertEquals(20, s.daysRemaining)
        assertEquals(ChallengePhase.IN_PROGRESS, s.phase)
    }

    @Test fun selfChecksBuildAStreakAndAreLabelledSelfReported() {
        val s = snap(selfChecks = checks(1..10))
        assertEquals(10, s.currentStreak); assertEquals(10, s.bestStreak)
        assertEquals(10, s.selfReportedDays)
        assertEquals(0, s.monitoredCleanDays)
        assertEquals(DayStatus.SELF_REPORTED_CLEAN, s.status(10))
    }

    @Test fun todayNotYetLoggedDoesNotBreakTheStreak() {
        assertEquals(9, snap(selfChecks = checks(1..9)).currentStreak)
    }

    @Test fun anUnloggedPastDayBreaksTheStreak() {
        val s = snap(selfChecks = checks(1..4) + checks(6..9))
        assertEquals(4, s.currentStreak); assertEquals(4, s.bestStreak)
        assertEquals(DayStatus.UNLOGGED, s.status(5))
    }

    // ── failures ─────────────────────────────────────────────────────────────
    @Test fun aFailureMarksTheDayAndResetsTheStreak() {
        val s = snap(events = manualOn("2026-11-05T21:00:00"), selfChecks = checks(1..9))
        assertEquals(DayStatus.FAILED, s.status(5))
        assertEquals(4, s.currentStreak)          // days 6..9
        assertEquals(4, s.bestStreak)
        assertEquals(1, s.failedDays)
        assertEquals(5, s.daysSinceLastConfirmedFailure)   // Nov 10 - Nov 5
    }

    @Test fun multipleIncidentsOnOneDayAreOneFailedDayWithTheRightCount() {
        val s = snap(events = manualOn("2026-11-05T09:00:00", "2026-11-05T14:00:00", "2026-11-05T22:00:00"))
        assertEquals(1, s.failedDays)
        assertEquals(3, s.confirmedIncidents)
        assertEquals(3, s.days[4].confirmedFailureCount)
        assertEquals(at(KOLKATA, "2026-11-05T09:00:00"), s.days[4].firstEventAt)
        assertEquals(at(KOLKATA, "2026-11-05T22:00:00"), s.days[4].lastEventAt)
    }

    @Test fun noFailureMeansNoDaysSinceValue() = assertNull(snap(selfChecks = checks(1..9)).daysSinceLastConfirmedFailure)

    @Test fun aFailureBeatsASelfCheckOnTheSameDay() {
        val s = snap(events = manualOn("2026-11-05T21:00:00"), selfChecks = checks(5..5))
        assertEquals(DayStatus.FAILED, s.status(5))
    }

    // ── possible exposure and review ─────────────────────────────────────────
    @Test fun possibleExposureIsPendingReviewAndInterruptsTheStreakUntilReviewed() {
        val s = snap(events = exposureOn("2026-11-07T10:00:00"), selfChecks = checks(1..9))
        assertEquals(DayStatus.POSSIBLE_EXPOSURE, s.status(7))
        assertEquals(2, s.currentStreak)           // days 8..9
        assertEquals(1, s.pendingReviewCount)
        assertEquals(0, s.failedDays)              // NOT a failure
        assertEquals(1, s.possibleExposureIncidents)
    }

    @Test fun falsePositiveReviewRestoresTheSelfReportedDayAndStreak() {
        val reviewed = (Fx.engine.review(exposureOn("2026-11-07T10:00:00"), "v", ReviewDecision.FALSE_POSITIVE,
            ReviewReason.BACKGROUND_TRAFFIC, now) as NnnChallengeEngine.ReviewResult.Updated).events
        val s = snap(events = reviewed, selfChecks = checks(1..9))
        assertEquals(DayStatus.SELF_REPORTED_CLEAN, s.status(7))
        assertEquals(9, s.currentStreak)
        assertEquals(0, s.pendingReviewCount)
        assertEquals(1, reviewed.single().reviews.size) // audit trail intact
    }

    @Test fun falsePositiveWithoutACheckInDoesNotBecomeClean() {
        val reviewed = (Fx.engine.review(exposureOn("2026-11-07T10:00:00"), "v", ReviewDecision.FALSE_POSITIVE,
            ReviewReason.NOT_ME, now) as NnnChallengeEngine.ReviewResult.Updated).events
        assertEquals(DayStatus.UNLOGGED, snap(events = reviewed).status(7))
    }

    @Test fun confirmingAPossibleExposureFailsTheDayAndRecalculatesTheStreak() {
        val reviewed = (Fx.engine.review(exposureOn("2026-11-07T10:00:00"), "v", ReviewDecision.CONFIRMED,
            ReviewReason.CONFIRMED_VISIT, now) as NnnChallengeEngine.ReviewResult.Updated).events
        val s = snap(events = reviewed, selfChecks = checks(1..9))
        assertEquals(DayStatus.FAILED, s.status(7))
        assertEquals(2, s.currentStreak)
        assertEquals(1, s.confirmedIncidents)
    }

    @Test fun aCorrectedManualReportNoLongerCounts() {
        val reviewed = (Fx.engine.review(manualOn("2026-11-05T21:00:00"), "m0", ReviewDecision.FALSE_POSITIVE,
            ReviewReason.MISTAKEN_REPORT, now) as NnnChallengeEngine.ReviewResult.Updated).events
        val s = snap(events = reviewed, selfChecks = checks(1..9))
        assertEquals(0, s.failedDays); assertEquals(9, s.currentStreak)
    }

    // ── monitoring coverage ──────────────────────────────────────────────────
    @Test fun sufficientlyMonitoredCompletedDaysAreMonitoredCleanButTodayIsNotYet() {
        val s = snap(segments = listOf(guard("2026-11-01T00:00:00", null)))
        (1..9).forEach { assertEquals(DayStatus.MONITORED_CLEAN, s.status(it)) }
        assertEquals(DayStatus.UNLOGGED, s.status(10))     // day not over → no clean claim
        assertEquals(9, s.currentStreak)
        assertEquals(9, s.monitoredCleanDays)
    }

    @Test fun partialCoverageIsFlaggedNotCalledClean() {
        val s = snap(segments = listOf(guard("2026-11-01T00:00:00", "2026-11-03T12:00:00"), guard("2026-11-04T00:00:00", null)))
        assertEquals(DayStatus.PARTIAL_COVERAGE, s.status(3))
        assertEquals(0.5, s.days[2].deviceGuardCoverage!!, 1e-9)
        assertEquals(DayStatus.MONITORED_CLEAN, s.status(4))
        assertEquals(6, s.currentStreak)          // days 4..9 — the partial day broke it
    }

    @Test fun selfReportOnAPartialDayStaysSelfReportedAndShowsCoverage() {
        val s = snap(segments = listOf(guard("2026-11-01T00:00:00", "2026-11-03T12:00:00"), guard("2026-11-04T00:00:00", null)),
            selfChecks = checks(3..3))
        assertEquals(DayStatus.SELF_REPORTED_CLEAN, s.status(3))
        assertEquals(0.5, s.days[2].deviceGuardCoverage!!, 1e-9)
    }

    @Test fun enabledButEmptyDaysAreUnmonitoredNotClean() {
        val s = snap(segments = listOf(guard("2026-11-06T00:00:00", null)))
        assertEquals(DayStatus.UNMONITORED, s.status(2))
        assertEquals(DayStatus.MONITORED_CLEAN, s.status(7))
    }

    @Test fun interruptedOrUnavailableTimeNeverCountsAsCoverage() {
        for (st in listOf(MonitorState.INTERRUPTED, MonitorState.UNAVAILABLE, MonitorState.PAUSED, MonitorState.CONFLICTED, MonitorState.AWAITING_PERMISSION)) {
            val s = snap(segments = listOf(guard("2026-11-01T00:00:00", null, state = st)))
            assertEquals("state $st", DayStatus.UNMONITORED, s.status(3))
            assertEquals(0, s.currentStreak)
        }
    }

    @Test fun safeBrowserAloneNeverProducesMonitoredClean() {
        val sbOnly = listOf(MonitoringSegment(MonitorSource.SAFE_BROWSER, MonitorState.ACTIVE, at(KOLKATA, "2026-11-01T00:00:00"), null))
        val s = snap(segments = sbOnly)
        (1..9).forEach { assertEquals(DayStatus.UNLOGGED, s.status(it)) }
    }

    @Test fun overlappingSegmentsAreNotDoubleCounted() {
        val s = snap(segments = listOf(guard("2026-11-03T00:00:00", "2026-11-03T18:00:00"), guard("2026-11-03T06:00:00", "2026-11-03T12:00:00")))
        assertEquals(0.75, s.days[2].deviceGuardCoverage!!, 1e-9)
    }

    @Test fun coverageUsesTheRealLengthOfTheDstFallBackDay() {
        val ny = Fx.challenge(zone = NEW_YORK, rules = guardRules)
        val nowNy = at(NEW_YORK, "2026-11-03T12:00:00")
        // Nov 1, 2026 is 25h long in New York. A 23h segment is 92% — NOT sufficient (it would look like 95.8% on a 24h day).
        val start = Instant.parse("2026-11-01T04:00:00Z")
        val seg = MonitoringSegment(MonitorSource.DEVICE_GUARD, MonitorState.ACTIVE, start, start.plusSeconds(23 * 3600))
        val s = NnnScoring.snapshot(ny, emptyList(), emptyList(), listOf(seg), nowNy)
        assertEquals(DayStatus.PARTIAL_COVERAGE, s.days[0].status)
        assertEquals(23.0 / 25.0, s.days[0].deviceGuardCoverage!!, 1e-9)
    }

    @Test fun lateStartDaysBeforeTheStartAreNeverMonitored() {
        val late = Fx.challenge(rules = guardRules, startedAt = at(KOLKATA, "2026-11-05T09:00:00"))
        val s = snap(ch = late, segments = listOf(guard("2026-11-05T09:00:00", null)))
        assertEquals(DayStatus.UNLOGGED, s.status(3))
        assertNull(s.days[2].deviceGuardCoverage)
        assertEquals(DayStatus.MONITORED_CLEAN, s.status(6))
    }

    // ── completion ───────────────────────────────────────────────────────────
    private val after = at(KOLKATA, "2026-12-02T10:00:00")

    @Test fun finishedWithEveryDayLoggedAndNoFailureIsAChampion() {
        val s = snap(selfChecks = checks(1..30), at = after)
        assertEquals(ChallengePhase.ENDED, s.phase)
        assertEquals(30, s.dayNumber); assertEquals(0, s.daysRemaining)
        assertEquals(30, s.currentStreak)
        assertTrue(s.completedWithoutFailures); assertTrue(s.isChampion)
    }

    @Test fun anUnloggedDayWithNoFailureIsCompletedButNotChampion() {
        val s = snap(selfChecks = checks(1..29), at = after)
        assertTrue(s.completedWithoutFailures); assertFalse(s.isChampion)
    }

    @Test fun aFailureOrPendingReviewBlocksTheChampionBadge() {
        assertFalse(snap(events = manualOn("2026-11-05T21:00:00"), selfChecks = checks(1..30), at = after).completedWithoutFailures)
        assertFalse(snap(events = exposureOn("2026-11-05T21:00:00"), selfChecks = checks(1..30), at = after).isChampion)
    }

    @Test fun nothingIsAChampionWhileTheChallengeIsStillRunning() = assertFalse(snap(selfChecks = checks(1..10)).isChampion)

    @Test fun upcomingChallengeHasNoProgress() {
        val s = snap(at = at(KOLKATA, "2026-10-25T10:00:00"))
        assertEquals(ChallengePhase.UPCOMING, s.phase)
        assertEquals(0, s.dayNumber); assertEquals(30, s.daysRemaining)
        assertTrue(s.days.all { it.status == DayStatus.FUTURE })
    }

    @Test fun eventsOutsideTheWindowAreNotCounted() {
        val stray = NnnEvent(id = "x", challengeId = c.id, at = at(KOLKATA, "2026-10-31T10:00:00"), challengeDate = java.time.LocalDate.of(2026, 10, 31),
            source = EventSource.MANUAL, originalOutcome = EventOutcome.CONFIRMED_FAIL, matchedDomain = null, category = null, confidence = null, dedupKey = "MANUAL|-")
        assertEquals(0, snap(events = listOf(stray)).confirmedIncidents)
    }

    // ── status text honesty ──────────────────────────────────────────────────
    @Test fun statusTextNeverOverclaims() {
        val banned = listOf("verified", "protected", "100%", "guaranteed", "all apps", "complete")
        for (sb in MonitorState.values()) for (dg in MonitorState.values()) for (cov in listOf(null, 0.0, 0.5, 1.0)) {
            val text = NnnStatusText.describe(sb, dg, cov).lowercase()
            banned.forEach { assertFalse("'$it' in \"$text\" for $sb/$dg/$cov", text.contains(it)) }
        }
    }

    @Test fun statusTextAdmitsGapsAndDisabledSources() {
        assertTrue(NnnStatusText.describe(MonitorState.DISABLED, MonitorState.DISABLED, null).contains("manual", ignoreCase = true))
        assertTrue(NnnStatusText.describe(MonitorState.ACTIVE, MonitorState.INTERRUPTED, 0.4).contains("interrupted", ignoreCase = true))
        assertTrue(NnnStatusText.describe(MonitorState.ACTIVE, MonitorState.ACTIVE, 0.5).contains("50%"))
    }
}
