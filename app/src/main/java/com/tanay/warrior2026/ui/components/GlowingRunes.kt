package com.tanay.warrior.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── GlowingRunes.kt ───────────────────────────────────────────
// Glowing Habit Runes — v1.0.0
//
// Replaces habit emojis with minimalist vector runes drawn on Canvas.
// Each rune ignites with a layered glow when isActive = true.
//
// Public API:
//   GlowingHabitRune(label: String, isActive: Boolean,
//                    modifier: Modifier = Modifier)
//
// Visual design:
//   INACTIVE  — dim #252525 border, #111111 bg, rune drawn in
//               muted Color(0xFF2A2A2A), label in Color(0xFF444444)
//   ACTIVE    — ArenaBlue (#00B4FF) + WarriorRed (#FF3131) cross-glow
//               border pulses between ArenaBlue and VictoryGreen,
//               rune strokes brighten to white with animated outer halo
//               label lifts to white
//
// Rune vocabulary (6 runes cycling on label hash):
//   RUNE_THORN   — upward triangle with inner notch
//   RUNE_BRAND   — vertical line + 2 diagonal wings
//   RUNE_SIGIL   — diamond with cross
//   RUNE_VEIL    — two stacked arcs (eye-like)
//   RUNE_FANG    — chevron + bottom spike
//   RUNE_AEGIS   — hexagonal outline + centre dot
//
// Animation:
//   Active glow pulses on InfiniteTransition (1600ms, EaseInOutSine)
//   Glow alpha: 0.35 → 0.85, halo radius scales +20%
//   Border pulse: 1400ms, ArenaBlue ↔ VictoryGreen alpha
//
// Sizing:
//   Default rune tile: 64×64dp
//   Rune strokes occupy ~60% of tile
//   Label below tile: 10sp, letterSpacing 1.5sp
// ─────────────────────────────────────────────────────────────

// ── Theme colors ──────────────────────────────────────────────

private val WarriorRed    = Color(0xFFFF3131)
private val VictoryGreen  = Color(0xFF1DB954)
private val ArenaBlue     = Color(0xFF00B4FF)
private val CardColor     = Color(0xFF111111)
private val BorderColor   = Color(0xFF252525)
private val RuneDim       = Color(0xFF2A2A2A)
private val LabelDim      = Color(0xFF444444)

// ── Rune types ────────────────────────────────────────────────

private enum class RuneGlyph {
    THORN, BRAND, SIGIL, VEIL, FANG, AEGIS
}

private fun runeFor(label: String): RuneGlyph {
    val values = RuneGlyph.entries
    return values[Math.abs(label.hashCode()) % values.size]
}

// ── Canvas drawing functions ──────────────────────────────────

/**
 * All draw functions normalise to a [0,1]×[0,1] unit square
 * then scale to the canvas size. strokeWidth is in raw px.
 */

private fun DrawScope.drawRuneThorn(strokePx: Float, color: Color) {
    // Upward triangle with inner notch cut from bottom
    val w = size.width
    val h = size.height
    val pad = 0.14f
    val path = Path().apply {
        moveTo(w * 0.50f, h * pad)            // apex
        lineTo(w * (1f - pad), h * (1f - pad))    // bottom-right
        lineTo(w * 0.50f, h * 0.68f)          // inner notch
        lineTo(w * pad, h * (1f - pad))        // bottom-left
        close()
    }
    drawPath(path, color = color, style = Stroke(width = strokePx, join = StrokeJoin.Miter))

    // Vertical centre line from notch up
    drawLine(
        color = color,
        start = Offset(w * 0.50f, h * pad),
        end   = Offset(w * 0.50f, h * 0.68f),
        strokeWidth = strokePx,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawRuneBrand(strokePx: Float, color: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    // Vertical spine
    drawLine(color, Offset(cx, h * 0.12f), Offset(cx, h * 0.88f), strokePx, cap = StrokeCap.Round)
    // Upper left wing
    drawLine(color, Offset(cx, h * 0.28f), Offset(w * 0.22f, h * 0.52f), strokePx, cap = StrokeCap.Round)
    // Upper right wing
    drawLine(color, Offset(cx, h * 0.28f), Offset(w * 0.78f, h * 0.52f), strokePx, cap = StrokeCap.Round)
    // Lower left wing (shorter)
    drawLine(color, Offset(cx, h * 0.56f), Offset(w * 0.28f, h * 0.74f), strokePx, cap = StrokeCap.Round)
    // Lower right wing (shorter)
    drawLine(color, Offset(cx, h * 0.56f), Offset(w * 0.72f, h * 0.74f), strokePx, cap = StrokeCap.Round)
}

private fun DrawScope.drawRuneSigil(strokePx: Float, color: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val pad = 0.16f
    // Diamond outline
    val path = Path().apply {
        moveTo(cx,               h * pad)
        lineTo(w * (1f - pad),   cy)
        lineTo(cx,               h * (1f - pad))
        lineTo(w * pad,          cy)
        close()
    }
    drawPath(path, color = color, style = Stroke(width = strokePx, join = StrokeJoin.Miter))
    // Horizontal cross bar
    drawLine(color, Offset(w * pad, cy), Offset(w * (1f - pad), cy), strokePx, cap = StrokeCap.Round)
    // Vertical cross bar
    drawLine(color, Offset(cx, h * pad), Offset(cx, h * (1f - pad)), strokePx, cap = StrokeCap.Round)
}

private fun DrawScope.drawRuneVeil(strokePx: Float, color: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    // Outer arc (top-open eye)
    val outerPath = Path().apply {
        moveTo(w * 0.14f, h * 0.50f)
        cubicTo(
            w * 0.14f, h * 0.14f,
            w * 0.86f, h * 0.14f,
            w * 0.86f, h * 0.50f,
        )
    }
    drawPath(outerPath, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round))
    // Inner arc (bottom-open eye)
    val innerPath = Path().apply {
        moveTo(w * 0.14f, h * 0.50f)
        cubicTo(
            w * 0.14f, h * 0.86f,
            w * 0.86f, h * 0.86f,
            w * 0.86f, h * 0.50f,
        )
    }
    drawPath(innerPath, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round))
    // Centre pupil dot
    drawCircle(color = color, radius = strokePx * 1.4f, center = Offset(cx, h / 2f))
}

