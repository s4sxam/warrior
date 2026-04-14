package com.tanay.warrior2026.ui.screens

// ── [NEW] CommanderProfileScreen.kt ──────────────────────────────────────────
// The "Commander Profile" setup flow: Name → DOB → Region → Fake Sync

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior2026.data.WarriorRegion
import com.tanay.warrior2026.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CommanderProfileScreen(
    isGeneratingBots: Boolean,
    onComplete: (name: String, dob: String, region: String) -> Unit
) {
    var step        by remember { mutableStateOf(0) }   // 0=name, 1=dob, 2=region, 3=syncing
    var name        by remember { mutableStateOf("") }
    var day         by remember { mutableStateOf("") }
    var month       by remember { mutableStateOf("") }
    var year        by remember { mutableStateOf("") }
    var region      by remember { mutableStateOf<WarriorRegion?>(null) }

    // Auto-advance to syncing screen when bots start generating
    LaunchedEffect(isGeneratingBots) {
        if (isGeneratingBots) step = 3
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A0000), BgBlack),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                (slideOutHorizontally { -it } + fadeOut())
            },
            label = "profile_step"
        ) { s ->
            when (s) {
                0 -> NameStep(name = name, onNext = { n -> name = n; step = 1 })
                1 -> DobStep(day = day, month = month, year = year,
                    onNext = { d, mo, y -> day = d; month = mo; year = y; step = 2 })
                2 -> RegionStep(selected = region,
                    onNext = { r ->
                        region = r
                        val dob = "${year.padStart(4,'0')}-${month.padStart(2,'0')}-${day.padStart(2,'0')}"
                        onComplete(name.trim(), dob, r.name)
                    })
                3 -> SyncingScreen()
            }
        }
    }
}

// ── Step 1: Name ──────────────────────────────────────────────────────────────
@Composable
private fun NameStep(name: String, onNext: (String) -> Unit) {
    var value by remember { mutableStateOf(name) }

    Column(
        modifier = Modifier.padding(32.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⚔️", fontSize = 56.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            "IDENTIFY YOURSELF,\nCOMMANDER.",
            fontSize = 24.sp, fontWeight = FontWeight.Black,
            color = WarriorRed, textAlign = TextAlign.Center,
            letterSpacing = 1.sp, lineHeight = 30.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your name will appear on the global leaderboard.",
            fontSize = 13.sp, color = TextTertiary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("WARRIOR NAME", color = TextTertiary, fontSize = 11.sp, letterSpacing = 2.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = WarriorRed,
                unfocusedBorderColor = BorderColor,
                focusedTextColor     = TextPrimary,
                unfocusedTextColor   = TextPrimary,
                cursorColor          = WarriorRed
            ),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(
                fontWeight = FontWeight.Bold, fontSize = 18.sp
            )
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { if (value.trim().isNotBlank()) onNext(value.trim()) },
            enabled = value.trim().isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = WarriorRed,
                disabledContainerColor = Color(0xFF330000)
            )
        ) {
            Text("NEXT  →", fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp, letterSpacing = 2.sp)
        }
    }
}

// ── Step 2: Date of Birth ─────────────────────────────────────────────────────
@Composable
private fun DobStep(
    day: String, month: String, year: String,
    onNext: (String, String, String) -> Unit
) {
    var d by remember { mutableStateOf(day) }
    var m by remember { mutableStateOf(month) }
    var y by remember { mutableStateOf(year) }
    val valid = d.toIntOrNull()?.let { it in 1..31 } == true &&
                m.toIntOrNull()?.let { it in 1..12 } == true &&
                y.toIntOrNull()?.let { it in 1900..2015 } == true

    Column(
        modifier = Modifier.padding(32.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎂", fontSize = 56.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            "DATE OF BIRTH",
            fontSize = 24.sp, fontWeight = FontWeight.Black,
            color = WarriorRed, textAlign = TextAlign.Center, letterSpacing = 2.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Used to calculate your Warrior Age.",
            fontSize = 13.sp, color = TextTertiary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DobField("DD", d, Modifier.weight(1f)) { d = it.take(2) }
            DobField("MM", m, Modifier.weight(1f)) { m = it.take(2) }
            DobField("YYYY", y, Modifier.weight(1.6f)) { y = it.take(4) }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { if (valid) onNext(d, m, y) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = WarriorRed,
                disabledContainerColor = Color(0xFF330000)
            )
        ) {
            Text("NEXT  →", fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun DobField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = TextTertiary, fontSize = 10.sp, letterSpacing = 1.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = WarriorRed,
            unfocusedBorderColor = BorderColor,
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary,
            cursorColor          = WarriorRed
        ),
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(
            fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center
        )
    )
}

// ── Step 3: Region ────────────────────────────────────────────────────────────
@Composable
private fun RegionStep(selected: WarriorRegion?, onNext: (WarriorRegion) -> Unit) {
    var picked by remember { mutableStateOf(selected) }

    Column(
        modifier = Modifier.padding(32.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌐", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "SELECT YOUR REGION",
            fontSize = 22.sp, fontWeight = FontWeight.Black,
            color = WarriorRed, letterSpacing = 2.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You will be ranked against 150 warriors from your region.",
            fontSize = 12.sp, color = TextTertiary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(WarriorRegion.entries) { r ->
                val isSelected = picked == r
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color(0xFF1A0000) else CardBlack)
                        .border(
                            1.dp,
                            if (isSelected) WarriorRed else BorderColor,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { picked = r }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(r.emoji, fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            r.displayName,
                            fontSize    = 11.sp,
                            fontWeight  = FontWeight.Bold,
                            color       = if (isSelected) WarriorRed else TextSecondary,
                            textAlign   = TextAlign.Center,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { picked?.let { onNext(it) } },
            enabled = picked != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = WarriorRed,
                disabledContainerColor = Color(0xFF330000)
            )
        ) {
            Text("JOIN THE WAR  →", fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp, letterSpacing = 2.sp)
        }
    }
}

// ── Step 4: Fake Server Sync ──────────────────────────────────────────────────
@Composable
private fun SyncingScreen() {
    val messages = listOf(
        "CONNECTING TO GLOBAL SERVERS...",
        "RETRIEVING REGIONAL DATA...",
        "SYNCHRONIZING 1,050 WARRIORS...",
        "CALCULATING LEADERBOARD RANKS...",
        "ACTIVATING COMMANDER PROFILE..."
    )
    var msgIdx   by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        repeat(messages.size) { i ->
            msgIdx = i
            progress = (i + 1).toFloat() / messages.size
            delay(550)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue  = progress,
        animationSpec = tween(500),
        label        = "sync_progress"
    )

    Column(
        modifier = Modifier.padding(40.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛰️", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "WARRIOR 2026",
            fontSize = 22.sp, fontWeight = FontWeight.Black,
            color = WarriorRed, letterSpacing = 3.sp
        )
        Spacer(Modifier.height(32.dp))

        LinearProgressIndicator(
            progress     = { animatedProgress },
            modifier     = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color        = WarriorRed,
            trackColor   = Color(0xFF1A0000)
        )
        Spacer(Modifier.height(16.dp))

        AnimatedContent(targetState = msgIdx, label = "sync_msg") { idx ->
            Text(
                messages.getOrElse(idx) { messages.last() },
                fontSize     = 11.sp,
                fontWeight   = FontWeight.Bold,
                color        = TextTertiary,
                letterSpacing = 1.sp,
                fontFamily   = FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(48.dp))
        Text(
            "${(animatedProgress * 100).toInt()}%",
            fontSize   = 36.sp,
            fontWeight = FontWeight.Black,
            color      = TextPrimary
        )
    }
}
