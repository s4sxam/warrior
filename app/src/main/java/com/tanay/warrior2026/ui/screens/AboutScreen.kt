package com.tanay.warrior.ui.screens

// [FIX]    v2.3.0: Removed onTestUpdate / developer tools.
// [UPDATE] v3.1.0: Export now has one-tap copy button.
//                  Import has two tabs — JSON and Plain Text.
//                  Plain Text parser understands natural language date + status entries.
//                  Info (ⓘ) button shows Plain Text format guide.

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tanay.warrior.data.DayData
import com.tanay.warrior.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Plain text parser ─────────────────────────────────────────────────────────

private val CLEAN_WORDS = setOf(
    "clean", "nofap", "no", "win", "victory", "stayed", "held",
    "strong", "resisted", "survived", "good", "passed", "clear"
)

private val FAILED_WORDS = setOf(
    "failed", "relapsed", "relapse", "fap", "masturbated", "masterbat",
    "goon", "gooned", "gooning", "cum", "came", "lost", "fell",
    "jacked", "jerked", "edged", "busted", "nutted", "bust",
    "watched", "porn", "slip", "slipped", "broke", "broken"
)

private val MONTH_NAMES = mapOf(
    "jan" to 1, "january" to 1,
    "feb" to 2, "february" to 2,
    "mar" to 3, "march" to 3,
    "apr" to 4, "april" to 4,
    "may" to 5,
    "jun" to 6, "june" to 6,
    "jul" to 7, "july" to 7,
    "aug" to 8, "august" to 8,
    "sep" to 9, "september" to 9,
    "oct" to 10, "october" to 10,
    "nov" to 11, "november" to 11,
    "dec" to 12, "december" to 12
)

data class ParsedEntry(val dateKey: String, val status: String)

/**
 * Parses plain text like:
 *   "21/4/2026 clean"
 *   "april 21 failed"
 *   "21 april no fap"
 *   "22 goon"
 *   "23/4 clean"
 *   "21 april 2026 masturbated"
 *
 * Each non-blank line is parsed independently.
 * Missing year → current year. Missing month → current month.
 */
