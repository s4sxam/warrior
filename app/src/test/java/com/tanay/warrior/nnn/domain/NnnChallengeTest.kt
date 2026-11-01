package com.tanay.warrior.nnn.domain

import com.tanay.warrior.nnn.domain.Fx.KOLKATA
import com.tanay.warrior.nnn.domain.Fx.NEW_YORK
import com.tanay.warrior.nnn.domain.Fx.at
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.util.TimeZone

class NnnChallengeTest {
    private val c = Fx.challenge()

    @Test fun defaultEditionIsNov1To30_2026Inclusive() {
        assertEquals(LocalDate.of(2026, 11, 1), c.startDate)
        assertEquals(LocalDate.of(2026, 11, 30), c.endDate)
        assertEquals(30, c.totalDays)
    }

    @Test fun dayNumbersAtTheBoundaries() {
        assertEquals(0, c.dayNumberAt(at(KOLKATA, "2026-10-31T23:59:59")))
        assertEquals(1, c.dayNumberAt(at(KOLKATA, "2026-11-01T00:00:00")))
        assertEquals(30, c.dayNumberAt(at(KOLKATA, "2026-11-30T23:59:59")))
        assertEquals(30, c.dayNumberAt(at(KOLKATA, "2026-12-01T00:00:00"))) // capped after the end
    }

    @Test fun phasesFollowRealDates() {
        assertEquals(ChallengePhase.UPCOMING, c.phaseAt(at(KOLKATA, "2026-10-31T12:00:00")))
        assertEquals(ChallengePhase.IN_PROGRESS, c.phaseAt(at(KOLKATA, "2026-11-01T00:00:00")))
        assertEquals(ChallengePhase.IN_PROGRESS, c.phaseAt(at(KOLKATA, "2026-11-30T23:59:59")))
        assertEquals(ChallengePhase.ENDED, c.phaseAt(at(KOLKATA, "2026-12-01T00:00:00")))
    }

    @Test fun daysRemaining() {
        assertEquals(30, c.daysRemainingAt(at(KOLKATA, "2026-10-20T12:00:00")))
        assertEquals(29, c.daysRemainingAt(at(KOLKATA, "2026-11-01T12:00:00")))
        assertEquals(20, c.daysRemainingAt(at(KOLKATA, "2026-11-10T12:00:00")))
        assertEquals(0, c.daysRemainingAt(at(KOLKATA, "2026-11-30T12:00:00")))
        assertEquals(0, c.daysRemainingAt(at(KOLKATA, "2026-12-15T12:00:00")))
    }

    @Test fun utcInstantsMapToTheChallengeZoneDate() {
        // 18:30Z on Oct 31 is exactly Nov 1 00:00 in Kolkata (UTC+5:30)
        assertEquals(LocalDate.of(2026, 11, 1), c.localDate(java.time.Instant.parse("2026-10-31T18:30:00Z")))
        assertEquals(LocalDate.of(2026, 10, 31), c.localDate(java.time.Instant.parse("2026-10-31T18:29:59Z")))
    }

    @Test fun newYorkBoundariesAcrossTheNov1DstChange() {
        val ny = Fx.challenge(zone = NEW_YORK)
        // 2026-11-01 04:00Z == 00:00 EDT  (day 1 begins)
        assertEquals(1, ny.dayNumberAt(java.time.Instant.parse("2026-11-01T04:00:00Z")))
        assertEquals(0, ny.dayNumberAt(java.time.Instant.parse("2026-11-01T03:59:59Z")))
        // Nov 30 23:59:59 EST == 2026-12-01T04:59:59Z ; Dec 1 00:00 EST == 05:00Z
        assertEquals(ChallengePhase.IN_PROGRESS, ny.phaseAt(java.time.Instant.parse("2026-12-01T04:59:59Z")))
        assertEquals(ChallengePhase.ENDED, ny.phaseAt(java.time.Instant.parse("2026-12-01T05:00:00Z")))
    }

    @Test fun dstFallBackDayIs25Hours() {
        val ny = Fx.challenge(zone = NEW_YORK)
        assertEquals(Duration.ofHours(25), ny.dayLength(LocalDate.of(2026, 11, 1)))
        assertEquals(Duration.ofHours(24), ny.dayLength(LocalDate.of(2026, 11, 2)))
    }

