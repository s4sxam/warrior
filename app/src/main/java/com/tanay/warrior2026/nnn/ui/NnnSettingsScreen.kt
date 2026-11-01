package com.tanay.warrior.nnn.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior.nnn.domain.*
import com.tanay.warrior.ui.screens.GlassCard
import com.tanay.warrior.ui.theme.*

private enum class Pending { STOP_ALL, DELETE, END, RESET }

/** Every control here is wired to a tested NnnActions transition. Nothing is decorative. */
@Composable
fun NnnSettingsScreen(vm: NnnViewModel, ui: NnnUiState, onBack: () -> Unit, onAfterReset: () -> Unit) {
    val st = ui.state
    val c = st.challenge
    val rules = c?.rules ?: NnnRuleSet()
    var pending by remember { mutableStateOf<Pending?>(null) }
    var newAllow by remember { mutableStateOf("") }
    var focusDraft by remember(st.settings.focusMessage) { mutableStateOf(st.settings.focusMessage) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportTo(uri)
    }

    NnnPage("NNN SETTINGS", "Monitoring, privacy and your data", onBack) {
        // ── Monitoring ───────────────────────────────────────────────
        NnnLabel("Monitoring")
        SettingRow("Safe Browser", "Records direct visits made inside Safe Browser only.",
            st.monitorState(MonitorSource.SAFE_BROWSER) == MonitorState.ACTIVE, enabled = c != null) { vm.setSafeBrowserMonitoring(it) }
        SettingRow("Direct visit counts as a failure", "Your rule for Safe Browser matches. Off = matches are blocked but not recorded.",
            rules.safeBrowserNavigationFails, enabled = c != null) { vm.updateRules(rules.copy(safeBrowserNavigationFails = it)) }
        SettingRow("Device-wide Domain Guard", "Not available in this version. It would need Android's VPN permission and is planned separately.",
            false, enabled = false) {}
        NnnBody(monitoringLine(ui), TextTertiary, Modifier.padding(vertical = 8.dp))
        NnnButton("STOP ALL MONITORING", { pending = Pending.STOP_ALL }, Modifier.fillMaxWidth(), primary = false, enabled = c != null)
        NnnBody("Stopping is recorded as a gap. It is never counted as a clean day.", TextTertiary, Modifier.padding(top = 4.dp))

        // ── Rule list & allowlist ────────────────────────────────────
        NnnLabel("Rule list")
        GlassCard(Modifier.fillMaxWidth()) {
            NnnBody("Version ${BundledDomainRules.VERSION} · ${BundledDomainRules.SOURCE} · ${BundledDomainRules.rules.size} well-known domains. Bundled with the app, never downloaded. " +
                "Matching uses the website name only — not page content, search text or URLs. The list is deliberately small, so detection is incomplete by design.")
        }
        NnnLabel("Allowlist")
        NnnBody("Sites you add are never matched (and subdomains of them). Use it for health, education or news sites that were flagged wrongly.")
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newAllow, onValueChange = { newAllow = it }, singleLine = true, label = { Text("site.example") },
                modifier = Modifier.weight(1f), colors = fieldColors()
            )
            Spacer(Modifier.width(8.dp))
            NnnButton("ADD", { vm.addAllowlist(newAllow); newAllow = "" }, enabled = newAllow.isNotBlank())
        }
        st.allowlist.sorted().forEach { entry ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(entry, color = TextPrimary, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.removeAllowlist(entry) }) { Text("REMOVE", color = WarriorRed, fontWeight = FontWeight.Bold) }
            }
        }

        // ── Privacy ──────────────────────────────────────────────────
        NnnLabel("Privacy")
        SettingRow("Keep website names on events", "Shows which rule matched. Off by default. Applies to new events only.",
            st.settings.storeDomainDetail) { vm.updateSettings(st.settings.copy(storeDomainDetail = it)) }
        NnnBody("Remove website names after", TextPrimary, Modifier.padding(top = 8.dp, bottom = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30, 90, 180, 365).forEach { d -> NnnChip("$d days", st.settings.retentionDays == d) { vm.updateSettings(st.settings.copy(retentionDays = d)) } }
        }
        NnnBody("Outcomes and dates are kept so history stays honest; only website names are removed.", TextTertiary, Modifier.padding(top = 4.dp))
        NnnBody("These records are excluded from Android cloud backup and device transfer, and are never uploaded.", TextTertiary, Modifier.padding(top = 4.dp))

        NnnLabel("Focus message")
        OutlinedTextField(
            value = focusDraft, onValueChange = { focusDraft = it.take(NnnSettings.MAX_FOCUS_MESSAGE) }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Shown on the Focus screen") }, supportingText = { Text("${focusDraft.length}/${NnnSettings.MAX_FOCUS_MESSAGE}", color = TextTertiary) },
            colors = fieldColors()
        )
        Spacer(Modifier.height(6.dp))
        NnnButton("SAVE MESSAGE", { vm.updateSettings(st.settings.copy(focusMessage = focusDraft)) }, enabled = focusDraft.trim() != st.settings.focusMessage)

        // ── Your data ────────────────────────────────────────────────
        NnnLabel("Your data")
        NnnButton("EXPORT NNN RECORDS (JSON)", { exportLauncher.launch("nnn-records.json") }, Modifier.fillMaxWidth(), primary = false)
        NnnBody("Saved only where you choose. It contains your events and check-ins.", TextTertiary, Modifier.padding(top = 4.dp, bottom = 10.dp))
        NnnButton("DELETE ACTIVITY RECORDS", { pending = Pending.DELETE }, Modifier.fillMaxWidth(), primary = false)
        Spacer(Modifier.height(8.dp))
        if (c?.status == ChallengeStatus.ACTIVE) {
            NnnButton("END CHALLENGE EARLY", { pending = Pending.END }, Modifier.fillMaxWidth(), primary = false)
            Spacer(Modifier.height(8.dp))
        }
        NnnButton("RESET NNN COMPLETELY", { pending = Pending.RESET }, Modifier.fillMaxWidth(), primary = false)
    }

    when (pending) {
        Pending.STOP_ALL -> NnnConfirm("Stop all monitoring?", "Safe Browser monitoring turns off and the time is recorded as a gap, not as clean days. You can turn it back on any time.", "STOP", { vm.stopAllMonitoring() }, { pending = null })
        Pending.DELETE -> NnnConfirm("Delete activity records?", "Deletes all events, check-ins and monitoring history. The challenge setup, settings and allowlist stay. This can't be undone.", "DELETE", { vm.deleteActivity() }, { pending = null }, destructive = true)
        Pending.END -> NnnConfirm("End the challenge early?", "The challenge is marked as ended early and recording stops. Your records stay until you delete them.", "END IT", { vm.endChallengeEarly() }, { pending = null }, destructive = true)
        Pending.RESET -> NnnConfirm("Reset NNN completely?", "Deletes every NNN record and setting, and returns to setup. Your normal habits are not touched. This can't be undone.", "RESET", { vm.resetEverything(); onAfterReset() }, { pending = null }, destructive = true)
        null -> {}
    }
}

@Composable
private fun SettingRow(title: String, body: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(14.dp)).background(CardBlack)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) TextPrimary else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(body, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = LocalAccentColor.current,
    focusedBorderColor = LocalAccentColor.current, unfocusedBorderColor = BorderColor,
    focusedLabelColor = LocalAccentColor.current, unfocusedLabelColor = TextSecondary
)
