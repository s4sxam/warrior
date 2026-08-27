package com.tanay.warrior.data

// [NEW] v4.3.0: Achievements.
//
// Every achievement's unlocked/locked state and progress is DERIVED from
// WarriorState at read time — nothing here is persisted as a separate
// "unlocked" flag. This mirrors how AnalysisScreen's computeRank() already
// works: the source of truth is the real habit history, so an achievement
// can never drift out of sync with what actually happened, and no DataStore
// migration was needed to add this feature.
//
// The one thing that IS persisted (see WarriorRepository.seenAchievementIds)
// is which achievement IDs have already shown their one-time unlock
// animation, so re-opening the screen doesn't replay it — that's a UI
// concern, not game-state, so it's kept separate from this file.

import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class AchievementRarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }

enum class AchievementCategory(val displayName: String) {
    STREAK("STREAK"),
    RECOVERY("RECOVERY"),
    AWARENESS("SELF-AWARENESS"),
    CONSISTENCY("CONSISTENCY"),
    ARENA("ARENA"),
    COMMANDER("COMMANDER")
}

/**
 * [current] / [target] describe progress toward unlock, e.g. streak 4 / 7.
 * For achievements that are pure booleans (no meaningful partial progress —
 * e.g. "create a second habit"), target = 1 and current is 0 or 1.
 */
data class AchievementProgress(val current: Int, val target: Int) {
    val fraction: Float get() = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
    val isComplete: Boolean get() = current >= target
}

data class Achievement(
    val id: String,                 // stable — used as the DataStore key for "seen" tracking, never rename
    val title: String,
    val story: String,              // the "why this matters" line, shown in the detail sheet
    val category: AchievementCategory,
    val rarity: AchievementRarity,
    val icon: String,                // single emoji — kept simple/app-consistent rather than pulling in icon assets
    val progressOf: (WarriorState) -> AchievementProgress
) {
    fun isUnlocked(state: WarriorState): Boolean = progressOf(state).isComplete
}

// ─────────────────────────────────────────────────────────────
// Shared helpers — used by several achievements below
// ─────────────────────────────────────────────────────────────

private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

/** How many currently-active habits have a streak of at least [minStreak]. */
private fun habitsWithStreakAtLeast(state: WarriorState, minStreak: Int): Int =
    state.habits.count { it.streak >= minStreak }

/** Longest logged-in-a-row run across the ACTIVE habit's history, counting both clean and failed days (i.e. "did they show up and log honestly"), not just clean days. */
private fun longestLoggingStreak(state: WarriorState): Int {
    val dates = state.history.keys.sorted()
    if (dates.isEmpty()) return 0
    var best = 1; var cur = 1
    for (i in 1 until dates.size) {
        val prev = runCatching { LocalDate.parse(dates[i - 1], ISO) }.getOrNull() ?: continue
        val curD = runCatching { LocalDate.parse(dates[i], ISO) }.getOrNull() ?: continue
        if (prev.plusDays(1) == curD) { cur++; best = maxOf(best, cur) } else cur = 1
    }
    return best
}

/** True if the day right after [failDate] (both "yyyy-MM-dd" strings) was logged clean. */
private fun loggedCleanTheDayAfter(state: WarriorState, failDate: String): Boolean {
    val d = runCatching { LocalDate.parse(failDate, ISO) }.getOrNull() ?: return false
    return state.history[d.plusDays(1).format(ISO)]?.status == "clean"
}

/** Count of fail-days where the very next day was logged clean (i.e. genuine same-day-after comebacks). */
private fun comebackCount(state: WarriorState): Int =
    state.history.entries.count { (date, day) -> day.status == "failed" && loggedCleanTheDayAfter(state, date) }

/** Longest historical clean run BEFORE the current one — i.e. excludes the still-active streak, so this only counts runs that actually ended in a relapse. Used by "The Comeback". */
private fun longestEndedCleanRun(state: WarriorState): Int {
    val sorted = state.history.entries.sortedBy { it.key }
    var best = 0; var cur = 0
    for ((_, day) in sorted) {
        if (day.status == "clean") cur++ else { best = maxOf(best, cur); cur = 0 }
    }
    // Deliberately do NOT fold the trailing `cur` into `best` — a still-active
    // streak hasn't "ended" yet, so it shouldn't count toward this achievement.
    return best
}

