package com.tanay.warrior2026.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun todayKey(): String = LocalDate.now().format(DATE_FORMATTER)

@Serializable
data class DayData(
    val status: String,   // "clean" | "failed"
    val site: String? = null
)

data class WarriorState(
    val history: Map<String, DayData> = emptyMap(),
    val triggers: Map<String, Int>    = emptyMap(),
    val hasCompletedOnboarding: Boolean = false
) {
    val streak: Int get() {
        var count = 0
        var date  = LocalDate.now()
        if (!history.containsKey(date.format(DATE_FORMATTER))) date = date.minusDays(1)
        while (true) {
            val key = date.format(DATE_FORMATTER)
            if (history[key]?.status == "clean") { count++; date = date.minusDays(1) }
            else break
        }
        return count
    }

    val bestStreak: Int get() {
        var best = 0; var cur = 0
        history.entries.sortedBy { it.key }.forEach { (_, d) ->
            if (d.status == "clean") { cur++; if (cur > best) best = cur }
            else cur = 0
        }
        return best
    }

    val totalClean:  Int get() = history.values.count { it.status == "clean" }
    val totalFailed: Int get() = history.values.count { it.status == "failed" }

    fun isTodayLogged(): Boolean = history.containsKey(todayKey())
}

enum class ViewState { DASHBOARD, ANALYSIS, ARCHIVE, ABOUT }