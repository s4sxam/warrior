package com.tanay.warrior2026.data

// [UPDATE] v3.0.0: Human-behaviour simulation rewrite
// [FIX]    v3.1.0: Start date now 365 days ago (not hardcoded 2026-04-12).
//                  realSimulatedCalendar() replaces generateBotCalendar() —
//                  only days that were actually simulated are returned (no fake history).

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.max
import kotlin.random.Random

object BotSimulator {

    private val json = Json { ignoreUnknownKeys = true }

    fun loadBots(botsJson: String): List<BotProfile> {
        if (botsJson.isBlank()) return generateBots()
        return runCatching {
            json.decodeFromString<List<BotProfile>>(botsJson)
        }.getOrElse { generateBots() }
    }

    fun saveBots(bots: List<BotProfile>): String =
        json.encodeToString(bots)

    /**
     * Advance every bot from lastSimulatedDay up to yesterday.
     * [FIX v3.1.0] Fallback start is now LocalDate.now().minusDays(365) — no
     * more hardcoded date. New installs always get a full year of history.
     */
    fun advanceSimulation(bots: List<BotProfile>): List<BotProfile> {
        val yesterday = LocalDate.now().minusDays(1)

        return bots.map { bot ->
            var b = bot

            val startDate: LocalDate = if (b.lastSimulatedDay.isBlank()) {
                LocalDate.now().minusDays(365)
            } else {
                runCatching {
                    LocalDate.parse(b.lastSimulatedDay, DATE_FORMATTER).plusDays(1)
                }.getOrElse { yesterday }
            }

            if (startDate.isAfter(yesterday)) return@map b

            val lifeRng = Random(b.seed xor startDate.toEpochDay() xor 0xCAFEBABEL)
            var simDate = startDate
            var lifeEventDaysLeft = b.lifeEventDaysLeft.coerceAtLeast(0)

            while (!simDate.isAfter(yesterday)) {
                if (lifeEventDaysLeft > 0) {
                    lifeEventDaysLeft--
                } else if (lifeRng.nextInt(b.lifeEventInterval) == 0) {
                    lifeEventDaysLeft = lifeRng.nextInt(3, 15)
                }
                val lifeEventActive = lifeEventDaysLeft > 0
                val dayOfWeek = simDate.dayOfWeek.value - 1
                val dayRng = Random(b.seed xor simDate.toEpochDay() xor 0xDEADBEEFL)
                val prob = survivalProbability(b, dayOfWeek, lifeEventActive)
                val clean = dayRng.nextDouble() < prob

                if (clean) {
                    val gained = pointsForCleanDay(b.currentStreak, b.momentum)
                    b = b.copy(
                        points            = b.points + gained,
                        currentStreak     = b.currentStreak + 1,
                        momentum          = min(b.momentum + 1.0, 50.0),
                        totalCleanDays    = b.totalCleanDays + 1,
                        inLifeEvent       = lifeEventActive,
                        lifeEventDaysLeft = lifeEventDaysLeft,
                        lastSimulatedDay  = simDate.format(DATE_FORMATTER)
                    )
                } else {
                    val lost = pointsLostOnRelapse(b.currentStreak)
                    b = b.copy(
                        points            = max(b.points - lost, 0),
                        currentStreak     = 0,
                        momentum          = max(b.momentum - 3.0, 0.0),
                        totalFailDays     = b.totalFailDays + 1,
                        inLifeEvent       = lifeEventActive,
                        lifeEventDaysLeft = lifeEventDaysLeft,
                        lastSimulatedDay  = simDate.format(DATE_FORMATTER)
                    )
                }

                simDate = simDate.plusDays(1)
            }
            b
        }
    }

    // ── Real simulated calendar ───────────────────────────────────────────────
    //
    // [NEW v3.1.0] Replaces generateBotCalendar() which was purely procedural and
    // showed fake data inconsistent with the bot's actual simulated points/streak.
    //
    // This replays the same RNG decisions as advanceSimulation so the heatmap
    // matches the bot's real state. Days before simulation start = null (grey cell).
    // The returned map has exactly 365 keys (today-364 … today-0), with null for
    // days before simulation began or after lastSimulatedDay.

