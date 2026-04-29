package com.tanay.warrior.ui.screens

// ── LeaderboardScreen.kt ─────────────────────────────────────────────────────
// [FIX]  Added empty-state UI when board is empty (Day 0 / bots not generated yet)
// [FIX]  Added "ALGORITHM" tab — third tab showing the full simulation equation
//        with every variable, formula, and archetype table, exactly as built.
// [KEEP] Existing "REGIONAL" and "GLOBAL" tabs are unchanged.

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tanay.warrior.data.BotArchetype
import com.tanay.warrior.data.BotProfile
import com.tanay.warrior.data.BotSimulator
import com.tanay.warrior.data.DATE_FORMATTER
import com.tanay.warrior.data.WarriorRegion
import com.tanay.warrior.data.tierOf
import com.tanay.warrior.ui.components.LiveRivalCard
import com.tanay.warrior.ui.theme.*
import java.time.LocalDate

private fun regionDisplayName(regionName: String): String =
    WarriorRegion.entries.firstOrNull { it.name == regionName }?.displayName ?: regionName

// ── Tab enum ──────────────────────────────────────────────────────────────────
private enum class LeaderboardTab { REGIONAL, GLOBAL, ALGORITHM }

@Composable
fun LeaderboardScreen(
    regionalBoard: List<BotSimulator.LeaderboardEntry>,
    globalBoard: List<BotSimulator.LeaderboardEntry>,
    userRegion: String,
    getBotProfile: (Int) -> BotProfile?,
    myStreak: Int    = 0,
    rivalStreak: Int = 0,
) {
    var activeTab        by remember { mutableStateOf(LeaderboardTab.REGIONAL) }
    var selectedBotId    by remember { mutableStateOf<Int?>(null) }

    val board = when (activeTab) {
        LeaderboardTab.REGIONAL  -> regionalBoard
        LeaderboardTab.GLOBAL    -> globalBoard
        LeaderboardTab.ALGORITHM -> emptyList()
    }
    val userEntry = board.find { it.isUser }

    selectedBotId?.let { botId ->
        val bot = getBotProfile(botId)
        if (bot != null) {
            BotProfileDialog(bot = bot, onDismiss = { selectedBotId = null })
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ────────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                "THE ARENA",
                fontSize = 11.sp, color = TextTertiary,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp
            )
            Text(
                "LEADERBOARD",
                fontSize = 26.sp, fontWeight = FontWeight.Black, color = Gold
            )

            Spacer(Modifier.height(12.dp))

            // Arena map + rival only shown on board tabs
            if (activeTab != LeaderboardTab.ALGORITHM) {
                LiveRivalCard(
                    myStreak    = myStreak,
                    rivalStreak = rivalStreak,
                    modifier    = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // User rank card
                userEntry?.let { u ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF1A0000), Color(0xFF0D0000)))
                            )
                            .border(1.dp, WarriorRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape)
                                    .background(Color(0xFF330000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚔️", fontSize = 22.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("YOUR RANK", fontSize = 10.sp, color = TextTertiary,
                                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "#${u.rank} • ${u.name}",
                                    fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary
                                )
                                Text(
                                    if (activeTab == LeaderboardTab.GLOBAL) "Global"
                                    else regionDisplayName(userRegion),
                                    fontSize = 11.sp, color = WarriorRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${u.points}",
                                    fontSize = 28.sp, fontWeight = FontWeight.Black, color = Gold
                                )
                                Text("pts", fontSize = 11.sp, color = TextTertiary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            // ── Tab row ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBlack)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton("🌍  REGIONAL", activeTab == LeaderboardTab.REGIONAL) {
                    activeTab = LeaderboardTab.REGIONAL
                }
                TabButton("🌐  GLOBAL", activeTab == LeaderboardTab.GLOBAL) {
                    activeTab = LeaderboardTab.GLOBAL
                }
                TabButton("⚙️  ALGO", activeTab == LeaderboardTab.ALGORITHM) {
                    activeTab = LeaderboardTab.ALGORITHM
                }
            }
        }

        // ── Content area ──────────────────────────────────────────────────────
        when (activeTab) {
            LeaderboardTab.ALGORITHM -> AlgorithmScreen()

            else -> {
                if (board.isEmpty() || board.all { it.points == 0 && !it.isUser }) {
                    // [FIX] Empty state — shown on Day 0 before any simulation has run
                    EmptyLeaderboardState(
                        isGlobal = activeTab == LeaderboardTab.GLOBAL,
                        region   = regionDisplayName(userRegion)
                    )
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(board.take(100)) { entry ->
                            LeaderboardRow(
                                entry   = entry,
                                onClick = { if (!entry.isUser && entry.botId >= 0) selectedBotId = entry.botId }
                            )
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyLeaderboardState(isGlobal: Boolean, region: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⏳", fontSize = 48.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            "SIMULATION LOADING",
            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
            color = Gold, letterSpacing = 3.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isGlobal)
                "1,050 warriors across 7 regions are being simulated. Log your first Victory or Relapse to kick off the competition."
            else
                "150 warriors from $region are being simulated. Log your first day to enter the board.",
            fontSize = 13.sp,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A0000))
                .border(1.dp, WarriorRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                "Tap ⚙️ ALGO to see how bots are scored while you wait.",
                fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center
            )
        }
    }
}

