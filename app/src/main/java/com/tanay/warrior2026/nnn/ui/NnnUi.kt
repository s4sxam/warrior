package com.tanay.warrior.nnn.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior.nnn.domain.*
import com.tanay.warrior.ui.screens.GlassCard
import com.tanay.warrior.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ── Tokens ──────────────────────────────────────────────────────────────────────
// Semantic status colours are fixed (green = clean, red = failure, amber = review, grey = unknown) so
// meaning never changes with the user's accent. Chrome (rings, primary buttons) follows LocalAccentColor.
internal val NnnAmber = Color(0xFFFFB020)
internal val NnnGray = Color(0xFF8A8A8A)

internal data class DayVisual(val color: Color, val glyph: String, val label: String)

/** Every status has a glyph AND a text label — colour is never the only signal. */
internal fun dayVisual(s: DayStatus): DayVisual = when (s) {
    DayStatus.SELF_REPORTED_CLEAN -> DayVisual(VictoryGreen, "✓", "Self-reported clean")
    DayStatus.MONITORED_CLEAN -> DayVisual(VictoryGreen, "✓", "Monitored · no events detected")
    DayStatus.FAILED -> DayVisual(WarriorRed, "✕", "Confirmed failure")
    DayStatus.POSSIBLE_EXPOSURE -> DayVisual(NnnAmber, "!", "Possible exposure · review needed")
    DayStatus.PARTIAL_COVERAGE -> DayVisual(NnnGray, "◐", "Partly monitored")
    DayStatus.UNMONITORED -> DayVisual(NnnGray, "⊘", "Monitoring was off")
    DayStatus.UNLOGGED -> DayVisual(NnnGray, "–", "Not logged")
    DayStatus.FUTURE -> DayVisual(NnnGray, "·", "Upcoming")
}

internal fun fmtDay(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("MMM d"))
internal fun fmtTime(i: Instant, zone: ZoneId): String = i.atZone(zone).format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))

// ── Layout ──────────────────────────────────────────────────────────────────────

@Composable
internal fun NnnPage(title: String, subtitle: String? = null, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = 1.sp)
                if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp),
            content = content
        )
    }
}

@Composable
internal fun NnnLabel(text: String) = Text(
    text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp,
    color = TextTertiary, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
)

@Composable
internal fun NnnBody(text: String, color: Color = TextSecondary, modifier: Modifier = Modifier) =
    Text(text, fontSize = 14.sp, lineHeight = 20.sp, color = color, modifier = modifier)

