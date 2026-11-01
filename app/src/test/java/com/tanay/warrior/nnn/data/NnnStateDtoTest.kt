package com.tanay.warrior.nnn.data

import com.tanay.warrior.nnn.domain.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class NnnStateDtoTest {
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val t0 = Instant.parse("2026-11-05T04:30:00Z")

    private fun fullState(): NnnState {
        val challenge = NnnChallenge.default2026(kolkata, t0).started(
            t0, NnnRuleSet(true, true, true, 1), consentVersion = 1).copy(status = ChallengeStatus.PAUSED, completedAt = t0.plusSeconds(5))
        val event = NnnEvent("e1", challenge.id, t0, LocalDate.of(2026, 11, 5), EventSource.LOCAL_VPN, EventOutcome.POSSIBLE_EXPOSURE,
            "example.test", "adult", 95, "LOCAL_VPN|example.test",
            listOf(ReviewEntry(ReviewDecision.FALSE_POSITIVE, ReviewReason.NOT_ME, t0.plusSeconds(60)), ReviewEntry(ReviewDecision.CONFIRMED, ReviewReason.CONFIRMED_VISIT, t0.plusSeconds(120))))
        return NnnState(
            challenge = challenge, events = listOf(event, event.copy(id = "e2", matchedDomain = null, category = null, confidence = null, reviews = emptyList())),
            selfChecks = listOf(SelfCheck(LocalDate.of(2026, 11, 4), t0)),
            segments = listOf(MonitoringSegment(MonitorSource.DEVICE_GUARD, MonitorState.ACTIVE, t0, t0.plusSeconds(10)), MonitoringSegment(MonitorSource.SAFE_BROWSER, MonitorState.INTERRUPTED, t0, null)),
            settings = NnnSettings(true, 120, true, 22 * 60, 6 * 60, "breathe"), allowlist = setOf("health.example.test")
        )
    }

    @Test fun everyFieldSurvivesARoundTrip() {
        val s = fullState()
        assertEquals(s, s.toDto().toDomain())
    }

    @Test fun emptyStateRoundTrips() = assertEquals(NnnState(), NnnState().toDto().toDomain())

    @Test fun unknownEnumValuesDropTheRecordInsteadOfCrashing() {
        val dto = fullState().toDto()
        val broken = dto.copy(events = dto.events.map { it.copy(source = "FROM_THE_FUTURE") }, segments = dto.segments.map { it.copy(state = "???") })
        val s = broken.toDomain()
        assertEquals(0, s.events.size); assertEquals(0, s.segments.size)
        assertEquals(dto.toDomain().challenge, s.challenge)
    }

    @Test fun anInvalidChallengeBecomesNoChallengeNotACrash() {
        val dto = fullState().toDto()
        val bad = dto.copy(challenge = dto.challenge!!.copy(startDate = "2026-11-30", endDate = "2026-11-01"))
        assertNull(bad.toDomain().challenge)
    }

    @Test fun unknownZoneFallsBackToSystemDefaultRatherThanCrashing() {
        val dto = fullState().toDto()
        val s = dto.copy(challenge = dto.challenge!!.copy(zoneId = "Mars/Olympus")).toDomain()
        assertEquals(ZoneId.systemDefault(), s.challenge!!.zoneId)
    }

    @Test fun dtoVersionIsWritten() = assertEquals(NnnStateDto.CURRENT_VERSION, NnnState().toDto().version)
}
