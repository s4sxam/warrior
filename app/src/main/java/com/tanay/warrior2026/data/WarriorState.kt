package com.tanay.warrior.data

// [UPDATE] v2.0.0: Added UserProfile, botsJson field, LEADERBOARD to ViewState
// [UPDATE] v2.2.0: A+ userPoints — logarithmic streak multiplier
//                  A+ rank thresholds — statistically justified (μ±σ model)
// [UPDATE] v3.0.0: userPoints now uses dynamic per-day scoring matching BotData:
//                  +2 base + streak bonus + momentum bonus per clean day
// [NEW]    v4.0.0: Multi-habit support — each habit has its own history, triggers,
//                  and streak. activeHabitId selects which habit is shown.
//                  The leaderboard and bot simulation are always tied to the
//                  primary (first) habit for compatibility.
// [NEW]    v4.0.2: DayData extended with relapseCount (number of fails on that day)
//                  and lastFailTime (ISO-8601 datetime of the most recent fail).
//                  WarriorScheduler can use lastFailTime to reschedule the next
//                  evening notification to fire at that exact time of day.

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun todayKey(): String = LocalDate.now().format(DATE_FORMATTER)

@Serializable
data class DayData(
    val status: String,        // "clean" | "failed"
    val site: String? = null,
    // v4.0.2 — relapse counter
    val relapseCount: Int = 0,         // how many times the user failed this day
    val lastFailTime: String? = null   // ISO-8601 datetime of the last fail e.g. "2026-04-30T21:45:00"
)

// ── v2.0.0: User profile ──────────────────────────────────────────────────────
@Serializable
data class UserProfile(
    val name: String = "",
    val dob: String  = "",          // stored as "YYYY-MM-DD"
    val region: String = ""         // WarriorRegion.name
)

// ── v4.0.0: Habit ─────────────────────────────────────────────────────────────
@Serializable
data class Habit(
    val id: String,                              // UUID string
    val name: String,                            // e.g. "No Porn", "No Smoking"
    val emoji: String        = "🔥",             // display icon
    val history: Map<String, DayData> = emptyMap(),
    val triggers: Map<String, Int>    = emptyMap()
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

    val points: Int get() {
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

data class WarriorState(
    val habits: List<Habit>           = emptyList(),
    val activeHabitId: String         = "",
    val hasCompletedOnboarding: Boolean = false,
    // v2.0.0 additions
    val userProfile: UserProfile      = UserProfile(),
    val hasCompletedProfile: Boolean  = false,
    val botsJson: String              = ""  // serialized List<BotProfile>
) {
    // ── Convenience accessors — delegate to the active habit ──────────────────
    val activeHabit: Habit? get() = habits.find { it.id == activeHabitId } ?: habits.firstOrNull()

    val history:  Map<String, DayData> get() = activeHabit?.history  ?: emptyMap()
    val triggers: Map<String, Int>     get() = activeHabit?.triggers ?: emptyMap()

    val streak:      Int get() = activeHabit?.streak      ?: 0
    val bestStreak:  Int get() = activeHabit?.bestStreak  ?: 0
    val totalClean:  Int get() = activeHabit?.totalClean  ?: 0
    val totalFailed: Int get() = activeHabit?.totalFailed ?: 0
    val userPoints:  Int get() = activeHabit?.points      ?: 0

    fun isTodayLogged(): Boolean = activeHabit?.isTodayLogged() ?: false

    // Legacy helpers kept for bot/leaderboard compatibility (uses primary habit)
    val primaryHabit: Habit? get() = habits.firstOrNull()
}

enum class ViewState { DASHBOARD, ANALYSIS, ARCHIVE, ABOUT, LEADERBOARD, HABITS }