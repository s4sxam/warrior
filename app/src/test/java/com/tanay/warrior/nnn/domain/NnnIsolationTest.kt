package com.tanay.warrior.nnn.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * NON-NEGOTIABLE: NNN state is independent of ordinary habits. The cheapest durable guarantee is
 * structural: nothing under nnn/ may import the habit/leaderboard/achievement data layer or the
 * main ViewModel, so an NNN failure cannot reach them.
 */
class NnnIsolationTest {
    private fun nnnSourceDir(): File? = listOf(
        "src/main/java/com/tanay/warrior2026/nnn",
        "app/src/main/java/com/tanay/warrior2026/nnn"
    ).map(::File).firstOrNull { it.isDirectory }

    @Test fun nnnPackageDoesNotDependOnHabitOrLeaderboardCode() {
        val dir = nnnSourceDir() ?: error("NNN source dir not found from ${File(".").absolutePath}")
        val files = dir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("expected NNN sources", files.isNotEmpty())
        val forbidden = Regex("""^\s*import\s+com\.tanay\.warrior\.(data\.|WarriorViewModel|WarriorApp)""")
        val offenders = files.flatMap { f -> f.readLines().filter { forbidden.containsMatchIn(it) }.map { "${f.name}: ${it.trim()}" } }
        assertTrue("NNN must not import habit/leaderboard code: $offenders", offenders.isEmpty())
    }

    @Test fun domainLayerHasNoAndroidDependencies() {
        val dir = nnnSourceDir() ?: error("NNN source dir not found")
        val domain = File(dir, "domain").walkTopDown().filter { it.extension == "kt" }.toList()
        val offenders = domain.flatMap { f -> f.readLines().filter { it.trim().startsWith("import android.") || it.trim().startsWith("import androidx.") }.map { f.name } }
        assertTrue("domain layer must stay pure JVM: $offenders", offenders.isEmpty())
    }
}