fun parsePlainTextHistory(input: String): Map<String, DayData> {
    val now = LocalDate.now()
    val result = mutableMapOf<String, DayData>()
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    input.lines().forEach { rawLine ->
        val line = rawLine.trim().lowercase()
        if (line.isBlank()) return@forEach

        val tokens = line.split(Regex("[\\s/,.-]+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@forEach

        var day: Int? = null
        var month: Int? = null
        var year: Int? = null
        var status: String? = null

        tokens.forEach { token ->
            when {
                token in FAILED_WORDS -> status = "failed"
                token in CLEAN_WORDS  -> status = "clean"
                MONTH_NAMES.containsKey(token) -> month = MONTH_NAMES[token]
                token.toIntOrNull() != null -> {
                    val n = token.toInt()
                    when {
                        n in 2000..2099 -> year = n
                        n in 1..31 && day == null -> day = n
                        n in 1..12 && month == null && day != null -> month = n
                    }
                }
            }
        }

        // "no fap" → two tokens, "no" → clean already caught above
        // Also catch multi-token combos
        if (status == null) {
            val joined = tokens.joinToString(" ")
            when {
                "no fap" in joined || "nofap" in joined -> status = "clean"
                "no porn" in joined -> status = "clean"
                "jerk" in joined || "jack" in joined -> status = "failed"
            }
        }

        val d = day ?: return@forEach
        val m = month ?: now.monthValue
        val y = year ?: now.year
        val s = status ?: return@forEach

        runCatching {
            val date = LocalDate.of(y, m, d)
            result[date.format(fmt)] = DayData(status = s)
        }
    }
    return result
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun AboutScreen(
    onExport: () -> String,
    onImport: (String) -> Boolean,
    onImportPlain: (Map<String, DayData>) -> Boolean = { false }
) {
    var exportText    by remember { mutableStateOf("") }
    var copied        by remember { mutableStateOf(false) }
    var showImport    by remember { mutableStateOf(false) }
    var importTab     by remember { mutableStateOf(0) }   // 0=JSON, 1=Plain Text
    var jsonInput     by remember { mutableStateOf("") }
    var plainInput    by remember { mutableStateOf("") }
    var importResult  by remember { mutableStateOf<Boolean?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current

    if (showInfoDialog) {
        PlainTextInfoDialog(onDismiss = { showInfoDialog = false })
    }

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

            // ── Export ──
            Button(
                onClick = { exportText = onExport(); copied = false },
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
                        maxLines = 6,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Copy button
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(exportText))
                        copied = true
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (copied) VictoryGreen else Color(0xFF1A1A1A)
                    ),
                    border = BorderStroke(1.dp, if (copied) VictoryGreen else BorderColor)
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy",
                        tint = if (copied) Color.Black else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (copied) "COPIED ✓" else "COPY TO CLIPBOARD",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = if (copied) Color.Black else TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Import toggle ──
            TextButton(onClick = { showImport = !showImport; importResult = null }) {
                Text(if (showImport) "▲ HIDE IMPORT" else "▼ IMPORT / RESTORE",
                    fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold)
            }

            if (showImport) {
                Spacer(Modifier.height(8.dp))

                // Tab selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf("JSON", "PLAIN TEXT").forEachIndexed { idx, label ->
                        val sel = importTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) Color(0xFF1A0000) else Color.Transparent)
                                .then(if (sel) Modifier.border(1.dp, WarriorRed.copy(0.4f), RoundedCornerShape(8.dp)) else Modifier)
                                .clickable { importTab = idx; importResult = null }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (sel) WarriorRed else TextTertiary,
                                letterSpacing = 1.sp)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (importTab == 0) {
                    // ── JSON tab ──
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it; importResult = null },
                        placeholder = { Text("Paste your backup JSON here", color = Color(0xFF444444), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = OutlinedTextFieldDefaults.colors(
                            focusedTextColor        = Color.White,
                            unfocusedTextColor      = Color.White,
                            focusedContainerColor   = Color(0xFF0D0D0D),
                            unfocusedContainerColor = Color(0xFF0D0D0D),
                            focusedBorderColor      = WarriorRed,
                            unfocusedBorderColor    = BorderColor,
                        ),
                        minLines = 3
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            importResult = onImport(jsonInput.trim())
                            if (importResult == true) jsonInput = ""
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = VictoryGreen)
                    ) {
                        Text("RESTORE JSON", fontWeight = FontWeight.Black,
                            fontSize = 13.sp, color = Color.Black)
                    }

                } else {
                    // ── Plain Text tab ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("One entry per line", fontSize = 10.sp, color = TextDim)
                        // ⓘ info button
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1A1A))
                                .border(1.dp, BorderColor, CircleShape)
                                .clickable { showInfoDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Info, "Info",
                                tint = TextTertiary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = plainInput,
                        onValueChange = { plainInput = it; importResult = null },
                        placeholder = {
                            Text(
                                "21/4/2026 clean\n22 april failed\n23 goon",
                                color = Color(0xFF3A3A3A), fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = OutlinedTextFieldDefaults.colors(
                            focusedTextColor        = Color.White,
                            unfocusedTextColor      = Color.White,
                            focusedContainerColor   = Color(0xFF0D0D0D),
                            unfocusedContainerColor = Color(0xFF0D0D0D),
                            focusedBorderColor      = WarriorRed,
                            unfocusedBorderColor    = BorderColor,
                        ),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                        minLines  = 4
                    )
                    Spacer(Modifier.height(8.dp))

                    // Preview parsed count
                    if (plainInput.isNotBlank()) {
                        val parsed = remember(plainInput) { parsePlainTextHistory(plainInput) }
                        val cleanCount  = parsed.values.count { it.status == "clean" }
                        val failedCount = parsed.values.count { it.status == "failed" }
                        if (parsed.isNotEmpty()) {
                            Text(
                                "Preview: $cleanCount clean · $failedCount failed",
                                fontSize = 10.sp,
                                color = VictoryGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                        } else if (plainInput.trim().isNotBlank()) {
                            Text("Nothing recognised yet. Check format.",
                                fontSize = 10.sp, color = TextDim)
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    Button(
                        onClick = {
                            val parsed = parsePlainTextHistory(plainInput.trim())
                            importResult = if (parsed.isNotEmpty()) {
                                val ok = onImportPlain(parsed)
                                if (ok) plainInput = ""
                                ok
                            } else false
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = VictoryGreen)
                    ) {
                        Text("RESTORE PLAIN TEXT", fontWeight = FontWeight.Black,
                            fontSize = 13.sp, color = Color.Black)
                    }
                }

                importResult?.let { ok ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (ok) "✅ Data restored successfully."
                        else if (importTab == 0) "❌ Invalid JSON. Check your backup."
                        else "❌ Nothing recognised. Check the format.",
                        fontSize = 11.sp,
                        color    = if (ok) VictoryGreen else WarriorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        Text("MADE BY TANAY × EL", fontSize = 9.sp, color = TextDimmest,
            fontWeight = FontWeight.Black, letterSpacing = 5.sp)
    }
}

// ── Plain Text info dialog ────────────────────────────────────────────────────

@Composable
private fun PlainTextInfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0D0D0D))
                .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text("HOW TO USE PLAIN TEXT",
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                color = WarriorRed, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Text("One entry per line. Write the date and status — any order, any format.",
                fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
            Spacer(Modifier.height(16.dp))

            Text("EXAMPLES", fontSize = 10.sp, color = TextTertiary,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))

            val examples = listOf(
                "21/4/2026 clean"        to "day/month/year + status",
                "april 21 failed"        to "month name + day",
                "22 goon"                to "day + any slang word",
                "23 no fap"              to "day + phrase",
                "21 april 2026 stayed"   to "full date, any order",
                "22/4 masturbated"       to "day/month, no year needed",
            )
            examples.forEach { (ex, note) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF111111))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(ex, fontSize = 11.sp, color = VictoryGreen,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f))
                    Text(note, fontSize = 9.sp, color = TextDim,
                        modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Missing year → current year\nMissing month → current month",
                fontSize = 10.sp, color = TextTertiary, lineHeight = 16.sp)

            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("GOT IT", color = WarriorRed, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
