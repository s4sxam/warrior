package com.tanay.warrior.nnn.domain

import com.tanay.warrior.nnn.domain.Fx.KOLKATA
import com.tanay.warrior.nnn.domain.Fx.at
import com.tanay.warrior.nnn.domain.Fx.checks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NnnInsightsTest {
    private val c = Fx.challenge()
    private fun snap(now: String, events: List<NnnEvent> = emptyList(), checks: List<SelfCheck> = emptyList()) =
        NnnScoring.snapshot(c, events, checks, emptyList(), at(KOLKATA, now))
    private fun insights(now: String, events: List<NnnEvent> = emptyList(), checks: List<SelfCheck> = emptyList()) =
        NnnInsights.from(snap(now, events, checks), events)

    @Test fun progressAndCleanPercentagesAreSeparate() {
        val i = insights("2026-11-10T12:00:00", checks = checks(1..5))
        assertEquals(33, i.progressPercent)          // day 10 of 30
        assertEquals(50, i.cleanDayPercent)          // 5 clean of 10 elapsed
        assertEquals(5, i.selfReportedDays)
    }

    @Test fun upcomingChallengeHasZeroPercentages() {
        val i = insights("2026-10-25T12:00:00")
        assertEquals(0, i.progressPercent); assertEquals(0, i.cleanDayPercent)
    }

    @Test fun weeksAreCountedFromTheStartDate() {
        val i = insights("2026-11-10T12:00:00", checks = checks(1..9))
        assertEquals(2, i.weeks.size)
        assertEquals(WeekInsight(1, 7, 7), i.weeks[0])
        assertEquals(WeekInsight(2, 2, 3), i.weeks[1])   // Nov 8,9 clean; Nov 10 (today) unlogged
    }

    @Test fun trendNeedsTwoWeeksWithEnoughDays() {
        assertNull(insights("2026-11-09T12:00:00", checks = checks(1..8)).weekOverWeekChange)   // week 2 has 2 days
        val i = insights("2026-11-12T12:00:00", checks = checks(1..4) + checks(8..11))          // w1: 4/7, w2: 4/5
        assertNotNull(i.weekOverWeekChange)
        assertEquals(((4 * 100) / 5) - ((4 * 100) / 7), i.weekOverWeekChange)
    }

    @Test fun timeOfDayPatternNeedsEnoughSamples() {
        val few = Fx.record(c, *(1..4).map { Fx.manual("m$it", KOLKATA, "2026-11-0${it}T22:15:00") as NnnSignal }.toTypedArray())
        val i = insights("2026-11-10T12:00:00", few)
        assertNull(i.hourlyCounts)
        assertEquals(4, i.eventSampleSize)
        assertEquals(NnnInsights.MIN_TIME_SAMPLES, 5)
    }

    @Test fun timeOfDayPatternUsesTheChallengeZoneHours() {
        val signals = (1..6).map { Fx.manual("m$it", KOLKATA, "2026-11-0${it}T22:15:00") as NnnSignal }
        val i = insights("2026-11-10T12:00:00", Fx.record(c, *signals.toTypedArray()))
        val hours = i.hourlyCounts!!
        assertEquals(24, hours.size)
        assertEquals(6, hours[22]); assertEquals(6, hours.sum())
        assertEquals(22, i.peakHour)
    }

    @Test fun correctedEventsAreNotCountedInThePattern() {
        val signals = (1..6).map { Fx.manual("m$it", KOLKATA, "2026-11-0${it}T22:15:00") as NnnSignal }
        var events = Fx.record(c, *signals.toTypedArray())
        events = (Fx.engine.review(events, "m1", ReviewDecision.FALSE_POSITIVE, ReviewReason.MISTAKEN_REPORT, at(KOLKATA, "2026-11-07T00:00:00"))
            as NnnChallengeEngine.ReviewResult.Updated).events
        assertEquals(5, insights("2026-11-10T12:00:00", events).eventSampleSize)
    }

    @Test fun totalsMatchTheSnapshot() {
        val events = Fx.record(c, Fx.manual("a", KOLKATA, "2026-11-03T10:00:00"), Fx.manual("b", KOLKATA, "2026-11-03T15:00:00"))
        val i = insights("2026-11-10T12:00:00", events)
        assertEquals(1, i.failedDays); assertEquals(2, i.confirmedIncidents); assertEquals(0, i.possibleExposureIncidents)
        assertTrue(i.hourlyCounts == null)
    }
}
