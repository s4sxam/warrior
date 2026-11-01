package com.tanay.warrior.nnn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior.nnn.domain.NnnInsights
import com.tanay.warrior.ui.screens.GlassCard
import com.tanay.warrior.ui.theme.*

/** Factual, local numbers only. No causes, no predictions, no pattern claims without enough samples. */
@Composable
fun NnnInsightsScreen(ui: NnnUiState, onBack: () -> Unit) {
    val i = ui.insights
    NnnPage("INSIGHTS", "Calculated on this phone from your records", onBack) {
        if (i == null) { NnnBody("Start the challenge to see insights."); return@NnnPage }

        NnnLabel("Overview")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NnnTile("Progress", "${i.progressPercent}%", "of the challenge", Modifier.weight(1f))
            NnnTile("Clean days", "${i.cleanDayPercent}%", "of days so far", Modifier.weight(1f), VictoryGreen)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NnnTile("Self-reported", "${i.selfReportedDays}", "days", Modifier.weight(1f))
            NnnTile("Monitored", "${i.monitoredCleanDays}", "no events", Modifier.weight(1f))
            NnnTile("Failed days", "${i.failedDays}", "${i.confirmedIncidents} incidents", Modifier.weight(1f), if (i.failedDays > 0) WarriorRed else TextPrimary)
        }
        if (i.possibleExposureIncidents > 0) NnnBody("${i.possibleExposureIncidents} possible exposure event(s) were flagged for review in total.", NnnAmber, Modifier.padding(top = 8.dp))

        NnnLabel("Weekly consistency")
        GlassCard(Modifier.fillMaxWidth()) {
            if (i.weeks.isEmpty()) NnnBody("Nothing to show yet.")
            i.weeks.forEach { w ->
                val frac = if (w.elapsedDays == 0) 0f else w.cleanDays / w.elapsedDays.toFloat()
                Text("Week ${w.weekNumber} · ${w.cleanDays} of ${w.elapsedDays} days clean", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Box(
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(BorderColor)
                        .semantics { contentDescription = "Week ${w.weekNumber}: ${w.cleanDays} of ${w.elapsedDays} days clean" }
                ) { Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(VictoryGreen)) }
            }
            i.weekOverWeekChange?.let {
                NnnBody(when {
                    it > 0 -> "Latest week is up $it points on the week before."
                    it < 0 -> "Latest week is down ${-it} points on the week before."
                    else -> "Latest week matches the week before."
                }, TextSecondary)
            }
        }

        NnnLabel("When events were recorded")
        GlassCard(Modifier.fillMaxWidth()) {
            val hours = i.hourlyCounts
            if (hours == null) {
                NnnBody("Not enough data yet (${i.eventSampleSize} of ${NnnInsights.MIN_TIME_SAMPLES} events). A time-of-day pattern only appears once there are enough recorded events to mean something.")
            } else {
                val max = (hours.maxOrNull() ?: 1).coerceAtLeast(1)
                Row(Modifier.fillMaxWidth().height(70.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    hours.forEachIndexed { h, n ->
                        Box(Modifier.weight(1f).fillMaxHeight(if (n == 0) 0.03f else (n / max.toFloat()).coerceAtLeast(0.08f))
                            .background(if (h == i.peakHour) LocalAccentColor.current else BorderColor)
                            .semantics { contentDescription = "$h o'clock: $n events" })
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    listOf("0", "6", "12", "18", "23").forEach { Text(it, Modifier.weight(1f), color = TextTertiary, fontSize = 10.sp) }
                }
                i.peakHour?.let { NnnBody("Most events were recorded around %02d:00 (${i.eventSampleSize} events).".format(it), TextPrimary, Modifier.padding(top = 6.dp)) }
                NnnBody("This describes when events were recorded — not why they happened.", TextTertiary, Modifier.padding(top = 4.dp))
            }
        }
    }
}
