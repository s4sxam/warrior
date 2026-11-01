package com.tanay.warrior.nnn.ui

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior.ui.screens.GlassCard
import com.tanay.warrior.ui.theme.*
import kotlinx.coroutines.delay

private val GROUNDING = listOf(
    "Stand up, drink a glass of water.",
    "Change rooms, or step outside for a few minutes.",
    "Message or call someone you trust.",
    "Move: a short walk or ten push-ups."
)

/** Emergency Focus: a timed breathing exercise + user's own message. Works offline; stores nothing. */
@Composable
fun NnnFocusScreen(message: String, onBack: () -> Unit) {
    val context = LocalContext.current
    // Respect the system "remove animations" setting.
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var minutes by remember { mutableIntStateOf(2) }
    var running by remember { mutableStateOf(false) }
    var remaining by remember { mutableIntStateOf(0) }

    LaunchedEffect(running) {
        if (running) {
            remaining = minutes * 60
            while (remaining > 0) { delay(1_000); remaining-- }
            running = false
        }
    }

    NnnPage("FOCUS", "A few minutes to let the urge pass", onBack) {
        if (message.isNotBlank()) {
            GlassCard(Modifier.fillMaxWidth()) { Text("“$message”", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp) }
            Spacer(Modifier.height(12.dp))
        }
        Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
            if (running) BreathingCircle(reduceMotion)
            else Box(Modifier.size(150.dp).clip(CircleShape).background(BorderColor))
            Text(
                if (running) "%d:%02d".format(remaining / 60, remaining % 60) else "$minutes min",
                color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black
            )
        }
        if (running) {
            Text(if ((minutes * 60 - remaining) % 8 < 4) "Breathe in…" else "Breathe out…", color = TextSecondary, fontSize = 16.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(14.dp))
            NnnButton("STOP", { running = false }, Modifier.fillMaxWidth(), primary = false)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                listOf(1, 2, 3, 5).forEach { m -> NnnChip("$m min", minutes == m) { minutes = m } }
            }
            Spacer(Modifier.height(14.dp))
            NnnButton("BEGIN", { running = true }, Modifier.fillMaxWidth())
        }
        NnnLabel("Or try one of these")
        GROUNDING.forEach { NnnBody("• $it", TextPrimary, Modifier.padding(vertical = 3.dp)) }
        NnnBody("If this keeps feeling bigger than a challenge, talking with someone you trust — or a professional — can really help.", TextTertiary, Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun BreathingCircle(reduceMotion: Boolean) {
    val accent = LocalAccentColor.current
    // 4s in, 4s out. Animation exists only while this composable is on screen (i.e. while running).
    val scale = if (reduceMotion) 0.85f else {
        val t = rememberInfiniteTransition(label = "breath")
        val s by t.animateFloat(0.6f, 1f, infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
        s
    }
    Box(Modifier.size(220.dp).scale(scale).clip(CircleShape).background(accent.copy(alpha = 0.25f)))
}
