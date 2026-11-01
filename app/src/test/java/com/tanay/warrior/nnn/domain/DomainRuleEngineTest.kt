package com.tanay.warrior.nnn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class DomainRuleEngineTest {

    private fun rule(domain: String, enabled: Boolean = true, confidence: Int = 90) =
        DomainRule(domain, category = "adult", confidence = confidence, enabled = enabled, source = "test", ruleVersion = 1)

    private val engine = DomainRuleEngine(listOf(rule("example.test")))

    private fun matchedDomain(host: String): String? =
        (engine.classify(host) as? DomainClassification.Match)?.rule?.domain

    @Test fun exactHostMatches() = assertEquals("example.test", matchedDomain("example.test"))

    @Test fun trueSubdomainMatches() {
        assertEquals("example.test", matchedDomain("video.example.test"))
        assertEquals("example.test", matchedDomain("a.b.example.test"))
    }

    @Test fun lookalikeSuffixDoesNotMatch() {
        assertEquals(null, matchedDomain("notexample.test"))
        assertEquals(null, matchedDomain("evil-example.test"))
        assertEquals(null, matchedDomain("example.test.evil.test"))
        assertEquals(null, matchedDomain("example.testing"))
    }

    @Test fun normalizesCaseAndTrailingDotBeforeMatching() {
        assertEquals("example.test", matchedDomain("VIDEO.Example.TEST."))
    }

    @Test fun matchesUnicodeHostViaPunycode() {
        val e = DomainRuleEngine(listOf(rule("bücher.test")))
        assertTrue(e.classify("xn--bcher-kva.test") is DomainClassification.Match)
        assertTrue(e.classify("shop.bücher.test") is DomainClassification.Match)
    }

    @Test fun unknownHostIsUnknownNotAdult() {
        assertTrue(engine.classify("weather.test") is DomainClassification.Unknown)
    }

    @Test fun wordInNameAloneDoesNotClassify() {
        // A hostname merely containing an adult-sounding word is never classified by the word.
        val e = DomainRuleEngine(listOf(rule("blocked.test")))
        assertTrue(e.classify("blocked-books.test") is DomainClassification.Unknown)
        assertTrue(e.classify("unblocked.test") is DomainClassification.Unknown)
    }

    @Test fun invalidHostIsInvalid() {
        assertTrue(engine.classify("exa mple.test") is DomainClassification.Invalid)
        assertTrue(engine.classify("example.test/path") is DomainClassification.Invalid)
        assertTrue(engine.classify("") is DomainClassification.Invalid)
    }

    @Test fun disabledRuleNeverMatches() {
        val e = DomainRuleEngine(listOf(rule("example.test", enabled = false)))
        assertTrue(e.classify("example.test") is DomainClassification.Unknown)
    }

    @Test fun mostSpecificRuleWins() {
        val e = DomainRuleEngine(listOf(rule("example.test", confidence = 50), rule("video.example.test", confidence = 99)))
        assertEquals("video.example.test", (e.classify("cdn.video.example.test") as DomainClassification.Match).rule.domain)
        assertEquals("example.test", (e.classify("www.example.test") as DomainClassification.Match).rule.domain)
    }

    @Test fun invalidRuleDomainsAreDroppedAndCounted() {
        val e = DomainRuleEngine(listOf(rule("example.test"), rule("bad domain"), rule("")))
        assertEquals(2, e.droppedRuleCount)
        assertTrue(e.classify("example.test") is DomainClassification.Match)
    }

    // ── allowlist ───────────────────────────────────────────────────────────

    @Test fun allowlistExcludesExactAndSubdomains() {
        val e = DomainRuleEngine(listOf(rule("example.test")), allowlist = setOf("health.example.test"))
        assertTrue(e.classify("health.example.test") is DomainClassification.Allowlisted)
        assertTrue(e.classify("www.health.example.test") is DomainClassification.Allowlisted)
        // sibling is still matched
        assertTrue(e.classify("video.example.test") is DomainClassification.Match)
    }

    @Test fun allowlistedMatchReportsEntryAndHost() {
        val e = DomainRuleEngine(listOf(rule("example.test")), allowlist = setOf("example.test"))
        val c = e.classify("a.example.test") as DomainClassification.Allowlisted
        assertEquals("example.test", c.entry)
        assertEquals("a.example.test", c.host)
    }

    @Test fun allowlistRejectsSingleLabelEntries() {
        val rules = listOf(rule("example.test"))
        assertEquals(AllowlistResult.Rejected(AllowlistRejection.TOO_BROAD),
            DomainRuleEngine.validateAllowlistEntry("test", rules))
    }

    @Test fun allowlistRejectsMalformedEntries() {
        assertEquals(AllowlistResult.Rejected(AllowlistRejection.INVALID_HOST),
            DomainRuleEngine.validateAllowlistEntry("not a host", emptyList()))
    }

    @Test fun allowlistRejectsEntriesThatWouldShadowManyRules() {
        val rules = listOf("a.broad.test", "b.broad.test", "c.broad.test", "d.broad.test").map { rule(it) }
        assertEquals(AllowlistResult.Rejected(AllowlistRejection.SHADOWS_TOO_MANY_RULES),
            DomainRuleEngine.validateAllowlistEntry("broad.test", rules))
        // an entry shadowing only a few rules is fine
        assertEquals(AllowlistResult.Accepted("a.broad.test"),
            DomainRuleEngine.validateAllowlistEntry("A.Broad.Test.", rules))
    }

    @Test fun broadAllowlistCannotSilentlyDisableRules() {
        val rules = listOf("a.broad.test", "b.broad.test", "c.broad.test", "d.broad.test").map { rule(it) }
        val e = DomainRuleEngine(rules, allowlist = setOf("broad.test", "test"))
        assertEquals(setOf("broad.test", "test"), e.rejectedAllowlist)
        assertTrue(e.classify("a.broad.test") is DomainClassification.Match)
        assertFalse(e.classify("d.broad.test") is DomainClassification.Allowlisted)
    }
}