    @Test fun deviceZoneChangeDoesNotMoveDates() {
        val original = TimeZone.getDefault()
        try {
            val instant = java.time.Instant.parse("2026-11-01T03:00:00Z")
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            val a = c.localDate(instant)
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Pago_Pago"))
            val b = c.localDate(instant)
            assertEquals(a, b)
            assertEquals(LocalDate.of(2026, 11, 1), a) // Kolkata: 08:30 on Nov 1
        } finally { TimeZone.setDefault(original) }
    }

    @Test fun rejectsInvertedRange() {
        assertThrows(IllegalArgumentException::class.java) {
            c.copy(startDate = LocalDate.of(2026, 11, 30), endDate = LocalDate.of(2026, 11, 1))
        }
    }

    @Test fun rejectsOverlongRange() {
        assertThrows(IllegalArgumentException::class.java) {
            c.copy(endDate = c.startDate.plusDays(NnnChallenge.MAX_DAYS.toLong()))
        }
        // exactly MAX_DAYS is fine
        assertEquals(NnnChallenge.MAX_DAYS, c.copy(endDate = c.startDate.plusDays(NnnChallenge.MAX_DAYS - 1L)).totalDays)
    }

    @Test fun customChallengesAreSupported() {
        val custom = c.copy(id = "custom", title = "Dry January", startDate = LocalDate.of(2027, 1, 1), endDate = LocalDate.of(2027, 1, 31))
        assertEquals(31, custom.totalDays)
        assertEquals(1, custom.dayNumberAt(at(KOLKATA, "2027-01-01T08:00:00")))
    }

    @Test fun startingStampsTimeRulesAndConsentVersion() {
        val fresh = NnnChallenge.default2026(KOLKATA, createdAt = at(KOLKATA, "2026-10-30T10:00:00"))
        assertEquals(ChallengeStatus.NOT_STARTED, fresh.status)
        val rules = NnnRuleSet(safeBrowserNavigationFails = true)
        val started = fresh.started(at(KOLKATA, "2026-11-02T09:00:00"), rules, consentVersion = 1)
        assertEquals(ChallengeStatus.ACTIVE, started.status)
        assertEquals(at(KOLKATA, "2026-11-02T09:00:00"), started.startedAt)
        assertEquals(1, started.consentVersion)
        // Starting late never moves the dates or pretends earlier days were monitored.
        assertEquals(LocalDate.of(2026, 11, 1), started.startDate)
    }

    @Test fun completionOnlyAfterEndDate() {
        val during = Fx.engine.completeIfEnded(c, at(KOLKATA, "2026-11-30T23:59:59"))
        assertEquals(ChallengeStatus.ACTIVE, during.status)
        val after = Fx.engine.completeIfEnded(c, at(KOLKATA, "2026-12-01T00:00:00"))
        assertEquals(ChallengeStatus.COMPLETED, after.status)
        assertEquals(at(KOLKATA, "2026-12-01T00:00:00"), after.completedAt)
        // paused / abandoned / not-started challenges are never auto-completed
        assertEquals(ChallengeStatus.ABANDONED,
            Fx.engine.completeIfEnded(c.copy(status = ChallengeStatus.ABANDONED), at(KOLKATA, "2026-12-05T00:00:00")).status)
        assertFalse(Fx.engine.completeIfEnded(c.copy(status = ChallengeStatus.NOT_STARTED), at(KOLKATA, "2026-12-05T00:00:00")).status == ChallengeStatus.COMPLETED)
    }

    @Test fun strictModeNeedsAcknowledgement() {
        assertFalse(NnnRuleSet(deviceGuardEnabled = true, strictDomainGuard = true).strictActive)
        assertTrue(NnnRuleSet(deviceGuardEnabled = true, strictDomainGuard = true, strictAcknowledgedConsentVersion = 1).strictActive)
        assertFalse(NnnRuleSet(deviceGuardEnabled = false, strictDomainGuard = true, strictAcknowledgedConsentVersion = 1).strictActive)
    }
}
