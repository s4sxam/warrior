package com.tanay.warrior.nnn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior.nnn.domain.*
import com.tanay.warrior.ui.theme.*
import java.time.Duration

// ── Plain-language labels (single source for Activity, day detail and insights) ──

internal fun sourceLabel(s: EventSource) = when (s) {
    EventSource.SAFE_BROWSER -> "Safe Browser"
    EventSource.LOCAL_VPN -> "Domain Guard"
    EventSource.MANUAL -> "Your report"
}

internal fun outcomeLabel(e: NnnEvent) = when (e.effectiveOutcome) {
    EventOutcome.CONFIRMED_FAIL -> "Confirmed failure"
    EventOutcome.POSSIBLE_EXPOSURE -> "Possible exposure · needs review"
    EventOutcome.REVIEWED_FALSE_POSITIVE -> "Reviewed · not counted"
    EventOutcome.REVIEWED_CONFIRMED -> "Reviewed · confirmed"
    EventOutcome.IGNORED -> "Ignored"
}

internal fun outcomeColor(e: NnnEvent) = when (e.effectiveOutcome) {
    EventOutcome.CONFIRMED_FAIL, EventOutcome.REVIEWED_CONFIRMED -> WarriorRed
    EventOutcome.POSSIBLE_EXPOSURE -> NnnAmber
    else -> NnnGray
}

/** Why the app recorded this — always framed as the user's own rule, never as a verdict. */
internal fun explainEvent(e: NnnEvent) = when (e.source) {
    EventSource.SAFE_BROWSER -> "You opened a site in Safe Browser that matches a rule you chose. Your rule counts a direct visit as a failure."
    EventSource.LOCAL_VPN -> "Your phone looked up a name on the rule list. That can be background traffic, so it is marked for your review."
    EventSource.MANUAL -> "You reported this yourself."
}

internal fun reasonLabel(r: ReviewReason) = when (r) {
    ReviewReason.NOT_ME -> "Not me"
    ReviewReason.BACKGROUND_TRAFFIC -> "Background traffic"
    ReviewReason.ALLOWED_SITE -> "A site I allow"
    ReviewReason.MISTAKEN_REPORT -> "Reported by mistake"
    ReviewReason.CONFIRMED_VISIT -> "It was a real visit"
    ReviewReason.OTHER -> "Other"
}

private enum class OutcomeFilter(val label: String) { ALL("All"), FAILURES("Failures"), REVIEW("Needs review"), REVIEWED("Reviewed") }
private enum class RangeFilter(val label: String) { ALL("All time"), TODAY("Today"), WEEK("Last 7 days") }

@Composable
fun NnnActivityScreen(ui: NnnUiState, onBack: () -> Unit, onReview: (String, ReviewDecision, ReviewReason) -> Unit) {
    val c = ui.challenge
    var source by remember { mutableStateOf<EventSource?>(null) }
    var outcome by remember { mutableStateOf(OutcomeFilter.ALL) }
    var range by remember { mutableStateOf(RangeFilter.ALL) }
    var reviewing by remember { mutableStateOf<NnnEvent?>(null) }

    val events = remember(ui.state.events, source, outcome, range, ui.now) {
        ui.state.events.asSequence()
            .filter { source == null || it.source == source }
            .filter {
                when (outcome) {
                    OutcomeFilter.ALL -> true
                    OutcomeFilter.FAILURES -> it.countsAsConfirmedFailure
                    OutcomeFilter.REVIEW -> it.isPendingReview
                    OutcomeFilter.REVIEWED -> it.reviews.isNotEmpty()
                }
            }
            .filter {
                when (range) {
                    RangeFilter.ALL -> true
                    RangeFilter.TODAY -> c != null && it.challengeDate == c.localDate(ui.now)
                    RangeFilter.WEEK -> Duration.between(it.at, ui.now) <= Duration.ofDays(7)
                }
            }
            .sortedByDescending { it.at }.toList()
    }

    NnnPage("ACTIVITY", "Every recorded event, with why", onBack) {
        NnnBody("Events are kept on this phone only. Website names are hidden unless you chose to keep them.")
        NnnLabel("Filter")
        ChipRow(listOf("All sources" to null) + EventSource.values().map { sourceLabel(it) to it }, source) { source = it }
        Spacer(Modifier.height(6.dp))
        ChipRow(OutcomeFilter.values().map { it.label to it }, outcome) { outcome = it ?: OutcomeFilter.ALL }
        Spacer(Modifier.height(6.dp))
        ChipRow(RangeFilter.values().map { it.label to it }, range) { range = it ?: RangeFilter.ALL }

        NnnLabel("${events.size} event${if (events.size == 1) "" else "s"}")
        if (events.isEmpty()) {
            NnnBody(if (ui.state.events.isEmpty()) "Nothing recorded. That is all this means — it is not proof of anything, because monitoring only covers what you turned on."
                    else "No events match these filters.")
        }
        events.forEach { e -> EventCard(e, c?.zoneId ?: java.time.ZoneId.systemDefault(), onReview = { reviewing = e }) }
    }

    reviewing?.let { e ->
        ReviewDialog(e, onDismiss = { reviewing = null }, onSave = { d, r -> onReview(e.id, d, r); reviewing = null })
    }
}

