package com.tanay.warrior.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

// ── StreakFuneral.kt ──────────────────────────────────────────
// Streak Funeral — v1.0.0
//
// Triggered when a streak of 30+ days is broken by a relapse.
// Caller is responsible for tracking deadStreak and visible.
//
// Timeline (3000ms total):
//   0 →  600ms   FADE IN  — dark overlay + content rise in
//   600 → 2400ms HOLD     — gravestone visible, rain falls, text shown
//   2400 → 3000ms FADE OUT — everything dissolves
//
// Layers (bottom → top):
//   1. Dark overlay     — semi-transparent black, full screen
//   2. Rain particles   — 25 droplets, same style as DashboardScreen rain
//   3. Gravestone       — Canvas-drawn arch shape, carved streak number
//   4. Epitaph text     — "DAY X — FALLEN" in dim red beneath stone
//
// Rain implementation:
//   Mirrors DashboardScreen's rain logic exactly — plain array of
//   mutable FuneralDrop objects, mutated in-place each withFrameMillis
//   tick, single mutableIntStateOf canvasVersion to trigger redraw.
//   No shared state with DashboardScreen — fully self-contained.
//
// Renders nothing when visible = false or deadStreak < 30.
// No new libraries.
//
// Usage:
//   StreakFuneralOverlay(deadStreak = state.lastDeadStreak, visible = showFuneral)
// ─────────────────────────────────────────────────────────────

// ── Colors ────────────────────────────────────────────────────

private val OverlayBlack  = Color(0xCC000000)   // 80% black
private val StoneBase     = Color(0xFF2A2A2A)
private val StoneDark     = Color(0xFF1A1A1A)
private val StoneEdge     = Color(0xFF3D3D3D)
private val MossGreen     = Color(0xFF2A3A2A)
private val CarveColor    = Color(0xFF0D0D0D)   // recessed number
private val EpitaphRed    = Color(0xFF8B1A1A)
private val EpitaphDim    = Color(0xFF5A1010)
private val RainBlue      = Color(0xFF2A3A4A)

// ── Rain ──────────────────────────────────────────────────────

private const val FUNERAL_DROP_COUNT = 25

private class FuneralDrop(
    var x:         Float,
    var y:         Float,
    val speedY:    Float,
    val radiusPx:  Float,
    val alpha:     Float,
    val aspect:    Float,   // length/width ratio for elongated stroke
)

private fun spawnFuneralDrop(canvasW: Float, above: Boolean = false): FuneralDrop {
    val w = if (canvasW > 0f) canvasW else 1080f
    return FuneralDrop(
        x        = Random.nextFloat(),
        y        = if (above) -Random.nextFloat() * 0.2f else Random.nextFloat(),
        speedY   = Random.nextFloat() * 0.06f + 0.03f,
        radiusPx = w * (Random.nextFloat() * 0.0015f + 0.0008f),
        alpha    = Random.nextFloat() * 0.08f + 0.03f,
        aspect   = Random.nextFloat() * 3f + 2f,
    )
}

// ── Gravestone canvas draw ────────────────────────────────────

/**
 * Draws the gravestone centered at ([cx], [cy]) with [stoneH] height.
 * Components: body rect + rounded arch top + subtle moss + carved cross.
 * The dead streak number is drawn via nativeCanvas for precise centering.
 */
