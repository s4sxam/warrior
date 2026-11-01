package com.tanay.warrior.nnn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior.nnn.domain.*
import com.tanay.warrior.ui.screens.GlassCard
import com.tanay.warrior.ui.theme.*

/** NNN Command Center — everything about the challenge in one place, all values derived from real records. */
@Composable
fun NnnCommandCenterScreen(
    ui: NnnUiState,
    onBack: () -> Unit,
    onCheckIn: () -> Unit,
    onReportSetback: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenFocus: () -> Unit
) {
    val snap = ui.snapshot ?: return
    val c = snap.challenge
    val zone = c.zoneId
    val running = c.status == ChallengeStatus.ACTIVE && snap.phase == ChallengePhase.IN_PROGRESS
    val today = if (snap.dayNumber in 1..snap.days.size && snap.phase == ChallengePhase.IN_PROGRESS) snap.days[snap.dayNumber - 1] else null
    var confirmSetback by remember { mutableStateOf(false) }
    var dayDetail by remember { mutableStateOf<DayRecord?>(null) }

    NnnPage(c.title.uppercase(), "${fmtDay(c.startDate)} – ${fmtDay(c.endDate)} · ${c.zoneId.id}", onBack) {
        // ── A. Progress ───────────────────────────────────────────────
        Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            NnnProgressRing(
                progress = snap.dayNumber / c.totalDays.toFloat(),
                top = when (snap.phase) { ChallengePhase.UPCOMING -> "SOON"; else -> "DAY ${snap.dayNumber}" },
                bottom = when (snap.phase) {
                    ChallengePhase.UPCOMING -> "starts ${fmtDay(c.startDate)}"
                    ChallengePhase.ENDED -> "challenge ended"
                    else -> "of ${c.totalDays} · ${snap.daysRemaining} left"
                }
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NnnTile("NNN streak", "${snap.currentStreak}", "days", Modifier.weight(1f), VictoryGreen)
            NnnTile("Best", "${snap.bestStreak}", "days", Modifier.weight(1f))
            NnnTile("Since failure", snap.daysSinceLastConfirmedFailure?.toString() ?: "—", if (snap.daysSinceLastConfirmedFailure == null) "none yet" else "days", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NnnTile("Failed days", "${snap.failedDays}", "${snap.confirmedIncidents} incident(s)", Modifier.weight(1f), if (snap.failedDays > 0) WarriorRed else TextPrimary)
            NnnTile("Self-reported", "${snap.selfReportedDays}", "clean days", Modifier.weight(1f))
            NnnTile("Monitored", "${snap.monitoredCleanDays}", "no events", Modifier.weight(1f))
        }

        if (snap.pendingReviewCount > 0) {
            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("${snap.pendingReviewCount} event(s) need your review", color = NnnAmber, fontWeight = FontWeight.Black)
                NnnBody("Your streak is held back until you decide whether each one counts.", modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(8.dp)); NnnButton("REVIEW NOW", onOpenActivity)
            }
        }

        // ── B. Today ─────────────────────────────────────────────────
        NnnLabel("Today")
        GlassCard(Modifier.fillMaxWidth()) {
            if (!running || today == null) {
                NnnBody(when {
                    c.status == ChallengeStatus.ABANDONED -> "This challenge was ended early."
                    snap.phase == ChallengePhase.UPCOMING -> "The challenge hasn't started yet."
                    snap.phase == ChallengePhase.ENDED || c.status == ChallengeStatus.COMPLETED -> resultText(snap)
                    else -> "The challenge is paused."
                })
            } else {
                val v = dayVisual(today.status)
                Text("${v.glyph}  ${v.label}", color = v.color, fontWeight = FontWeight.Black, fontSize = 16.sp)
                if (today.status == DayStatus.FAILED) {
                    NnnBody("A setback is recorded for today. Your NNN streak restarts — the challenge continues, and tomorrow is a fresh day.", modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(10.dp)); NnnButton("OPEN FOCUS EXERCISE", onOpenFocus, Modifier.fillMaxWidth(), primary = false)
                } else {
                    NnnBody("A check-in is a self-report. It is honest because you make it — the app doesn't pretend to verify it.", modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(10.dp))
                    val checked = today.status == DayStatus.SELF_REPORTED_CLEAN || today.selfCheckedAt != null
                    NnnButton(if (checked) "CHECKED IN TODAY" else "CHECK IN · CLEAN TODAY", onCheckIn, Modifier.fillMaxWidth(), enabled = !checked)
                    Spacer(Modifier.height(8.dp))
                    NnnButton("REPORT A SETBACK", { confirmSetback = true }, Modifier.fillMaxWidth(), primary = false)
                }
            }
        }

        // ── C. Monitoring ────────────────────────────────────────────
        NnnLabel("Monitoring")
        GlassCard(Modifier.fillMaxWidth()) {
            NnnBody(monitoringLine(ui), TextPrimary)
            NnnBody("Monitoring covers only what is turned on. A quiet day is never proof of anything.", TextTertiary, Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(8.dp)); NnnButton("MONITORING SETTINGS", onOpenSettings, primary = false)
        }

        // ── D. Calendar ──────────────────────────────────────────────
        NnnLabel("Calendar")
        NnnCalendar(snap.days) { dayDetail = it }
        NnnLegend()

        // ── E. Recent activity ───────────────────────────────────────
        NnnLabel("Recent activity")
        val recent = ui.state.events.sortedByDescending { it.at }.take(3)
        if (recent.isEmpty()) NnnBody("Nothing recorded yet.")
        recent.forEach {
            Row(Modifier.padding(vertical = 3.dp)) {
                Text(fmtTime(it.at, zone), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("${sourceLabel(it.source)} · ${outcomeLabel(it)}", color = outcomeColor(it), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        NnnButton("ALL ACTIVITY", onOpenActivity, Modifier.fillMaxWidth(), primary = false)

        // ── F. Tools ─────────────────────────────────────────────────
        NnnLabel("Tools")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NnnButton("SAFE BROWSER", onOpenBrowser, Modifier.weight(1f), primary = false)
            NnnButton("INSIGHTS", onOpenInsights, Modifier.weight(1f), primary = false)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NnnButton("FOCUS", onOpenFocus, Modifier.weight(1f), primary = false)
            NnnButton("SETTINGS", onOpenSettings, Modifier.weight(1f), primary = false)
        }
        NnnBody("NNN is optional and separate from your normal habit tracking. Nothing here changes your habit streaks.", TextTertiary, Modifier.padding(top = 18.dp))
    }

    if (confirmSetback) {
        NnnConfirm(
            "Record a setback?", "This counts as a failure for today in NNN only. Your normal habits aren't affected, and the challenge continues.",
            "RECORD IT", onReportSetback, { confirmSetback = false }
        )
    }
    dayDetail?.let { d -> DayDetailDialog(d, ui) { dayDetail = null } }
}

private fun resultText(s: NnnSnapshot) = when {
    s.isChampion -> "Completed — every day logged and no confirmed failures."
    s.completedWithoutFailures -> "Completed — no confirmed failures. Some days weren't logged."
    else -> "Completed — ${s.failedDays} failed day${if (s.failedDays == 1) "" else "s"}. The result is yours; what you do next is up to you."
}

@Composable
private fun DayDetailDialog(d: DayRecord, ui: NnnUiState, onDismiss: () -> Unit) {
    val v = dayVisual(d.status)
    val zone = ui.challenge?.zoneId ?: java.time.ZoneId.systemDefault()
    val dayEvents = ui.state.events.filter { it.challengeDate == d.date }.sortedBy { it.at }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardBlack,
        title = { Text("Day ${d.dayNumber} · ${fmtDay(d.date)}", color = TextPrimary, fontWeight = FontWeight.Black) },
        text = {
            Column {
                Text("${v.glyph}  ${v.label}", color = v.color, fontWeight = FontWeight.Black)
                d.selfCheckedAt?.let { NnnBody("Self check-in at ${fmtTime(it, zone)}.", modifier = Modifier.padding(top = 6.dp)) }
                d.deviceGuardCoverage?.let { NnnBody("Domain Guard was on for ${(it * 100).toInt()}% of this day.", modifier = Modifier.padding(top = 6.dp)) }
                if (d.status == DayStatus.UNLOGGED || d.status == DayStatus.UNMONITORED || d.status == DayStatus.PARTIAL_COVERAGE)
                    NnnBody("Nothing confirms this day either way. It isn't counted as clean.", TextTertiary, Modifier.padding(top = 6.dp))
                if (dayEvents.isNotEmpty()) {
                    NnnLabel("Events")
                    dayEvents.forEach {
                        NnnBody("${fmtTime(it.at, zone)} · ${sourceLabel(it.source)} · ${outcomeLabel(it)}", outcomeColor(it))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = LocalAccentColor.current, fontWeight = FontWeight.ExtraBold) } }
    )
}
