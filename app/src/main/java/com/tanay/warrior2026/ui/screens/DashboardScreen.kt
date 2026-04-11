package com.tanay.warrior2026.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Skull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior2026.WarriorViewModel
import com.tanay.warrior2026.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: WarriorViewModel,
    onPanicClick: () -> Unit,
    onVictoryClick: () -> Unit,
    onRelapseClick: () -> Unit,
    state: com.tanay.warrior2026.data.WarriorState
) {
    val scrollState = rememberScrollState()
    val streakAnim by animateIntAsState(
        targetValue = state.streak,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "streak"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Streak Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CURRENT STREAK",
                    fontSize = 9.sp,
                    color = TextDim,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$streakAnim",
                    fontSize = 70.sp,
                    fontWeight = FontWeight.Black,
                    color = WarriorRed,
                    lineHeight = 72.sp
                )
                Text(
                    text = "DAYS OF DOMINANCE",
                    fontSize = 9.sp,
                    color = TextDimmer,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Panic Button
        Button(
            onClick = onPanicClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = WarriorRed)
        ) {
            Icon(Icons.Filled.Skull, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "PANIC BUTTON",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onVictoryClick,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VictoryGreen)
            ) {
                Text(
                    text = "I STAY CLEAN",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }
            OutlinedButton(
                onClick = onRelapseClick,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarriorRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222))
            ) {
                Text(
                    text = "I FAILED",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = WarriorRed
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Battle Calendar
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "BATTLE CALENDAR",
                    fontSize = 9.sp,
                    color = Color(0xFF888888),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                BattleCalendar(state = state)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "MADE BY TANAY × EL",
            fontSize = 9.sp,
            color = TextDimmest,
            fontWeight = FontWeight.Black,
            letterSpacing = 5.sp
        )
    }
}

@Composable
fun BattleCalendar(state: com.tanay.warrior2026.data.WarriorState) {
    val now = Calendar.getInstance()
    val year = now.get(Calendar.YEAR)
    val month = now.get(Calendar.MONTH)
    val daysInMonth = Calendar.getInstance().apply {
        set(year, month + 1, 0)
    }.get(Calendar.DAY_OF_MONTH)

    val sdf = SimpleDateFormat("EEE MMM dd yyyy", Locale.US)

    val days = (1..daysInMonth).map { day ->
        val cal = Calendar.getInstance().apply { set(year, month, day) }
        val key = sdf.format(cal.time)
        val data = state.history[key]
        Triple(day, data?.status, data?.site)
    }

    val rows = days.chunked(7)
    rows.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            row.forEach { (day, status, _) ->
                val bg = when (status) {
                    "clean" -> VictoryGreen
                    "failed" -> WarriorRed
                    else -> CardBlack
                }
                val textColor = when (status) {
                    "clean" -> Color.Black
                    "failed" -> Color.White
                    else -> TextDimmer
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$day",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                }
            }
            // Fill remaining cells
            repeat(7 - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(25.dp))
            .background(GlassSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(25.dp))
            .padding(25.dp),
        content = content
    )
}
