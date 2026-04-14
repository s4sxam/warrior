package com.tanay.warrior2026.data

// ── [NEW] BotSimulator.kt ─────────────────────────────────────────────────────
// Runs the Momentum & Fatigue Algorithm for all 1,050 bots.
// Called every time the user opens the app — advances simulation day by day.

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object BotSimulator {

    private val json = Json { ignoreUnknownKeys = true }

    /** Deserialize bots from stored JSON string. Returns generated bots if empty. */
    fun loadBots(botsJson: String): List<BotProfile> {
        if (botsJson.isBlank()) return generateBots()
        return runCatching {
            json.decodeFromString<List<BotProfile>>(botsJson)
        }.getOrElse { generateBots() }
    }

    /** Serialize bots to JSON string for storage. */
    fun saveBots(bots: List<BotProfile>): String =
        json.encodeToString(bots)

    /**
     * Advance the simulation from each bot's lastSimulatedDay up to yesterday.
     * We never simulate today — the user's own log determines today.
     * Returns updated list.
     */
    fun advanceSimulation(bots: List<BotProfile>): List<BotProfile> {
        val yesterday  = LocalDate.now().minusDays(1)
        val todayStr   = todayKey()

        return bots.map { bot ->
            var b = bot

            // Determine the start date for simulation
            val startDate: LocalDate = if (b.lastSimulatedDay.isBlank()) {
                LocalDate.now().minusDays(365)  // first ever run: bootstrap 365 days
            } else {
                runCatching {
                    LocalDate.parse(b.lastSimulatedDay, DATE_FORMATTER).plusDays(1)
                }.getOrElse { yesterday }
            }

            var simDate = startDate
            val rng = Random(b.seed xor simDate.toEpochDay())

            while (!simDate.isAfter(yesterday)) {
                val prob  = survivalProbability(b)
                // Use a seeded random per-day so results are reproducible
                val dayRng = Random(b.seed xor simDate.toEpochDay() xor 0xDEADBEEFL)
                val clean = dayRng.nextDouble() < prob

                if (clean) {
                    b = b.copy(
                        points        = b.points + 2,
                        currentStreak = b.currentStreak + 1,
                        momentum      = min(b.momentum + 1.0, 50.0),
                        totalCleanDays = b.totalCleanDays + 1,
                        lastSimulatedDay = simDate.format(DATE_FORMATTER)
                    )
                } else {
                    b = b.copy(
                        points        = 0,
                        currentStreak = 0,
                        momentum      = max(b.momentum - 3.0, 0.0),
                        totalFailDays  = b.totalFailDays + 1,
                        lastSimulatedDay = simDate.format(DATE_FORMATTER)
                    )
                }
                simDate = simDate.plusDays(1)
            }
            b
        }
    }

    /**
     * Build a leaderboard entry list sorted by points descending.
     * Combines user + bots for the given region, then global.
     */
    data class LeaderboardEntry(
        val rank: Int,
        val name: String,
        val points: Int,
        val region: String,
        val isUser: Boolean,
        val botId: Int = -1,        // -1 for real user
        val winRate: Float = 0f     // 0..100
    )

    fun regionalLeaderboard(
        bots: List<BotProfile>,
        userRegion: String,
        userName: String,
        userPoints: Int
    ): List<LeaderboardEntry> {
        val regional = bots.filter { it.region == userRegion }
        return buildBoard(regional, userName, userPoints, userRegion)
    }

    fun globalLeaderboard(
        bots: List<BotProfile>,
        userName: String,
        userPoints: Int,
        userRegion: String
    ): List<LeaderboardEntry> {
        return buildBoard(bots, userName, userPoints, userRegion)
    }

    private fun buildBoard(
        bots: List<BotProfile>,
        userName: String,
        userPoints: Int,
        userRegion: String
    ): List<LeaderboardEntry> {
        val entries = mutableListOf<LeaderboardEntry>()

        // Add all bots
        bots.forEach { bot ->
            val total = (bot.totalCleanDays + bot.totalFailDays).coerceAtLeast(1)
            entries.add(
                LeaderboardEntry(
                    rank     = 0,
                    name     = bot.name,
                    points   = bot.points,
                    region   = bot.region,
                    isUser   = false,
                    botId    = bot.id,
                    winRate  = (bot.totalCleanDays.toFloat() / total * 100f)
                )
            )
        }

        // Add user
        entries.add(
            LeaderboardEntry(
                rank    = 0,
                name    = userName.ifBlank { "You" },
                points  = userPoints,
                region  = userRegion,
                isUser  = true,
                winRate = 0f
            )
        )

        // Sort and rank
        val sorted = entries.sortedByDescending { it.points }
        return sorted.mapIndexed { i, e -> e.copy(rank = i + 1) }
    }
}
