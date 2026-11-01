package com.tanay.warrior.nnn.domain

import com.tanay.warrior.nnn.domain.Fx.KOLKATA
import com.tanay.warrior.nnn.domain.Fx.at
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NnnActionsTest {
    private val actions = NnnActions(listOf(DomainRule("example.test", "adult", 95, true, "t", 1)))
    private val start = at(KOLKATA, "2026-10-31T21:00:00")
    private val now = at(KOLKATA, "2026-11-10T12:00:00")

    private fun started(rules: NnnRuleSet = NnnRuleSet(), settings: NnnSettings = NnnSettings()): NnnState =
        actions.startDefaultChallenge(NnnState(settings = settings), KOLKATA, rules, consentVersion = 1, now = start)

    private fun apply(s: NnnState, sig: NnnSignal) = actions.apply(s, sig, now)
    private fun snapAt(s: NnnState, t: java.time.Instant) = actions.snapshot(s, t)!!

    // ── starting ─────────────────────────────────────────────────────────────
    @Test fun startingStampsTheChallengeAndKeepsDefaultDates() {
        val c = started().challenge!!
        assertEquals(ChallengeStatus.ACTIVE, c.status)
        assertEquals(start, c.startedAt)
        assertEquals(1, c.consentVersion)
        assertEquals(30, c.totalDays)
    }

    @Test fun startingTwiceDoesNotRestartAnActiveChallenge() {
        val s = started()
        assertSame(s, actions.startDefaultChallenge(s, KOLKATA, NnnRuleSet(), 1, now))
    }

    @Test fun strictRequiresAcknowledgementAndDeviceGuard() {
        val noGuard = started(NnnRuleSet(deviceGuardEnabled = false, strictDomainGuard = true, strictAcknowledgedConsentVersion = 1)).challenge!!.rules
        assertFalse(noGuard.strictDomainGuard); assertNull(noGuard.strictAcknowledgedConsentVersion)
        val ok = started(NnnRuleSet(deviceGuardEnabled = true, strictDomainGuard = true, strictAcknowledgedConsentVersion = 1)).challenge!!.rules
        assertTrue(ok.strictActive)
    }

    @Test fun aLateStartNeverBackfillsEarlierDaysAsMonitored() {
        val late = actions.startDefaultChallenge(NnnState(), KOLKATA, NnnRuleSet(), 1, at(KOLKATA, "2026-11-08T09:00:00"))
        val snap = snapAt(late, now)
        assertEquals(DayStatus.UNLOGGED, snap.days[2].status)
        assertEquals(LocalDateOf(2026, 11, 1), late.challenge!!.startDate)
    }
    private fun LocalDateOf(y: Int, m: Int, d: Int) = java.time.LocalDate.of(y, m, d)

    // ── signals ──────────────────────────────────────────────────────────────
    @Test fun recordedSignalsAreAppendedAndIgnoredOnesLeaveStateUntouched() {
        val s = started()
        val a = apply(s, Fx.manual("m1", KOLKATA, "2026-11-05T22:00:00"))
        assertEquals(1, a.state.events.size)
        val b = apply(a.state, SafeBrowserNavigation("x", at(KOLKATA, "2026-11-05T23:00:00"), "weather.test", true))
        assertSame(a.state, b.state)
        assertTrue(b.result is NnnChallengeEngine.ProcessResult.Ignored)
    }

    @Test fun domainDetailIsNotStoredByDefault() {
        val s = started()
        val e = apply(s, SafeBrowserNavigation("x", at(KOLKATA, "2026-11-05T23:00:00"), "video.example.test", true)).state.events.single()
        assertNull(e.matchedDomain)
        assertEquals(EventOutcome.CONFIRMED_FAIL, e.originalOutcome)
    }

    @Test fun domainDetailIsStoredOnlyWhenTheUserOptedIn() {
        val s = started(settings = NnnSettings(storeDomainDetail = true))
        val e = apply(s, SafeBrowserNavigation("x", at(KOLKATA, "2026-11-05T23:00:00"), "video.example.test", true)).state.events.single()
        assertEquals("example.test", e.matchedDomain)
    }

    @Test fun signalsBeforeAChallengeExistAreIgnored() {
        val r = actions.apply(NnnState(), Fx.manual("m", KOLKATA, "2026-11-05T22:00:00"), now)
        assertEquals(NnnChallengeEngine.ProcessResult.Ignored(IgnoreReason.CHALLENGE_NOT_ACTIVE), r.result)
    }

    @Test fun anNnnFailureTouchesNothingExceptNnnState() {
        // NnnState has no habit/leaderboard fields at all; this pins the shape so adding one is a conscious act.
        val fields = NnnState::class.java.declaredFields.map { it.name }.toSet()
        assertEquals(setOf("challenge", "events", "selfChecks", "segments", "settings", "allowlist"), fields)
    }

    // ── check-in & review → snapshot ─────────────────────────────────────────
    @Test fun checkInThenFailureThenCorrectionRecalculates() {
        var s = started(NnnRuleSet(deviceGuardEnabled = true))
        for (d in 1..9) s = actions.checkIn(s, Fx.day(d), at(KOLKATA, "2026-11-%02dT21:00:00".format(d))).state
        s = apply(s, VpnDomainSignal("v", at(KOLKATA, "2026-11-05T10:00:00"), "example.test")).state
        assertEquals(DayStatus.POSSIBLE_EXPOSURE, snapAt(s, now).days[4].status)
        assertEquals(4, snapAt(s, now).currentStreak)
        s = actions.review(s, "v", ReviewDecision.FALSE_POSITIVE, ReviewReason.NOT_ME, now).state
        assertEquals(9, snapAt(s, now).currentStreak)
        assertEquals(1, s.events.single().reviews.size)
    }

    // ── monitoring states ────────────────────────────────────────────────────
    @Test fun monitorStateChangesAreRecordedAsSegments() {
        var s = started()
        s = actions.setMonitorState(s, MonitorSource.DEVICE_GUARD, MonitorState.ACTIVE, at(KOLKATA, "2026-11-02T08:00:00"))
        s = actions.setMonitorState(s, MonitorSource.DEVICE_GUARD, MonitorState.INTERRUPTED, at(KOLKATA, "2026-11-02T20:00:00"))
        assertEquals(2, s.segments.size)
        assertEquals(at(KOLKATA, "2026-11-02T20:00:00"), s.segments[0].end)
        assertNull(s.segments[1].end)
        assertEquals(MonitorState.INTERRUPTED, s.monitorState(MonitorSource.DEVICE_GUARD))
        assertEquals(MonitorState.DISABLED, s.monitorState(MonitorSource.SAFE_BROWSER))
    }

    @Test fun repeatingTheSameStateDoesNotCreateSegments() {
        var s = started()
        s = actions.setMonitorState(s, MonitorSource.SAFE_BROWSER, MonitorState.ACTIVE, now)
        assertSame(s, actions.setMonitorState(s, MonitorSource.SAFE_BROWSER, MonitorState.ACTIVE, now.plusSeconds(60)))
    }

    @Test fun disablingAnUnseenSourceAddsNothing() {
        val s = started()
        assertSame(s, actions.setMonitorState(s, MonitorSource.DEVICE_GUARD, MonitorState.DISABLED, now))
    }

    @Test fun stopAllMonitoringClosesSegmentsKeepsTheGapAndDisablesDeviceGuardSignals() {
        var s = started(NnnRuleSet(deviceGuardEnabled = true, strictDomainGuard = true, strictAcknowledgedConsentVersion = 1))
        s = actions.setMonitorState(s, MonitorSource.DEVICE_GUARD, MonitorState.ACTIVE, at(KOLKATA, "2026-11-01T00:00:00"))
        s = actions.setMonitorState(s, MonitorSource.SAFE_BROWSER, MonitorState.ACTIVE, at(KOLKATA, "2026-11-01T00:00:00"))
        s = actions.stopAllMonitoring(s, at(KOLKATA, "2026-11-04T12:00:00"))
        assertEquals(MonitorState.DISABLED, s.monitorState(MonitorSource.DEVICE_GUARD))
        assertEquals(MonitorState.DISABLED, s.monitorState(MonitorSource.SAFE_BROWSER))
        assertFalse(s.challenge!!.rules.deviceGuardEnabled)
        assertFalse(s.challenge!!.rules.strictActive)
        // the stopped period is a visible gap, not a clean day
        val snap = snapAt(s, now)
        assertEquals(DayStatus.MONITORED_CLEAN, snap.days[0].status)
        assertEquals(DayStatus.MONITORED_CLEAN, snap.days[1].status)
        assertEquals(DayStatus.PARTIAL_COVERAGE, snap.days[3].status)    // Nov 4: half covered
        assertEquals(DayStatus.UNMONITORED, snap.days[5].status)         // Nov 6: guard was on earlier, now off
        // and later device-guard signals are ignored
        val r = apply(s, VpnDomainSignal("late", at(KOLKATA, "2026-11-06T10:00:00"), "example.test"))
        assertEquals(NnnChallengeEngine.ProcessResult.Ignored(IgnoreReason.SOURCE_DISABLED), r.result)
    }

    // ── rules & settings edits ───────────────────────────────────────────────
    @Test fun updateRulesTakesEffectAndKeepsStrictSanitized() {
        val s = started()
        val off = actions.updateRules(s, NnnRuleSet(safeBrowserNavigationFails = false))
        assertEquals(IgnoreReason.RULE_DISABLED, (apply(off, SafeBrowserNavigation("x", at(KOLKATA, "2026-11-05T23:00:00"), "example.test", true)).result
            as NnnChallengeEngine.ProcessResult.Ignored).reason)
        val bad = actions.updateRules(s, NnnRuleSet(deviceGuardEnabled = false, strictDomainGuard = true, strictAcknowledgedConsentVersion = 1)).challenge!!.rules
        assertFalse(bad.strictDomainGuard); assertNull(bad.strictAcknowledgedConsentVersion)
        assertNull(actions.updateRules(NnnState(), NnnRuleSet()).challenge)
    }

    @Test fun focusMessageIsTrimmedAndCapped() {
        val long = "x".repeat(500)
        assertEquals(NnnSettings.MAX_FOCUS_MESSAGE, actions.updateSettings(NnnState(), NnnSettings(focusMessage = "  $long ")).settings.focusMessage.length)
        assertEquals("keep going", actions.updateSettings(NnnState(), NnnSettings(focusMessage = "  keep going ")).settings.focusMessage)
    }

    // ── allowlist ────────────────────────────────────────────────────────────
    @Test fun allowlistEntriesAreValidatedNormalizedAndUsed() {
        var s = started()
        val ok = actions.addAllowlistEntry(s, "HEALTH.Example.test.")
        assertEquals(AllowlistResult.Accepted("health.example.test"), ok.result)
        s = ok.state
        assertEquals(setOf("health.example.test"), s.allowlist)
        assertEquals(NnnChallengeEngine.ProcessResult.Ignored(IgnoreReason.ALLOWLISTED),
            apply(s, SafeBrowserNavigation("x", at(KOLKATA, "2026-11-05T23:00:00"), "health.example.test", true)).result)
        val bad = actions.addAllowlistEntry(s, "test")
        assertEquals(AllowlistResult.Rejected(AllowlistRejection.TOO_BROAD), bad.result)
        assertSame(s, bad.state)
        assertTrue(actions.removeAllowlistEntry(s, "health.example.test").allowlist.isEmpty())
    }

    // ── retention / deletion ─────────────────────────────────────────────────
    private fun withEvents(): NnnState {
        var s = started(settings = NnnSettings(storeDomainDetail = true, retentionDays = 30))
        s = apply(s, SafeBrowserNavigation("old", at(KOLKATA, "2026-11-02T10:00:00"), "example.test", true)).state
        return apply(s, SafeBrowserNavigation("new", at(KOLKATA, "2026-11-09T10:00:00"), "example.test", true)).state
    }

    @Test fun retentionRedactsOldDomainDetailButNeverChangesTheOutcomeHistory() {
        val s = withEvents()
        val later = at(KOLKATA, "2026-12-15T10:00:00")           // 43 days after "old", 36 after "new"
        val cleaned = actions.redactExpiredDetail(s, later)
        assertTrue(cleaned.events.all { it.matchedDomain == null && it.category == null && !it.dedupKey.contains("example") })
        assertEquals(s.events.map { it.originalOutcome }, cleaned.events.map { it.originalOutcome })
        assertEquals(snapAt(s, later).failedDays, snapAt(cleaned, later).failedDays)
    }

    @Test fun retentionKeepsDetailInsideTheWindow() {
        val s = withEvents()
        val cleaned = actions.redactExpiredDetail(s, at(KOLKATA, "2026-11-20T10:00:00"))   // old = 18d, new = 11d
        assertTrue(cleaned.events.all { it.matchedDomain == "example.test" })
    }

    @Test fun deleteActivityRemovesRecordsButKeepsChallengeSettingsAndAllowlist() {
        var s = withEvents()
        s = actions.checkIn(s, Fx.day(3), now).state
        s = actions.setMonitorState(s, MonitorSource.SAFE_BROWSER, MonitorState.ACTIVE, now)
        s = actions.addAllowlistEntry(s, "health.example.test").state
        val d = actions.deleteActivity(s)
        assertTrue(d.events.isEmpty() && d.selfChecks.isEmpty() && d.segments.isEmpty())
        assertEquals(s.challenge, d.challenge); assertEquals(s.settings, d.settings); assertEquals(s.allowlist, d.allowlist)
    }

    @Test fun resetEverythingReturnsToANeverSetUpState() {
        assertEquals(NnnState(), actions.resetEverything(withEvents()))
    }

    // ── settings & lifecycle ─────────────────────────────────────────────────
    @Test fun retentionIsClamped() {
        assertEquals(NnnSettings.MIN_RETENTION_DAYS, actions.updateSettings(NnnState(), NnnSettings(retentionDays = 1)).settings.retentionDays)
        assertEquals(NnnSettings.MAX_RETENTION_DAYS, actions.updateSettings(NnnState(), NnnSettings(retentionDays = 9999)).settings.retentionDays)
    }

    @Test fun tickCompletesAnEndedChallengeOnly() {
        val s = started()
        assertEquals(ChallengeStatus.ACTIVE, actions.tick(s, at(KOLKATA, "2026-11-30T23:00:00")).challenge!!.status)
        assertEquals(ChallengeStatus.COMPLETED, actions.tick(s, at(KOLKATA, "2026-12-01T00:00:01")).challenge!!.status)
        assertNull(actions.tick(NnnState(), now).challenge)
    }

    @Test fun pausingStopsRecordingAndResumingRestoresIt() {
        var s = started()
        s = actions.setChallengeStatus(s, ChallengeStatus.PAUSED)
        assertTrue(apply(s, Fx.manual("m", KOLKATA, "2026-11-05T22:00:00")).result is NnnChallengeEngine.ProcessResult.Ignored)
        s = actions.setChallengeStatus(s, ChallengeStatus.ACTIVE)
        assertTrue(apply(s, Fx.manual("m", KOLKATA, "2026-11-05T22:00:00")).result is NnnChallengeEngine.ProcessResult.Recorded)
    }
}