private fun daysLoggedThisMonth(state: WarriorState): Int {
    val now = LocalDate.now()
    val prefix = now.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    return state.history.keys.count { it.startsWith(prefix) }
}

// ─────────────────────────────────────────────────────────────
// The list
// ─────────────────────────────────────────────────────────────

val ALL_ACHIEVEMENTS: List<Achievement> = listOf(

    // ── STREAK ──────────────────────────────────────────────
    Achievement("streak_1", "First Blood", "One clean day. Every streak in this app started exactly here.",
        AchievementCategory.STREAK, AchievementRarity.COMMON, "🩸") { s -> AchievementProgress(minOf(s.streak, 1), 1) },
    Achievement("streak_3", "The Line Holds", "Three days. Most impulses lose their grip by now — you didn't blink.",
        AchievementCategory.STREAK, AchievementRarity.COMMON, "🛑") { s -> AchievementProgress(minOf(s.streak, 3), 3) },
    Achievement("streak_7", "Week One", "Seven days straight. The first real week is always the loudest one.",
        AchievementCategory.STREAK, AchievementRarity.UNCOMMON, "📅") { s -> AchievementProgress(minOf(s.streak, 7), 7) },
    Achievement("streak_14", "Fortnight Standing", "Fourteen days. Whatever your body was telling you at day 3, it was lying.",
        AchievementCategory.STREAK, AchievementRarity.UNCOMMON, "🗓") { s -> AchievementProgress(minOf(s.streak, 14), 14) },
    Achievement("streak_21", "The Twenty-One", "Twenty-one days. Old habits don't die on a schedule, but this is when new ones start claiming territory.",
        AchievementCategory.STREAK, AchievementRarity.RARE, "🧱") { s -> AchievementProgress(minOf(s.streak, 21), 21) },
    Achievement("streak_30", "One Month Clean", "Thirty days. A full lunar cycle without breaking your word to yourself.",
        AchievementCategory.STREAK, AchievementRarity.RARE, "🌙") { s -> AchievementProgress(minOf(s.streak, 30), 30) },
    Achievement("streak_60", "Sixty and Standing", "Sixty days. Most people who start this don't make it here. You did.",
        AchievementCategory.STREAK, AchievementRarity.EPIC, "⛰") { s -> AchievementProgress(minOf(s.streak, 60), 60) },
    Achievement("streak_90", "The Ninety", "Ninety days. Somewhere in the last month this stopped being a challenge and started being who you are.",
        AchievementCategory.STREAK, AchievementRarity.EPIC, "🔱") { s -> AchievementProgress(minOf(s.streak, 90), 90) },
    Achievement("streak_180", "Half a Year", "One hundred eighty days clean. Half a year of choosing this, one day at a time.",
        AchievementCategory.STREAK, AchievementRarity.LEGENDARY, "🏔") { s -> AchievementProgress(minOf(s.streak, 180), 180) },
    Achievement("streak_365", "The Full Year", "Three hundred sixty-five days. One entire year where your word to yourself was worth something every single day.",
        AchievementCategory.STREAK, AchievementRarity.LEGENDARY, "👑") { s -> AchievementProgress(minOf(s.streak, 365), 365) },

    // ── RECOVERY ────────────────────────────────────────────
    Achievement("recovery_1", "Back on Your Feet", "Relapsed, then logged clean the very next day. That turnaround is the whole game.",
        AchievementCategory.RECOVERY, AchievementRarity.COMMON, "🩹") { s -> AchievementProgress(minOf(comebackCount(s), 1), 1) },
    Achievement("recovery_3", "No Excuses", "Bounced back the morning after, three separate times. You stopped negotiating with bad days.",
        AchievementCategory.RECOVERY, AchievementRarity.RARE, "🔁") { s -> AchievementProgress(minOf(comebackCount(s), 3), 3) },
    Achievement("recovery_pb", "Broke the Pattern", "Beat your own best streak. The old ceiling is now just a floor.",
        AchievementCategory.RECOVERY, AchievementRarity.EPIC, "📈") { s ->
        // "beat the old best" only makes sense once there IS an old best to beat —
        // approximate via bestStreak vs. the longest run that has actually ended,
        // so an ongoing first-ever streak doesn't trivially satisfy this.
        val ended = longestEndedCleanRun(s)
        val done = ended > 0 && s.bestStreak > ended
        AchievementProgress(if (done) 1 else 0, 1)
    },
    Achievement("recovery_comeback30", "The Comeback", "Lost a streak of 15+ days, then rebuilt a full 30-day run from scratch. That takes more than the first attempt ever did.",
        AchievementCategory.RECOVERY, AchievementRarity.LEGENDARY, "🔥") { s ->
        // This one has a real prerequisite (must have LOST a 15+ day streak
        // first), not just "reach a number". If that prerequisite isn't met
        // yet, we still show the user's real current streak against 30 —
        // showing a fake 0 here would look like a bug to someone sitting at,
        // say, streak 25 with no qualifying loss in their history yet.
        // isComplete only ever fires once hadBigLoss is genuinely true, since
        // current can equal target (30) without unlocking otherwise — see the
        // explicit check folded into `current` below.
        val hadBigLoss = longestEndedCleanRun(s) >= 15
        val current = if (hadBigLoss) minOf(s.streak, 30) else minOf(s.streak, 29) // capped at 29 pre-requisite so it can never read as "complete" on progress alone
        AchievementProgress(current, 30)
    },

    // ── SELF-AWARENESS ──────────────────────────────────────
    Achievement("aware_1", "Naming the Enemy", "Logged your first trigger. You can't out-plan what you won't name.",
        AchievementCategory.AWARENESS, AchievementRarity.COMMON, "🎯") { s -> AchievementProgress(minOf(s.triggers.values.sum(), 1), 1) },
    Achievement("aware_pattern", "Know Thy Trigger", "The same trigger, logged five separate times. That's not bad luck — that's a pattern, and now you can see it.",
        AchievementCategory.AWARENESS, AchievementRarity.UNCOMMON, "🔍") { s ->
        val top = s.triggers.values.maxOrNull() ?: 0
        AchievementProgress(minOf(top, 5), 5)
    },
    Achievement("aware_5triggers", "Full Disclosure", "Five different triggers identified. The clearer the map, the fewer places left to be ambushed.",
        AchievementCategory.AWARENESS, AchievementRarity.RARE, "🗺") { s -> AchievementProgress(minOf(s.triggers.keys.size, 5), 5) },
    Achievement("aware_mirror", "The Mirror", "Ten trigger-tagged entries in your history. You're not hiding from what set you off anymore.",
        AchievementCategory.AWARENESS, AchievementRarity.EPIC, "🪞") { s ->
        val tagged = s.history.values.count { it.status == "failed" && it.site != null && it.site != "unknown" }
        AchievementProgress(minOf(tagged, 10), 10)
    },

    // ── CONSISTENCY ─────────────────────────────────────────
    Achievement("consist_showup7", "Showing Up", "Logged something — win or loss — seven days straight. Honesty with yourself is a streak too.",
        AchievementCategory.CONSISTENCY, AchievementRarity.COMMON, "✋") { s -> AchievementProgress(minOf(longestLoggingStreak(s), 7), 7) },
    Achievement("consist_fullmonth", "No Days Skipped", "Every single day of a calendar month, logged. No gaps, no silence.",
        AchievementCategory.CONSISTENCY, AchievementRarity.RARE, "🗓") { s ->
        val now = LocalDate.now()
        val daysElapsed = now.dayOfMonth
        AchievementProgress(minOf(daysLoggedThisMonth(s), daysElapsed), daysElapsed)
    },
    Achievement("consist_80month", "Eighty Percent Rule", "80%+ clean days this month. Not perfection — just enough to matter.",
        AchievementCategory.CONSISTENCY, AchievementRarity.EPIC, "📊") { s -> AchievementProgress(minOf(s.activeHabit?.monthCleanPercent ?: 0, 80), 80) },
    Achievement("consist_paragon", "Paragon", "Hit ⚡ PARAGON rank — the top tier of 30-day consistency this app tracks.",
        AchievementCategory.CONSISTENCY, AchievementRarity.LEGENDARY, "⚡") { s ->
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val now = LocalDate.now()
        var v = 0; var d = 0
        (0 until 30).forEach { offset ->
            when (s.history[now.minusDays(offset.toLong()).format(fmt)]?.status) {
                "clean" -> v++; "failed" -> d++
            }
        }
        val pct = if (v + d > 0) (v.toFloat() / (v + d) * 100).toInt() else 0
        AchievementProgress(minOf(pct, 88), 88)
    },

    // ── ARENA ────────────────────────────────────────────────
    Achievement("arena_points", "First Points", "Scored your first points on the leaderboard. The arena now knows your name.",
        AchievementCategory.ARENA, AchievementRarity.COMMON, "🏅") { s -> AchievementProgress(minOf(s.userPoints, 1), 1) },
    Achievement("arena_top100", "Cracking the Top 100", "Top 100 in your region. Out of 150 simulated warriors, you're no longer in the back half.",
        AchievementCategory.ARENA, AchievementRarity.RARE, "🥈") { _ ->
        // Rank position depends on the live regional board, which this file doesn't
        // have access to (it's a StateFlow computed in the ViewModel from bot data,
        // not stored on WarriorState). AchievementsScreen passes the actual rank in
        // separately for this one — see achievementsWithArenaRank() below.
        AchievementProgress(0, 1)
    },
    Achievement("arena_top10global", "Global Contender", "Top 10 worldwide. Out of 1,050 simulated warriors across every region, you're in the final ten.",
        AchievementCategory.ARENA, AchievementRarity.LEGENDARY, "🌍") { _ -> AchievementProgress(0, 1) },

    // ── COMMANDER ────────────────────────────────────────────
    Achievement("cmd_second", "Second Front", "Started tracking a second habit. One battle at a time was never the plan.",
        AchievementCategory.COMMANDER, AchievementRarity.COMMON, "🎖") { s -> AchievementProgress(minOf(s.habits.size, 2), 2) },
    Achievement("cmd_multi3", "Multi-Domain Warrior", "Three or more habits, all active at once.",
        AchievementCategory.COMMANDER, AchievementRarity.RARE, "🛡") { s -> AchievementProgress(minOf(s.habits.size, 3), 3) },
    Achievement("cmd_manybattles", "One Warrior, Many Battles", "A 7-day streak on three different habits — not necessarily all at once, but each one proven.",
        AchievementCategory.COMMANDER, AchievementRarity.EPIC, "⚔") { s -> AchievementProgress(minOf(habitsWithStreakAtLeast(s, 7), 3), 3) }
)

/**
 * arena_top100 and arena_top10global need live leaderboard position, which
 * isn't part of WarriorState — it's a StateFlow the ViewModel computes from
 * bot data. AchievementsScreen calls this with the user's actual regional
 * and global rank (1-indexed; 0 or null if not yet placed) to get progress
 * for those two specific achievements filled in correctly. Every other
 * achievement's progressOf already reads everything it needs from
 * WarriorState alone.
 */
fun achievementsWithArenaRank(
    base: List<Achievement>,
    regionalRank: Int?,
    globalRank: Int?
): List<Achievement> = base.map { a ->
    when (a.id) {
        "arena_top100" -> a.copy(progressOf = { _ ->
            val r = regionalRank
            AchievementProgress(if (r != null && r in 1..100) 1 else 0, 1)
        })
        "arena_top10global" -> a.copy(progressOf = { _ ->
            val r = globalRank
            AchievementProgress(if (r != null && r in 1..10) 1 else 0, 1)
        })
        else -> a
    }
}