private fun DrawScope.drawGravestone(
    cx:        Float,
    cy:        Float,
    stoneW:    Float,
    stoneH:    Float,
    deadStreak: Int,
    alpha:     Float,
) {
    val stoneTop  = cy - stoneH / 2f
    val stoneLeft = cx - stoneW / 2f

    // ── Body: rectangular lower portion ──────────────────────
    val bodyH    = stoneH * 0.65f
    val bodyTop  = cy + stoneH / 2f - bodyH

    drawRoundRect(
        color       = StoneBase.copy(alpha = alpha),
        topLeft     = Offset(stoneLeft, bodyTop),
        size        = Size(stoneW, bodyH),
        cornerRadius = CornerRadius(6f, 6f),
    )
    // Edge highlight (left + top of body)
    drawLine(
        color       = StoneEdge.copy(alpha = alpha * 0.6f),
        start       = Offset(stoneLeft, bodyTop),
        end         = Offset(stoneLeft, bodyTop + bodyH),
        strokeWidth = 2f,
    )

    // ── Arch top ──────────────────────────────────────────────
    val archH    = stoneH - bodyH
    val archCx   = cx
    val archCy   = bodyTop   // arch bottom aligns with body top

    val archPath = Path().apply {
        // Start bottom-left of arch
        moveTo(stoneLeft, archCy)
        // Left side up
        lineTo(stoneLeft, archCy - archH * 0.45f)
        // Bezier over the arch crown
        cubicTo(
            stoneLeft,              archCy - archH,
            stoneLeft + stoneW,     archCy - archH,
            stoneLeft + stoneW,     archCy - archH * 0.45f,
        )
        // Right side back down
        lineTo(stoneLeft + stoneW, archCy)
        close()
    }

    drawPath(path = archPath, color = StoneBase.copy(alpha = alpha))
    drawPath(
        path  = archPath,
        color = StoneEdge.copy(alpha = alpha * 0.4f),
        style = Stroke(width = 1.5f),
    )

    // ── Moss strip at base ────────────────────────────────────
    drawRoundRect(
        color        = MossGreen.copy(alpha = alpha * 0.5f),
        topLeft      = Offset(stoneLeft + 4f, bodyTop + bodyH - 8f),
        size         = Size(stoneW - 8f, 8f),
        cornerRadius = CornerRadius(3f, 3f),
    )

    // ── Carved cross in arch ──────────────────────────────────
    val crossCy    = archCy - archH * 0.58f
    val crossArmV  = archH * 0.22f
    val crossArmH  = stoneW  * 0.18f
    val crossStroke = 3f

    // Vertical bar
    drawLine(
        color       = CarveColor.copy(alpha = alpha),
        start       = Offset(archCx, crossCy - crossArmV),
        end         = Offset(archCx, crossCy + crossArmV),
        strokeWidth = crossStroke,
        cap         = StrokeCap.Round,
    )
    // Horizontal bar (upper third)
    drawLine(
        color       = CarveColor.copy(alpha = alpha),
        start       = Offset(archCx - crossArmH, crossCy - crossArmV * 0.3f),
        end         = Offset(archCx + crossArmH, crossCy - crossArmV * 0.3f),
        strokeWidth = crossStroke,
        cap         = StrokeCap.Round,
    )

    // ── Carved streak number on body ──────────────────────────
    val textY     = bodyTop + bodyH * 0.46f
    val paint     = android.graphics.Paint().apply {
        isAntiAlias  = true
        textAlign    = android.graphics.Paint.Align.CENTER
        textSize     = stoneH * 0.22f
        color        = android.graphics.Color.argb(
            (alpha * 200).toInt().coerceIn(0, 255),
            13, 13, 13,
        )
        typeface     = android.graphics.Typeface.DEFAULT_BOLD
    }
    drawContext.canvas.nativeCanvas.drawText(
        "$deadStreak",
        cx,
        textY + paint.textSize * 0.38f,
        paint,
    )

    // Subtle emboss highlight one px above the carved number
    val highlightPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign   = android.graphics.Paint.Align.CENTER
        textSize    = paint.textSize
        color       = android.graphics.Color.argb(
            (alpha * 60).toInt().coerceIn(0, 255),
            80, 80, 80,
        )
        typeface    = android.graphics.Typeface.DEFAULT_BOLD
    }
    drawContext.canvas.nativeCanvas.drawText(
        "$deadStreak",
        cx,
        textY + paint.textSize * 0.38f - 1.5f,
        highlightPaint,
    )
}

// ── Public composable ─────────────────────────────────────────

/**
 * Full-screen streak funeral. Fades in over 600ms, holds, fades out
 * and auto-dismisses after 3000ms total.
 *
 * @param deadStreak  The streak that just died.
 * @param visible     Drive true to show; false renders nothing.
 *                    Caller should set false once onDismiss fires.
 * @param onDismiss   Called after 3000ms — use to reset visible in caller.
 */
