package com.tanay.warrior.ui.components

import androidx.compose.runtime.withFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

// ── SlashEffect.kt ────────────────────────────────────────────
// Sword Slash on Victory — v1.0.0
//
// Timeline (total 400ms):
//   0 → 200ms   SWEEP phase
//                 The slash line draws progressively from
//                 top-left → bottom-right of the screen.
//                 Three stacked strokes: wide glow, mid gold, thin white core.
//                 At sweep end the full line is visible.
//
//   200 → 400ms  FLASH phase
//                 A full-screen white flash fades in quickly then out,
//                 peaking at t=250ms, gone by t=400ms.
//                 The slash line simultaneously fades out with the flash.
//
// Design:
//   • Canvas-only — no Modifier.graphicsLayer, no new libraries
//   • Two composable layers in a Box: Canvas (slash) + Box (flash)
//   • Single withFrameMillis loop drives both phases via one progress float
//   • Zero allocations inside the draw block
//
// Usage:
//   SlashOverlay(isSlashing = isSlashing)
//   Place AFTER ShatterOverlay in the root Box.
// ─────────────────────────────────────────────────────────────

private const val SWEEP_MS      = 200L
private const val TOTAL_MS      = 400L

private val SlashGold  = Color(0xFFFFD700)
private val SlashWhite = Color(0xFFFFFFFF)
private val SlashGlow  = Color(0xFFFFEE88)

/**
 * Full-screen sword slash overlay.
 *
 * @param isSlashing  Drive true to trigger. Renders nothing when false.
 */
@Composable
fun SlashOverlay(isSlashing: Boolean) {
    if (!isSlashing) return

    // Single progress value 0→1 over TOTAL_MS drives everything.
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isSlashing) {
        val startMs = withFrameMillis { it }
        while (true) {
            val now     = withFrameMillis { it }
            val elapsed = now - startMs
            progress    = (elapsed / TOTAL_MS.toFloat()).coerceIn(0f, 1f)
            if (elapsed >= TOTAL_MS) break
        }
    }

    // ── Derived values ────────────────────────────────────────
    // sweepT  : 0→1 during first 200ms (sweep phase), clamped at 1 after
    val sweepT  = (progress / (SWEEP_MS.toFloat() / TOTAL_MS)).coerceIn(0f, 1f)

    // flashT  : 0→1 over the full 400ms
    //   peaks around progress=0.625 (250ms) then drops to 0 at progress=1
    //   shape: ramp up fast (0→0.625), ramp down (0.625→1)
    val flashT  = when {
        progress < 0.625f -> progress / 0.625f          // 0 → 1
        else              -> 1f - (progress - 0.625f) / 0.375f  // 1 → 0
    }.coerceIn(0f, 1f)

    // Slash line alpha: full during sweep, fades with the flash
    val slashAlpha = if (progress < 0.5f) 1f else (1f - (progress - 0.5f) / 0.5f).coerceIn(0f, 1f)

    // ── Slash line (Canvas) ───────────────────────────────────
    Canvas(modifier = Modifier.fillMaxSize()) {
        val startX = 0f
        val startY = 0f
        val endX   = size.width
        val endY   = size.height

        // Interpolated end-point for the sweep animation
        val curX = startX + (endX - startX) * sweepT
        val curY = startY + (endY - startY) * sweepT

        val start = Offset(startX, startY)
        val cur   = Offset(curX, curY)

        if (sweepT > 0f && slashAlpha > 0f) {
            // Layer 1 — wide outer glow
            drawLine(
                color       = SlashGlow.copy(alpha = 0.18f * slashAlpha),
                start       = start,
                end         = cur,
                strokeWidth = 48f,
                cap         = StrokeCap.Round,
            )
            // Layer 2 — gold mid stroke
            drawLine(
                color       = SlashGold.copy(alpha = 0.55f * slashAlpha),
                start       = start,
                end         = cur,
                strokeWidth = 14f,
                cap         = StrokeCap.Round,
            )
            // Layer 3 — bright white core
            drawLine(
                color       = SlashWhite.copy(alpha = 0.95f * slashAlpha),
                start       = start,
                end         = cur,
                strokeWidth = 3f,
                cap         = StrokeCap.Round,
            )
        }
    }

    // ── White flash (Box) ─────────────────────────────────────
    if (flashT > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SlashWhite.copy(alpha = flashT * 0.45f))
        )
    }
}
