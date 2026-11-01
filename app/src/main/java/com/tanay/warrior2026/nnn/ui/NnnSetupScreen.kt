package com.tanay.warrior.nnn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import java.time.Instant
import java.time.LocalDate

/**
 * Four-step setup: 1 rules → 2 monitoring → 3 privacy & limits → 4 review & start.
 * Starts only after a deliberate tap on the final step. Nothing here requests an Android permission:
 * the optional device-wide Domain Guard is not part of this version and is shown as unavailable.
 */
@Composable
fun NnnSetupScreen(ui: NnnUiState, onStart: (NnnRuleSet, NnnSettings, Boolean) -> Unit, onClose: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var safeBrowserFails by rememberSaveable { mutableStateOf(true) }
    var safeBrowserOn by rememberSaveable { mutableStateOf(true) }
    var keepDomainDetail by rememberSaveable { mutableStateOf(false) }
    var understood by rememberSaveable { mutableStateOf(false) }

    val start = LocalDate.of(2026, 11, 1)
    val end = LocalDate.of(2026, 11, 30)
    val today = LocalDate.now()
    val late = today.isAfter(start)
    val over = today.isAfter(end)

    BackHandler { if (step == 0) onClose() else step-- }

    NnnPage(
        title = "SET UP NNN", subtitle = "Step ${step + 1} of 4",
        onBack = { if (step == 0) onClose() else step-- }
    ) {
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth(), color = LocalAccentColor.current, trackColor = BorderColor)
        when (step) {
            0 -> {
                NnnLabel("1 · Challenge rules")
                NnnBody("These are rules YOU choose for this challenge. A website match is a rule you set — it is not proof of anything you did, and it never judges you.")
                Spacer(Modifier.height(12.dp))
                RuleRow("Direct visit in Safe Browser counts as a failure",
                    "Only when you open a known adult domain yourself in WARRIOR's Safe Browser.", safeBrowserFails) { safeBrowserFails = it }
                RuleRow("Manual report counts as a failure", "Always on. You can always record a setback honestly.", true, enabled = false) {}
                NnnBody("Never counted: wet dreams, health/education sites you allowlist, background ads and page resources.", TextTertiary, Modifier.padding(top = 10.dp))
            }
            1 -> {
                NnnLabel("2 · Monitoring")
                NnnBody("Pick what, if anything, watches for the rule above. Each is separate and optional.")
                Spacer(Modifier.height(12.dp))
                RuleRow("Manual reporting", "Always available. Daily self check-in and setback reports.", true, enabled = false) {}
                RuleRow("Safe Browser", "A simple in-app browser. Sees only what you open inside it. Needs no extra Android permission.", safeBrowserOn) { safeBrowserOn = it }
                RuleRow("Device-wide Domain Guard", "Not available in this version. It would need Android's VPN permission and is planned separately.", false, enabled = false) {}
                NnnBody("Turning a source off later is always possible. Time it was off is recorded as a gap — never as a clean day.", TextTertiary, Modifier.padding(top = 10.dp))
            }
            2 -> {
                NnnLabel("3 · Privacy & limits")
                Bullet("What Safe Browser can see: the website name (host) of pages you open in it. Not page content, searches, passwords or messages.")
                Bullet("What it cannot see: anything you do in other browsers or apps. No monitoring is complete, and a quiet day is never proof.")
                Bullet("What is stored, on this phone only: the date/time, source and outcome of each event, your check-ins and monitoring on/off times. Website names are NOT stored unless you turn that on below.")
                Bullet("NNN records never leave this phone — no NNN account, analytics or upload. Safe Browser itself connects to the sites you choose to open.")
                Bullet("You can pause, stop, export or delete everything any time in NNN settings. Stopping never counts as a victory.")
                Bullet("These records are excluded from Android cloud backup and device transfer.")
                Spacer(Modifier.height(8.dp))
                RuleRow("Keep website names on events", "Lets you see which rule matched. Off by default; old names are removed after the retention period.", keepDomainDetail) { keepDomainDetail = it }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().toggleableRow(understood, Role.Checkbox) { understood = it }) {
                    Checkbox(checked = understood, onCheckedChange = null)
                    Text("I understand what is monitored, what is not, and how to turn it off.", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
            else -> {
                NnnLabel("4 · Review & start")
                SummaryRow("Dates", "${fmtDay(start)} – ${fmtDay(end)}, 2026 · 30 days (both inclusive)")
                SummaryRow("Time zone", java.time.ZoneId.systemDefault().id + " (saved at start)")
                SummaryRow("Failure rules", buildString {
                    append("Manual report"); if (safeBrowserFails) append(" · Safe Browser direct visit")
                })
                SummaryRow("Monitoring", buildString {
                    append("Manual"); if (safeBrowserOn) append(" · Safe Browser (Safe Browser only)"); append(" · Domain Guard: not available")
                })
                SummaryRow("Stored data", if (keepDomainDetail) "Events + website names, redacted after 90 days" else "Events only, no website names")
                if (over) {
                    NnnBody("This challenge's dates have already passed. You can still start it, but no day can be logged.", NnnAmber, Modifier.padding(top = 8.dp))
                } else if (late) {
                    NnnBody("Today is day ${java.time.temporal.ChronoUnit.DAYS.between(start, today) + 1}. Days before today stay 'Not logged' — they are never counted as monitored or clean.", NnnAmber, Modifier.padding(top = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        val canContinue = step != 2 || understood
        if (step < 3) {
            NnnButton("CONTINUE", { step++ }, Modifier.fillMaxWidth(), enabled = canContinue)
        } else {
            NnnButton("START NNN", {
                val rules = NnnRuleSet(safeBrowserNavigationFails = safeBrowserFails)
                val settings = ui.state.settings.copy(storeDomainDetail = keepDomainDetail)
                onStart(rules, settings, safeBrowserOn)
            }, Modifier.fillMaxWidth(), enabled = !over)
        }
    }
}

@Composable
private fun RuleRow(title: String, body: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(14.dp)).background(CardBlack)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(body, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun Bullet(text: String) = Row(Modifier.padding(vertical = 4.dp)) {
    Text("•", color = LocalAccentColor.current, fontSize = 16.sp, modifier = Modifier.width(18.dp))
    NnnBody(text)
}

@Composable
private fun SummaryRow(label: String, value: String) = Column(Modifier.padding(vertical = 6.dp)) {
    Text(label.uppercase(), fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
    Text(value, fontSize = 15.sp, color = TextPrimary)
}
