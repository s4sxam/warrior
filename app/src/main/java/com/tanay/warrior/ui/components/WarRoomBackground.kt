package com.tanay.warrior.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*
import kotlin.random.Random

// ── WarRoomBackground.kt ──────────────────────────────────────
// War Room Background — v1.0.0
//
// A particle-system background layer that evolves with the warrior's streak.
//
// Public API:
//   WarRoomBackground(streak: Int, modifier: Modifier = Modifier)
//
// Behaviour by streak milestone:
//   Day 0–9   → DIGITAL RAIN / FOG
//              Green matrix-style falling glyphs + dim fog vignette.
//              Colour: dark green #0D2B0D. Heavy column rain, low opacity.
//
//   Day 10–29 → TRANSITION (rain fades, first embers appear)
//              Rain opacity drops, sparse ember count rises.
//              Colour: warm amber starts bleeding in.
//
//   Day 30+   → GLOWING EMBERS
//              Particles drift upward, wobble on sine wave, fade at top.
//              Colour: WarriorRed → Gold (#FF3131 → #00B4FF) per streak tier.
//              Day 90+ embers become brighter + larger + more numerous.
//
// Architecture:
//   • withInfiniteAnimationFrameMillis loop for frame timing — zero new libs.
//   • Two particle pools: RainDrop (falling), Ember (rising).
//   • Pool is initialised once; particles respawn when they leave the canvas.
//   • All state held in SnapshotState arrays — no LaunchedEffect restart on
//     recomposition.
//   • Uses a single Canvas composable for all particles.
//
// Sizing:
//   fillMaxSize() — place behind all content with no pointer interception.
//   Wrap with Box { WarRoomBackground(); content() }
//
// Performance:
//   Rain: 40 columns × 1 visible drop each   = ~40 drawText/drawLine calls
//   Embers: 60 particles (90+ streak: 90)     = 60–90 drawCircle calls
//   Targets 60fps; particles update in-place with no GC pressure.
// ─────────────────────────────────────────────────────────────

// ── Constants ─────────────────────────────────────────────────

private const val RAIN_COUNT          = 40
private const val EMBER_COUNT_BASE    = 55
private const val EMBER_COUNT_LEGEND  = 90   // Day 90+

private val MatrixGreen   = Color(0xFF00FF41)
private val FogColor      = Color(0xFF000D00)
private val EmberRedLow   = Color(0xFFFF3131)   // WarriorRed
private val EmberGold     = Color(0xFF00B4FF)   // ArenaBlue/Gold
private val EmberAmber    = Color(0xFFFF8C00)
private val EmberWhite    = Color(0xFFFFFFFF)

// ── Data classes ──────────────────────────────────────────────

private data class RainDrop(
    var x:          Float = 0f,
    var y:          Float = 0f,
    var speed:      Float = 0f,
    var length:     Float = 0f,
    var alpha:      Float = 0f,
    var glyphIndex: Int   = 0,
)

private data class Ember(
    var x:        Float = 0f,
    var y:        Float = 0f,
    var vy:       Float = 0f,   // upward speed (negative y-axis)
    var vx:       Float = 0f,   // lateral drift
    var life:     Float = 0f,   // 0..1, 1=fresh 0=dead
    var lifeRate: Float = 0f,   // how fast life depletes
    var radius:   Float = 0f,
    var phase:    Float = 0f,   // sine wobble phase offset
    var hue:      Float = 0f,   // 0=red 1=gold/blue
)

// ── Rain glyph chars (Matrix-style katakana + digits) ─────────

private val RAIN_GLYPHS = charArrayOf(
    'ア','イ','ウ','エ','オ','カ','キ','ク','ケ','コ',
    'サ','シ','ス','セ','ソ','タ','チ','ツ','テ','ト',
    '0','1','2','3','4','5','6','7','8','9',
    'Z','X','|',':','=','+','-','*',
)

// ── Particle pool init ────────────────────────────────────────

