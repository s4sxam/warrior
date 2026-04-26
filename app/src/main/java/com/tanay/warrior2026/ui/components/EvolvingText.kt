package com.tanay.warrior.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// ── EvolvingText.kt ───────────────────────────────────────────
// Evolving Typography — v1.0.0
//
// Stages:
//   Day  0–6    BROKEN     FontWeight.Thin,      dimmed rust-grey
//                          Subtle random flicker (alpha jitter)
//                          Conveys defeat, fragility
//
//   Day  7–29   SOLDIER    FontWeight.SemiBold,  iron-red
//                          No animation — steady, reliable
//
//   Day 30–89   ELITE      FontWeight.Bold,      steel-blue
//                          Slight letter spacing (4sp)
//                          No animation — disciplined, controlled
//
//   Day 90+     LEGEND     FontWeight.ExtraBold, battle-gold
//                          Shimmer: a moving highlight sweep across the text
//                          Implemented via drawWithContent + animated gradient
//
// Public API:
//   EvolvingStreakNumber(streak: Int, modifier: Modifier = Modifier)
//
// Design rules:
//   • No custom font files — weight/color/spacing only
//   • The BROKEN flicker uses a single InfiniteTransition float
//     that drives a low-frequency alpha oscillation — not a random
//     frame loop, so it's deterministic and battery-friendly
//   • The LEGEND shimmer uses a single InfiniteTransition offset
//     that sweeps a translucent white gradient left→right across
//     the text bounding box via drawWithContent + BlendMode.SrcAtop
//     so it clips perfectly to the glyph shapes
// ─────────────────────────────────────────────────────────────

// ── Colors ────────────────────────────────────────────────────

private val BrokenColor  = Color(0xFF4A3A32)   // dim rust-grey
private val SoldierColor = Color(0xFFB22222)   // iron-red
private val EliteColor   = Color(0xFF8BAFC4)   // steel-blue
private val LegendColor  = Color(0xFFFFD700)   // battle-gold

// ── Stage config ──────────────────────────────────────────────

private enum class TextStage { BROKEN, SOLDIER, ELITE, LEGEND }

private fun stageFor(streak: Int): TextStage = when {
    streak < 7  -> TextStage.BROKEN
    streak < 30 -> TextStage.SOLDIER
    streak < 90 -> TextStage.ELITE
    else        -> TextStage.LEGEND
}

// ── Stage composables ─────────────────────────────────────────

/**
 * BROKEN — thin, dimmed, low-frequency alpha flicker.
 * InfiniteTransition oscillates alpha between 0.38 and 0.62
 * at 2 200ms period — slow enough to feel like a dying signal.
 */
@Composable
private fun BrokenNumber(text: String, fontSize: TextUnit, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "broken")
    val flicker by inf.animateFloat(
        initialValue  = 0.38f,
        targetValue   = 0.62f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "flicker",
    )

    Text(
        text          = text,
        fontSize      = fontSize,
        fontWeight    = FontWeight.Thin,
        color         = BrokenColor.copy(alpha = flicker),
        textAlign     = TextAlign.Center,
        letterSpacing = 0.sp,
        modifier      = modifier,
    )
}

/**
 * SOLDIER — steady iron-red, SemiBold. No animation.
 */
@Composable
private fun SoldierNumber(text: String, fontSize: TextUnit, modifier: Modifier) {
    Text(
        text          = text,
        fontSize      = fontSize,
        fontWeight    = FontWeight.SemiBold,
        color         = SoldierColor,
        textAlign     = TextAlign.Center,
        letterSpacing = 0.sp,
        modifier      = modifier,
    )
}

/**
 * ELITE — steel-blue, Bold, 4sp letter spacing.
 * The spacing makes it feel precise and disciplined.
 */
@Composable
private fun EliteNumber(text: String, fontSize: TextUnit, modifier: Modifier) {
    Text(
        text          = text,
        fontSize      = fontSize,
        fontWeight    = FontWeight.Bold,
        color         = EliteColor,
        textAlign     = TextAlign.Center,
        letterSpacing = 4.sp,
        modifier      = modifier,
    )
}

/**
 * LEGEND — ExtraBold gold with a shimmer sweep.
 *
 * Shimmer implementation:
 *   1. Draw the text normally (gold, ExtraBold).
 *   2. In drawWithContent, after drawing the content, overlay a
 *      narrow translucent-white linear gradient that moves left→right.
 *      BlendMode.SrcAtop clips the overlay to existing drawn pixels
 *      (i.e. the glyph shapes) — so the highlight only appears on
 *      the actual letterforms, not the surrounding space.
 *   3. The gradient offset is driven by an InfiniteTransition float
 *      that goes −1→2 (relative to content width) so the sweep enters
 *      from left, crosses fully, and exits right before repeating.
 *      Period: 2 400ms so it feels weighty, not cheap.
 */
@Composable
private fun LegendNumber(text: String, fontSize: TextUnit, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by inf.animateFloat(
        initialValue  = -1f,
        targetValue   = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    // graphicsLayer with compositingStrategy = CompositingStrategy.Offscreen
    // is required for BlendMode.SrcAtop to work correctly.
    val shimmerModifier = modifier
        .graphicsLayer {
            compositingStrategy =
                androidx.compose.ui.graphics.CompositingStrategy.Offscreen
        }
        .drawWithContent {
            // 1. Draw text first
            drawContent()

            // 2. Sweep overlay — narrow beam across the text width
            val beamWidth = size.width * 0.38f
            val cx        = size.width * shimmerOffset

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.28f),
                        Color.Transparent,
                    ),
                    start = Offset(cx - beamWidth / 2f, 0f),
                    end   = Offset(cx + beamWidth / 2f, 0f),
                ),
                blendMode = BlendMode.SrcAtop,
                size = this.size,
            )
        }

    Text(
        text          = text,
        fontSize      = fontSize,
        fontWeight    = FontWeight.ExtraBold,
        color         = LegendColor,
        textAlign     = TextAlign.Center,
        letterSpacing = 2.sp,
        modifier      = shimmerModifier,
    )
}

// ── Public composable ─────────────────────────────────────────

/**
 * Displays [streak] as a large number whose typography evolves
 * with the warrior's rank.
 *
 * @param streak    Current streak (pass streakAnim for smooth transitions).
 * @param modifier  Optional — size/alignment overrides from the call site.
 * @param fontSize  Base font size. Defaults to 48sp; LEGEND auto-scales to 54sp.
 */
@Composable
fun EvolvingStreakNumber(
    streak:   Int,
    modifier: Modifier  = Modifier,
    fontSize: TextUnit  = 48.sp,
) {
    val stage = stageFor(streak)

    // LEGEND gets a slightly larger font to reinforce its status
    val effectiveFontSize = if (stage == TextStage.LEGEND) (fontSize.value * 1.125f).sp
                            else fontSize

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (stage) {
            TextStage.BROKEN  -> BrokenNumber ("$streak", effectiveFontSize, Modifier)
            TextStage.SOLDIER -> SoldierNumber("$streak", effectiveFontSize, Modifier)
            TextStage.ELITE   -> EliteNumber  ("$streak", effectiveFontSize, Modifier)
            TextStage.LEGEND  -> LegendNumber ("$streak", effectiveFontSize, Modifier)
        }
    }
}
