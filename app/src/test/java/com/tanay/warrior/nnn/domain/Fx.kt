package com.tanay.warrior.nnn.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Shared test fixtures. Only reserved `.test` domains are used. */
internal object Fx {
    val KOLKATA: ZoneId = ZoneId.of("Asia/Kolkata")
    val NEW_YORK: ZoneId = ZoneId.of("America/New_York")

    val domains = DomainRuleEngine(listOf(DomainRule("example.test", "adult", 95, true, "test", 1)))
    val engine = NnnChallengeEngine(domains)

    fun at(zone: ZoneId, local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    fun challenge(
        zone: ZoneId = KOLKATA,
        rules: NnnRuleSet = NnnRuleSet(),
        status: ChallengeStatus = ChallengeStatus.ACTIVE,
        startedAt: Instant? = at(zone, "2026-10-31T20:00:00")
    ): NnnChallenge = NnnChallenge.default2026(zone, createdAt = at(zone, "2026-10-30T10:00:00"))
        .copy(status = status, startedAt = startedAt, rules = rules)

    fun day(n: Int): LocalDate = LocalDate.of(2026, 11, n)
    fun check(n: Int) = SelfCheck(day(n), Instant.parse("2026-11-01T00:00:00Z"))
    fun checks(range: IntRange) = range.map { check(it) }

    /** Run signals through the engine exactly as production does and collect stored events. */
    fun record(challenge: NnnChallenge, vararg signals: NnnSignal): List<NnnEvent> {
        var events = emptyList<NnnEvent>()
        for (s in signals) {
            val r = engine.process(challenge, s, events)
            if (r is NnnChallengeEngine.ProcessResult.Recorded) events = events + r.event
        }
        return events
    }

    fun manual(id: String, zone: ZoneId, local: String) = ManualReport(id, at(zone, local))
}
