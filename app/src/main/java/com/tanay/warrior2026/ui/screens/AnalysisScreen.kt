package com.tanay.warrior2026.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior2026.data.WarriorState
import com.tanay.warrior2026.ui.theme.*
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────
private data class MonthStats(
    val month: Month,
    val year: Int,
    val victories: Int,
    val defeats: Int,
    val logged: Int,
    val consistency: Int  // 0–100
)

private fun buildMonthStats(state: WarriorState): List<MonthStats> {
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val now = LocalDate.now()

    return (0..5).reversed().mapNotNull { monthOffset ->
        val target = now.minusMonths(monthOffset.toLong())
        val daysInMonth = target.month.length(target.isLeapYear)
        var v = 0; var d = 0
        (1..daysInMonth).forEach { day ->
            val key = target.withDayOfMonth(day).format(fmt)
            when (state.history[key]?.status) {
                "clean"  -> v++
                "failed" -> d++
            }
        }
        if (v + d == 0) null
        else MonthStats(
            month = target.month,
            year  = target.year,
            victories = v,
            defeats = d,
            logged = v + d,
            consistency = ((v.toFloat() / (v + d)) * 100).toInt()
        )
    }
}

private fun computeRank(state: WarriorState): Pair<String, Color> {
    // Based on consistency over last 30 days
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val now = LocalDate.now()
    var v = 0; var d = 0
    (0 until 30).forEach { offset ->
        when (state.history[now.minusDays(offset.toLong()).format(fmt)]?.status) {
            "clean"  -> v++
            "failed" -> d++
        }
    }
    val pct = if (v + d > 0) (v.toFloat() / (v + d) * 100).toInt() else 0
    return when {
        pct == 100 -> "⚡ PARAGON"    to Color(0xFFFFD700)
        pct >= 85  -> "🛡 ADHERENT"  to Color(0xFF1DB954)
        pct >= 70  -> "⚔ SOLDIER"   to Color(0xFF4FC3F7)
        pct >= 50  -> "😐 RECRUIT"   to Color(0xFFFF9800)
        else       -> "💀 FALLEN"    to WarriorRed
    }
}

