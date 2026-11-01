package com.tanay.warrior.nnn.domain

import com.tanay.warrior.nnn.domain.Fx.KOLKATA
import com.tanay.warrior.nnn.domain.Fx.at
import com.tanay.warrior.nnn.domain.NnnChallengeEngine.ProcessResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NnnEngineTest {
    private val rules = NnnRuleSet(deviceGuardEnabled = true)
    private val c = Fx.challenge(rules = rules)

    private fun sb(id: String, host: String, local: String = "2026-11-05T10:00:00", main: Boolean = true) =
        SafeBrowserNavigation(id, at(KOLKATA, local), host, main)
    private fun vpn(id: String, host: String = "example.test", local: String = "2026-11-05T10:00:00") =
        VpnDomainSignal(id, at(KOLKATA, local), host)
    private fun run(s: NnnSignal, existing: List<NnnEvent> = emptyList(), ch: NnnChallenge = c, engine: NnnChallengeEngine = Fx.engine) =
        engine.process(ch, s, existing)
    private fun ev(r: ProcessResult) = (r as ProcessResult.Recorded).event
    private fun why(r: ProcessResult) = (r as ProcessResult.Ignored).reason

    // ── Safe Browser ─────────────────────────────────────────────────────────
    @Test fun safeBrowserMainFrameAdultNavigationIsConfirmedFail() {
        val e = ev(run(sb("a", "video.example.test")))
        assertEquals(EventOutcome.CONFIRMED_FAIL, e.originalOutcome)
        assertEquals(EventSource.SAFE_BROWSER, e.source)
        assertEquals(Fx.day(5), e.challengeDate)
        assertEquals(EventOutcome.CONFIRMED_FAIL, e.effectiveOutcome)
    }

    @Test fun storesTheRuleDomainNotTheVisitedHost() {
        val e = ev(run(sb("a", "private-subdomain.example.test")))
        assertEquals("example.test", e.matchedDomain)
        assertFalse(e.toString().contains("private-subdomain"))
    }

    @Test fun domainDetailCanBeTurnedOff() {
        val e = ev(Fx.engine.process(c, sb("a", "example.test"), emptyList(), storeDomainDetail = false))
        assertNull(e.matchedDomain)
        assertEquals(EventOutcome.CONFIRMED_FAIL, e.originalOutcome) // decision unaffected
    }

    @Test fun subFrameRequestsNeverTrigger() =
        assertEquals(IgnoreReason.NOT_MAIN_FRAME, why(run(sb("a", "example.test", main = false))))

    @Test fun unknownHostIsIgnoredNotAdult() =
        assertEquals(IgnoreReason.NOT_ADULT_DOMAIN, why(run(sb("a", "weather.test"))))

    @Test fun lookalikeHostIsIgnored() =
        assertEquals(IgnoreReason.NOT_ADULT_DOMAIN, why(run(sb("a", "notexample.test"))))

    @Test fun invalidHostIsIgnored() =
        assertEquals(IgnoreReason.INVALID_HOST, why(run(sb("a", "exa mple.test"))))

    @Test fun allowlistedHostIsIgnored() {
        val engine = NnnChallengeEngine(DomainRuleEngine(listOf(DomainRule("example.test", "adult", 95, true, "t", 1)),
            allowlist = setOf("health.example.test")))
        assertEquals(IgnoreReason.ALLOWLISTED, why(run(sb("a", "health.example.test"), engine = engine)))
        assertTrue(run(sb("b", "video.example.test"), engine = engine) is ProcessResult.Recorded)
    }

    @Test fun safeBrowserRuleCanBeDisabledByTheUser() {
        val ch = Fx.challenge(rules = NnnRuleSet(safeBrowserNavigationFails = false))
        assertEquals(IgnoreReason.RULE_DISABLED, why(run(sb("a", "example.test"), ch = ch)))
    }

    // ── Device guard (VPN) ───────────────────────────────────────────────────
    @Test fun vpnSignalIsPossibleExposureByDefault() {
        val e = ev(run(vpn("v1")))
        assertEquals(EventOutcome.POSSIBLE_EXPOSURE, e.originalOutcome)
        assertTrue(e.isPendingReview)
        assertFalse(e.countsAsConfirmedFailure)
    }

    @Test fun vpnSignalFailsOnlyInAcknowledgedStrictMode() {
        val strict = Fx.challenge(rules = NnnRuleSet(deviceGuardEnabled = true, strictDomainGuard = true, strictAcknowledgedConsentVersion = 1))
        assertEquals(EventOutcome.CONFIRMED_FAIL, ev(run(vpn("v1"), ch = strict)).originalOutcome)
        val unacked = Fx.challenge(rules = NnnRuleSet(deviceGuardEnabled = true, strictDomainGuard = true))
        assertEquals(EventOutcome.POSSIBLE_EXPOSURE, ev(run(vpn("v1"), ch = unacked)).originalOutcome)
    }

    @Test fun vpnSignalIgnoredWhenDeviceGuardNotEnabled() {
        val off = Fx.challenge(rules = NnnRuleSet(deviceGuardEnabled = false))
        assertEquals(IgnoreReason.SOURCE_DISABLED, why(run(vpn("v1"), ch = off)))
    }

    @Test fun vpnUnknownHostIgnored() = assertEquals(IgnoreReason.NOT_ADULT_DOMAIN, why(run(vpn("v1", host = "cdn.weather.test"))))

    // ── Manual ───────────────────────────────────────────────────────────────
    @Test fun manualReportAlwaysRecordsConfirmedFail() {
        val e = ev(run(Fx.manual("m1", KOLKATA, "2026-11-05T22:00:00")))
        assertEquals(EventOutcome.CONFIRMED_FAIL, e.originalOutcome)
        assertEquals(EventSource.MANUAL, e.source)
        assertNull(e.matchedDomain)
    }

    @Test fun manualDoubleTapIsOneIncidentButLaterReportIsAnother() {
        val first = Fx.manual("m1", KOLKATA, "2026-11-05T22:00:00")
        val events = Fx.record(c, first, Fx.manual("m2", KOLKATA, "2026-11-05T22:00:05"), Fx.manual("m3", KOLKATA, "2026-11-05T22:05:00"))
        assertEquals(listOf("m1", "m3"), events.map { it.id })
    }

    // ── Scope guards ─────────────────────────────────────────────────────────
    @Test fun inactiveChallengesIgnoreEverything() {
        for (st in listOf(ChallengeStatus.NOT_STARTED, ChallengeStatus.PAUSED, ChallengeStatus.COMPLETED, ChallengeStatus.ABANDONED)) {
            assertEquals(IgnoreReason.CHALLENGE_NOT_ACTIVE, why(run(Fx.manual("m", KOLKATA, "2026-11-05T22:00:00"), ch = Fx.challenge(status = st))))
        }
    }

    @Test fun eventsOutsideTheWindowAreIgnored() {
        assertEquals(IgnoreReason.OUT_OF_WINDOW, why(run(Fx.manual("a", KOLKATA, "2026-10-31T23:59:59"))))
        assertEquals(IgnoreReason.OUT_OF_WINDOW, why(run(Fx.manual("b", KOLKATA, "2026-12-01T00:00:00"))))
        assertTrue(run(Fx.manual("c", KOLKATA, "2026-11-01T00:00:00")) is ProcessResult.Recorded)
        assertTrue(run(Fx.manual("d", KOLKATA, "2026-11-30T23:59:59")) is ProcessResult.Recorded)
    }

    @Test fun eventsBeforeTheChallengeWasStartedAreIgnored() {
        val late = Fx.challenge(startedAt = at(KOLKATA, "2026-11-05T09:00:00"))
        assertEquals(IgnoreReason.BEFORE_START, why(run(Fx.manual("a", KOLKATA, "2026-11-05T08:00:00"), ch = late)))
    }

    // ── Idempotency / dedup ──────────────────────────────────────────────────
    @Test fun sameEventIdIsIdempotent() {
        val first = ev(run(vpn("same")))
        assertEquals(IgnoreReason.DUPLICATE, why(run(vpn("same", local = "2026-11-06T10:00:00"), existing = listOf(first))))
    }

    @Test fun redirectOrRetryWithinWindowIsOneIncident() {
        val events = Fx.record(c, sb("a", "example.test", "2026-11-05T10:00:00"),
            sb("b", "m.example.test", "2026-11-05T10:00:10"), sb("c", "www.example.test", "2026-11-05T10:00:29"))
        assertEquals(1, events.size)
    }

    @Test fun separateIncidentsAfterWindowArePreserved() {
        val events = Fx.record(c, sb("a", "example.test", "2026-11-05T10:00:00"), sb("b", "example.test", "2026-11-05T10:00:31"))
        assertEquals(2, events.size)
    }

    @Test fun dnsRetryStormCollapsesToOneEvent() {
        val storm = (0 until 50).map { vpn("v$it", local = "2026-11-05T10:%02d:%02d".format(0, it)) }
        assertEquals(1, Fx.record(c, *storm.toTypedArray()).size)
    }

    @Test fun vpnIncidentsMoreThanWindowApartAreSeparate() {
        val events = Fx.record(c, vpn("a", local = "2026-11-05T10:00:00"), vpn("b", local = "2026-11-05T10:06:00"))
        assertEquals(2, events.size)
    }

    @Test fun outOfOrderDeliveryStillDeduplicates() {
        val events = Fx.record(c, vpn("late", local = "2026-11-05T10:00:20"), vpn("early", local = "2026-11-05T10:00:00"))
        assertEquals(1, events.size)
    }

    @Test fun outOfOrderDeliveryBeyondTheWindowIsStillASeparateIncident() {
        val events = Fx.record(c, vpn("late", local = "2026-11-05T10:06:00"), vpn("early", local = "2026-11-05T10:00:00"))
        assertEquals(2, events.size)
    }

    @Test fun dedupKeyCarriesNoDomainWhenDetailRetentionIsOff() {
        val e = ev(Fx.engine.process(c, sb("a", "example.test"), emptyList(), storeDomainDetail = false))
        assertFalse(e.dedupKey.contains("example"))
        assertNull(e.category); assertNull(e.confidence)
    }

    @Test fun differentSourcesAndDomainsAreNeverMerged() {
        val two = NnnChallengeEngine(DomainRuleEngine(listOf(
            DomainRule("example.test", "adult", 95, true, "t", 1), DomainRule("other.test", "adult", 95, true, "t", 1))))
        var events = emptyList<NnnEvent>()
        for (s in listOf(sb("a", "example.test"), vpn("b", "example.test"), sb("c", "other.test"))) {
            val r = two.process(c, s, events); if (r is ProcessResult.Recorded) events = events + r.event
        }
        assertEquals(3, events.size)
    }

    // ── Review / correction ──────────────────────────────────────────────────
    private val possible get() = ev(run(vpn("v1")))

    @Test fun falsePositiveReviewKeepsTheAuditTrail() {
        val r = Fx.engine.review(listOf(possible), "v1", ReviewDecision.FALSE_POSITIVE, ReviewReason.BACKGROUND_TRAFFIC, at(KOLKATA, "2026-11-06T09:00:00"))
        val e = (r as NnnChallengeEngine.ReviewResult.Updated).event
        assertEquals(EventOutcome.POSSIBLE_EXPOSURE, e.originalOutcome)           // original preserved
        assertEquals(EventOutcome.REVIEWED_FALSE_POSITIVE, e.effectiveOutcome)
        assertEquals(1, e.reviews.size)
        assertEquals(ReviewReason.BACKGROUND_TRAFFIC, e.reviews.single().reason)
        assertFalse(e.isPendingReview)
        assertFalse(e.countsAsConfirmedFailure)
    }

    @Test fun confirmingAPossibleExposureMakesItAFailure() {
        val e = (Fx.engine.review(listOf(possible), "v1", ReviewDecision.CONFIRMED, ReviewReason.CONFIRMED_VISIT, at(KOLKATA, "2026-11-06T09:00:00"))
            as NnnChallengeEngine.ReviewResult.Updated).event
        assertEquals(EventOutcome.REVIEWED_CONFIRMED, e.effectiveOutcome)
        assertTrue(e.countsAsConfirmedFailure)
    }

    @Test fun correctionsCanBeReversedAndEveryStepIsKept() {
        val a = Fx.engine.review(listOf(possible), "v1", ReviewDecision.FALSE_POSITIVE, ReviewReason.NOT_ME, at(KOLKATA, "2026-11-06T09:00:00")) as NnnChallengeEngine.ReviewResult.Updated
        val b = Fx.engine.review(a.events, "v1", ReviewDecision.CONFIRMED, ReviewReason.CONFIRMED_VISIT, at(KOLKATA, "2026-11-07T09:00:00")) as NnnChallengeEngine.ReviewResult.Updated
        assertEquals(2, b.event.reviews.size)
        assertEquals(EventOutcome.REVIEWED_CONFIRMED, b.event.effectiveOutcome)
    }

    @Test fun anAutomaticFailureCanBeCorrectedToo() {
        val auto = ev(run(sb("s1", "example.test")))
        val e = (Fx.engine.review(listOf(auto), "s1", ReviewDecision.FALSE_POSITIVE, ReviewReason.ALLOWED_SITE, at(KOLKATA, "2026-11-06T09:00:00"))
            as NnnChallengeEngine.ReviewResult.Updated).event
        assertEquals(EventOutcome.REVIEWED_FALSE_POSITIVE, e.effectiveOutcome)
    }

    @Test fun aMistakenManualReportCanBeCorrected() {
        val m = ev(run(Fx.manual("m1", KOLKATA, "2026-11-05T22:00:00")))
        val r = Fx.engine.review(listOf(m), "m1", ReviewDecision.FALSE_POSITIVE, ReviewReason.MISTAKEN_REPORT, at(KOLKATA, "2026-11-05T22:01:00"))
        assertEquals(EventOutcome.REVIEWED_FALSE_POSITIVE, (r as NnnChallengeEngine.ReviewResult.Updated).event.effectiveOutcome)
    }

    @Test fun reviewRejectsInconsistentReasonsAndUnknownEvents() {
        val e = possible
        assertEquals(NnnChallengeEngine.ReviewResult.InvalidReason,
            Fx.engine.review(listOf(e), "v1", ReviewDecision.FALSE_POSITIVE, ReviewReason.CONFIRMED_VISIT, at(KOLKATA, "2026-11-06T09:00:00")))
        assertEquals(NnnChallengeEngine.ReviewResult.NotFound,
            Fx.engine.review(listOf(e), "nope", ReviewDecision.CONFIRMED, ReviewReason.OTHER, at(KOLKATA, "2026-11-06T09:00:00")))
    }

    @Test fun reviewDoesNotMutateTheInputList() {
        val original = listOf(possible)
        Fx.engine.review(original, "v1", ReviewDecision.CONFIRMED, ReviewReason.OTHER, at(KOLKATA, "2026-11-06T09:00:00"))
        assertEquals(0, original.single().reviews.size)
    }

    // ── Self check-in ────────────────────────────────────────────────────────
    private val now = at(KOLKATA, "2026-11-10T12:00:00")
    private fun selfCheck(day: Int, existing: Collection<SelfCheck> = emptyList(), ch: NnnChallenge = c) =
        Fx.engine.recordSelfCheck(ch, existing, Fx.day(day), now)

    @Test fun selfCheckForTodayAndPastDays() {
        assertTrue(selfCheck(10) is NnnChallengeEngine.SelfCheckResult.Recorded)
        assertTrue(selfCheck(3) is NnnChallengeEngine.SelfCheckResult.Recorded)
        assertEquals(now, (selfCheck(10) as NnnChallengeEngine.SelfCheckResult.Recorded).check.at)
    }

    @Test fun selfCheckRefusesFutureAndOutOfWindowDays() {
        assertEquals(NnnChallengeEngine.SelfCheckResult.NotAllowed(SelfCheckDenied.FUTURE_DAY), selfCheck(11))
        assertEquals(NnnChallengeEngine.SelfCheckResult.NotAllowed(SelfCheckDenied.OUTSIDE_WINDOW),
            Fx.engine.recordSelfCheck(c, emptyList(), java.time.LocalDate.of(2026, 10, 31), now))
        assertEquals(NnnChallengeEngine.SelfCheckResult.NotAllowed(SelfCheckDenied.CHALLENGE_NOT_ACTIVE),
            selfCheck(5, ch = Fx.challenge(status = ChallengeStatus.PAUSED)))
    }

    @Test fun selfCheckIsIdempotent() {
        val first = (selfCheck(4) as NnnChallengeEngine.SelfCheckResult.Recorded).check
        val again = selfCheck(4, existing = listOf(first))
        assertEquals(NnnChallengeEngine.SelfCheckResult.AlreadyRecorded(first), again)
    }
}