// ── Algorithm screen ──────────────────────────────────────────────────────────
// Shows the complete simulation engine — survival probability equation, all
// variables, archetype table, and the dynamic points scoring formula.
// This is the "old leaderboard" algorithm, now exposed as its own tab.
@Composable
private fun AlgorithmScreen() {
    val Mono = FontFamily.Monospace

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        // ── Section: Phantom Leaderboard intro ────────────────────────────
        AlgoSectionHeader("THE PHANTOM LEADERBOARD")
        AlgoBody(
            "1,050 simulated warriors — 150 per region — compete against you in real time. " +
            "Every bot has a name, a region, a personality archetype, and a seeded history. " +
            "The board updates every time you open the app."
        )

        Spacer(Modifier.height(20.dp))

        // ── Section: Survival Probability ─────────────────────────────────
        AlgoSectionHeader("SURVIVAL PROBABILITY EQUATION")
        AlgoBody("Every bot's daily outcome — clean or relapse — is decided by:")

        Spacer(Modifier.height(10.dp))
        AlgoEquationBlock(
            """P(clean) = clamp(
  σ(D)                     — sigmoid discipline base
+ 0.08·ln(1+M)            — logarithmic momentum boost
− 0.35·(1−e^(−F·S))       — plateau fatigue penalty
− Ps·σ(S−Sₜ)              — psychological pressure
+ A·sin(2π·d/7 + φ)       — weekly rhythm (weak-day wave)
+ Rm·0.05·e^(−0.01·Tf)    — recovery bonus after relapse
− Ev·0.55                 — life event penalty
, 0.05, 0.98)"""
        )

        Spacer(Modifier.height(16.dp))
        AlgoSectionHeader("VARIABLES")

        AlgoVarRow("D",  "baseDiscipline (0.0–1.0)", "Core willpower. Higher = cleaner bot. Seeded per bot.")
        AlgoVarRow("M",  "momentum (0–50)", "Builds on clean days (+1), drops on relapse (−3). Max 50.")
        AlgoVarRow("F",  "fatigueFactor (0.003–0.020)", "How fast a long streak fatigues this bot. Tier 1 = low, Tier 5 = high.")
        AlgoVarRow("S",  "currentStreak", "Consecutive clean days right now. Drives fatigue and pressure terms.")
        AlgoVarRow("Ps", "pressureSensitivity (0.0–1.0)", "Archetype-driven. Sprinter = 0.70, Grinder = 0.05.")
        AlgoVarRow("Sₜ", "streakThreshold", "Streak length before psychological pressure activates. Sprinter = 14, Grinder = 40.")
        AlgoVarRow("A",  "rhythmAmplitude (0.01–0.10)", "How much the bot's weekly cycle swings. Personal.")
        AlgoVarRow("φ",  "rhythmPhaseOffset (0–2π)", "Shifts which weekday is the bot's weakest day. Unique per bot.")
        AlgoVarRow("Rm", "recoveryMultiplier", "Archetype recovery bonus. Comeback Kid = 2.5×, Fragile Elite = 0.6×.")
        AlgoVarRow("Tf", "totalFailDays", "All-time relapse count. Recovery bonus fades as failures accumulate.")
        AlgoVarRow("Ev", "lifeEventSeverity (0.2–0.7)", "Active only during disruption windows. Tanks P(clean) by up to 55%.")

        Spacer(Modifier.height(20.dp))
        AlgoSectionHeader("LIFE EVENTS")
        AlgoBody(
            "Every bot has a personal disruption interval of 30–70 days. When triggered, " +
            "a life event lasts 3–14 days and applies the Ev·0.55 penalty across that window. " +
            "This is why a top-10 bot suddenly drops 40 places and then climbs back — " +
            "something happened to them. The interval and severity are seeded and fixed per bot."
        )

        Spacer(Modifier.height(20.dp))
        AlgoSectionHeader("WEEKLY RHYTHM")
        AlgoBody(
            "The sin wave term A·sin(2π·d/7 + φ) gives every bot a personal weak day " +
            "and strong day each week. Positive peak = strong day, negative = weak day. " +
            "φ is different for every bot, so not everyone fails on Sunday."
        )

        Spacer(Modifier.height(20.dp))
        AlgoSectionHeader("ARCHETYPES")
        AlgoBody("Each bot is assigned one of 6 personality types at generation:")

        Spacer(Modifier.height(10.dp))
        AlgoArchetypeTable()

        Spacer(Modifier.height(20.dp))
        AlgoSectionHeader("DYNAMIC POINTS SCORING")
        AlgoBody("Points scale with consistency — not just days clean. Same formula for bots and the user:")

        Spacer(Modifier.height(10.dp))
        AlgoEquationBlock(
            """CLEAN DAY:
  base          = 2
  streak bonus  = floor(streak / 7)      (+1 per full week)
  momentum bonus= floor(momentum / 10)   (+1 per 10 momentum)
  earned        = base + streak_bonus + momentum_bonus

RELAPSE PENALTY:
  base_loss     = 3
  streak_tax    = floor(streak / 5)      (longer run = harder fall)
  total_loss    = min(base_loss + streak_tax, 12)"""
        )

        Spacer(Modifier.height(10.dp))
        AlgoBody(
            "A bot on a 30-day streak earns ~6 pts/day. A relapse from that streak costs ~9 pts. " +
            "This creates real leaderboard drama — ranks shift every day."
        )

        Spacer(Modifier.height(20.dp))
        AlgoSectionHeader("CAN YOU WIN?")
        AlgoBody(
            "Yes — but only with genuine consistency. To crack the global top 10 you need roughly:\n" +
            "• 45+ day streak, OR\n" +
            "• 80+ total clean days with a strong win rate\n\n" +
            "A casual user won't outrank Tier 1 Grinders. A focused 60-day streak user will " +
            "overtake Sprinters who keep collapsing. The competition is earned, not handed to you."
        )

        Spacer(Modifier.height(20.dp))
        AlgoSectionHeader("DETERMINISM")
        AlgoBody(
            "Every bot's stats, name, archetype, and daily outcomes are fully deterministic — " +
            "seeded from a fixed value. The same bot always has their rough patch in week 6. " +
            "Their story never changes between launches, making them feel like real characters."
        )

        Spacer(Modifier.height(32.dp))
    }
}

