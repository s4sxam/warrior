package com.tanay.warrior.nnn.domain

/**
 * A single local domain rule. [domain] matches itself and its true subdomains only.
 * Rules are data, not code: the engine never fetches, scores or sends anything off-device.
 */
data class DomainRule(
    val domain: String,
    val category: String,
    val confidence: Int,
    val enabled: Boolean = true,
    val source: String,
    val ruleVersion: Int
)

sealed interface DomainClassification {
    /** Host is the rule's domain or a true subdomain of it. */
    data class Match(val rule: DomainRule, val host: String) : DomainClassification
    /** Host is covered by a user allowlist entry; adult-domain rules do not apply. */
    data class Allowlisted(val entry: String, val host: String) : DomainClassification
    /** Valid host that no enabled rule covers. Unknown is NOT adult. */
    data class Unknown(val host: String) : DomainClassification
    /** Not a valid hostname (or carries URL syntax). */
    data object Invalid : DomainClassification
}

enum class AllowlistRejection { INVALID_HOST, TOO_BROAD, SHADOWS_TOO_MANY_RULES }

sealed interface AllowlistResult {
    data class Accepted(val domain: String) : AllowlistResult
    data class Rejected(val reason: AllowlistRejection) : AllowlistResult
}

/**
 * Deterministic, local classifier.
 *  - Hostnames are normalized first ([HostNormalizer]); URL paths/queries never reach here.
 *  - Matching is "equals or ends with '.' + rule" — so notexample.test never matches example.test.
 *  - The most specific (longest) matching rule wins.
 *  - Allowlist entries are validated so a broad entry cannot silently switch off many rules.
 */
class DomainRuleEngine(
    rules: List<DomainRule>,
    allowlist: Set<String> = emptySet()
) {
    private val activeRules: Map<String, DomainRule>
    private val allowed: Set<String>

    /** Rules whose domain was not a valid hostname and were therefore ignored. */
    val droppedRuleCount: Int

    /** Raw allowlist entries that failed validation and are NOT in effect. */
    val rejectedAllowlist: Set<String>

    init {
        var dropped = 0
        val byDomain = LinkedHashMap<String, DomainRule>()
        for (rule in rules) {
            val domain = HostNormalizer.normalize(rule.domain)
            if (domain == null) { dropped++; continue }
            if (!rule.enabled) continue
            val current = byDomain[domain]
            if (current == null || rule.confidence > current.confidence) {
                byDomain[domain] = rule.copy(domain = domain)
            }
        }
        activeRules = byDomain
        droppedRuleCount = dropped

        val accepted = LinkedHashSet<String>()
        val rejected = LinkedHashSet<String>()
        for (entry in allowlist) {
            when (val r = validateAllowlistEntry(entry, byDomain.values.toList())) {
                is AllowlistResult.Accepted -> accepted += r.domain
                is AllowlistResult.Rejected -> rejected += entry
            }
        }
        allowed = accepted
        rejectedAllowlist = rejected
    }

    fun classify(rawHost: String): DomainClassification {
        val host = HostNormalizer.normalize(rawHost) ?: return DomainClassification.Invalid
        val suffixes = suffixesOf(host)
        suffixes.firstOrNull { it in allowed }?.let { return DomainClassification.Allowlisted(it, host) }
        suffixes.firstNotNullOfOrNull { activeRules[it] }?.let { return DomainClassification.Match(it, host) }
        return DomainClassification.Unknown(host)
    }

    companion object {
        /** An allowlist entry may not cover more than this many enabled rules. */
        const val MAX_SHADOWED_RULES = 3

        fun validateAllowlistEntry(entry: String, rules: List<DomainRule>): AllowlistResult {
            val domain = HostNormalizer.normalize(entry)
                ?: return AllowlistResult.Rejected(AllowlistRejection.INVALID_HOST)
            if (!domain.contains('.')) return AllowlistResult.Rejected(AllowlistRejection.TOO_BROAD)
            val shadowed = rules.asSequence()
                .filter { it.enabled }
                .mapNotNull { HostNormalizer.normalize(it.domain) }
                .distinct()
                .count { it == domain || it.endsWith(".$domain") }
            if (shadowed > MAX_SHADOWED_RULES) return AllowlistResult.Rejected(AllowlistRejection.SHADOWS_TOO_MANY_RULES)
            return AllowlistResult.Accepted(domain)
        }

        /** "a.b.c" -> ["a.b.c", "b.c", "c"] (most specific first). */
        private fun suffixesOf(host: String): List<String> {
            val out = ArrayList<String>()
            var rest = host
            while (true) {
                out += rest
                val dot = rest.indexOf('.')
                if (dot < 0) break
                rest = rest.substring(dot + 1)
            }
            return out
        }
    }
}
