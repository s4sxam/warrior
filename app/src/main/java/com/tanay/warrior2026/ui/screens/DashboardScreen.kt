package com.tanay.warrior.ui.screens

// [UPDATE] v5.0.0: WarRoomBackground particle system added as bottom layer.
//                  StreakRingColor replaces hardcoded ring color logic.
//                  LiveRival + CommanderVoice already wired in v2.4.0 (no change needed).

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior.data.WarriorState
import com.tanay.warrior.ui.theme.*
import com.tanay.warrior.ui.components.BlackoutOverlay
import com.tanay.warrior.ui.components.WarriorRankCard
import com.tanay.warrior.ui.components.ConfessionalSheet
import com.tanay.warrior.ui.components.EvolvingStreakNumber
import com.tanay.warrior.ui.components.GlitchOverlay
import com.tanay.warrior.ui.components.HoloRadarRing
import com.tanay.warrior.ui.components.MilestoneBurst
import com.tanay.warrior.ui.components.ShatterOverlay
import com.tanay.warrior.ui.components.SlashOverlay
import com.tanay.warrior.ui.components.StreakFuneralOverlay
import com.tanay.warrior.ui.components.VillainArcOverlay
import com.tanay.warrior.ui.components.WarRoomBackground          // ← NEW v5.0.0
import com.tanay.warrior.ui.components.rememberStreakRingColor    // ← NEW v5.0.0
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Motivational quotes pool ──────────────────────────────────────────────────
private val QUOTES = listOf(
    "Discipline is choosing between what you want NOW and what you want MOST.",
    "Every day clean is a vote for the man you're becoming.",
    "The pain of discipline is nothing compared to the pain of regret.",
    "Warriors are not born. They are built — one clean day at a time.",
    "Control your mind or it will control you.",
    "Your future self is watching. Make him proud.",
    "Suffering now. Champion later.",
    "The enemy inside you is the only enemy that matters.",
    "Hard choices, easy life. Easy choices, hard life.",
    "Don't wish it were easier. Make yourself stronger.",
)

