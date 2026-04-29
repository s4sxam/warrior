package com.tanay.warrior.ui.screens

// ─────────────────────────────────────────────────────────────────
// DashboardScreen.kt  — v6.0.0 (Redesign)
//
// CORE FIXES:
//   1. ONE dominant action per state (LOG TODAY button / ALREADY LOGGED)
//   2. Visual hierarchy: Streak number → Action button → Stats → Quote
//   3. Removed: GlitchOverlay, ShatterOverlay, SlashOverlay (too many effects)
//   4. Removed: VillainArcOverlay, StreakFuneralOverlay (cognitive overload)
//   5. Kept: MilestoneBurst only (1 signature effect, earns its place)
//   6. Kept: WarRoomBackground but at 20% opacity so it doesn't compete
//   7. Kept: StreakHero ring — emotionally resonant, not expensive
//   8. RecoveryCard shown when yesterday failed — clear contextual prompt
//   9. BattleCalendar moved to BOTTOM (supporting detail, not hero)
//  10. Performance: no continuous per-frame animations on main screen
// ─────────────────────────────────────────────────────────────────

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.tanay.warrior.ui.components.MilestoneBurst
import com.tanay.warrior.ui.components.WarRoomBackground
import com.tanay.warrior.ui.components.rememberStreakRingColor
import com.tanay.warrior.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Quotes pool ───────────────────────────────────────────────
private val QUOTES = listOf(
    "Discipline is choosing between what you want now and what you want most.",
    "Every day clean is a vote for the man you're becoming.",
    "The pain of discipline is nothing compared to the pain of regret.",
    "Warriors are not born. They are built — one clean day at a time.",
    "Control your mind or it will control you.",
    "Hard choices, easy life. Easy choices, hard life.",
)

// ── Action state — drives the ONE dominant button ─────────────
private sealed class DayActionState {
    object NotLogged  : DayActionState()
    object LoggedWin  : DayActionState()
    object LoggedLoss : DayActionState()
}

@Composable
fun DashboardScreen(
    state: WarriorState,
    onPanicClick: () -> Unit,
    onVictoryClick: () -> Unit,
    onRelapseClick: () -> Unit,
    onSaveConfession: (String) -> Unit = {},
) {
    // ── Animate streak count ──────────────────────────────────
    val streakAnim by animateIntAsState(
        targetValue   = state.streak,
        animationSpec = tween(700, easing = EaseOutCubic),
        label         = "streak"
    )

    val quote = remember { QUOTES.random() }

    val todayStatus = remember(state.history) {
        state.history[LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)]?.status
    }
    val yesterdayFailed = remember(state.history) {
        val yKey = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        state.history[yKey]?.status == "failed"
    }

    val actionState: DayActionState = when (todayStatus) {
        "clean"  -> DayActionState.LoggedWin
        "failed" -> DayActionState.LoggedLoss
        else     -> DayActionState.NotLogged
    }

    // ── Root ──────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // Subtle animated background — low opacity, doesn't compete
        WarRoomBackground(
            streak   = streakAnim,
            modifier = Modifier.fillMaxSize(),
        )

        // Scrim so content stays readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgBlack.copy(alpha = 0.82f))
        )

        // Scrollable content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── 1. Recovery nudge (contextual, not always on screen) ──
            if (yesterdayFailed && actionState == DayActionState.NotLogged) {
                RecoveryCard(bestStreak = state.bestStreak)
                Spacer(Modifier.height(16.dp))
            }

            // ── 2. Streak Hero — emotional anchor ─────────────────────
            StreakHero(
                streak      = streakAnim,
                best        = state.bestStreak,
                todayStatus = todayStatus,
            )

            Spacer(Modifier.height(24.dp))

            // ── 3. THE ONE ACTION — dominant, impossible to miss ───────
            PrimaryActionButton(
                state     = actionState,
                onVictory = onVictoryClick,
                onRelapse = onRelapseClick,
                onPanic   = onPanicClick,
            )

            Spacer(Modifier.height(20.dp))

            // ── 4. Quick stats row — secondary, subdued ────────────────
            QuickStatsRow(state = state)

            Spacer(Modifier.height(20.dp))

            // ── 5. Quote card — quiet, tertiary ───────────────────────
            QuoteCard(quote = quote)

            Spacer(Modifier.height(24.dp))

            // ── 6. Battle Calendar — supporting detail ─────────────────
            SectionLabel("THIS MONTH")
            Spacer(Modifier.height(10.dp))
            BattleCalendar(state = state)

            Spacer(Modifier.height(32.dp))

            Text(
                "MADE BY TANAY × EL",
                fontSize      = 9.sp,
                color         = TextDisabled,
                fontWeight    = FontWeight.Black,
                letterSpacing = 5.sp,
            )
        }

        // ── Effect: only MilestoneBurst — earned, brief, meaningful ───
        MilestoneBurst(streak = streakAnim)
    }
}

