package com.tanay.warrior2026.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior2026.data.WarriorState
import com.tanay.warrior2026.ui.theme.*
import java.util.*

@Composable
fun AnalysisScreen(state: WarriorState) {
    val monthName = remember {
        Calendar.getInstance().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US)
            ?.uppercase() ?: "CURRENT"
    }

    var victories = 0
    var defeats = 0
    state.history.values.forEach {
        if (it.status == "clean") victories++ else defeats++
    }

    val rank = when {
        defeats == 0  -> "Paragon ⚡"
        defeats <= 2  -> "Adherent 🐧"
        defeats <= 5  -> "Soldier 🛡️"
        defeats <= 10 -> "Recruit 😐"
        else          -> "Fallen 💀"
    }

    val consistency = if (victories + defeats > 0)
        (victories.toFloat() / (victories + defeats) * 100).toInt() else 0

    val sorted = state.triggers.entries.sortedByDescending { it.value }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Performance Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(25.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("MONTHLY PERFORMANCE", fontSize = 9.sp, color = Color(0xFF888888), fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text(monthName, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.Black)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    Text("$victories Victory", fontSize = 18.sp, fontWeight = FontWeight.Black, color = VictoryGreen)
                    Text("  |  ", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF888888))
                    Text("$defeats Defeat", fontSize = 18.sp, fontWeight = FontWeight.Black, color = WarriorRed)
                }
                Spacer(Modifier.height(6.dp))
                Text("RANK: $rank", fontSize = 16.sp, fontWeight = FontWeight.Black, color = WarriorRed)
                Text("CONSISTENCY: $consistency%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF888888))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Weakness Ranking
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "WEAKNESS RANKING",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = WarriorRed,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(16.dp))

            if (sorted.isEmpty()) {
                Text("NO DATA — STAY CLEAN", fontSize = 11.sp, color = TextDimmer, fontWeight = FontWeight.ExtraBold)
            } else {
                val maxCount = sorted.first().value.toFloat()
                sorted.forEach { (site, count) ->
                    val pct = count / maxCount
                    Column(modifier = Modifier.padding(bottom = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(site, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = TextDim)
                            Text("$count TIMES", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = TextDim)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardBlack)
                        ) {
                            var animated by remember { mutableStateOf(0f) }
                            val animPct by animateFloatAsState(
                                targetValue = pct,
                                animationSpec = tween(1200, easing = EaseOutCubic),
                                label = "bar"
                            )
                            LaunchedEffect(Unit) { animated = pct }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animPct)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WarriorRed)
                            )
                        }
                    }
                }
            }
        }
    }
}