private fun initRainDrops(count: Int, canvasW: Float, canvasH: Float): Array<RainDrop> {
    val rng = Random(seed = 42L)
    return Array(count) {
        RainDrop(
            x          = rng.nextFloat() * canvasW,
            y          = rng.nextFloat() * canvasH,
            speed      = 80f + rng.nextFloat() * 200f,
            length     = 40f + rng.nextFloat() * 120f,
            alpha      = 0.15f + rng.nextFloat() * 0.35f,
            glyphIndex = rng.nextInt(RAIN_GLYPHS.size),
        )
    }
}

private fun initEmbers(count: Int, canvasW: Float, canvasH: Float): Array<Ember> {
    val rng = Random(seed = 99L)
    return Array(count) {
        Ember(
            x        = rng.nextFloat() * canvasW,
            y        = canvasH * (0.2f + rng.nextFloat() * 0.8f),
            vy       = 25f + rng.nextFloat() * 70f,
            vx       = (rng.nextFloat() - 0.5f) * 18f,
            life     = rng.nextFloat(),
            lifeRate = 0.004f + rng.nextFloat() * 0.009f,
            radius   = 1.5f + rng.nextFloat() * 3.5f,
            phase    = rng.nextFloat() * 2f * PI.toFloat(),
            hue      = rng.nextFloat(),
        )
    }
}

// ── Draw helpers ──────────────────────────────────────────────

private fun DrawScope.drawRainDrop(drop: RainDrop, alpha: Float) {
    if (alpha <= 0f) return
    val a = (drop.alpha * alpha).coerceIn(0f, 1f)
    // Trailing streak
    drawLine(
        color       = MatrixGreen.copy(alpha = a * 0.5f),
        start       = Offset(drop.x, drop.y - drop.length),
        end         = Offset(drop.x, drop.y),
        strokeWidth = 1.5f,
        cap         = StrokeCap.Round,
    )
    // Bright head glyph (approximated as a bright dot + short mark)
    drawLine(
        color       = MatrixGreen.copy(alpha = a),
        start       = Offset(drop.x, drop.y - 6f),
        end         = Offset(drop.x, drop.y),
        strokeWidth = 2f,
        cap         = StrokeCap.Round,
    )
}

private fun emberColor(hue: Float, life: Float, streak: Int): Color {
    // Interpolate: red → amber → gold/blue depending on streak tier
    return when {
        streak >= 90 -> {
            // Near-white core embers with gold tinge
            val t = (life * hue).coerceIn(0f, 1f)
            Color(
                red   = (0.9f + 0.1f * t).coerceAtMost(1f),
                green = (0.5f + 0.4f * t).coerceAtMost(1f),
                blue  = (0.1f + 0.8f * t).coerceAtMost(1f),
                alpha = life,
            )
        }
        streak >= 30 -> {
            // Rusty orange → glowing amber
            Color(
                red   = 1.0f,
                green = (0.2f + 0.4f * hue).coerceAtMost(1f),
                blue  = 0.05f + 0.15f * hue,
                alpha = life,
            )
        }
        else -> {
            // Early days: dim red embers
            EmberRedLow.copy(alpha = life * 0.5f)
        }
    }
}

private fun DrawScope.drawEmber(ember: Ember, alpha: Float, streak: Int, time: Float) {
    if (alpha <= 0f || ember.life <= 0f) return
    val wobbleX = ember.x + sin(time * 1.2f + ember.phase) * 12f
    val col = emberColor(ember.hue, ember.life * alpha, streak)
    val r = ember.radius * (0.7f + 0.3f * ember.life)

    // Outer soft glow
    if (streak >= 30) {
        drawCircle(
            color  = col.copy(alpha = col.alpha * 0.25f),
            radius = r * 3.0f,
            center = Offset(wobbleX, ember.y),
        )
    }
    // Core dot
    drawCircle(
        color  = col,
        radius = r,
        center = Offset(wobbleX, ember.y),
    )
}

// ── Transition weight helpers ─────────────────────────────────

/** 0..1 weight for rain layer. Full at day 0, gone at day 20. */
private fun rainWeight(streak: Int): Float =
    (1f - streak / 20f).coerceIn(0f, 1f)

