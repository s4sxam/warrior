package com.tanay.warrior.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── CharacterGuild.kt ─────────────────────────────────────────
// Character Guild — v1.0.0
//
// Ranks:
//   Day  0–6   RECRUIT   — dim rust, broken feel, ☠ icon
//   Day  7–29  SOLDIER   — solid iron, ⚔ icon
//   Day 30–89  ELITE     — glowing steel, 🛡 icon
//   Day 90+    LEGEND    — battle gold, animated glow pulse, ★ icon
//
// Public API:
//   WarriorRankCard(streak: Int, modifier: Modifier = Modifier)
//
// Internal:
//   RankConfig      — data class per rank
//   rankFor()       — selects config from streak
//   LegendGlowBadge — animated gold badge for LEGEND only
//   StaticBadge     — non-animated badge for RECRUIT/SOLDIER/ELITE
// ─────────────────────────────────────────────────────────────

// ── Colors ────────────────────────────────────────────────────

private val RustRed     = Color(0xFF7A2A0A)
private val RustDim     = Color(0xFF3A1A08)
private val IronGrey    = Color(0xFF6A7A8A)
private val IronDark    = Color(0xFF1A2028)
private val SteelBlue   = Color(0xFF8BAFC4)
private val SteelDark   = Color(0xFF0A1828)
private val BattleGold  = Color(0xFFFFD700)
private val GoldDark    = Color(0xFF1A1200)
private val TextDim     = Color(0xFF555555)
private val TextMid     = Color(0xFF8A8A8A)

// ── Rank data ─────────────────────────────────────────────────

private data class RankConfig(
    val title:       String,
    val subtitle:    String,
    val icon:        String,
    val borderColor: Color,
    val glowColor:   Color,
    val badgeBg:     Color,
    val badgeFg:     Color,    // icon + title tint
    val subColor:    Color,
    val cardBg:      Color,
    val isLegend:    Boolean = false,
)

private fun rankFor(streak: Int): RankConfig = when {
    streak < 7  -> RankConfig(
        title       = "RECRUIT",
        subtitle    = "The fall broke you. Rise anyway.",
        icon        = "🗡",
        borderColor = RustRed.copy(alpha = 0.5f),
        glowColor   = RustRed,
        badgeBg     = RustDim,
        badgeFg     = RustRed,
        subColor    = TextDim,
        cardBg      = Color(0xFF0D0500),
    )
    streak < 30 -> RankConfig(
        title       = "SOLDIER",
        subtitle    = "Iron forged. The war has begun.",
        icon        = "⚔",
        borderColor = IronGrey.copy(alpha = 0.6f),
        glowColor   = IronGrey,
        badgeBg     = IronDark,
        badgeFg     = IronGrey,
        subColor    = TextMid,
        cardBg      = Color(0xFF080C10),
    )
    streak < 90 -> RankConfig(
        title       = "ELITE",
        subtitle    = "Steel mind. Unbroken discipline.",
        icon        = "🛡",
        borderColor = SteelBlue.copy(alpha = 0.7f),
        glowColor   = SteelBlue,
        badgeBg     = SteelDark,
        badgeFg     = SteelBlue,
        subColor    = SteelBlue.copy(alpha = 0.8f),
        cardBg      = Color(0xFF050C14),
    )
    else        -> RankConfig(
        title       = "LEGEND",
        subtitle    = "Battle gold. Few ever reach this.",
        icon        = "★",
        borderColor = BattleGold,
        glowColor   = BattleGold,
        badgeBg     = GoldDark,
        badgeFg     = BattleGold,
        subColor    = BattleGold.copy(alpha = 0.75f),
        cardBg      = Color(0xFF0E0900),
        isLegend    = true,
    )
}

// ── Badge composables ─────────────────────────────────────────

/**
 * Animated gold badge for LEGEND rank.
 * Pulses a double-ring glow using InfiniteTransition.
 */
@Composable
private fun LegendGlowBadge(config: RankConfig, size: Dp = 72.dp) {
    val inf = rememberInfiniteTransition(label = "legendGlow")

    val pulse by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "legendPulse",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .drawBehind {
                val cx      = this.size.width  / 2f
                val cy      = this.size.height / 2f
                val radius  = this.size.minDimension / 2f
                val glowAlpha = 0.12f + 0.28f * pulse

                // Outer soft halo
                drawCircle(
                    color  = BattleGold.copy(alpha = glowAlpha * 0.5f),
                    radius = radius * 1.55f,
                    center = Offset(cx, cy),
                )
                // Inner glow ring
                drawCircle(
                    color  = BattleGold.copy(alpha = glowAlpha),
                    radius = radius * 1.2f,
                    center = Offset(cx, cy),
                    style  = Stroke(width = 3f + 4f * pulse),
                )
            }
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        BattleGold.copy(alpha = 0.18f + 0.12f * pulse),
                        config.badgeBg,
                    )
                )
            )
            .border(
                width = (1.5f + 1.5f * pulse).dp,
                color = BattleGold.copy(alpha = 0.7f + 0.3f * pulse),
                shape = androidx.compose.foundation.shape.CircleShape,
            )
    ) {
        Text(
            text       = config.icon,
            fontSize   = (size.value * 0.44f).sp,
            fontWeight = FontWeight.Black,
        )
    }
}

/**
 * Static badge for RECRUIT, SOLDIER, ELITE.
 * No animation — keeps composition lightweight for lower ranks.
 */
@Composable
private fun StaticBadge(config: RankConfig, size: Dp = 72.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(config.badgeBg)
            .border(
                width = 1.5.dp,
                color = config.borderColor,
                shape = androidx.compose.foundation.shape.CircleShape,
            )
    ) {
        Text(
            text       = config.icon,
            fontSize   = (size.value * 0.44f).sp,
            fontWeight = FontWeight.Black,
        )
    }
}

// ── Public composable ─────────────────────────────────────────

/**
 * Displays the warrior's current rank as a card.
 * Drop this anywhere — it is fully self-contained.
 *
 * @param streak  Current streak count (use streakAnim from Dashboard
 *                so color transitions stay smooth).
 */
@Composable
fun WarriorRankCard(
    streak:   Int,
    modifier: Modifier = Modifier,
) {
    val config = rankFor(streak)

    // Card border: static for lower ranks, animated for LEGEND
    val borderWidth = if (config.isLegend) 1.5.dp else 1.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(config.cardBg)
            .border(borderWidth, config.borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Badge
        if (config.isLegend) {
            LegendGlowBadge(config = config)
        } else {
            StaticBadge(config = config)
        }

        // Text block
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Rank label
            Text(
                text          = "RANK",
                fontSize      = 8.sp,
                color         = config.subColor.copy(alpha = 0.6f),
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
            )
            // Rank title
            Text(
                text       = config.title,
                fontSize   = 22.sp,
                color      = config.badgeFg,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            // Subtitle / flavour
            Text(
                text       = config.subtitle,
                fontSize   = 11.sp,
                color      = config.subColor,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp,
            )
            // Streak day count
            Text(
                text          = "DAY $streak",
                fontSize      = 9.sp,
                color         = config.subColor.copy(alpha = 0.5f),
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
            )
        }
    }
}