// ─────────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun AnalysisScreen(state: WarriorState) {
    val monthsData = remember(state.history) { buildMonthStats(state) }
    val (rank, rankColor) = remember(state.history) { computeRank(state) }

    val allVictories = remember(monthsData) { monthsData.sumOf { it.victories } }
    val allDefeats   = remember(monthsData) { monthsData.sumOf { it.defeats } }
    val allConsistency = if (allVictories + allDefeats > 0)
        (allVictories.toFloat() / (allVictories + allDefeats) * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {

        // ── Hero rank card ──
        RankCard(rank = rank, rankColor = rankColor, streak = state.streak, best = state.bestStreak)

        Spacer(Modifier.height(16.dp))

        // ── Overall stats row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniStatCard(Modifier.weight(1f), "$allVictories", "VICTORIES", VictoryGreen)
            MiniStatCard(Modifier.weight(1f), "$allDefeats",   "DEFEATS",   WarriorRed)
            MiniStatCard(Modifier.weight(1f), "$allConsistency%", "CLEAN",  Color(0xFFFFD700))
        }

        Spacer(Modifier.height(20.dp))

        // ── Monthly bar chart ──
        if (monthsData.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("MONTHLY VICTORIES", fontSize = 10.sp, color = TextSecondary,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(16.dp))
                MonthlyBarChart(months = monthsData)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Consistency ring ──
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ConsistencyRing(
                    percentage = allConsistency,
                    modifier = Modifier.size(110.dp)
                )
                Column {
                    Text("OVERALL CONSISTENCY", fontSize = 10.sp,
                        color = TextSecondary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("$allConsistency%", fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = consistencyColor(allConsistency))
                    Text(
                        consistencyLabel(allConsistency),
                        fontSize = 12.sp, color = TextTertiary, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Monthly breakdown list ──
        if (monthsData.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("MONTHLY BREAKDOWN", fontSize = 10.sp, color = TextSecondary,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(14.dp))
                monthsData.reversed().forEach { m ->
                    MonthRow(m)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Weakness ranking ──
        val sorted = remember(state.triggers) {
            state.triggers.entries.sortedByDescending { it.value }
        }
        if (sorted.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("TRIGGER RANKING", fontSize = 10.sp, color = TextSecondary,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(14.dp))
                val maxCount = sorted.first().value.toFloat()
                sorted.forEach { (site, count) ->
                    WeaknessBar(site = site, count = count, maxCount = maxCount)
                    Spacer(Modifier.height(12.dp))
                }
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("TRIGGER RANKING", fontSize = 10.sp, color = TextSecondary,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))
                Text("NO TRIGGERS LOGGED — STAY CLEAN", fontSize = 12.sp,
                    color = VictoryGreen, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// COMPONENTS
// ─────────────────────────────────────────────────────────────

@Composable
private fun RankCard(rank: String, rankColor: Color, streak: Int, best: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0D0D0D))
            .border(1.dp, rankColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("WARRIOR RANK", fontSize = 9.sp, color = TextTertiary,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
            Spacer(Modifier.height(8.dp))
            Text(rank, fontSize = 28.sp, fontWeight = FontWeight.Black, color = rankColor)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$streak", fontSize = 28.sp, fontWeight = FontWeight.Black, color = WarriorRed)
                    Text("STREAK", fontSize = 8.sp, color = TextTertiary,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$best", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
                    Text("BEST", fontSize = 8.sp, color = TextTertiary,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
private fun MiniStatCard(modifier: Modifier, value: String, label: String, color: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBlack)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 8.sp, color = TextTertiary,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}

@Composable
private fun MonthlyBarChart(months: List<MonthStats>) {
    val maxV = remember(months) { months.maxOf { it.victories }.coerceAtLeast(1).toFloat() }
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "bar_chart"
    )
    // Trigger animation on first composition
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val chartHeight = 120.dp
    Box(modifier = Modifier.fillMaxWidth().height(chartHeight + 32.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight).align(Alignment.TopCenter)) {
            val barWidth = (size.width / months.size) * 0.55f
            val gap      = (size.width / months.size) * 0.45f / 2f
            val slotW    = size.width / months.size

            months.forEachIndexed { i, m ->
                val vFrac  = (m.victories.toFloat() / maxV) * animProgress
                val dFrac  = (m.defeats.toFloat() / maxV) * animProgress
                val x      = i * slotW + gap
                val barW2  = barWidth / 2f

                // Victory bar (left half)
                drawRoundRect(
                    color  = ChartClean,
                    topLeft = Offset(x, size.height * (1f - vFrac)),
                    size   = Size(barW2 - 2f, size.height * vFrac),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Defeat bar (right half)
                drawRoundRect(
                    color  = ChartFailed,
                    topLeft = Offset(x + barW2 + 2f, size.height * (1f - dFrac)),
                    size   = Size(barW2 - 2f, size.height * dFrac),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            months.forEach { m ->
                Text(
                    m.month.getDisplayName(TextStyle.SHORT, Locale.US).uppercase(),
                    fontSize = 8.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Legend
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendDot(color = ChartClean, label = "Clean")
        LegendDot(color = ChartFailed, label = "Failed")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConsistencyRing(percentage: Int, modifier: Modifier = Modifier) {
    val animPct by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "ring"
    )
    val color = consistencyColor(percentage)

    Canvas(modifier = modifier) {
        val strokeW = 12.dp.toPx()
        val inset   = strokeW / 2f
        drawArc(
            color      = CardBlack,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter  = false,
            topLeft    = Offset(inset, inset),
            size       = Size(size.width - strokeW, size.height - strokeW),
            style      = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeW,
                cap   = StrokeCap.Round
            )
        )
        drawArc(
            color      = color,
            startAngle = -90f,
            sweepAngle = 360f * animPct,
            useCenter  = false,
            topLeft    = Offset(inset, inset),
            size       = Size(size.width - strokeW, size.height - strokeW),
            style      = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeW,
                cap   = StrokeCap.Round
            )
        )
    }
}

@Composable
private fun MonthRow(m: MonthStats) {
    val total = m.victories + m.defeats
    val pct   = if (total > 0) (m.victories.toFloat() / total * 100).toInt() else 0
    val animPct by animateFloatAsState(
        targetValue = m.victories.toFloat() / total.coerceAtLeast(1),
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "month_bar_${m.month}"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                m.month.getDisplayName(TextStyle.SHORT, Locale.US).uppercase() + " ${m.year}",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = TextSecondary
            )
            Text(
                "${m.victories}W · ${m.defeats}L · $pct%",
                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextTertiary
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardBlack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animPct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(consistencyColor(pct))
            )
        }
    }
}

@Composable
private fun WeaknessBar(site: String, count: Int, maxCount: Float) {
    val animPct by animateFloatAsState(
        targetValue = count / maxCount,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "weakness_$site"
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(site, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = TextSecondary)
            Text("$count×", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = WarriorRed)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardBlack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animPct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WarriorRed)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────
private fun consistencyColor(pct: Int) = when {
    pct >= 85 -> VictoryGreen
    pct >= 60 -> Color(0xFFFFD700)
    pct >= 40 -> Color(0xFFFF9800)
    else      -> WarriorRed
}

private fun consistencyLabel(pct: Int) = when {
    pct >= 85 -> "ELITE TIER"
    pct >= 70 -> "SOLID"
    pct >= 50 -> "AVERAGE"
    pct >= 30 -> "STRUGGLING"
    else      -> "CRITICAL"
}