@Composable
fun DashboardScreen(
    state: WarriorState,
    onPanicClick: () -> Unit,
    onVictoryClick: () -> Unit,
    onRelapseClick: () -> Unit,
    onSaveConfession: (String) -> Unit = {},
) {
    val streakAnim by animateIntAsState(
        targetValue   = state.streak,
        animationSpec = tween(900, easing = EaseOutCubic),
        label         = "streak"
    )
    val bestAnim by animateIntAsState(
        targetValue   = state.bestStreak,
        animationSpec = tween(900, easing = EaseOutCubic),
        label         = "best"
    )

    val quote = remember { QUOTES.random() }

    val yesterdayFailed = remember(state.history) {
        val yKey = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        state.history[yKey]?.status == "failed"
    }
    val todayLogged = state.isTodayLogged()
    val todayStatus = remember(state.history) {
        state.history[LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)]?.status
    }

    val consecutiveRelapses = remember(state.history) {
        var count = 0
        var date  = LocalDate.now()
        while (true) {
            val key = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            if (state.history[key]?.status == "failed") { count++; date = date.minusDays(1) }
            else break
        }
        count
    }

    val lastDeadStreak = remember(state.bestStreak, state.streak) {
        if (state.streak == 0) state.bestStreak.coerceAtLeast(1) else 0
    }

    val showFuneral = remember(state.streak, todayStatus) {
        state.streak == 0 && todayStatus == "failed"
    }

    val isGlitching = remember(state.streak, todayStatus) {
        state.streak == 0 && todayStatus == "failed"
    }
    val isShattered = isGlitching
    val isSlashing  = remember(todayStatus) { todayStatus == "clean" }

    val showConfessional = remember(todayStatus) { todayStatus == "failed" }
    var confessionalDismissed by remember(todayStatus) { mutableStateOf(false) }

    // ── Root Box: all layers stacked ──────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // ── [NEW v5.0.0] War Room Background — bottommost layer ───────────────
        // Sits behind ALL content. Reads streak to evolve its particle mode:
        //   Day 0–9:   digital rain / fog
        //   Day 10–29: transition (rain fades, first embers)
        //   Day 30+:   glowing embers drifting upward
        //   Day 90+:   more embers, larger radius, near-white gold cores
        WarRoomBackground(
            streak   = streakAnim,
            modifier = Modifier.fillMaxSize(),
        )

        // ── Scrollable content ─────────────────────────────────────────────
        // [FIX] Overlay alpha was hardcoded 0.72f (very dark regardless of system
        // brightness). Now adapts to streak tier so the War Room background breathes:
        //   Day 0–9   → 0.55f  (digital rain shows through clearly)
        //   Day 10–29 → 0.58f  (transition, first embers bleeding in)
        //   Day 30+   → 0.62f  (embers glow richly behind content)
        //   Day 90+   → 0.60f  (legend — max particle brightness wins)
        val overlayAlpha = when {
            streakAnim >= 90 -> 0.60f
            streakAnim >= 30 -> 0.62f
            streakAnim >= 10 -> 0.58f
            else             -> 0.55f
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Semi-transparent surface so background particles show through
                .background(BgBlack.copy(alpha = overlayAlpha))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (yesterdayFailed && !todayLogged) {
                Spacer(Modifier.height(4.dp))
                RecoveryCard(bestStreak = state.bestStreak)
                Spacer(Modifier.height(14.dp))
            }

            // ── [UPDATED v5.0.0] StreakHero now uses animated ring color ──
            StreakHero(
                streak      = streakAnim,
                best        = bestAnim,
                todayStatus = todayStatus
            )

            Spacer(Modifier.height(16.dp))

            WarriorRankCard(
                streak   = streakAnim,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBlack)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    "\"$quote\"",
                    fontSize   = 12.sp,
                    color      = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    textAlign  = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier   = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick  = onPanicClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .shadow(8.dp, CircleShape, spotColor = WarriorRed.copy(alpha = 0.4f)),
                shape  = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = WarriorRed)
            ) {
                Icon(Icons.Filled.Warning, contentDescription = "Emergency", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("PANIC BUTTON", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White, letterSpacing = 1.sp)
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick  = onVictoryClick,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape    = RoundedCornerShape(18.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = VictoryGreen),
                    enabled  = !todayLogged || todayStatus != "clean"
                ) {
                    Text("I STAY CLEAN", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Black)
                }
                OutlinedButton(
                    onClick  = onRelapseClick,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape    = RoundedCornerShape(18.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = WarriorRed),
                    border   = BorderStroke(1.dp, Color(0xFF2A0000)),
                    enabled  = !todayLogged || todayStatus != "failed"
                ) {
                    Text("I FAILED", fontWeight = FontWeight.Black, fontSize = 12.sp, color = WarriorRed)
                }
            }

            if (todayLogged) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (todayStatus == "clean") "✅ Today logged. Tap UNDO in header to correct."
                    else "❌ Relapse logged. Tomorrow is a new war.",
                    fontSize   = 10.sp,
                    color      = if (todayStatus == "clean") VictoryGreen else WarriorRed,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
            }

            if (showConfessional && !confessionalDismissed) {
                Spacer(Modifier.height(20.dp))
                ConfessionalSheet(
                    onSubmit = { text ->
                        onSaveConfession(text)
                        confessionalDismissed = true
                    },
                    lastConfession = state.lastConfession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            Spacer(Modifier.height(22.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("BATTLE CALENDAR", fontSize = 10.sp, color = TextSecondary,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(14.dp))
                    BattleCalendar(state = state)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendItem(color = VictoryGreen, label = "Clean")
                        LegendItem(color = WarriorRed,   label = "Failed")
                        LegendItem(color = CardBlack,    label = "Not logged", border = true)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("MADE BY TANAY × EL", fontSize = 9.sp, color = TextDimmest,
                fontWeight = FontWeight.Black, letterSpacing = 5.sp)
        }

        // ── Overlay layers ─────────────────────────────────────────────────
        VillainArcOverlay(relapseCount = consecutiveRelapses)
        MilestoneBurst(streak = streakAnim)
        GlitchOverlay(isGlitching = isGlitching)
        ShatterOverlay(isShattered = isShattered)
        SlashOverlay(isSlashing = isSlashing)
        StreakFuneralOverlay(deadStreak = lastDeadStreak, visible = showFuneral)
        BlackoutOverlay(streak = streakAnim)
    }
}

// ── Streak hero with animated ring color ──────────────────────────────────────
// [UPDATED v5.0.0] ringColor now comes from rememberStreakRingColor() which
// animates smoothly: rusty iron (Day 0) → steel blue (Day 30) → radiant gold (Day 90+)

@Composable
private fun StreakHero(streak: Int, best: Int, todayStatus: String?) {
    val ringPct = if (best > 0) (streak.toFloat() / best).coerceIn(0f, 1f) else 0f
    val animRing by animateFloatAsState(
        targetValue   = ringPct,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label         = "ring"
    )

    // [NEW v5.0.0] Animated ring color replacing the hardcoded when-block.
    // Milestones: Day 0 = dim iron, Day 30 = steel blue, Day 90 = radiant gold.
    val ringColor by rememberStreakRingColor(streak = streak)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF100000), BgBlack))
            )
            .border(1.dp, BorderColor, RoundedCornerShape(28.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(120.dp)
            ) {
                HoloRadarRing(modifier = Modifier.fillMaxSize())

                androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp)) {
                    val stroke = 10.dp.toPx()
                    val inset  = stroke / 2f
                    // Track (background arc)
                    drawArc(
                        color      = Color(0xFF1A1A1A),
                        startAngle = -90f, sweepAngle = 360f,
                        useCenter  = false,
                        topLeft    = Offset(inset, inset),
                        size       = Size(size.width - stroke, size.height - stroke),
                        style      = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    // Progress arc — color animates via rememberStreakRingColor
                    drawArc(
                        color      = ringColor,
                        startAngle = -90f, sweepAngle = 360f * animRing,
                        useCenter  = false,
                        topLeft    = Offset(inset, inset),
                        size       = Size(size.width - stroke, size.height - stroke),
                        style      = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EvolvingStreakNumber(streak = streak, fontSize = 38.sp)
                    Text(
                        "DAYS",
                        fontSize      = 8.sp,
                        color         = TextTertiary,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$best", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Gold)
                    Text("BEST", fontSize = 8.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val statusIcon = when (todayStatus) {
                        "clean"  -> "✅"
                        "failed" -> "❌"
                        else     -> "⬜"
                    }
                    Text(statusIcon, fontSize = 24.sp)
                    Text("TODAY", fontSize = 8.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
            }
        }
    }
}

// ── Recovery card ──────────────────────────────────────────────────────────────
@Composable
private fun RecoveryCard(bestStreak: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0D0500))
            .border(1.dp, Color(0xFF3A1A00), RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Text("⚡ BOUNCE BACK", fontSize = 11.sp, color = Color(0xFFFF9800),
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Yesterday was a loss. Today is a new war.\nYour best streak was $bestStreak days — you've done it before. Do it again.",
                fontSize   = 13.sp,
                color      = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

// ── Calendar ───────────────────────────────────────────────────────────────────
@Composable
fun BattleCalendar(state: WarriorState) {
    val today       = remember { LocalDate.now() }
    val firstDay    = remember { today.withDayOfMonth(1) }
    val startOffset = remember { firstDay.dayOfWeek.value % 7 }
    val daysInMonth = remember { today.month.length(today.isLeapYear) }
    val fmt         = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    val dowHeaders = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        dowHeaders.forEach { d ->
            Text(d, modifier = Modifier.weight(1f), fontSize = 7.sp,
                fontWeight = FontWeight.ExtraBold, color = TextTertiary,
                textAlign  = TextAlign.Center)
        }
    }
    Spacer(Modifier.height(6.dp))

    val cells: List<Int?> = List(startOffset) { null } + (1..daysInMonth).toList()
    cells.chunked(7).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (row + List(7 - row.size) { null }).forEach { day ->
                if (day == null) {
                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                } else {
                    val key     = firstDay.withDayOfMonth(day).format(fmt)
                    val data    = state.history[key]
                    val isToday = firstDay.withDayOfMonth(day) == today
                    val bg = when (data?.status) {
                        "clean"  -> VictoryGreen
                        "failed" -> WarriorRed
                        else     -> CardBlack
                    }
                    val tc = when (data?.status) {
                        "clean"  -> Color.Black
                        "failed" -> Color.White
                        else     -> if (isToday) Color.White else TextTertiary
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f).aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .then(
                                if (isToday && data == null)
                                    Modifier.border(1.5.dp, WarriorRed, RoundedCornerShape(8.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$day", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = tc)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ── Legend item ────────────────────────────────────────────────────────────────
@Composable
private fun LegendItem(color: Color, label: String, border: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp))
                .background(color)
                .then(if (border) Modifier.border(1.dp, BorderColor, RoundedCornerShape(2.dp)) else Modifier)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
    }
}

// ── Glass Card ─────────────────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(GlassSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(22.dp))
            .padding(20.dp),
        content = content
    )
}

// ── Stat card ──────────────────────────────────────────────────────────────────
@Composable
fun StatCard(
    modifier:   Modifier = Modifier,
    label:      String,
    value:      String,
    sub:        String,
    valueColor: Color
) {
    GlassCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 8.sp, color = TextTertiary,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 48.sp, fontWeight = FontWeight.Black,
                color = valueColor, lineHeight = 50.sp)
            Text(sub, fontSize = 8.sp, color = TextTertiary,
                fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        }
    }
}
