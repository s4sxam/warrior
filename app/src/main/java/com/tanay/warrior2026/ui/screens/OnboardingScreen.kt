package com.tanay.warrior.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior.ui.theme.*
import kotlinx.coroutines.launch

private data class OnboardPage(
    val emoji: String,
    val title: String,
    val body: String,
    val accentColor: Color = WarriorRed
)

private val PAGES = listOf(
    OnboardPage(
        "🔒",
        "YOUR DATA.\nYOUR DEVICE.",
        "Warrior has zero internet access.\nNo servers. No accounts. No tracking.\nYour streak lives on your phone and\nnowhere else — ever.",
        Color(0xFF00B4FF)
    ),
    OnboardPage(
        "⚔️",
        "THIS IS WAR.",
        "You're not here to track habits.\nYou're here to win a battle\nagainst the weakest version of yourself.",
        WarriorRed
    ),
    OnboardPage(
        "📊",
        "TRACK EVERY DAY.",
        "Log CLEAN or FAILED before midnight.\nEvery day you log is a battle report.\nEvery streak is a weapon.",
        VictoryGreen
    ),
    OnboardPage(
        "💀",
        "RELAPSE IS DATA.",
        "When you fall — confess it.\nThe app tracks your triggers.\nKnow your enemy. Destroy it.",
        Color(0xFFFF6B35)
    ),
    OnboardPage(
        "🔥",
        "COMMANDER MODE\nACTIVATED.",
        "No excuses. No shortcuts.\nYour streak starts the moment\nyou press BEGIN.",
        Color(0xFF00B4FF)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { PAGES.size }
    val scope      = rememberCoroutineScope()
    val isLast     = pagerState.currentPage == PAGES.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
    ) {
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardPage(page = PAGES[page])
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dot indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(PAGES.size) { i ->
                    val selected = pagerState.currentPage == i
                    val width by animateDpAsState(
                        targetValue = if (selected) 24.dp else 6.dp,
                        animationSpec = tween(300),
                        label = "dot_w"
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) PAGES[pagerState.currentPage].accentColor
                                else Color(0xFF333333)
                            )
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            AnimatedContent(
                targetState = isLast,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "btn"
            ) { last ->
                if (last) {
                    Button(
                        onClick    = onFinish,
                        modifier   = Modifier.fillMaxWidth().height(60.dp),
                        shape      = RoundedCornerShape(20.dp),
                        colors     = ButtonDefaults.buttonColors(containerColor = WarriorRed)
                    ) {
                        Text(
                            "BEGIN THE WAR",
                            fontWeight = FontWeight.Black,
                            fontSize   = 18.sp,
                            letterSpacing = 2.sp,
                            color      = Color.White
                        )
                    }
                } else {
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape    = RoundedCornerShape(20.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = CardBlack),
                        border   = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text(
                            "NEXT  →",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 16.sp,
                            color      = TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!isLast) {
                TextButton(onClick = onFinish) {
                    Text("SKIP", fontSize = 11.sp, color = TextDim, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OnboardPage(page: OnboardPage) {
    val alphAnim by animateFloatAsState(1f, tween(600), label = "page_alpha")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors  = listOf(page.accentColor.copy(alpha = 0.08f), BgBlack),
                    radius  = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 40.dp).padding(bottom = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(page.emoji, fontSize = 72.sp)
            Spacer(Modifier.height(32.dp))
            Text(
                page.title,
                fontSize      = 28.sp,
                fontWeight    = FontWeight.Black,
                color         = page.accentColor,
                textAlign     = TextAlign.Center,
                letterSpacing = 1.sp,
                lineHeight    = 34.sp
            )
            Spacer(Modifier.height(20.dp))
            Text(
                page.body,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Normal,
                color      = TextSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 26.sp
            )
        }
    }
}