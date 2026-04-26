package com.tanay.warrior.ui.components

import androidx.compose.runtime.withFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── ShatterEffect.kt ──────────────────────────────────────────
// Shattering Glass on Relapse — v1.0.0
//
// Design:
//   • Triggered after GlitchOverlay completes (600ms delay in Dashboard)
//   • 18 glass shards burst outward from screen center
//   • Each shard is a Canvas-drawn triangle — random size, angle, speed
//   • Shards travel outward with slight angular drift for natural scatter
//   • Fade out linearly over 800ms then composable exits
//   • Zero external libraries — pure Compose Canvas + coroutines
//
// Usage:
//   ShatterOverlay(isShattered = isShattered)
//   Place AFTER GlitchOverlay in the root Box.
// ─────────────────────────────────────────────────────────────

private const val SHARD_COUNT       = 18
private const val SHATTER_DURATION_MS = 800L

// Glass shard tint — icy blue-white, like broken tempered glass
private val GlassColor = Color(0xFFCCEEFF)

/**
 * Runtime state of one glass shard.
 * All positions stored as canvas-space px — computed once on spawn.
 */
private data class Shard(
    // Triangle vertices relative to the shard's local origin
    val v0: Offset,
    val v1: Offset,
    val v2: Offset,
    // Spawn position (screen center at first frame)
    val originX: Float,
    val originY: Float,
    // Velocity in px/sec
    val velX: Float,
    val velY: Float,
    // Slow angular drift (radians/sec) for natural tumble
    val angularVel: Float,
    // Intrinsic alpha for variety (0.55–1.0)
    val baseAlpha: Float,
)

/**
 * Spawns all shards at screen center with randomised properties.
 * Call once when canvas dimensions are first known.
 */
private fun buildShards(centerX: Float, centerY: Float, screenW: Float): List<Shard> {
    val baseSpeed = screenW * 0.55f   // px/sec — shards travel ~half a screen width over 800ms
    return List(SHARD_COUNT) { i ->
        // Evenly distributed launch angles + random jitter so shards don't clump
        val baseAngle = (i.toFloat() / SHARD_COUNT) * (2f * Math.PI.toFloat())
        val angle     = baseAngle + (Random.nextFloat() - 0.5f) * (2f * Math.PI.toFloat() / SHARD_COUNT)

        val speed = baseSpeed * (0.6f + Random.nextFloat() * 0.8f)
        val velX  = cos(angle) * speed
        val velY  = sin(angle) * speed

        // Triangle vertices — asymmetric so each shard looks unique
        val sz = screenW * (0.025f + Random.nextFloat() * 0.045f)  // 2.5–7% of screen width
        val v0 = Offset(0f, -sz)
        val v1 = Offset(-sz * (0.4f + Random.nextFloat() * 0.4f),  sz * (0.5f + Random.nextFloat() * 0.5f))
        val v2 = Offset( sz * (0.4f + Random.nextFloat() * 0.4f),  sz * (0.3f + Random.nextFloat() * 0.6f))

        Shard(
            v0          = v0,
            v1          = v1,
            v2          = v2,
            originX     = centerX,
            originY     = centerY,
            velX        = velX,
            velY        = velY,
            angularVel  = (Random.nextFloat() - 0.5f) * 6f,   // ±3 rad/sec tumble
            baseAlpha   = 0.55f + Random.nextFloat() * 0.45f,
        )
    }
}

/**
 * Draws one shard at elapsed time [tSec], with fade controlled by [alphaMul].
 * Rotation is applied around the shard's current world position.
 */
private fun DrawScope.drawShard(shard: Shard, tSec: Float, alphaMul: Float) {
    val cx = shard.originX + shard.velX * tSec
    val cy = shard.originY + shard.velY * tSec
    val angle = shard.angularVel * tSec

    val cosA = cos(angle)
    val sinA = sin(angle)

    // Rotate each vertex around (cx, cy)
    fun rotate(v: Offset): Offset {
        val rx = v.x * cosA - v.y * sinA
        val ry = v.x * sinA + v.y * cosA
        return Offset(cx + rx, cy + ry)
    }

    val p0 = rotate(shard.v0)
    val p1 = rotate(shard.v1)
    val p2 = rotate(shard.v2)

    val path = Path().apply {
        moveTo(p0.x, p0.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }

    // Fill — semi-transparent glass tint
    drawPath(
        path  = path,
        color = GlassColor.copy(alpha = shard.baseAlpha * alphaMul * 0.35f),
    )
    // Edge highlight — brighter stroke for glass refraction feel
    drawPath(
        path  = path,
        color = Color.White.copy(alpha = shard.baseAlpha * alphaMul * 0.75f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
    )
}

/**
 * Full-screen shard burst overlay.
 *
 * @param isShattered  Drive this true to trigger the effect.
 *                     The composable renders nothing when false.
 */
@Composable
fun ShatterOverlay(isShattered: Boolean) {
    if (!isShattered) return

    // Shards are built once canvas size is known (first frame).
    var shards by remember { mutableStateOf<List<Shard>>(emptyList()) }

    // Progress 0→1 over SHATTER_DURATION_MS; drives position + fade.
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isShattered) {
        val startMs = withFrameMillis { it }

        while (true) {
            val now     = withFrameMillis { it }
            val elapsed = now - startMs
            progress    = (elapsed / SHATTER_DURATION_MS.toFloat()).coerceIn(0f, 1f)
            if (elapsed >= SHATTER_DURATION_MS) break
        }
    }

    // alpha fades from 1 → 0 as progress goes 0 → 1
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val tSec  = progress * (SHATTER_DURATION_MS / 1000f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Build shards on the first real draw when we know the canvas size
        if (shards.isEmpty() && size.width > 0f) {
            shards = buildShards(
                centerX  = size.width  / 2f,
                centerY  = size.height / 2f,
                screenW  = size.width,
            )
        }

        for (shard in shards) {
            drawShard(shard, tSec, alpha)
        }
    }
}
