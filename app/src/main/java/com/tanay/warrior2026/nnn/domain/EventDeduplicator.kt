package com.tanay.warrior.nnn.domain

import java.time.Duration

/**
 * Collapses retries/redirects of ONE incident into one event without merging genuinely separate ones.
 * A signal is a duplicate when a stored event has the same key (source + matched rule domain) within
 * the source's window. Duplicates are never stored, so the window is anchored on the incident's first
 * event — a continuous stream cannot extend one incident forever.
 */
class EventDeduplicator(
    private val safeBrowserWindow: Duration = Duration.ofSeconds(30),   // redirect chains / reload retries
    private val deviceGuardWindow: Duration = Duration.ofMinutes(5),    // DNS retry storms, app re-resolves
    private val manualWindow: Duration = Duration.ofSeconds(10)         // double taps
) {
    fun windowFor(source: EventSource): Duration = when (source) {
        EventSource.SAFE_BROWSER -> safeBrowserWindow
        EventSource.LOCAL_VPN -> deviceGuardWindow
        EventSource.MANUAL -> manualWindow
    }

    /** Keyed by the RULE domain so www./m./cdn. subdomains of one site are one incident. */
    fun keyFor(source: EventSource, ruleDomain: String?): String = "${source.name}|${ruleDomain ?: "-"}"

    fun isDuplicate(key: String, source: EventSource, at: java.time.Instant, existing: List<NnnEvent>): Boolean {
        val window = windowFor(source)
        return existing.any { it.dedupKey == key && Duration.between(it.at, at).abs() < window }
    }
}
