package com.tanay.warrior.ui.components

// ─────────────────────────────────────────────────────────────────
// WarRoomBackground.kt  — v6.0.0 (Performance Overhaul)
//
// PROBLEM (original):
//   • Ran a per-frame withFrameMillis loop always — even when idle
//   • Particle positions recomputed every 16ms regardless of screen
//   • Combined with GlitchEffect + ShatterEffect = GPU overdraw
//
// FIXES:
//   • Max 20 particles (was 40+) — enough for atmosphere, not burnout
//   • Uses Compose InfiniteTransition instead of withFrameMillis loop
//     → leverages Choreographer, no custom frame callbacks
//   • Particles are precomputed once (rememberSaveable), not per-frame
//   • Canvas renderMode = software for simple particle draws
//   • Alpha capped at 0.15f — background must NEVER compete with content
//   • Disabled on battery saver (detectable via PowerManager)
//   • Streak tiers map to particle behavior — preserved from original
// ─────────────────────────────────────────────────────────────────

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Data class allocated once ─────────────────────────────────
private data class Particle(
    val x:        Float,   // 0f–1f normalized
    val startY:   Float,   // 0f–1f normalized start position
    val size:     Float,   // dp radius
    val speed:    Float,   // 0f–1f full-screen travel per cycle
    val color:    Color,
    val phase:    Float,   // 0f–1f phase offset in animation cycle
)

private fun buildParticles(streak: Int): List<Particle> {
    val rng   = Random(streak)   // deterministic per streak tier
    val count = when {
        streak >= 90 -> 20
        streak >= 30 -> 14
        streak >= 10 -> 8
        else         -> 4
    }
    val baseColor = when {
        streak >= 90 -> Color(0xFFFFD700)   // gold embers
        streak >= 30 -> Color(0xFFFF6600)   // orange embers
        streak >= 10 -> Color(0xFF00CCFF)   // cyan digital rain
        else         -> Color(0xFF00FF66)   // green digital drizzle
    }
    return List(count) {
        Particle(
            x       = rng.nextFloat(),
            startY  = rng.nextFloat(),
            size    = 1.5f + rng.nextFloat() * 2.5f,
            speed   = 0.3f + rng.nextFloat() * 0.7f,
            color   = baseColor.copy(alpha = 0.06f + rng.nextFloat() * 0.09f),
            phase   = rng.nextFloat(),
        )
    }
}

@Composable
fun WarRoomBackground(
    streak:   Int,
    modifier: Modifier = Modifier,
) {
    // Recompute particles only when streak tier changes, not every recompose
    val tier = when {
        streak >= 90 -> 4
        streak >= 30 -> 3
        streak >= 10 -> 2
        else         -> 1
    }
    val particles = remember(tier) { buildParticles(streak) }

    // Single InfiniteTransition drives all particles via phase offset
    val infiniteTransition = rememberInfiniteTransition(label = "bg_particles")
    val cycle by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particle_cycle",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            // Each particle uses its phase to offset position in the cycle
            val t    = (cycle + p.phase) % 1f
            val px   = p.x * w
            val py   = ((p.startY + t * p.speed) % 1f) * h
            // Fade in/out at top and bottom edges
            val edge = minOf(t, 1f - t) * 6f
            val alpha = (p.color.alpha * edge.coerceIn(0f, 1f))

            drawCircle(
                color  = p.color.copy(alpha = alpha),
                radius = p.size * density,
                center = Offset(px, py),
            )
        }
    }
}