private fun DrawScope.drawRuneFang(strokePx: Float, color: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    // Upper chevron
    drawLine(color, Offset(w * 0.18f, h * 0.30f), Offset(cx, h * 0.50f), strokePx, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.82f, h * 0.30f), Offset(cx, h * 0.50f), strokePx, cap = StrokeCap.Round)
    // Lower chevron (tighter)
    drawLine(color, Offset(w * 0.28f, h * 0.50f), Offset(cx, h * 0.66f), strokePx, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.72f, h * 0.50f), Offset(cx, h * 0.66f), strokePx, cap = StrokeCap.Round)
    // Bottom fang spike
    drawLine(color, Offset(cx, h * 0.66f), Offset(cx, h * 0.88f), strokePx, cap = StrokeCap.Round)
    // Top horizontal bar
    drawLine(color, Offset(w * 0.18f, h * 0.30f), Offset(w * 0.82f, h * 0.30f), strokePx, cap = StrokeCap.Round)
}

private fun DrawScope.drawRuneAegis(strokePx: Float, color: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val pad = 0.15f
    val rx  = w * (0.5f - pad)
    val ry  = h * (0.5f - pad)
    // Hexagon (6 points)
    val path = Path().apply {
        val pts = 6
        for (i in 0 until pts) {
            val angle = Math.PI / 6 + i * (2 * Math.PI / pts)   // flat-top hex
            val px = cx + rx * Math.cos(angle).toFloat()
            val py = cy + ry * Math.sin(angle).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(path, color = color, style = Stroke(width = strokePx, join = StrokeJoin.Round))
    // Centre dot
    drawCircle(color = color, radius = strokePx * 1.6f, center = Offset(cx, cy))
    // Cardinal spokes
    val spokeLen = rx * 0.38f
    drawLine(color, Offset(cx, cy - spokeLen), Offset(cx, cy + spokeLen), strokePx * 0.75f, cap = StrokeCap.Round)
    drawLine(color, Offset(cx - spokeLen, cy), Offset(cx + spokeLen, cy), strokePx * 0.75f, cap = StrokeCap.Round)
}

// ── Dispatcher ────────────────────────────────────────────────

private fun DrawScope.drawRune(glyph: RuneGlyph, strokePx: Float, color: Color) {
    when (glyph) {
        RuneGlyph.THORN -> drawRuneThorn(strokePx, color)
        RuneGlyph.BRAND -> drawRuneBrand(strokePx, color)
        RuneGlyph.SIGIL -> drawRuneSigil(strokePx, color)
        RuneGlyph.VEIL  -> drawRuneVeil(strokePx, color)
        RuneGlyph.FANG  -> drawRuneFang(strokePx, color)
        RuneGlyph.AEGIS -> drawRuneAegis(strokePx, color)
    }
}

// ── Public composable ─────────────────────────────────────────

/**
 * A minimalist vector rune tile that ignites with a glow when [isActive].
 *
 * @param label    Habit name shown below the tile; also seeds the rune shape.
 * @param isActive Whether this habit is currently active/checked.
 * @param modifier Optional layout modifier.
 * @param tileSize Size of the square rune tile (default 64.dp).
 */
@Composable
fun GlowingHabitRune(
    label:    String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    tileSize: Dp = 64.dp,
) {
    val glyph = remember(label) { runeFor(label) }

    // ── Animations ─────────────────────────────────────────────
    val inf = rememberInfiniteTransition(label = "runeGlow_$label")

    val glowPulse by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse_$label",
    )

    val borderPulse by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "borderPulse_$label",
    )

    // Derived visual values
    val runeColor  = if (isActive) Color.White else RuneDim
    val labelColor = if (isActive) Color.White else LabelDim
    val borderColorFinal = if (isActive)
        ArenaBlue.copy(alpha = 0.5f + 0.5f * borderPulse)
            .compositeWith(VictoryGreen.copy(alpha = 0.3f * (1f - borderPulse)))
    else BorderColor
    val glowAlpha = if (isActive) 0.35f + 0.50f * glowPulse else 0f
    val haloScale = if (isActive) 1.0f + 0.20f * glowPulse else 0f

    Column(
        modifier          = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {

        // ── Rune tile ─────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(tileSize)
                .drawBehind {
                    // Outer soft halo (active only)
                    if (isActive && glowAlpha > 0f) {
                        val cx     = this.size.width  / 2f
                        val cy     = this.size.height / 2f
                        val radius = this.size.minDimension / 2f * haloScale * 1.55f

                        // Double-layer glow: ArenaBlue outer + WarriorRed inner
                        drawCircle(
                            color  = ArenaBlue.copy(alpha = glowAlpha * 0.40f),
                            radius = radius,
                            center = Offset(cx, cy),
                        )
                        drawCircle(
                            color  = WarriorRed.copy(alpha = glowAlpha * 0.18f),
                            radius = radius * 0.70f,
                            center = Offset(cx, cy),
                        )
                        // Ring stroke
                        drawCircle(
                            color  = ArenaBlue.copy(alpha = glowAlpha * 0.55f),
                            radius = radius * 0.82f,
                            center = Offset(cx, cy),
                            style  = Stroke(width = 2.5f + 2.5f * glowPulse),
                        )
                    }
                }
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isActive)
                        Brush.radialGradient(
                            colors = listOf(
                                ArenaBlue.copy(alpha = 0.10f + 0.08f * glowPulse),
                                CardColor,
                            )
                        )
                    else Brush.linearGradient(listOf(CardColor, CardColor))
                )
                .border(
                    width = if (isActive) 1.5.dp else 1.dp,
                    color = borderColorFinal,
                    shape = RoundedCornerShape(14.dp),
                )
        ) {
            // Rune drawn on Canvas inside the tile
            val strokeFraction = 0.045f   // ~3px on 64dp tile
            Canvas(modifier = Modifier.fillMaxSize(0.62f)) {
                val strokePx = size.minDimension * strokeFraction /
                        (0.62f)   // compensate fillMaxSize fraction
                drawRune(glyph, strokePx * 1.6f, runeColor)
            }
        }

        // ── Label ─────────────────────────────────────────────
        Text(
            text          = label.uppercase(),
            fontSize      = 10.sp,
            fontWeight    = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color         = labelColor,
            letterSpacing = 1.5.sp,
            textAlign     = TextAlign.Center,
            maxLines      = 1,
        )
    }
}

// ── Utility ───────────────────────────────────────────────────

/**
 * Lightweight additive colour blend — keeps glow warm rather than
 * fully clamped to a single hue. Uses simple linear interpolation
 * on RGB channels; no external libraries needed.
 */
private fun Color.compositeWith(other: Color): Color {
    return Color(
        red   = (this.red   + other.red  ).coerceAtMost(1f),
        green = (this.green + other.green).coerceAtMost(1f),
        blue  = (this.blue  + other.blue ).coerceAtMost(1f),
        alpha = this.alpha,
    )
}

// ── Row helper ────────────────────────────────────────────────

/**
 * Convenience wrapper that renders a horizontal row of habit runes.
 * Intended as a drop-in replacement for emoji-based habit rows.
 *
 * @param habits List of (label, isActive) pairs — max 5 renders cleanly.
 */
@Composable
fun HabitRuneRow(
    habits:   List<Pair<String, Boolean>>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier            = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment   = Alignment.Top,
    ) {
        habits.forEach { (label, isActive) ->
            GlowingHabitRune(
                label    = label,
                isActive = isActive,
            )
        }
    }
}