// ─────────────────────────────────────────────────────────────
// PRIMARY ACTION BUTTON — the entire screen's reason to exist
// ─────────────────────────────────────────────────────────────
@Composable
private fun PrimaryActionButton(
    state: DayActionState,
    onVictory: () -> Unit,
    onRelapse: () -> Unit,
    onPanic: () -> Unit,
) {
    AnimatedContent(
        targetState    = state,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label          = "action",
    ) { s ->
        when (s) {
            is DayActionState.NotLogged -> Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // BIG WIN button — primary
                Button(
                    onClick  = onVictory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape  = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VictoryGreen,
                        contentColor   = Color.Black,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                ) {
                    Text(
                        "✓  LOG TODAY AS WIN",
                        fontSize      = 16.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }

                // Relapse — secondary, smaller, less prominent
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick  = onRelapse,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(14.dp),
                        border   = BorderStroke(1.dp, WarriorRed.copy(alpha = 0.6f)),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = WarriorRed),
                    ) {
                        Text("Log Relapse", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick  = onPanic,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(14.dp),
                        border   = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f)),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                    ) {
                        Text("🆘 Panic", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            is DayActionState.LoggedWin -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(VictoryGreen.copy(alpha = 0.12f))
                    .border(1.dp, VictoryGreen.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✓", fontSize = 32.sp, color = VictoryGreen)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "TODAY IS LOGGED",
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = VictoryGreen,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        "Come back tomorrow. Stay strong.",
                        fontSize = 12.sp,
                        color    = TextTertiary,
                    )
                }
            }

            is DayActionState.LoggedLoss -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(WarriorRed.copy(alpha = 0.10f))
                    .border(1.dp, WarriorRed.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("↺", fontSize = 32.sp, color = WarriorRed)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "RESTART BEGINS NOW",
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = WarriorRed,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        "Every champion has risen from zero.",
                        fontSize = 12.sp,
                        color    = TextTertiary,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STREAK HERO — emotional anchor, single canvas, no overdraw
// ─────────────────────────────────────────────────────────────
@Composable
private fun StreakHero(streak: Int, best: Int, todayStatus: String?) {
    val ringPct = if (best > 0) (streak.toFloat() / best).coerceIn(0f, 1f) else 0f
    val animRing by animateFloatAsState(
        targetValue   = ringPct,
        animationSpec = tween(900, easing = EaseOutCubic),
        label         = "ring",
    )
    val ringColor by rememberStreakRingColor(streak = streak)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0E0000), BgBlack)
                )
            )
            .border(1.dp, BorderColor, RoundedCornerShape(28.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            // Ring + number
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp)) {
                    val stroke = 10.dp.toPx()
                    val inset  = stroke / 2f
                    // Track
                    drawArc(
                        color      = Color(0xFF1E1E1E),
                        startAngle = -90f, sweepAngle = 360f,
                        useCenter  = false,
                        topLeft    = Offset(inset, inset),
                        size       = Size(size.width - stroke, size.height - stroke),
                        style      = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    // Progress
                    if (animRing > 0f) drawArc(
                        color      = ringColor,
                        startAngle = -90f, sweepAngle = 360f * animRing,
                        useCenter  = false,
                        topLeft    = Offset(inset, inset),
                        size       = Size(size.width - stroke, size.height - stroke),
                        style      = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$streak",
                        fontSize   = 42.sp,
                        fontWeight = FontWeight.Black,
                        color      = TextPrimary,
                        lineHeight = 44.sp,
                    )
                    Text(
                        "DAYS",
                        fontSize      = 8.sp,
                        color         = TextTertiary,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                    )
                }
            }

            // Side stats — secondary prominence
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SideStat(value = "$best", label = "BEST")
                SideStat(
                    value = when (todayStatus) {
                        "clean"  -> "✅"
                        "failed" -> "❌"
                        else     -> "⬜"
                    },
                    label = "TODAY",
                )
            }
        }
    }
}