// ── Algorithm UI helpers ──────────────────────────────────────────────────────

@Composable
private fun AlgoSectionHeader(text: String) {
    Text(
        text,
        fontSize = 11.sp, color = Gold,
        fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun AlgoBody(text: String) {
    Text(text, fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp)
}

@Composable
private fun AlgoEquationBlock(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF080808))
            .border(1.dp, WarriorRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF00FF88),
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun AlgoVarRow(variable: String, label: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            variable,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Gold,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(36.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(description, fontSize = 10.sp, color = TextDim, lineHeight = 14.sp)
        }
    }
    Divider(color = BorderColor.copy(alpha = 0.4f), thickness = 0.5.dp)
}

@Composable
private fun AlgoArchetypeTable() {
    val archetypes = listOf(
        Triple("⚙️ Grinder",       "Ps=0.05, Sₜ=40", "Slow and steady. Rarely spikes or crashes. Most consistent."),
        Triple("⚡ Sprinter",      "Ps=0.70, Sₜ=14", "Builds huge streaks fast, then cracks under psychological pressure."),
        Triple("🔄 Comeback Kid",  "Ps=0.30, Rm=2.5×","Fails often but recovers faster than anyone. High bounce-back rate."),
        Triple("💎 Fragile Elite", "Ps=0.85, Sₜ=10", "Very high discipline but psychologically brittle. One bad week unravels them."),
        Triple("🐉 Underdog",      "Ps=0.20, Sₜ=30", "Low base discipline but capable of surprise winning runs."),
        Triple("📉 Plateauer",     "Ps=0.15, A×1.5", "Good early progress then long flat stretches of mediocrity.")
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBlack)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        archetypes.forEach { (name, params, desc) ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        params,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Gold
                    )
                }
                Text(desc, fontSize = 11.sp, color = TextDim, lineHeight = 15.sp)
            }
            if (name != archetypes.last().first) {
                Divider(color = BorderColor.copy(alpha = 0.3f), thickness = 0.5.dp)
            }
        }
    }
}

