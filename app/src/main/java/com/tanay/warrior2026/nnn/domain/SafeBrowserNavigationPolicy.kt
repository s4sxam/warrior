package com.tanay.warrior.nnn.domain

import java.net.URLEncoder

sealed interface NavigationDecision {
    /** Let the WebView load it (also used for every sub-resource / sub-frame). */
    data object Allow : NavigationDecision

    /** Do not load. [host] is for the in-memory event signal only; its toString is redacted. */
    data class Block(val host: String, val ruleDomain: String) : NavigationDecision {
        override fun toString() = "Block(rule=$ruleDomain)"
    }

    /** Malformed address: do not load, do not record. */
    data object InvalidAddress : NavigationDecision

    /** Non-web scheme (javascript:, file:, intent:, …): never loaded inside the WebView. */
    data class UnsupportedScheme(val scheme: String) : NavigationDecision
}

sealed interface InputResolution {
    data class Navigate(val url: String) : InputResolution
    data class Search(val url: String) : InputResolution
}

/**
 * Pure URL → decision logic for WARRIOR Safe Browser. Only the HOST of a main-frame navigation is ever
 * examined; paths, queries, fragments, titles and page content are never matched, stored or logged.
 * The Android WebViewClient is a thin shell around this class so the rules are unit-testable.
 */
class SafeBrowserNavigationPolicy(private val domains: DomainRuleEngine) {

    fun evaluate(rawUrl: String, isMainFrame: Boolean): NavigationDecision {
        if (!isMainFrame) return NavigationDecision.Allow          // never even parse sub-resources
        val url = rawUrl.trim()
        if (url.isEmpty()) return NavigationDecision.InvalidAddress

        val schemeEnd = url.indexOf(':')
        if (schemeEnd <= 0) return NavigationDecision.InvalidAddress
        val scheme = url.substring(0, schemeEnd).lowercase()
        if (!SCHEME.matches(scheme)) return NavigationDecision.InvalidAddress
        if (scheme == "about") return if (url.lowercase() == "about:blank") NavigationDecision.Allow else NavigationDecision.UnsupportedScheme(scheme)
        if (scheme != "http" && scheme != "https") return NavigationDecision.UnsupportedScheme(scheme)
        if (!url.startsWith("://", schemeEnd)) return NavigationDecision.InvalidAddress

        val host = extractHost(url.substring(schemeEnd + 3)) ?: return NavigationDecision.InvalidAddress
        return when (val c = domains.classify(host)) {
            is DomainClassification.Match -> NavigationDecision.Block(host = c.host, ruleDomain = c.rule.domain)
            is DomainClassification.Allowlisted, is DomainClassification.Unknown -> NavigationDecision.Allow
            DomainClassification.Invalid -> NavigationDecision.InvalidAddress
        }
    }

    /** Turns address-bar text into a URL to load or a search URL. Never produces a non-http(s) navigation. */
    fun resolveInput(raw: String): InputResolution {
        val text = raw.trim()
        if (text.isNotEmpty() && text.none { it.isWhitespace() }) {
            val lower = text.lowercase()
            if (lower.startsWith("http://") || lower.startsWith("https://")) {
                if (extractHost(text.substring(text.indexOf("://") + 3)) != null) return InputResolution.Navigate(text)
            } else if (!SCHEME_PREFIX.containsMatchIn(lower.substringBefore('/'))) {
                val host = extractHost(text)
                if (host != null && host.contains('.')) return InputResolution.Navigate("https://$text")
            }
        }
        return InputResolution.Search(SEARCH_BASE + URLEncoder.encode(text, Charsets.UTF_8))
    }

    /** `authority/path…` → normalized host, or null. Handles userinfo, ports, backslashes; rejects IPv6 literals. */
    private fun extractHost(afterScheme: String): String? {
        val authority = afterScheme.takeWhile { it != '/' && it != '?' && it != '#' && it != '\\' }
        val hostPort = authority.substringAfterLast('@')
        if (hostPort.isEmpty() || hostPort.startsWith("[")) return null
        val host = if (':' in hostPort) {
            val port = hostPort.substringAfterLast(':')
            if (port.isNotEmpty() && !port.all { it in '0'..'9' }) return null
            hostPort.substringBeforeLast(':')
        } else hostPort
        return HostNormalizer.normalize(host)
    }

    companion object {
        /** Search terms go to this engine only when the user types a search; they are never stored or logged by WARRIOR. */
        const val SEARCH_BASE = "https://duckduckgo.com/?q="
        private val SCHEME = Regex("^[a-z][a-z0-9+.-]*$")
        private val SCHEME_PREFIX = Regex("^[a-z][a-z0-9+.-]*:")
    }
}