@Composable
internal fun NnnButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true) {
    val accent = LocalAccentColor.current
    if (primary) {
        Button(
            onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
        ) { Text(text, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, textAlign = TextAlign.Center) }
    } else {
        OutlinedButton(
            onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) { Text(text, color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
    }
}

@Composable
internal fun NnnTile(label: String, value: String, sub: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(CardBlack).border(1.dp, BorderColor, RoundedCornerShape(16.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label.uppercase(), fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, textAlign = TextAlign.Center)
        Text(value, fontSize = 30.sp, fontWeight = FontWeight.Black, color = valueColor)
        Text(sub, fontSize = 10.sp, color = TextTertiary, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun NnnProgressRing(progress: Float, top: String, bottom: String, modifier: Modifier = Modifier.size(180.dp)) {
    val accent = LocalAccentColor.current
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val arc = Size(size.width - stroke, size.height - stroke)
            val tl = Offset(stroke / 2, stroke / 2)
            drawArc(BorderColor, -90f, 360f, false, tl, arc, style = Stroke(stroke))
            drawArc(accent, -90f, 360f * progress.coerceIn(0f, 1f), false, tl, arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(top, fontSize = 38.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            Text(bottom, fontSize = 11.sp, color = TextTertiary, letterSpacing = 1.sp)
        }
    }
}

@Composable
internal fun NnnCalendar(days: List<DayRecord>, onDay: (DayRecord) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        days.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { d -> DayCell(d, Modifier.weight(1f), onDay) }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(d: DayRecord, modifier: Modifier, onDay: (DayRecord) -> Unit) {
    val v = dayVisual(d.status)
    val future = d.status == DayStatus.FUTURE
    val desc = "Day ${d.dayNumber}, ${fmtDay(d.date)}: ${v.label}"
    Column(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
            .background(v.color.copy(alpha = if (future) 0.05f else 0.16f))
            .border(1.dp, v.color.copy(alpha = if (future) 0.2f else 0.7f), RoundedCornerShape(10.dp))
            .clickable { onDay(d) }
            .clearAndSetSemantics { contentDescription = desc; role = Role.Button; onClick(label = "Show day details") { onDay(d); true } },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text("${d.dayNumber}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (future) TextTertiary else TextPrimary)
        Text(v.glyph, fontSize = 12.sp, color = v.color)
    }
}

@Composable
internal fun NnnLegend() {
    val items = listOf(DayStatus.SELF_REPORTED_CLEAN, DayStatus.MONITORED_CLEAN, DayStatus.FAILED, DayStatus.POSSIBLE_EXPOSURE,
        DayStatus.PARTIAL_COVERAGE, DayStatus.UNMONITORED, DayStatus.UNLOGGED, DayStatus.FUTURE)
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth()) {
                pair.forEach { s ->
                    val v = dayVisual(s)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(v.glyph, color = v.color, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                        Text(v.label, fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NnnChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text, fontSize = 12.sp) })
}

@Composable
internal fun NnnConfirm(
    title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardBlack,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Black) },
        text = { Text(text, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = if (destructive) WarriorRed else LocalAccentColor.current, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

// ── Dashboard card (War Room) ───────────────────────────────────────────────────

/** Compact NNN status for the War Room. Separate from, and never merged into, the ordinary habit streak. */
@Composable
fun NnnDashboardCard(ui: NnnUiState, onOpen: () -> Unit) {
    val accent = LocalAccentColor.current
    val snap = ui.snapshot
    val c = ui.challenge
    GlassCard(Modifier.fillMaxWidth().clickable(onClickLabel = "Open NNN Command Center") { onOpen() }) {
        Text("NNN CHALLENGE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = accent)
        Spacer(Modifier.height(6.dp))
        when {
            !ui.loaded -> NnnBody("Loading…")
            c == null || snap == null || ui.needsSetup -> {
                Text("No Nut November · Nov 1–30", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                NnnBody("Optional challenge, tracked separately from your habits. Set it up in under a minute.", modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp)); NnnButton("SET UP NNN", onOpen, Modifier.fillMaxWidth())
            }
            snap.phase == ChallengePhase.UPCOMING -> {
                Text(c.title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                NnnBody("Starts ${fmtDay(c.startDate)} · ${fmtDay(c.endDate)} (${c.totalDays} days)", modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp)); NnnButton("OPEN NNN COMMAND", onOpen, Modifier.fillMaxWidth(), primary = false)
            }
            snap.phase == ChallengePhase.ENDED || c.status != ChallengeStatus.ACTIVE -> {
                Text(c.title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                val result = when {
                    c.status == ChallengeStatus.ABANDONED -> "Ended early"
                    snap.isChampion -> "Completed · every day logged, no confirmed failures"
                    snap.completedWithoutFailures -> "Completed · no confirmed failures"
                    else -> "Completed · ${snap.failedDays} failed day${if (snap.failedDays == 1) "" else "s"}"
                }
                NnnBody(result, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp)); NnnButton("VIEW RESULTS", onOpen, Modifier.fillMaxWidth(), primary = false)
            }
            else -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("DAY ${snap.dayNumber}/${c.totalDays}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                    Spacer(Modifier.weight(1f))
                    Text("${snap.daysRemaining} days left", fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NnnTile("NNN streak", "${snap.currentStreak}", "days", Modifier.weight(1f), VictoryGreen)
                    NnnTile("Failures", "${snap.confirmedIncidents}", "confirmed", Modifier.weight(1f), if (snap.confirmedIncidents > 0) WarriorRed else TextPrimary)
                }
                if (snap.pendingReviewCount > 0) {
                    NnnBody("${snap.pendingReviewCount} event(s) need your review — your streak may change.", NnnAmber, Modifier.padding(top = 8.dp))
                }
                NnnBody(monitoringLine(ui), TextTertiary, Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(12.dp)); NnnButton("OPEN NNN COMMAND", onOpen, Modifier.fillMaxWidth())
            }
        }
    }
}

/** One honest sentence about what is (not) being monitored right now. */
internal fun monitoringLine(ui: NnnUiState): String {
    val snap = ui.snapshot
    val today = snap?.days?.getOrNull((snap.dayNumber - 1).coerceAtLeast(0))
    return NnnStatusText.describe(
        ui.state.monitorState(MonitorSource.SAFE_BROWSER),
        ui.state.monitorState(MonitorSource.DEVICE_GUARD),
        today?.deviceGuardCoverage
    )
}

/** A full-width row that toggles as a unit (checkbox/switch + label), with a 48dp+ touch target. */
internal fun Modifier.toggleableRow(value: Boolean, role: Role, onChange: (Boolean) -> Unit): Modifier =
    this.heightIn(min = 48.dp).toggleable(value = value, role = role, onValueChange = onChange)
