package com.tanay.warrior.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── LiveRival.kt ──────────────────────────────────────────────
// Live Rival Card — v1.0.0
//
// Pits the warrior against a ghost of their own past streak.
// Side-by-side layout: YOU (left, ArenaBlue) vs GHOST (right, dim red).
//
// Public API:
//   LiveRivalCard(
//       myStreak:    Int,
//       rivalStreak: Int,
//       modifier:    Modifier = Modifier,
//   )
//
// Visual layout:
//   ┌──────────────────────────────────────────┐
//   │       RACING YOUR PAST SELF              │  header
//   ├────────────────┬─────────────────────────┤
//   │   YOU          │   GHOST                 │
//   │   [streak bar] │   [streak bar]          │
//   │   DAY N        │   DAY N                 │
//   ├────────────────┴─────────────────────────┤
//   │          ▲ AHEAD / ▼ BEHIND / = TIED     │  status row
//   └──────────────────────────────────────────┘
//
// Streak bars:
//   Each bar fills proportionally relative to max(myStreak, rivalStreak).
//   If both are 0, bars render at 0 width.
//   Bars animate from 0 → target on first composition (tween 800ms EaseOutCubic).
//   YOU bar fills left→right; GHOST bar fills right→left (mirror symmetry).
//
// Status:
//   myStreak > rivalStreak → "▲ AHEAD"  VictoryGreen #1DB954
//   myStreak < rivalStreak → "▼ BEHIND" WarriorRed   #FF3131
//   myStreak == rivalStreak → "= TIED"  BattleGold   #FFD700
//
// Bar anatomy:
//   • Filled gradient bar (not wireframe — contrast against Blueprint)
//   • Thin border matching side color
//   • Glow bloom drawBehind
//   • Bar height: 10dp, corner radius 4dp
// ─────────────────────────────────────────────────────────────

// ── Colors ────────────────────────────────────────────────────

private val ArenaBlue     = Color(0xFF00B4FF)
private val GhostRed      = Color(0xFF8B0000)
private val GhostRedLight = Color(0xFFBB3333)
private val VictoryGreen  = Color(0xFF1DB954)
private val WarriorRed    = Color(0xFFFF3131)
private val BattleGold    = Color(0xFFFFD700)
private val CardBg        = Color(0xFF0A0F14)
private val CardBorder    = Color(0xFF141E28)
private val DividerColor  = Color(0xFF141E28)
private val LabelDim      = Color(0xFF2A4A60)

// ── Status ────────────────────────────────────────────────────

private enum class RaceStatus { AHEAD, BEHIND, TIED }

private fun raceStatus(my: Int, rival: Int): RaceStatus = when {
    my > rival  -> RaceStatus.AHEAD
    my < rival  -> RaceStatus.BEHIND
    else        -> RaceStatus.TIED
}

private fun RaceStatus.label(): String = when (this) {
    RaceStatus.AHEAD  -> "▲  AHEAD"
    RaceStatus.BEHIND -> "▼  BEHIND"
    RaceStatus.TIED   -> "=  TIED"
}

private fun RaceStatus.color(): Color = when (this) {
    RaceStatus.AHEAD  -> VictoryGreen
    RaceStatus.BEHIND -> WarriorRed
    RaceStatus.TIED   -> BattleGold
}

// ── Streak bar ────────────────────────────────────────────────

/**
 * Single animated progress bar.
 *
 * @param fraction      0f–1f fill level (animated externally).
 * @param color         Primary bar color.
 * @param mirrorFill    If true, bar fills right→left (GHOST side).
 */
@Composable
private fun StreakBar(
    fraction:   Float,
    color:      Color,
    mirrorFill: Boolean = false,
    modifier:   Modifier = Modifier,
) {
    val clampedFraction = fraction.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.08f))
            .border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
    ) {
        // Filled portion
        if (clampedFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedFraction)
                    .align(if (mirrorFill) Alignment.CenterEnd else Alignment.CenterStart)
                    .clip(RoundedCornerShape(4.dp))
                    .drawBehind {
                        // Bloom glow behind the bar
                        drawRect(
                            color       = color.copy(alpha = 0.18f),
                            blendMode   = androidx.compose.ui.graphics.BlendMode.Plus,
                        )
                    }
                    .background(
                        if (mirrorFill)
                            Brush.horizontalGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.45f),
                                    color.copy(alpha = 0.90f),
                                )
                            )
                        else
                            Brush.horizontalGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.90f),
                                    color.copy(alpha = 0.45f),
                                )
                            )
                    )
            )
        }
    }
}

// ── Side panel ────────────────────────────────────────────────