@Composable
private fun SideStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        Text(label, fontSize = 8.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// QUICK STATS ROW — secondary, 3-value, no animation
// ─────────────────────────────────────────────────────────────
@Composable
private fun QuickStatsRow(state: WarriorState) {
    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickStatChip(
            modifier = Modifier.weight(1f),
            value    = "${state.totalClean}",
            label    = "WINS",
            color    = VictoryGreen,
        )
        QuickStatChip(
            modifier = Modifier.weight(1f),
            value    = "${state.totalFailed}",
            label    = "RELAPSES",
            color    = WarriorRed,
        )
        QuickStatChip(
            modifier = Modifier.weight(1f),
            value    = if (state.totalClean + state.totalFailed > 0)
                "${(state.totalClean * 100 / (state.totalClean + state.totalFailed))}%"
            else "—",
            label    = "RATE",
            color    = ArenaCyan,
        )
    }
}

@Composable
private fun QuickStatChip(modifier: Modifier, value: String, label: String, color: Color) {
    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
        Text(label, fontSize = 8.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// QUOTE CARD — quiet, tertiary
// ─────────────────────────────────────────────────────────────
@Composable
private fun QuoteCard(quote: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            "\"$quote\"",
            fontSize   = 12.sp,
            color      = TextTertiary,
            fontWeight = FontWeight.Medium,
            textAlign  = TextAlign.Center,
            lineHeight  = 20.sp,
            modifier    = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// RECOVERY CARD — shown only when yesterday failed
// ─────────────────────────────────────────────────────────────
@Composable
private fun RecoveryCard(bestStreak: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0D0500))
            .border(1.dp, Color(0xFF3A1A00), RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) {
        Column {
            Text(
                "⚡ BOUNCE BACK",
                fontSize   = 11.sp,
                color      = WarningAmber,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Yesterday was a loss. Today is a new war.\nYour best was $bestStreak days — do it again.",
                fontSize   = 13.sp,
                color      = TextSecondary,
                lineHeight = 20.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SECTION LABEL
// ─────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.ExtraBold,
        color         = TextTertiary,
        letterSpacing = 3.sp,
        modifier      = Modifier.fillMaxWidth(),
    )
}

// ─────────────────────────────────────────────────────────────
// BATTLE CALENDAR — unchanged logic, moved to bottom
// ─────────────────────────────────────────────────────────────
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
            Text(
                d,
                modifier   = Modifier.weight(1f),
                fontSize   = 7.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = TextTertiary,
                textAlign  = TextAlign.Center,
            )
        }
    }
    Spacer(Modifier.height(6.dp))

    val cells = List(startOffset) { null } + (1..daysInMonth).toList()
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
                        else     -> ElevatedCard
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
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$day", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = tc)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    // Legend
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(color = VictoryGreen, label = "Win")
        LegendItem(color = WarriorRed,   label = "Relapse")
        LegendItem(color = ElevatedCard, label = "Not logged", border = true)
    }
}

@Composable
private fun LegendItem(color: Color, label: String, border: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp))
                .background(color)
                .then(if (border) Modifier.border(1.dp, BorderColor, RoundedCornerShape(2.dp)) else Modifier),
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────
// REUSABLE GLASS CARD — kept for external use (AnalysisScreen)
// ─────────────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor, RoundedCornerShape(22.dp))
            .padding(20.dp),
        content = content,
    )
}

@Composable
fun StatCard(
    modifier:   Modifier = Modifier,
    label:      String,
    value:      String,
    sub:        String,
    valueColor: Color,
) {
    GlassCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 8.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 48.sp, fontWeight = FontWeight.Black, color = valueColor, lineHeight = 50.sp)
            Text(sub, fontSize = 8.sp, color = TextTertiary, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        }
    }
}