@Composable
private fun <T> ChipRow(items: List<Pair<String, T?>>, selected: T?, onSelect: (T?) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) -> NnnChip(label, selected == value) { onSelect(value) } }
    }
}

@Composable
private fun EventCard(e: NnnEvent, zone: java.time.ZoneId, onReview: () -> Unit) {
    var showDomain by remember(e.id) { mutableStateOf(false) }
    var showTrail by remember(e.id) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp)).background(CardBlack)
            .border(1.dp, outcomeColor(e).copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(fmtTime(e.at, zone), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(sourceLabel(e.source).uppercase(), color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        }
        Text(outcomeLabel(e), color = outcomeColor(e), fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
        NnnBody(explainEvent(e), modifier = Modifier.padding(top = 4.dp))
        if (e.source != EventSource.MANUAL) {
            Spacer(Modifier.height(6.dp))
            if (e.matchedDomain == null) NnnBody("Website name not stored.", TextTertiary)
            else TextButton(onClick = { showDomain = !showDomain }) {
                Text(if (showDomain) "Matched rule: ${e.matchedDomain}  (hide)" else "Show matched rule domain", color = LocalAccentColor.current)
            }
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NnnButton(if (e.reviews.isEmpty()) "REVIEW" else "CHANGE REVIEW", onReview, primary = e.isPendingReview)
            if (e.reviews.isNotEmpty()) NnnButton(if (showTrail) "HIDE TRAIL" else "AUDIT TRAIL", { showTrail = !showTrail }, primary = false)
        }
        if (showTrail) {
            Column(Modifier.padding(top = 8.dp)) {
                NnnBody("Original: ${if (e.originalOutcome == EventOutcome.CONFIRMED_FAIL) "Confirmed failure" else "Possible exposure"}", TextTertiary)
                e.reviews.forEach {
                    NnnBody("${fmtTime(it.at, zone)} · ${if (it.decision == ReviewDecision.FALSE_POSITIVE) "Not counted" else "Confirmed"} · ${reasonLabel(it.reason)}", TextTertiary)
                }
            }
        }
    }
}

@Composable
private fun ReviewDialog(e: NnnEvent, onDismiss: () -> Unit, onSave: (ReviewDecision, ReviewReason) -> Unit) {
    var decision by remember { mutableStateOf(ReviewDecision.FALSE_POSITIVE) }
    var reason by remember { mutableStateOf<ReviewReason?>(null) }
    val reasons = ReviewReason.values().filter { it.accepts(decision) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardBlack,
        title = { Text("Review this event", color = TextPrimary, fontWeight = FontWeight.Black) },
        text = {
            Column {
                NnnBody("You know what happened — the app only knows a rule matched. Your answer recalculates your days and streak, and the original record is kept.")
                Spacer(Modifier.height(10.dp))
                Choice("Don't count it", decision == ReviewDecision.FALSE_POSITIVE) { decision = ReviewDecision.FALSE_POSITIVE; reason = null }
                Choice("It was real — count it", decision == ReviewDecision.CONFIRMED) { decision = ReviewDecision.CONFIRMED; reason = null }
                NnnLabel("Why")
                reasons.forEach { r -> Choice(reasonLabel(r), reason == r) { reason = r } }
            }
        },
        confirmButton = {
            TextButton(enabled = reason != null, onClick = { onSave(decision, reason!!) }) {
                Text("Save", color = if (reason != null) LocalAccentColor.current else TextTertiary, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Text(label, color = TextPrimary, modifier = Modifier.padding(start = 10.dp))
    }
}
