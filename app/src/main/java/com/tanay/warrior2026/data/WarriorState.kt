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

    /**
     * The most recently completed streak before the current one.
     * Used as the Live Rival ghost — you race your actual past self, not just your best.
     *
     * Algorithm: walk the history in chronological order, collect completed streaks
     * (runs of consecutive clean days that end with a failure or a gap), return
     * the second-to-last one. Falls back to bestStreak if only one streak exists.
     */
    val previousStreak: Int get() {
        val sorted = history.entries.sortedBy { it.key }
        val completedStreaks = mutableListOf<Int>()
        var run = 0
        sorted.forEach { (_, d) ->
            if (d.status == "clean") {
                run++
            } else {
                if (run > 0) completedStreaks.add(run)
                run = 0
            }
        }
        // If current run is ongoing (today is clean), don't count it as completed
        // — it's the active streak we're beating, not a ghost.
        return when {
            completedStreaks.size >= 2 -> completedStreaks[completedStreaks.size - 2]
            completedStreaks.size == 1 -> completedStreaks[0]
            else                       -> bestStreak
        }
    }

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
    val botsJson: String              = "",  // serialized List<BotProfile>
    val lastConfession: String?        = null
) {
    // ── Convenience accessors — delegate to the active habit ──────────────────
    val activeHabit: Habit? get() = habits.find { it.id == activeHabitId } ?: habits.firstOrNull()

    val history:  Map<String, DayData> get() = activeHabit?.history  ?: emptyMap()
    val triggers: Map<String, Int>     get() = activeHabit?.triggers ?: emptyMap()

    val streak:      Int get() = activeHabit?.streak         ?: 0
    val bestStreak:  Int get() = activeHabit?.bestStreak     ?: 0
    val previousStreak: Int get() = activeHabit?.previousStreak ?: 0
    val totalClean:  Int get() = activeHabit?.totalClean  ?: 0
    val totalFailed: Int get() = activeHabit?.totalFailed ?: 0
    val userPoints:  Int get() = activeHabit?.points      ?: 0

    fun isTodayLogged(): Boolean = activeHabit?.isTodayLogged() ?: false

    // Legacy helpers kept for bot/leaderboard compatibility (uses primary habit)
    val primaryHabit: Habit? get() = habits.firstOrNull()
}

enum class ViewState { DASHBOARD, ANALYSIS, ARCHIVE, ABOUT, LEADERBOARD, HABITS }