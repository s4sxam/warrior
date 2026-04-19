package com.tanay.warrior2026.data

// [UPDATE] v2.0.0: Added UserProfile, botsJson field, LEADERBOARD to ViewState
// [UPDATE] v2.2.0: A+ userPoints — logarithmic streak multiplier
//                  A+ rank thresholds — statistically justified (μ±σ model)

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ln

val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun todayKey(): String = LocalDate.now().format(DATE_FORMATTER)

@Serializable
data class DayData(
    val status: String,   // "clean" | "failed"
    val site: String? = null
)

// ── v2.0.0: User profile ──────────────────────────────────────────────────────
@Serializable
data class UserProfile(
    val name: String = "",
    val dob: String  = "",          // stored as "YYYY-MM-DD"
    val region: String = ""         // WarriorRegion.name
)

data class WarriorState(
    val history: Map<String, DayData> = emptyMap(),
    val triggers: Map<String, Int>    = emptyMap(),
    val hasCompletedOnboarding: Boolean = false,
    // v2.0.0 additions
    val userProfile: UserProfile      = UserProfile(),
    val hasCompletedProfile: Boolean  = false,
    val botsJson: String              = ""  // serialized List<BotProfile>
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

    // v2.2.0: A+ logarithmic streak multiplier — matches bot scoring formula
    // Points per clean day = 2 × (1 + floor(ln(1 + streakAtThatDay)))
    // Day 1 → 2pts | Day 7 → 6pts | Day 30 → 8pts | Day 90 → 10pts
    // Rewards consistency over raw day count — same formula used for bots
    val userPoints: Int get() {
        var pts = 0
        var currentStreak = 0
        history.entries.sortedBy { it.key }.forEach { (_, d) ->
            if (d.status == "clean") {
                currentStreak++
                val streakBonus = ln(1.0 + currentStreak).toInt().coerceAtLeast(0)
                pts += 2 * (1 + streakBonus)
            } else {
                currentStreak = 0
            }
        }
        return pts
    }
}

enum class ViewState { DASHBOARD, ANALYSIS, ARCHIVE, ABOUT, LEADERBOARD }