/** 0..1 weight for ember layer. Starts day 10, full at day 30. */
private fun emberWeight(streak: Int): Float =
    ((streak - 10f) / 20f).coerceIn(0f, 1f)

// ── Public Composable ─────────────────────────────────────────

/**
 * Full-screen particle background that evolves with the warrior's [streak].
 *
 * Place behind all screen content:
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     WarRoomBackground(streak = state.streak)
 *     // … screen content on top …
 * }
 * ```
 *
 * @param streak Current warrior streak day count.
 * @param modifier Optional layout modifier. Defaults to fillMaxSize.
 */
@Composable
fun WarRoomBackground(
    streak:   Int,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    // ── Frame time ────────────────────────────────────────────
    var frameMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var prevMs = 0L
        while (true) {
            withInfiniteAnimationFrameMillis { ms ->
                frameMs = ms
                prevMs  = ms
            }
        }
    }

    // ── Particle state — initialised lazily once canvas size known ──
    var canvasW by remember { mutableFloatStateOf(0f) }
    var canvasH by remember { mutableFloatStateOf(0f) }

    val rainDrops = remember { mutableStateOf<Array<RainDrop>?>(null) }
    val embers    = remember { mutableStateOf<Array<Ember>?>(null) }
    var lastFrame by remember { mutableLongStateOf(0L) }

    Canvas(modifier = modifier) {

        // ── Init pools on first draw or canvas resize ─────────
        val w = size.width
        val h = size.height
        if (w != canvasW || h != canvasH || rainDrops.value == null) {
            canvasW = w
            canvasH = h
            val emberCount = if (streak >= 90) EMBER_COUNT_LEGEND else EMBER_COUNT_BASE
            rainDrops.value = initRainDrops(RAIN_COUNT, w, h)
            embers.value    = initEmbers(emberCount, w, h)
            lastFrame       = frameMs
        }

        // ── Delta time ────────────────────────────────────────
        val nowMs  = frameMs
        val dt     = ((nowMs - lastFrame).coerceIn(0L, 50L)) / 1000f   // seconds, max 50ms
        lastFrame  = nowMs
        val timeSec = nowMs / 1000f

        val rw = rainWeight(streak)
        val ew = emberWeight(streak)

        // ── Update + draw rain ────────────────────────────────
        if (rw > 0f) {
            rainDrops.value?.forEach { drop ->
                drop.y += drop.speed * dt
                if (drop.y - drop.length > h) {
                    drop.y          = -drop.length * 0.5f
                    drop.x          = Random.nextFloat() * w
                    drop.glyphIndex = Random.nextInt(RAIN_GLYPHS.size)
                }
                drawRainDrop(drop, rw)
            }
        }

        // ── Update + draw embers ──────────────────────────────
        if (ew > 0f) {
            embers.value?.forEach { ember ->
                ember.y    -= ember.vy * dt
                ember.life -= ember.lifeRate

                if (ember.life <= 0f || ember.y < -20f) {
                    // Respawn at bottom
                    ember.x        = Random.nextFloat() * w
                    ember.y        = h + 10f
                    ember.life     = 0.6f + Random.nextFloat() * 0.4f
                    ember.lifeRate = 0.004f + Random.nextFloat() * 0.009f
                    ember.radius   = if (streak >= 90) 2f + Random.nextFloat() * 5f
                                     else 1.5f + Random.nextFloat() * 3.5f
                    ember.hue      = Random.nextFloat()
                    ember.phase    = Random.nextFloat() * 2f * PI.toFloat()
                    ember.vy       = 25f + Random.nextFloat() * 70f
                }
                drawEmber(ember, ew, streak, timeSec)
            }
        }

        // ── Fog vignette (Day 0–20) ───────────────────────────
        if (rw > 0f) {
            // bottom fog strip
            drawRect(
                color   = FogColor.copy(alpha = rw * 0.55f),
                topLeft = Offset(0f, h * 0.75f),
                size    = androidx.compose.ui.geometry.Size(w, h * 0.25f),
            )
        }
    }
}
