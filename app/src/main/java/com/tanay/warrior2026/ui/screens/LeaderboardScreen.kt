package com.tanay.warrior.ui.screens

// ── [NEW] LeaderboardScreen.kt ────────────────────────────────────────────────
// The Phantom Leaderboard — regional + global tabs, live bot profiles
// [WIRED v+1] ArenaMapCard added above user rank card.
//             LiveRivalCard added below ArenaMap, before tab toggle.

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
import com.tanay.warrior.ui.components.ArenaMapCard
import com.tanay.warrior.ui.components.LiveRivalCard
import com.tanay.warrior.ui.theme.*
import java.time.LocalDate

@Composable
fun LeaderboardScreen(
    regionalBoard: List<BotSimulator.LeaderboardEntry>,
    globalBoard: List<BotSimulator.LeaderboardEntry>,
    userRegion: String,
    getBotProfile: (Int) -> BotProfile?,
    myStreak: Int    = 0,
    rivalStreak: Int = 0,
) {
    var isGlobal         by remember { mutableStateOf(false) }
    var selectedBotId    by remember { mutableStateOf<Int?>(null) }
    val board = if (isGlobal) globalBoard else regionalBoard
    val userEntry = board.find { it.isUser }

    val arenaPlayers = remember(board) {
        board.take(7).map { it.name to it.points }
    }

    selectedBotId?.let { botId ->
        val bot = getBotProfile(botId)
        if (bot != null) {
            BotProfileDialog(bot = bot, onDismiss = { selectedBotId = null })
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

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

            // ── ARENA MAP ───────────────────────────────────────────────────
            ArenaMapCard(
                players  = arenaPlayers,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // ── LIVE RIVAL ──────────────────────────────────────────────────
            LiveRivalCard(
                myStreak    = myStreak,
                rivalStreak = rivalStreak,
                modifier    = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // ── User rank card ──────────────────────────────────────────────
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
                                if (isGlobal) "Global" else regionDisplayName(userRegion),
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

            // ── Regional / Global toggle ────────────────────────────────────
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBlack)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton("🌍  REGIONAL", !isGlobal) { isGlobal = false }
                TabButton("🌐  GLOBAL",   isGlobal)  { isGlobal = true  }
            }
        }

        // ── Board list ───────────────────────────────────────────────────────
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

// ── Row ───────────────────────────────────────────────────────────────────────
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontSize   = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = if (selected) WarriorRed else TextTertiary,
            letterSpacing = 1.sp
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
