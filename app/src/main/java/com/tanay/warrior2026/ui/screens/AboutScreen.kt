package com.tanay.warrior2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior2026.ui.theme.*

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("WARRIOR CODE", fontSize = 22.sp, fontWeight = FontWeight.Black, color = WarriorRed, letterSpacing = 2.sp)
            Spacer(Modifier.height(20.dp))

            val rules = listOf(
                "HONESTY" to "If you lie to the app, you lie to your soul.",
                "OWNERSHIP" to "Your failures are yours. Your victory is earned.",
                "DATA" to "We track your triggers to kill them. This phone is your server.",
                "CHAPTER 2" to "We analyze the enemy. We destroy the weakness.",
                "CONSISTENCY" to "One clean day is nothing. A thousand is identity.",
                "DISCIPLINE" to "Motivation fades. Discipline stays. Build the system.",
            )
            rules.forEachIndexed { i, (label, desc) ->
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = WarriorRed, fontWeight = FontWeight.Black)) {
                            append("${i + 1}. $label: ")
                        }
                        withStyle(SpanStyle(color = Color(0xFF666666), fontWeight = FontWeight.Normal)) {
                            append(desc)
                        }
                    },
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(14.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("NOTIFICATION SYSTEM", fontSize = 13.sp, fontWeight = FontWeight.Black, color = WarriorRed, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            val notifInfo = listOf(
                "☀️ Morning" to "6–7 AM — Daily battle briefing",
                "☕ Afternoon" to "1–2:30 PM — Danger hour check-in",
                "🌆 Evening" to "8–9 PM — Log your status reminder",
                "⚡ Random" to "Anytime 9AM–11PM — Motivation drop",
                "🏆 Milestone" to "Streak achievements — Instant fire",
            )
            notifInfo.forEach { (label, desc) ->
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF888888), modifier = Modifier.width(100.dp))
                    Text(desc, fontSize = 11.sp, fontWeight = FontWeight.Normal, color = TextDim)
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        Text(
            "MADE BY TANAY × EL",
            fontSize = 9.sp,
            color = TextDimmest,
            fontWeight = FontWeight.Black,
            letterSpacing = 5.sp
        )
    }
}
