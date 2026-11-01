package com.tanay.warrior.nnn.domain

import java.net.IDN
import java.util.Locale

/**
 * Turns a raw hostname into one canonical ASCII form, or null if it is not a valid hostname.
 *
 * Accepts a HOST only — never a URL. Anything carrying URL syntax (scheme, path, query, fragment,
 * userinfo, port, brackets) is rejected so a caller can never leak path/query data into matching.
 *
 * Pure JVM, no Android dependencies.
 */
object HostNormalizer {

    private const val MAX_HOST_LENGTH = 253
    private const val MAX_LABEL_LENGTH = 63
    private val LABEL = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$")
    private const val FORBIDDEN = "/\\?#@:[]%"

    fun normalize(raw: String): String? {
        var host = raw.trim()
        if (host.isEmpty()) return null
        if (host.any { it.isWhitespace() || it.isISOControl() || it in FORBIDDEN }) return null

        // Ideographic / fullwidth / halfwidth full stops are dots in IDN (RFC 3490).
        host = host.replace('\u3002', '.').replace('\uFF0E', '.').replace('\uFF61', '.')

        if (host.endsWith(".")) host = host.dropLast(1)   // exactly one trailing dot (FQDN form)
        if (host.isEmpty()) return null

        val ascii = try {
            IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES)
        } catch (_: IllegalArgumentException) {
            return null
        }.lowercase(Locale.ROOT)

        if (ascii.isEmpty() || ascii.length > MAX_HOST_LENGTH) return null
        val labels = ascii.split('.')
        if (labels.any { it.isEmpty() || it.length > MAX_LABEL_LENGTH || !LABEL.matches(it) }) return null
        return ascii
    }
}
