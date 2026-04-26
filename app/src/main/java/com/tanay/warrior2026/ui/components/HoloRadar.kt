package com.tanay.warrior.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

// ── HoloRadar.kt ──────────────────────────────────────────────
// Holographic Radar Ring — v1.0.0
//
// A radar sweep animation intended to sit BEHIND the streak ring
// as an atmospheric background layer.
//
// Visual layers (bottom → top):
//   1. Concentric grid rings   — dim green #1A2A1A, 4 rings evenly spaced
//   2. Cardinal cross-hairs    — same dim green, hairline strokes
//   3. Sweep trail             — 90° arc, ArenaBlue fading to transparent
//   4. Sweep line              — ArenaBlue bright stroke with soft halo
//   5. Centre origin dot       — ArenaBlue, small
//
// Animation:
//   Full 360° rotation every 3000ms, linear, infinite.
//   Trail is drawn as a segmented arc fan (TRAIL_SEGMENTS steps),
//   each segment alpha proportional to its angular distance behind
//   the sweep head — creates a natural phosphor-decay feel.
//
// Public API:
//   HoloRadarRing(modifier: Modifier = Modifier)
//
// Placement:
//   Box(Modifier.size(280.dp)) {
//       HoloRadarRing(Modifier.fillMaxSize())   // bottom layer
//       StreakRing(...)                          // top layer
//   }
// ─────────────────────────────────────────────────────────────

// ── Constants ─────────────────────────────────────────────────

private val GridColor    = Color(0xFF1A2A1A)       // dim green grid
private val SweepColor   = Color(0xFF00B4FF)       // ArenaBlue
private const val ROTATION_MS     = 3000           // full rotation period
private const val TRAIL_DEGREES   = 90f            // decay arc behind sweep
private const val TRAIL_SEGMENTS  = 36             // steps in trail fan
private const val GRID_RINGS      = 4              // concentric ring count
private const val GRID_ALPHA      = 0.45f          // grid line opacity
private const val GRID_STROKE     = 1.2f           // grid stroke width px
private const val SWEEP_STROKE    = 2.4f           // sweep line width px
private const val HALO_ALPHA      = 0.18f          // bloom behind sweep line

// ── Internal draw helpers ─────────────────────────────────────

private fun DrawScope.drawGrid(radius: Float, cx: Float, cy: Float) {
    val center = Offset(cx, cy)

    // Concentric rings
    for (i in 1..GRID_RINGS) {
        val r = radius * (i.toFloat() / GRID_RINGS)
        drawCircle(
            color       = GridColor.copy(alpha = GRID_ALPHA),
            radius      = r,
            center      = center,
            style       = Stroke(strokeWidth = GRID_STROKE),
        )
    }

    // Horizontal cross-hair
    drawLine(
        color       = GridColor.copy(alpha = GRID_ALPHA),
        start       = Offset(cx - radius, cy),
        end         = Offset(cx + radius, cy),
        strokeWidth = GRID_STROKE,
    )
    // Vertical cross-hair
    drawLine(
        color       = GridColor.copy(alpha = GRID_ALPHA),
        start       = Offset(cx, cy - radius),
        end         = Offset(cx, cy + radius),
        strokeWidth = GRID_STROKE,
    )
    // Diagonal cross-hairs (45°)
    val diag = radius * cos(Math.PI.toFloat() / 4f)
    drawLine(
        color       = GridColor.copy(alpha = GRID_ALPHA * 0.55f),
        start       = Offset(cx - diag, cy - diag),
        end         = Offset(cx + diag, cy + diag),
        strokeWidth = GRID_STROKE * 0.7f,
    )
    drawLine(
        color       = GridColor.copy(alpha = GRID_ALPHA * 0.55f),
        start       = Offset(cx + diag, cy - diag),
        end         = Offset(cx - diag, cy + diag),
        strokeWidth = GRID_STROKE * 0.7f,
    )
}

private fun DrawScope.drawTrail(
    sweepAngleDeg: Float,
    radius: Float,
    cx: Float,
    cy: Float,
) {
    // Draw TRAIL_SEGMENTS thin lines fanning back TRAIL_DEGREES behind
    // the sweep head. Alpha decays linearly from near-full at head to 0.
    val segStep = TRAIL_DEGREES / TRAIL_SEGMENTS
    for (i in 0 until TRAIL_SEGMENTS) {
        val fraction  = 1f - (i.toFloat() / TRAIL_SEGMENTS)   // 1 at head, 0 at tail
        val segAngle  = sweepAngleDeg - i * segStep
        val rad       = Math.toRadians(segAngle.toDouble())
        val ex        = cx + radius * cos(rad).toFloat()
        val ey        = cy + radius * sin(rad).toFloat()

        // Alpha: cubic decay so the first few segments are rich, tail vanishes fast
        val alpha = fraction * fraction * fraction * 0.55f

        drawLine(
            color       = SweepColor.copy(alpha = alpha),
            start       = Offset(cx, cy),
            end         = Offset(ex, ey),
            strokeWidth = SWEEP_STROKE * (0.6f + 0.4f * fraction),
        )
    }
}

private fun DrawScope.drawSweepLine(
    sweepAngleDeg: Float,
    radius: Float,
    cx: Float,
    cy: Float,
) {
    val rad = Math.toRadians(sweepAngleDeg.toDouble())
    val ex  = cx + radius * cos(rad).toFloat()
    val ey  = cy + radius * sin(rad).toFloat()

    // Halo bloom — wider, low alpha
    drawLine(
        color       = SweepColor.copy(alpha = HALO_ALPHA),
        start       = Offset(cx, cy),
        end         = Offset(ex, ey),
        strokeWidth = SWEEP_STROKE * 5f,
    )
    // Core line — bright
    drawLine(
        color       = SweepColor.copy(alpha = 0.90f),
        start       = Offset(cx, cy),
        end         = Offset(ex, ey),
        strokeWidth = SWEEP_STROKE,
    )
}

private fun DrawScope.drawOriginDot(cx: Float, cy: Float) {
    drawCircle(
        color  = SweepColor.copy(alpha = 0.80f),
        radius = SWEEP_STROKE * 1.8f,
        center = Offset(cx, cy),
    )
    // Tiny halo
    drawCircle(
        color  = SweepColor.copy(alpha = 0.22f),
        radius = SWEEP_STROKE * 4.5f,
        center = Offset(cx, cy),
    )
}

// ── Public composable ─────────────────────────────────────────

/**
 * Rotating holographic radar sweep, designed to sit behind the streak ring.
 *
 * The composable fills whatever [modifier] provides and is fully self-contained.
 * No state or callbacks needed from the caller.
 *
 * Usage:
 * ```
 * Box(Modifier.size(280.dp)) {
 *     HoloRadarRing(Modifier.fillMaxSize())  // background
 *     StreakRingComposable(...)               // foreground
 * }
 * ```
 */
@Composable
fun HoloRadarRing(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "radarSweep")

    // Animate 0° → 360° linearly over ROTATION_MS, infinite
    val sweepAngle by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = ROTATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarAngle",
    )

    Canvas(modifier = modifier) {
        val cx     = size.width  / 2f
        val cy     = size.height / 2f
        val radius = size.minDimension / 2f

        // Layer 1 — grid
        drawGrid(radius, cx, cy)

        // Layer 2 — sweep trail (drawn before the sweep line so line sits on top)
        drawTrail(sweepAngle, radius, cx, cy)

        // Layer 3 — sweep line + halo
        drawSweepLine(sweepAngle, radius, cx, cy)

        // Layer 4 — origin dot
        drawOriginDot(cx, cy)
    }
}