    fun realSimulatedCalendar(bot: BotProfile): Map<String, Boolean?> {
        val today = LocalDate.now()
        val gridStart = today.minusDays(364)

        // Build the full 365-day key grid initialised to null
        val result = LinkedHashMap<String, Boolean?>()
        var d = gridStart
        while (!d.isAfter(today)) {
            result[d.format(DATE_FORMATTER)] = null
            d = d.plusDays(1)
        }

        if (bot.lastSimulatedDay.isBlank()) return result

        val simEnd = runCatching {
            LocalDate.parse(bot.lastSimulatedDay, DATE_FORMATTER)
        }.getOrElse { return result }

        val simStart = gridStart // replay from grid start (same as advanceSimulation fallback)

        val lifeRng = Random(bot.seed xor simStart.toEpochDay() xor 0xCAFEBABEL)
        var tempStreak = 0
        var tempMomentum = 0.0
        var lifeEventDaysLeft = 0
        var simDate = simStart

        while (!simDate.isAfter(simEnd)) {
            if (lifeEventDaysLeft > 0) {
                lifeEventDaysLeft--
            } else if (lifeRng.nextInt(bot.lifeEventInterval) == 0) {
                lifeEventDaysLeft = lifeRng.nextInt(3, 15)
            }
            val lifeEventActive = lifeEventDaysLeft > 0
            val dayOfWeek = simDate.dayOfWeek.value - 1
            val dayRng = Random(bot.seed xor simDate.toEpochDay() xor 0xDEADBEEFL)
            val tempBot = bot.copy(
                currentStreak = tempStreak,
                momentum = tempMomentum,
                inLifeEvent = lifeEventActive,
                lifeEventDaysLeft = lifeEventDaysLeft
            )
            val prob = survivalProbability(tempBot, dayOfWeek, lifeEventActive)
            val clean = dayRng.nextDouble() < prob

            val key = simDate.format(DATE_FORMATTER)
            if (result.containsKey(key)) {
                result[key] = clean
            }

            if (clean) {
                tempStreak++
                tempMomentum = min(tempMomentum + 1.0, 50.0)
            } else {
                tempStreak = 0
                tempMomentum = max(tempMomentum - 3.0, 0.0)
            }
            simDate = simDate.plusDays(1)
        }
        return result
    }

    // ── Leaderboard ───────────────────────────────────────────────────────────

    data class LeaderboardEntry(
        val rank: Int,
        val name: String,
        val points: Int,
        val region: String,
        val isUser: Boolean,
        val botId: Int = -1,
        val winRate: Float = 0f,
        val archetype: String = "",
        val currentStreak: Int = 0
    )

    fun regionalLeaderboard(
        bots: List<BotProfile>,
        userRegion: String,
        userName: String,
        userPoints: Int,
        userTotalClean: Int = 0,
        userTotalLogged: Int = 0,
        userStreak: Int = 0
    ): List<LeaderboardEntry> {
        val regional = bots.filter { it.region == userRegion }
        return buildBoard(regional, userName, userPoints, userRegion,
                          userTotalClean, userTotalLogged, userStreak)
    }

    fun globalLeaderboard(
        bots: List<BotProfile>,
        userName: String,
        userPoints: Int,
        userRegion: String,
        userTotalClean: Int = 0,
        userTotalLogged: Int = 0,
        userStreak: Int = 0
    ): List<LeaderboardEntry> {
        return buildBoard(bots, userName, userPoints, userRegion,
                          userTotalClean, userTotalLogged, userStreak)
    }

    private fun buildBoard(
        bots: List<BotProfile>,
        userName: String,
        userPoints: Int,
        userRegion: String,
        userTotalClean: Int,
        userTotalLogged: Int,
        userStreak: Int
    ): List<LeaderboardEntry> {
        val entries = mutableListOf<LeaderboardEntry>()
        bots.forEach { bot ->
            val total = (bot.totalCleanDays + bot.totalFailDays).coerceAtLeast(1)
            entries.add(
                LeaderboardEntry(
                    rank          = 0,
                    name          = bot.name,
                    points        = bot.points,
                    region        = bot.region,
                    isUser        = false,
                    botId         = bot.id,
                    winRate       = bot.totalCleanDays.toFloat() / total * 100f,
                    archetype     = bot.archetype,
                    currentStreak = bot.currentStreak
                )
            )
        }
        val userWinRate = if (userTotalLogged > 0)
            userTotalClean.toFloat() / userTotalLogged * 100f
        else 0f
        entries.add(
            LeaderboardEntry(
                rank          = 0,
                name          = userName.ifBlank { "You" },
                points        = userPoints,
                region        = userRegion,
                isUser        = true,
                winRate       = userWinRate,
                currentStreak = userStreak
            )
        )
        return entries
            .sortedByDescending { it.points }
            .mapIndexed { i, e -> e.copy(rank = i + 1) }
    }
}