@Composable
fun StreakFuneralOverlay(
    deadStreak: Int,
    visible:    Boolean,
    onDismiss:  () -> Unit = {},
) {
    if (!visible || deadStreak < 30) return

    // ── Master alpha: fade in 0→600ms, hold, fade out 2400→3000ms ──
    var masterAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startMs = withFrameMillis { it }
        while (true) {
            val now     = withFrameMillis { it }
            val elapsed = now - startMs

            masterAlpha = when {
                elapsed < 600   -> (elapsed / 600f).coerceIn(0f, 1f)
                elapsed < 2400  -> 1f
                elapsed < 3000  -> 1f - ((elapsed - 2400f) / 600f).coerceIn(0f, 1f)
                else            -> { onDismiss(); break }
            }
        }
    }

    // ── Rain simulation ────────────────────────────────────────
    var canvasW by remember { mutableFloatStateOf(0f) }
    var canvasH by remember { mutableFloatStateOf(0f) }

    val drops = remember {
        Array(FUNERAL_DROP_COUNT) {
            spawnFuneralDrop(canvasW = canvasW, above = true)
        }
    }

    var canvasVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var lastMs = 0L
        while (true) {
            val frameMs = withFrameMillis { it }
            val dtSec   = if (lastMs == 0L) 0f
                          else ((frameMs - lastMs) / 1000f).coerceIn(0f, 0.05f)
            lastMs = frameMs

            val w = canvasW
            val h = canvasH
            if (w <= 0f || h <= 0f || dtSec == 0f) continue

            for (i in drops.indices) {
                drops[i].y += drops[i].speedY * dtSec
                if (drops[i].y > 1.05f) drops[i] = spawnFuneralDrop(canvasW = w)
            }
            canvasVersion++
        }
    }

    // ── Render ─────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // Layer 1: dark overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = OverlayBlack.copy(alpha = masterAlpha * 0.88f))
        }

        // Layer 2: rain + gravestone
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Capture dimensions
            canvasW = size.width
            canvasH = size.height

            @Suppress("UNUSED_EXPRESSION")
            canvasVersion

            val w = size.width
            val h = size.height

            // Rain droplets
            for (drop in drops) {
                val px      = drop.x * w
                val py      = drop.y * h
                val halfLen = (drop.radiusPx * drop.aspect).coerceAtLeast(2f)
                drawLine(
                    color       = RainBlue.copy(alpha = drop.alpha * masterAlpha),
                    start       = Offset(px, py - halfLen),
                    end         = Offset(px, py + halfLen),
                    strokeWidth = drop.radiusPx.coerceAtLeast(1f),
                    cap         = StrokeCap.Round,
                )
            }

            // Gravestone — centered slightly above vertical center
            val stoneW = w * 0.28f
            val stoneH = h * 0.26f
            val stoneCx = w / 2f
            val stoneCy = h * 0.44f

            drawGravestone(
                cx          = stoneCx,
                cy          = stoneCy,
                stoneW      = stoneW,
                stoneH      = stoneH,
                deadStreak  = deadStreak,
                alpha       = masterAlpha,
            )

            // Ground line beneath stone
            val groundY = stoneCy + stoneH / 2f + 4f
            drawLine(
                color       = StoneEdge.copy(alpha = masterAlpha * 0.35f),
                start       = Offset(stoneCx - stoneW * 0.8f, groundY),
                end         = Offset(stoneCx + stoneW * 0.8f, groundY),
                strokeWidth = 1.5f,
            )
        }

        // Layer 3: epitaph text
        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier            = Modifier.offset(
                    y = (120 * (1f - masterAlpha)).dp  // rises in as overlay fades in
                ),
            ) {
                // Spacer pushes text below gravestone
                Spacer(modifier = Modifier.height(180.dp))

                Text(
                    text          = "DAY $deadStreak — FALLEN",
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = EpitaphRed.copy(alpha = masterAlpha * 0.9f),
                    textAlign     = TextAlign.Center,
                    letterSpacing = 3.sp,
                )

                Text(
                    text          = "In memory of the streak\nthat almost made it.",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Medium,
                    color         = EpitaphDim.copy(alpha = masterAlpha * 0.7f),
                    textAlign     = TextAlign.Center,
                    lineHeight    = 16.sp,
                )
            }
        }
    }
}
