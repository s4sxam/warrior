package com.tanay.warrior.nnn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HostNormalizerTest {

    @Test fun lowercases() = assertEquals("example.test", HostNormalizer.normalize("Example.TEST"))

    @Test fun stripsSingleTrailingDot() = assertEquals("example.test", HostNormalizer.normalize("example.test."))

    @Test fun trimsSurroundingWhitespace() = assertEquals("example.test", HostNormalizer.normalize("  example.test \n"))

    @Test fun rejectsDoubleTrailingDot() = assertNull(HostNormalizer.normalize("example.test.."))

    @Test fun rejectsEmptyAndBlank() {
        assertNull(HostNormalizer.normalize(""))
        assertNull(HostNormalizer.normalize("   "))
        assertNull(HostNormalizer.normalize("."))
    }

    @Test fun rejectsLeadingDotAndEmptyLabels() {
        assertNull(HostNormalizer.normalize(".example.test"))
        assertNull(HostNormalizer.normalize("a..example.test"))
    }

    @Test fun rejectsUrlSyntaxCharacters() {
        for (bad in listOf("example.test/path", "example.test?q=1", "example.test#frag",
            "user@example.test", "example.test:8080", "exa mple.test", "http://example.test", "[::1]")) {
            assertNull("should reject '$bad'", HostNormalizer.normalize(bad))
        }
    }

    @Test fun convertsUnicodeToPunycode() {
        assertEquals("xn--bcher-kva.test", HostNormalizer.normalize("bücher.test"))
        assertEquals("xn--bcher-kva.test", HostNormalizer.normalize("BÜCHER.test"))
    }

    @Test fun acceptsExistingPunycode() = assertEquals("xn--bcher-kva.test", HostNormalizer.normalize("xn--bcher-kva.test"))

    @Test fun treatsIdeographicFullStopAsDot() = assertEquals("example.test", HostNormalizer.normalize("example\u3002test"))

    @Test fun rejectsBadLabels() {
        assertNull(HostNormalizer.normalize("-bad.test"))
        assertNull(HostNormalizer.normalize("bad-.test"))
        assertNull(HostNormalizer.normalize("under_score.test"))
        assertNull(HostNormalizer.normalize("a".repeat(64) + ".test"))
    }

    @Test fun acceptsMaxLengthLabel() {
        val label = "a".repeat(63)
        assertEquals("$label.test", HostNormalizer.normalize("$label.test"))
    }

    @Test fun rejectsOverlongHost() {
        val host = (1..60).joinToString(".") { "abcd" } // 60*4 + 59 = 299 chars
        assertNull(HostNormalizer.normalize(host))
    }

    @Test fun allowsSingleLabelHost() = assertEquals("localhost", HostNormalizer.normalize("LocalHost"))
}
