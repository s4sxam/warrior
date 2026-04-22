package com.tanay.warrior2026.data

// [UPDATE] v3.0.0: Human-behaviour simulation rewrite
//   - advanceSimulation now tracks day-of-week, life events per bot
//   - Points use dynamic pointsForCleanDay / pointsLostOnRelapse from BotData
//   - LeaderboardEntry exposes archetype for UI display
//   - All logic fully seeded — deterministic per bot

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.max
import kotlin.random.Random

object BotSimulator {

    private val json = Json { ignoreUnknownKeys = true }

    /** Deserialize bots from stored JSON. Returns freshly generated bots if blank/corrupt. */
    fun loadBots(botsJson: String): List<BotProfile> {
        if (botsJson.isBlank()) return generateBots()
        return runCatching {
            json.decodeFromString<List<BotProfile>>(botsJson)
        }.getOrElse { generateBots() }
    }

    /** Serialize bots to JSON for storage. */
    fun saveBots(bots: List<BotProfile>): String =
        json.encodeToString(bots)

    /**
     * Advance every bot day-by-day from lastSimulatedDay up to yesterday.
     * Today is always excluded — the user's own log determines today's score.
     *
     * Per day:
     *   1. Determine if a life event starts (seeded RNG, bot-specific interval)
     *   2. Determine day-of-week for weekly rhythm term
     *   3. Compute survivalProbability with all terms
     *   4. Award/deduct points using dynamic scoring from BotData
     */
    fun advanceSimulation(bots: List<BotProfile>): List<BotProfile> {
        val yesterday = LocalDate.now().minusDays(1)

        return bots.map { bot ->
            var b = bot

            val startDate: LocalDate = if (b.lastSimulatedDay.isBlank()) {
                LocalDate.of(2026, 4, 12)
            } else {
                runCatching {
                    LocalDate.parse(b.lastSimulatedDay, DATE_FORMATTER).plusDays(1)
                }.getOrElse { yesterday }
            }

            // Seeded RNG for life-event decisions — separate from survivalProbability RNG
            // Use bot seed + epoch of startDate so it's reproducible across app launches
            val lifeRng = Random(b.seed xor startDate.toEpochDay() xor 0xCAFEBABEL)

            var simDate          = startDate
            var lifeEventDaysLeft = b.lifeEventDaysLeft.coerceAtLeast(0)

            while (!simDate.isAfter(yesterday)) {
                // ── Life event tick ───────────────────────────────────────
                if (lifeEventDaysLeft > 0) {
                    lifeEventDaysLeft--
                } else if (lifeRng.nextInt(b.lifeEventInterval) == 0) {
                    // New life event starts: lasts 3–14 days
                    lifeEventDaysLeft = lifeRng.nextInt(3, 15)
                }
                val lifeEventActive = lifeEventDaysLeft > 0

                // ── Day-of-week for weekly rhythm (0=Mon … 6=Sun) ────────
                val dayOfWeek = simDate.dayOfWeek.value - 1

                // ── Deterministic clean/fail decision ─────────────────────
                val dayRng = Random(b.seed xor simDate.toEpochDay() xor 0xDEADBEEFL)
                val prob   = survivalProbability(b, dayOfWeek, lifeEventActive)
                val clean  = dayRng.nextDouble() < prob

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

    // ── Leaderboard ───────────────────────────────────────────────────────────

    data class LeaderboardEntry(
        val rank: Int,
        val name: String,
        val points: Int,
        val region: String,
        val isUser: Boolean,
        val botId: Int = -1,
        val winRate: Float = 0f,
        val archetype: String = "",       // BotArchetype.name — available for UI
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
