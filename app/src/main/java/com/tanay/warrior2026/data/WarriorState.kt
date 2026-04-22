package com.tanay.warrior2026.data

// [UPDATE] v2.0.0: Added UserProfile, botsJson field, LEADERBOARD to ViewState
// [UPDATE] v2.2.0: A+ userPoints — logarithmic streak multiplier
//                  A+ rank thresholds — statistically justified (μ±σ model)
// [UPDATE] v3.0.0: userPoints now uses dynamic per-day scoring matching BotData:
//                  +2 base + streak bonus + momentum bonus per clean day

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

    // Dynamic points matching bot scoring: +2 base + floor(streak/7) + floor(momentum/10)
    // Replays clean days chronologically so scoring matches bots exactly.
    val userPoints: Int get() {
        var total = 0
        var streak = 0
        var momentum = 0.0
        history.entries.sortedBy { it.key }.forEach { (_, d) ->
            if (d.status == "clean") {
                total += 2 + (streak / 7) + (momentum / 10).toInt()
                streak++
                momentum = minOf(momentum + 1.0, 50.0)
            } else {
                val lost = minOf(3 + streak / 5, 12)
                total = maxOf(total - lost, 0)
                streak = 0
                momentum = maxOf(momentum - 3.0, 0.0)
            }
        }
        return total
    }
}

enum class ViewState { DASHBOARD, ANALYSIS, ARCHIVE, ABOUT, LEADERBOARD }
