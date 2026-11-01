package com.tanay.warrior.nnn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeBrowserNavigationPolicyTest {
    private val policy = SafeBrowserNavigationPolicy(DomainRuleEngine(
        rules = listOf(DomainRule("example.test", "adult", 95, true, "t", 1), DomainRule("bücher.test", "adult", 95, true, "t", 1)),
        allowlist = setOf("health.example.test")
    ))

    private fun main(url: String) = policy.evaluate(url, isMainFrame = true)
    private fun blocked(url: String) = main(url) is NavigationDecision.Block
    private fun allowed(url: String) = main(url) == NavigationDecision.Allow

    @Test fun matchedMainFrameNavigationIsBlockedWithTheRuleDomain() {
        val d = main("https://video.example.test/watch") as NavigationDecision.Block
        assertEquals("example.test", d.ruleDomain)
        assertEquals("video.example.test", d.host)
    }

    @Test fun schemeHostCaseTrailingDotAndPortAreNormalized() {
        assertTrue(blocked("HTTPS://WWW.Example.TEST./"))
        assertTrue(blocked("http://example.test:8080/x"))
    }

    @Test fun matchesUnicodeHosts() = assertTrue(blocked("https://bücher.test/"))

    @Test fun pathsQueriesAndFragmentsNeverMatch() {
        assertTrue(allowed("https://search.test/?q=example.test"))
        assertTrue(allowed("https://other.test/example.test"))
        assertTrue(allowed("https://other.test/#example.test"))
    }

    @Test fun lookalikeHostsAreAllowed() {
        assertTrue(allowed("https://notexample.test/"))
        assertTrue(allowed("https://example.test.evil.test/"))
        assertTrue(allowed("https://evil-example.test/"))
    }

    @Test fun userinfoCannotSpoofTheHost() {
        assertTrue(blocked("https://harmless.test@example.test/"))     // real host is example.test
        assertTrue(allowed("https://example.test@harmless.test/"))     // real host is harmless.test
    }

    @Test fun backslashIsTreatedLikeASlashLikeBrowsersDo() = assertTrue(blocked("https://example.test\\@harmless.test/"))

    @Test fun subframesAndSubresourcesNeverBlockOrRecord() {
        assertEquals(NavigationDecision.Allow, policy.evaluate("https://example.test/ad.js", isMainFrame = false))
        assertEquals(NavigationDecision.Allow, policy.evaluate("https://example.test/frame", isMainFrame = false))
    }

    @Test fun unknownAndAllowlistedHostsAreAllowed() {
        assertTrue(allowed("https://weather.test/"))
        assertTrue(allowed("https://health.example.test/"))
        assertTrue(allowed("https://www.health.example.test/"))
        assertTrue(blocked("https://video.example.test/"))   // sibling still blocked
    }

    @Test fun malformedAddressesAreInvalidNotNavigated() {
        for (bad in listOf("", "   ", "https://", "https:///path", "https://exa mple.test/", "https://[::1]/",
            "https://example.test:abc/", "https://%65xample.test/", "https://-bad.test/", "not a url")) {
            assertEquals("'$bad'", NavigationDecision.InvalidAddress, main(bad))
        }
    }

    @Test fun nonWebSchemesAreNeverLoaded() {
        for ((url, scheme) in listOf("javascript:alert(1)" to "javascript", "file:///etc/hosts" to "file",
            "intent://x#Intent;end" to "intent", "mailto:a@b.test" to "mailto", "data:text/html,hi" to "data",
            "content://x/y" to "content", "ftp://example.test/" to "ftp", "JAVASCRIPT:1" to "javascript")) {
            assertEquals(url, NavigationDecision.UnsupportedScheme(scheme), main(url))
        }
    }

    @Test fun aboutBlankIsAllowed() = assertTrue(allowed("about:blank"))

    @Test fun blockDecisionNeverPrintsTheHost() {
        val d = main("https://secret-sub.example.test/") as NavigationDecision.Block
        assertFalse(d.toString().contains("secret-sub"))
    }

    // ── address-bar input ────────────────────────────────────────────────────
    @Test fun bareHostBecomesHttps() {
        assertEquals(InputResolution.Navigate("https://example.test"), policy.resolveInput("  example.test "))
        assertEquals(InputResolution.Navigate("https://example.test/a?b=c"), policy.resolveInput("example.test/a?b=c"))
    }

    @Test fun fullUrlsAreKeptAsTyped() =
        assertEquals(InputResolution.Navigate("https://example.test/a?b=c#d"), policy.resolveInput("https://example.test/a?b=c#d"))

    @Test fun plainHttpIsOnlyUsedIfTheUserTypesIt() =
        assertEquals(InputResolution.Navigate("http://example.test/"), policy.resolveInput("http://example.test/"))

    @Test fun everythingElseIsASearchAndIsUrlEncoded() {
        val r = policy.resolveInput("hello world & more") as InputResolution.Search
        assertEquals("https://duckduckgo.com/?q=hello+world+%26+more", r.url)
        assertTrue(policy.resolveInput("localhost") is InputResolution.Search)   // no dot -> not a site
    }

    @Test fun dangerousInputBecomesASearchNeverANavigation() {
        for (s in listOf("javascript:alert(1)", "file:///etc/hosts", "intent://x", "data:text/html,hi")) {
            assertTrue(s, policy.resolveInput(s) is InputResolution.Search)
        }
    }
}
