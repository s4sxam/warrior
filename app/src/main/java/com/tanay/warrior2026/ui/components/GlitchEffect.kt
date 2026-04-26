package com.tanay.warrior.ui.components

import androidx.compose.runtime.withFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.delay
import kotlin.random.Random

// ── GlitchEffect.kt ───────────────────────────────────────────
// Glitch & Corruption on Relapse — v1.0.0
//
// Design:
//   • Full-screen aggressive red flash
//   • UI shake: random ±3–8dp offset per frame for 600ms
//   • Horizontal red scanline bars that flicker on/off
//   • Auto-stops after 600ms via LaunchedEffect + delay
//   • Zero external libraries — pure Compose + coroutines
//
// Usage:
//   GlitchOverlay(isGlitching = isGlitching)
//   Place as the LAST child in your root Box so it layers on top.
// ─────────────────────────────────────────────────────────────

private const val GLITCH_DURATION_MS = 600L
private const val SCANLINE_COUNT     = 8

/**
 * Full-screen glitch overlay.
 *
 * @param isGlitching  Drive this true to trigger the effect.
 *                     The composable auto-resets after 600 ms.
 */
@Composable
fun GlitchOverlay(isGlitching: Boolean) {
    if (!isGlitching) return

    // ── Shake state ───────────────────────────────────────────
    var shakeX by remember { mutableFloatStateOf(0f) }
    var shakeY by remember { mutableFloatStateOf(0f) }

    // ── Scanline visibility bitmap ────────────────────────────
    // Each entry is a 0f–1f vertical position for one scanline bar.
    // Recomputed every frame for flicker.
    var scanlinePositions by remember { mutableStateOf(List(SCANLINE_COUNT) { Random.nextFloat() }) }
    var scanlineVisible   by remember { mutableStateOf(List(SCANLINE_COUNT) { Random.nextBoolean() }) }

    // ── Red flash alpha ───────────────────────────────────────
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    // ── Frame loop — drives shake + scanline flicker ──────────
    LaunchedEffect(isGlitching) {
        val startMs = withFrameMillis { it }
        var lastMs  = startMs

        while (true) {
            val now  = withFrameMillis { it }
            val elapsed = now - startMs

            if (elapsed >= GLITCH_DURATION_MS) break

            // Progress 0→1 over the 600ms window
            val t = (elapsed / GLITCH_DURATION_MS.toFloat()).coerceIn(0f, 1f)

            // Flash: spike hard at start, decay toward end
            flashAlpha = (1f - t).coerceIn(0f, 0.55f) * (0.4f + Random.nextFloat() * 0.6f)

            // Shake: random offset 3–8dp, decaying intensity
            val maxShift = (8f - 5f * t)
            shakeX = (Random.nextFloat() * 2f - 1f) * maxShift
            shakeY = (Random.nextFloat() * 2f - 1f) * maxShift

            // Scanlines: randomise positions and flicker visibility
            scanlinePositions = List(SCANLINE_COUNT) { Random.nextFloat() }
            scanlineVisible   = List(SCANLINE_COUNT) { Random.nextBoolean() }

            lastMs = now
        }

        // Reset — composable will exit early on next recompose (isGlitching = false)
        shakeX     = 0f
        shakeY     = 0f
        flashAlpha = 0f
    }

    // ── Red flash layer ───────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = shakeX.dp, y = shakeY.dp)
            .background(Color(0xFFFF0000).copy(alpha = flashAlpha))
    )

    // ── Scanline bars layer ───────────────────────────────────
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = shakeX.dp, y = (-shakeY).dp) // opposite axis for extra chaos
    ) {
        val barH = size.height * 0.025f  // each bar is ~2.5% of screen height

        for (i in 0 until SCANLINE_COUNT) {
            if (!scanlineVisible[i]) continue

            val yPos = scanlinePositions[i] * size.height

            drawRect(
                color   = Color(0xFFFF0000).copy(alpha = 0.45f + Random.nextFloat() * 0.35f),
                topLeft = Offset(0f, yPos),
                size    = Size(size.width, barH)
            )
        }
    }
}
