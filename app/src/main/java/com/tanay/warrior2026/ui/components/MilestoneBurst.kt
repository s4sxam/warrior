package com.tanay.warrior.ui.components

import androidx.compose.runtime.withFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── MilestoneBurst.kt ─────────────────────────────────────────
// Streak Milestone Burst — v1.0.0
//
// Milestone thresholds and their identity:
//   Day  7  → IRON WILL     (iron-red,    Color 0xFFB22222)
//   Day 30  → STEEL MIND    (steel-blue,  Color 0xFF8BAFC4)
//   Day 90  → BATTLE GOLD   (battle-gold, Color 0xFFFFD700)
//
// Timeline (1200ms total):
//   0 → 400ms   BURST phase
//                 40 particles explode outward from screen center.
//                 Speed peaks at launch, decays with easing.
//                 Each particle has a random angle, speed, size, alpha.
//
//   0 → 1000ms  LABEL phase
//                 Centered text "7 DAYS — IRON WILL" fades in over 200ms,
//                 holds, then fades out over the final 200ms.
//
//   400 → 1200ms FADE phase
//                 Particles slow and alpha fades to 0.
//
// Architecture:
//   • MilestoneConfig  — data for each milestone (color, label)
//   • BurstParticle    — plain data class, allocated once
//   • buildParticles() — called once when canvas size is known
//   • MilestoneBurst   — public composable, accepts streak: Int
//                        internally derives which milestone triggered
//
// Usage:
//   MilestoneBurst(streak = streakAnim)
//   No isTriggered flag needed — composable self-activates on milestone
//   values and auto-exits after 1200ms.
// ─────────────────────────────────────────────────────────────

private const val PARTICLE_COUNT   = 40
private const val BURST_DURATION   = 1200L
private const val LABEL_DURATION   = 1000L

// ── Milestone definitions ─────────────────────────────────────

private data class MilestoneConfig(
    val streak: Int,
    val label:  String,
    val color:  Color,
)

private val MILESTONES = listOf(
    MilestoneConfig(7,  "7 DAYS — IRON WILL",    Color(0xFFB22222)),
    MilestoneConfig(30, "30 DAYS — STEEL MIND",  Color(0xFF8BAFC4)),
    MilestoneConfig(90, "90 DAYS — BATTLE GOLD", Color(0xFFFFD700)),
)

private fun milestoneFor(streak: Int): MilestoneConfig? =
    MILESTONES.firstOrNull { it.streak == streak }

// ── Particle ──────────────────────────────────────────────────

private data class BurstParticle(
    val angle:     Float,   // radians, fixed launch direction
    val speed:     Float,   // px/sec at launch
    val radiusPx:  Float,   // dot radius
    val baseAlpha: Float,   // max alpha (0.5–1.0)
    // tail: secondary smaller dot offset backwards on same axis
    val hasTail:   Boolean,
)

private fun buildParticles(screenW: Float): List<BurstParticle> {
    val baseSpeed = screenW * 1.1f   // crosses ~screen width in 1s at launch
    return List(PARTICLE_COUNT) { i ->
        // Evenly spread + small random jitter so no gaps
        val base  = (i.toFloat() / PARTICLE_COUNT) * 2f * Math.PI.toFloat()
        val angle = base + (Random.nextFloat() - 0.5f) * (2f * Math.PI.toFloat() / PARTICLE_COUNT * 0.9f)
        BurstParticle(
            angle     = angle,
            speed     = baseSpeed * (0.5f + Random.nextFloat() * 0.8f),
            radiusPx  = screenW * (0.008f + Random.nextFloat() * 0.014f),
            baseAlpha = 0.5f + Random.nextFloat() * 0.5f,
            hasTail   = Random.nextBoolean(),
        )
    }
}

// ── Composable ────────────────────────────────────────────────

/**
 * Place in the root Box of DashboardScreen, keyed to streakAnim.
 * Activates automatically when [streak] equals 7, 30, or 90.
 * Renders nothing for any other value.
 */
@Composable
fun MilestoneBurst(streak: Int) {
    val config = milestoneFor(streak) ?: return

    // Re-key on streak so the burst replays correctly if somehow retriggered
    key(streak) {
        MilestoneBurstContent(config)
    }
}

@Composable
private fun MilestoneBurstContent(config: MilestoneConfig) {
    // progress: 0→1 over BURST_DURATION ms
    var progress by remember { mutableFloatStateOf(0f) }
    var done     by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val startMs = withFrameMillis { it }
        while (true) {
            val now     = withFrameMillis { it }
            val elapsed = now - startMs
            progress    = (elapsed / BURST_DURATION.toFloat()).coerceIn(0f, 1f)
            if (elapsed >= BURST_DURATION) { done = true; break }
        }
    }

    if (done) return

    // ── Particle easing ───────────────────────────────────────
    // Particles decelerate sharply — most distance in first 400ms.
    // Using a simple inverse-square decay: distance = speed * sqrt(progress)
    // This gives the "burst then drift" feel without physics libraries.
    val tPos   = kotlin.math.sqrt(progress)            // 0→1, fast at start
    val tAlpha = (1f - progress).coerceIn(0f, 1f)     // linear fade

    // ── Label alpha: fade in 0→200ms, hold, fade out last 200ms ──
    val labelAlpha = when {
        progress < (200f / BURST_DURATION)  ->
            progress / (200f / BURST_DURATION)
        progress > (LABEL_DURATION.toFloat() / BURST_DURATION) ->
            1f - (progress - LABEL_DURATION.toFloat() / BURST_DURATION) /
                 (1f - LABEL_DURATION.toFloat() / BURST_DURATION)
        else -> 1f
    }.coerceIn(0f, 1f)

    // Particles built once canvas size is known
    var particles by remember { mutableStateOf<List<BurstParticle>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Particle canvas ───────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (particles.isEmpty() && size.width > 0f) {
                particles = buildParticles(size.width)
            }

            val cx = size.width  / 2f
            val cy = size.height / 2f

            for (p in particles) {
                val dist = p.speed * tPos
                val px   = cx + cos(p.angle) * dist
                val py   = cy + sin(p.angle) * dist
                val a    = p.baseAlpha * tAlpha

                // Main dot — core bright
                drawCircle(
                    color  = config.color.copy(alpha = a),
                    radius = p.radiusPx,
                    center = Offset(px, py),
                )
                // Bloom halo
                drawCircle(
                    color  = config.color.copy(alpha = a * 0.25f),
                    radius = p.radiusPx * 2.8f,
                    center = Offset(px, py),
                )
                // Tail: short line trailing back toward center
                if (p.hasTail && tPos > 0.01f) {
                    val tailLen = p.radiusPx * 3.5f
                    val tailX   = cx + cos(p.angle) * (dist - tailLen).coerceAtLeast(0f)
                    val tailY   = cy + sin(p.angle) * (dist - tailLen).coerceAtLeast(0f)
                    drawLine(
                        color       = config.color.copy(alpha = a * 0.55f),
                        start       = Offset(tailX, tailY),
                        end         = Offset(px, py),
                        strokeWidth = p.radiusPx * 0.8f,
                        cap         = StrokeCap.Round,
                    )
                }
            }
        }

        // ── Milestone label ───────────────────────────────────
        if (labelAlpha > 0f) {
            Box(
                modifier        = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = config.label,
                    color      = config.color.copy(alpha = labelAlpha),
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Black,
                    textAlign  = TextAlign.Center,
                    letterSpacing = 3.sp,
                    modifier   = Modifier.wrapContentSize(),
                )
            }
        }
    }
}
