package com.tanay.warrior.ui.screens

// ─────────────────────────────────────────────────────────────────
// AnalysisScreen.kt  — v6.0.0 (Redesign)
//
// CHANGES:
//   • One clear primary insight at the top: win rate + streak quality
//   • Removed duplicate stat cards that echo the dashboard
//   • Month chart kept — it's unique to this screen
//   • Trigger analysis kept — actionable data
//   • Reduced on-screen info by ~50% (fixes issue #7)
//   • Header consistent with other screens
// ─────────────────────────────────────────────────────────────────

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior.data.WarriorState
import com.tanay.warrior.ui.components.BlueprintBarChart
import com.tanay.warrior.ui.theme.*
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ── Data ──────────────────────────────────────────────────────
private data class MonthStats(
    val month:       Month,
    val year:        Int,
    val victories:   Int,
    val defeats:     Int,
    val logged:      Int,
    val consistency: Int,  // 0–100
)

private fun buildMonthStats(state: WarriorState): List<MonthStats> {
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val now = LocalDate.now()
    return (0..5).reversed().mapNotNull { offset ->
        val target      = now.minusMonths(offset.toLong())
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
            month       = target.month,
            year        = target.year,
            victories   = v,
            defeats     = d,
            logged      = v + d,
            consistency = ((v.toFloat() / (v + d)) * 100).toInt(),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun AnalysisScreen(state: WarriorState) {
    val months  = remember(state.history) { buildMonthStats(state) }
    val total   = state.totalClean + state.totalFailed
    val winRate = if (total > 0) (state.totalClean * 100 / total) else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {

        // ── Header ────────────────────────────────────────────
        Text(
            text          = "PROGRESS",
            color         = TextPrimary,
            fontSize      = 22.sp,
            fontWeight    = FontWeight.Black,
            letterSpacing = 3.sp,
        )

        // ── Primary insight card ──────────────────────────────
        PrimaryInsightCard(
            winRate    = winRate,
            streak     = state.streak,
            bestStreak = state.bestStreak,
            total      = total,
        )

        // ── Monthly chart ─────────────────────────────────────
        if (months.isNotEmpty()) {
            SectionHeader("MONTHLY PERFORMANCE")
            MonthlyChart(months = months)
        }

        // ── Trigger breakdown ─────────────────────────────────
        if (state.triggers.isNotEmpty()) {
            SectionHeader("RELAPSE TRIGGERS")
            TriggerBreakdown(triggers = state.triggers)
        }

        // ── Empty state ───────────────────────────────────────
        if (total == 0) {
            EmptyState()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PRIMARY INSIGHT — one card, most important numbers
// ─────────────────────────────────────────────────────────────
@Composable
private fun PrimaryInsightCard(
    winRate:    Int,
    streak:     Int,
    bestStreak: Int,
    total:      Int,
) {
    val rateColor = when {
        winRate >= 80 -> VictoryGreen
        winRate >= 50 -> WarningAmber
        else          -> WarriorRed
    }

    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Win rate — primary
        Column(
            modifier = Modifier
                .weight(1.3f)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark)
                .border(1.dp, rateColor.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("WIN RATE", fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text("$winRate%", fontSize = 44.sp, fontWeight = FontWeight.Black, color = rateColor, lineHeight = 46.sp)
            Text("$total days logged", fontSize = 10.sp, color = TextTertiary)
        }

        // Streak stats — secondary
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MiniStatCard("STREAK", "$streak days", TextPrimary)
            MiniStatCard("BEST", "$bestStreak days", ArenaCyan)
        }
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 8.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
    }
}

// ─────────────────────────────────────────────────────────────
// MONTHLY CHART
// ─────────────────────────────────────────────────────────────
@Composable
private fun MonthlyChart(months: List<MonthStats>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // BlueprintBarChart from original — kept as-is (good component)
        BlueprintBarChart(
            months = months.map { m ->
                com.tanay.warrior.ui.components.MonthBar(
                    label      = m.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    victories  = m.victories,
                    defeats    = m.defeats,
                    consistency = m.consistency,
                )
            },
        )

        // Summary row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            val best = months.maxByOrNull { it.consistency }
            if (best != null) {
                LegendChip(
                    "Best month",
                    "${best.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${best.consistency}%",
                    VictoryGreen,
                )
            }
            val avg = if (months.isNotEmpty()) months.sumOf { it.consistency } / months.size else 0
            LegendChip("Average", "$avg%", ArenaCyan)
        }
    }
}

@Composable
private fun LegendChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, color = color, fontWeight = FontWeight.Black)
    }
}

// ─────────────────────────────────────────────────────────────
// TRIGGER BREAKDOWN
// ─────────────────────────────────────────────────────────────
@Composable
private fun TriggerBreakdown(triggers: Map<String, Int>) {
    val sorted = triggers.entries.sortedByDescending { it.value }.take(5)
    val max    = sorted.firstOrNull()?.value ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        sorted.forEach { (domain, count) ->
            TriggerRow(domain = domain, count = count, max = max)
        }
        if (sorted.size < triggers.size) {
            Text(
                "+ ${triggers.size - sorted.size} more triggers tracked",
                fontSize = 10.sp,
                color    = TextTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TriggerRow(domain: String, count: Int, max: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(domain, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Text(
                "$count×",
                fontSize   = 12.sp,
                color      = WarriorRed,
                fontWeight = FontWeight.Black,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ElevatedCard),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(count.toFloat() / max)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(WarriorRed.copy(alpha = 0.7f)),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SECTION HEADER
// ─────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(text: String) {
    Text(
        text          = text,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.ExtraBold,
        color         = TextTertiary,
        letterSpacing = 3.sp,
    )
}

// ─────────────────────────────────────────────────────────────
// EMPTY STATE
// ─────────────────────────────────────────────────────────────
@Composable
private fun EmptyState() {
    Box(
        modifier         = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("📊", fontSize = 48.sp)
            Text("No data yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Text(
                "Start logging wins and relapses\nto see your progress charts here.",
                fontSize  = 13.sp,
                color     = TextTertiary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
        }
    }
}
