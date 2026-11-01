package com.tanay.warrior.nnn.domain

/**
 * Versioned, bundled seed list of well-known adult-content domains.
 *
 * PROVENANCE: hand-curated by the WARRIOR project from widely known site names. It is NOT copied from
 * any third-party blocklist, so no external licence or attribution applies. It is deliberately small
 * and conservative (major, unambiguous sites only) — domains are matched by name only, never by page
 * content, and an unlisted domain is UNKNOWN, not adult. Detection is therefore incomplete by design.
 *
 * UPDATING: edit [DOMAINS] and bump [VERSION] in the same change; ships with the app (no download
 * channel — see README "NNN Edition"). If a third-party list is ever adopted, record its licence and
 * attribution in README before merging.
 */
object BundledDomainRules {
    const val VERSION = 1
    const val SOURCE = "warrior-seed"
    private const val CATEGORY = "adult"
    private const val CONFIDENCE = 95

    private val DOMAINS = listOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com",
        "youporn.com", "spankbang.com", "eporner.com", "tube8.com", "porn.com",
        "brazzers.com", "chaturbate.com", "stripchat.com", "bongacams.com",
        "livejasmin.com", "cam4.com", "motherless.com", "rule34.xxx"
    )

    val rules: List<DomainRule> = DOMAINS.map {
        DomainRule(domain = it, category = CATEGORY, confidence = CONFIDENCE, enabled = true,
            source = SOURCE, ruleVersion = VERSION)
    }
}