// ── Leaderboard row ───────────────────────────────────────────────────────────
@Composable
private fun LeaderboardRow(entry: BotSimulator.LeaderboardEntry, onClick: () -> Unit) {
    val rankColor = when (entry.rank) {
        1    -> Color(0xFFFFD700)
        2    -> Color(0xFFC0C0C0)
        3    -> Color(0xFFCD7F32)
        else -> if (entry.isUser) WarriorRed else TextTertiary
    }
    val bg = if (entry.isUser)
        Brush.horizontalGradient(listOf(Color(0xFF1A0000), Color(0xFF0A0000)))
    else
        Brush.horizontalGradient(listOf(CardBlack, CardBlack))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(
                if (entry.isUser) Modifier.border(1.dp, WarriorRed.copy(0.3f), RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            )
            .clickable(enabled = !entry.isUser) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "#${entry.rank}",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Black,
            color      = rankColor,
            modifier   = Modifier.width(40.dp)
        )

        if (entry.rank <= 3) {
            Text(
                when (entry.rank) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" },
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
        } else {
            Spacer(Modifier.width(22.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.name,
                fontSize   = 14.sp,
                fontWeight = if (entry.isUser) FontWeight.Black else FontWeight.SemiBold,
                color      = if (entry.isUser) TextPrimary else TextSecondary,
                maxLines   = 1
            )
            if (!entry.isUser) {
                val archetypeShort = when (entry.archetype) {
                    BotArchetype.GRINDER.name       -> "Grinder"
                    BotArchetype.SPRINTER.name      -> "Sprinter"
                    BotArchetype.COMEBACK_KID.name  -> "Comeback Kid"
                    BotArchetype.FRAGILE_ELITE.name -> "Fragile Elite"
                    BotArchetype.UNDERDOG.name      -> "Underdog"
                    BotArchetype.PLATEAUER.name     -> "Plateauer"
                    else                            -> ""
                }
                val sub = buildString {
                    append("${entry.winRate.toInt()}% win")
                    if (entry.currentStreak > 0) append(" · 🔥${entry.currentStreak}d")
                    if (archetypeShort.isNotBlank()) append(" · $archetypeShort")
                }
                Text(sub, fontSize = 10.sp, color = TextDim, maxLines = 1)
            } else {
                Text("YOU", fontSize = 10.sp, color = WarriorRed,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }

        Text(
            "${entry.points} pts",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,
            color      = if (entry.isUser) Gold else TextSecondary
        )
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF1A0000) else Color.Transparent)
            .then(
                if (selected) Modifier.border(1.dp, WarriorRed.copy(0.5f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = if (selected) WarriorRed else TextTertiary,
            letterSpacing = 0.8.sp
        )
    }
}

// ── Bot Profile Dialog ────────────────────────────────────────────────────────
@Composable
fun BotProfileDialog(bot: BotProfile, onDismiss: () -> Unit) {
    val calendar = remember(bot.id) { BotSimulator.realSimulatedCalendar(bot) }
    val total    = (bot.totalCleanDays + bot.totalFailDays).coerceAtLeast(1)
    val winRate  = (bot.totalCleanDays.toFloat() / total * 100f).toInt()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgBlack.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "WARRIOR INTEL",
                        fontSize = 11.sp, color = TextTertiary,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Close", tint = TextTertiary)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBlack)
                        .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                            .background(Color(0xFF111111))
                            .border(2.dp, BorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person, "Bot",
                            tint = TextTertiary, modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(bot.name, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        Text(regionDisplayName(bot.region), fontSize = 12.sp, color = TextTertiary)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Tier ${tierOf(bot)}",
                                fontSize = 11.sp, color = WarriorRed,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                            )
                            Text("•", fontSize = 11.sp, color = TextDim)
                            val archetypeLabel = when (
                                runCatching { BotArchetype.valueOf(bot.archetype) }.getOrDefault(BotArchetype.GRINDER)
                            ) {
                                BotArchetype.GRINDER       -> "⚙️ Grinder"
                                BotArchetype.SPRINTER      -> "⚡ Sprinter"
                                BotArchetype.COMEBACK_KID  -> "🔄 Comeback Kid"
                                BotArchetype.FRAGILE_ELITE -> "💎 Fragile Elite"
                                BotArchetype.UNDERDOG      -> "🐉 Underdog"
                                BotArchetype.PLATEAUER     -> "📉 Plateauer"
                            }
                            Text(archetypeLabel, fontSize = 11.sp, color = Gold, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("POINTS",   "${bot.points}",             Gold,          Modifier.weight(1f))
                    StatCard("WIN RATE", "$winRate%",                 VictoryGreen,  Modifier.weight(1f))
                    StatCard("STREAK",   "${bot.currentStreak} days", TextSecondary, Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("VICTORIES", "${bot.totalCleanDays}", VictoryGreen, Modifier.weight(1f))
                    StatCard("DEFEATS",   "${bot.totalFailDays}",  WarriorRed,   Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "365-DAY RECORD",
                    fontSize = 11.sp, color = TextTertiary,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp
                )
                Spacer(Modifier.height(10.dp))
                BotCalendarHeatmap(calendar = calendar)

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String, value: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBlack)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = valueColor)
        Text(label, fontSize = 9.sp, color = TextDim, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BotCalendarHeatmap(calendar: Map<String, Boolean?>) {
    val today     = LocalDate.now()
    val start     = today.minusDays(364)
    val dayOfWeek = start.dayOfWeek.value % 7

    val cells = mutableListOf<Boolean?>()
    repeat(dayOfWeek) { cells.add(null) }
    var d = start
    while (!d.isAfter(today)) {
        cells.add(calendar[d.format(DATE_FORMATTER)])
        d = d.plusDays(1)
    }

    val cellSize = 10.dp
    val gap      = 2.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBlack)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            val weeks = (cells.size + 6) / 7
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                repeat(weeks) { w ->
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(7) { dayInWeek ->
                            val idx   = w * 7 + dayInWeek
                            val value = cells.getOrNull(idx)
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        when (value) {
                                            true  -> VictoryGreen.copy(alpha = 0.85f)
                                            false -> WarriorRed.copy(alpha = 0.75f)
                                            null  -> Color(0xFF111111)
                                        }
                                    )
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(VictoryGreen,       "Clean")
                LegendDot(WarriorRed,         "Relapse")
                LegendDot(Color(0xFF333333),  "Not yet")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, fontSize = 10.sp, color = TextDim)
    }
}
