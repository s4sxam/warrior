package com.tanay.warrior2026.ui.screens

// [FIX] v2.3.0: Wrapped "DEVELOPER TOOLS" section in BuildConfig.DEBUG guard.
//               The TEST UPDATE DIALOG button is now invisible in release builds
//               and only visible when running a debug build from Android Studio.

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior2026.BuildConfig
import com.tanay.warrior2026.ui.theme.*

@Composable
fun AboutScreen(
    onExport: () -> String,
    onImport: (String) -> Boolean,
    onTestUpdate: () -> Unit = {},
) {
    var exportText    by remember { mutableStateOf("") }
    var importInput   by remember { mutableStateOf("") }
    var importSuccess by remember { mutableStateOf<Boolean?>(null) }
    var showImport    by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // ── Warrior Code ──
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("WARRIOR CODE", fontSize = 22.sp, fontWeight = FontWeight.Black,
                color = WarriorRed, letterSpacing = 2.sp)
            Spacer(Modifier.height(20.dp))
            listOf(
                "HONESTY"     to "If you lie to the app, you lie to your soul.",
                "OWNERSHIP"   to "Your failures are yours. Your victories are earned.",
                "DATA"        to "We track your triggers to kill them. This phone is your server.",
                "ANALYSIS"    to "We study the enemy. We destroy the weakness.",
                "CONSISTENCY" to "One clean day is nothing. A thousand is identity.",
                "DISCIPLINE"  to "Motivation fades. Discipline stays. Build the system.",
            ).forEachIndexed { i, (label, desc) ->
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = WarriorRed, fontWeight = FontWeight.Black)) {
                            append("${i + 1}. $label: ")
                        }
                        withStyle(SpanStyle(color = TextSecondary, fontWeight = FontWeight.Normal)) {
                            append(desc)
                        }
                    },
                    fontSize = 14.sp, lineHeight = 22.sp
                )
                Spacer(Modifier.height(14.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Notification system ──
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("NOTIFICATION SYSTEM", fontSize = 13.sp, fontWeight = FontWeight.Black,
                color = WarriorRed, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            listOf(
                "☀️ Morning"   to "6–7 AM — Daily battle briefing",
                "☕ Afternoon" to "1–3 PM — Danger hour check-in",
                "🌆 Evening"   to "8–9 PM — Log your status reminder",
                "⚡ Random"    to "Anytime 9AM–11PM — Motivation drop",
                "🏆 Milestone" to "Streak achievements — Instant fire",
            ).forEach { (label, desc) ->
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        color = TextSecondary, modifier = Modifier.width(100.dp))
                    Text(desc, fontSize = 11.sp, fontWeight = FontWeight.Normal, color = TextTertiary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Backup / Restore ──
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("BACKUP & RESTORE", fontSize = 13.sp, fontWeight = FontWeight.Black,
                color = WarriorRed, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text("Export your history as JSON. Copy it somewhere safe.",
                fontSize = 11.sp, color = TextTertiary)
            Spacer(Modifier.height(14.dp))

            // Export
            Button(
                onClick = { exportText = onExport() },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = CardBlack),
                border   = BorderStroke(1.dp, BorderColor)
            ) {
                Text("EXPORT HISTORY", fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp, color = TextSecondary)
            }

            if (exportText.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        exportText,
                        fontSize = 9.sp, color = TextTertiary,
                        fontWeight = FontWeight.Normal,
                        maxLines = 6
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Copy the text above and save it.",
                    fontSize = 10.sp, color = TextDim)
            }

            Spacer(Modifier.height(16.dp))

            // Import toggle
            TextButton(onClick = { showImport = !showImport }) {
                Text(if (showImport) "▲ HIDE IMPORT" else "▼ IMPORT / RESTORE",
                    fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold)
            }

            if (showImport) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = importInput,
                    onValueChange = { importInput = it; importSuccess = null },
                    placeholder = { Text("Paste your backup JSON here", color = Color(0xFF444444), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedTextColor      = Color.White,
                        unfocusedTextColor    = Color.White,
                        focusedContainerColor = Color(0xFF0D0D0D),
                        unfocusedContainerColor = Color(0xFF0D0D0D),
                        focusedBorderColor    = WarriorRed,
                        unfocusedBorderColor  = BorderColor,
                    ),
                    minLines = 3
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        importSuccess = onImport(importInput.trim())
                        if (importSuccess == true) importInput = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = VictoryGreen)
                ) {
                    Text("RESTORE", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.Black)
                }
                importSuccess?.let { ok ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (ok) "✅ Data restored successfully." else "❌ Invalid JSON. Check your backup.",
                        fontSize = 11.sp,
                        color    = if (ok) VictoryGreen else WarriorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Developer Tools — DEBUG ONLY ──────────────────────────────────────
        // [FIX BUG 9] This entire section is hidden in release builds.
        // BuildConfig.DEBUG is true only when running from Android Studio (debug variant).
        // It is always false in a release APK — users will never see this button.
        if (BuildConfig.DEBUG) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("DEVELOPER TOOLS", fontSize = 13.sp, fontWeight = FontWeight.Black,
                    color = WarriorRed, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text("Force the update dialog to appear using a fake old version.",
                    fontSize = 11.sp, color = TextTertiary)
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick  = { onTestUpdate() },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = WarriorRed)
                ) {
                    Text("TEST UPDATE DIALOG", fontWeight = FontWeight.Black,
                        fontSize = 13.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(40.dp))
        Text("MADE BY TANAY × EL", fontSize = 9.sp, color = TextDimmest,
            fontWeight = FontWeight.Black, letterSpacing = 5.sp)
    }
}