@Composable
private fun RivalSidePanel(
    title:      String,
    streakDay:  Int,
    fraction:   Float,
    color:      Color,
    mirrorFill: Boolean = false,
    modifier:   Modifier = Modifier,
) {
    Column(
        modifier            = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = if (mirrorFill) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Side title (YOU / GHOST)
        Text(
            text          = title,
            fontSize      = 9.sp,
            fontWeight    = FontWeight.ExtraBold,
            fontFamily    = FontFamily.Monospace,
            color         = color.copy(alpha = 0.70f),
            letterSpacing = 3.sp,
            textAlign     = if (mirrorFill) TextAlign.End else TextAlign.Start,
        )

        // Large day number
        Text(
            text       = "DAY $streakDay",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Black,
            color      = color,
            textAlign  = if (mirrorFill) TextAlign.End else TextAlign.Start,
        )

        // Progress bar
        StreakBar(
            fraction    = fraction,
            color       = color,
            mirrorFill  = mirrorFill,
        )

        // Streak description
        val descriptor = when {
            streakDay == 0       -> "NOT STARTED"
            streakDay < 7        -> "EARLY GRIND"
            streakDay < 30       -> "BUILDING"
            streakDay < 90       -> "FORGED"
            else                 -> "LEGEND"
        }
        Text(
            text          = descriptor,
            fontSize      = 8.sp,
            fontWeight    = FontWeight.Medium,
            fontFamily    = FontFamily.Monospace,
            color         = color.copy(alpha = 0.40f),
            letterSpacing = 2.sp,
            textAlign     = if (mirrorFill) TextAlign.End else TextAlign.Start,
        )
    }
}

// ── Divider with VS ───────────────────────────────────────────

@Composable
private fun VsDivider() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(28.dp)
            .fillMaxHeight()
    ) {
        // Vertical line
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(DividerColor)
        )
        // VS pill
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(CardBg)
                .border(1.dp, DividerColor, RoundedCornerShape(50))
        ) {
            Text(
                text       = "VS",
                fontSize   = 7.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color      = LabelDim,
            )
        }
    }
}

// ── Public composable ─────────────────────────────────────────

/**
 * Side-by-side YOU vs GHOST (past self) streak comparison card.
 *
 * @param myStreak    Current warrior streak (your live count).
 * @param rivalStreak Your past best / ghost streak to race against.
 * @param modifier    Layout modifier for the outer card.
 *
 * Usage:
 * ```
 * LiveRivalCard(
 *     myStreak    = currentStreak,
 *     rivalStreak = personalBest,
 *     modifier    = Modifier.fillMaxWidth(),
 * )
 * ```
 */
@Composable
fun LiveRivalCard(
    myStreak:    Int,
    rivalStreak: Int,
    modifier:    Modifier = Modifier,
) {
    val status   = raceStatus(myStreak, rivalStreak)
    val maxStreak = maxOf(myStreak, rivalStreak, 1)   // avoid div-by-zero

    // Animate bar fractions from 0 on first composition
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }

    val myFraction by animateFloatAsState(
        targetValue   = if (triggered) myStreak.toFloat() / maxStreak else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label         = "myBarFraction",
    )
    val rivalFraction by animateFloatAsState(
        targetValue   = if (triggered) rivalStreak.toFloat() / maxStreak else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 80, easing = EaseOutCubic),
        label         = "rivalBarFraction",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
    ) {

        // ── Header ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ArenaBlue.copy(alpha = 0.07f),
                            Color.Transparent,
                            GhostRed.copy(alpha = 0.07f),
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text          = "RACING YOUR PAST SELF",
                fontSize      = 9.sp,
                fontWeight    = FontWeight.ExtraBold,
                fontFamily    = FontFamily.Monospace,
                color         = LabelDim,
                letterSpacing = 3.sp,
                modifier      = Modifier.align(Alignment.CenterStart),
            )
        }

        // Header divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )

        // ── Side panels ────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // YOU
            RivalSidePanel(
                title      = "YOU",
                streakDay  = myStreak,
                fraction   = myFraction,
                color      = ArenaBlue,
                mirrorFill = false,
                modifier   = Modifier.weight(1f),
            )

            // VS divider
            VsDivider()

            // GHOST
            RivalSidePanel(
                title      = "GHOST",
                streakDay  = rivalStreak,
                fraction   = rivalFraction,
                color      = GhostRedLight,
                mirrorFill = true,
                modifier   = Modifier.weight(1f),
            )
        }

        // Status divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )

        // ── Status row ─────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            status.color().copy(alpha = 0.04f),
                            status.color().copy(alpha = 0.10f),
                            status.color().copy(alpha = 0.04f),
                        )
                    )
                )
                .padding(vertical = 10.dp),
        ) {
            val gap = maxOf(myStreak, rivalStreak) - minOf(myStreak, rivalStreak)
            val gapText = if (status == RaceStatus.TIED) "" else "  +$gap DAYS"

            Text(
                text          = status.label() + gapText,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Black,
                fontFamily    = FontFamily.Monospace,
                color         = status.color(),
                letterSpacing = 2.sp,
                textAlign     = TextAlign.Center,
            )
        }
    }
}
