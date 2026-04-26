package com.tanay.warrior.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── BlueprintCharts.kt ────────────────────────────────────────
// Blueprint Bar Chart — v1.0.0
//
// Military wireframe aesthetic: dark void background, neon ArenaBlue
// stroked bars with glow, dim grid lines, monospace labels.
// Bars are OUTLINES ONLY — no fills. Pure wireframe.
//
// Public API:
//   BlueprintBarChart(
//       data:     List<Pair<String, Float>>,  // (label, value) pairs
//       modifier: Modifier = Modifier,
//       maxValue: Float?   = null,            // null = auto from data
//   )
//
// Visual layers (bottom → top):
//   1. Chart background + outer border
//   2. Horizontal grid lines (dim blue) + grid value labels
//   3. Wireframe bars — stroked rect + corner tick marks + inner glow line
//   4. Bar top value readout
//   5. Bar labels below x-axis
//
// Animation:
//   On first composition each bar animates from 0 → target height
//   via animateFloatAsState, tween(700ms, EaseOutCubic), staggered
//   by bar index × 60ms so bars rise sequentially left to right.
//
// Grid:
//   4 horizontal lines at 25%, 50%, 75%, 100% of maxValue.
//   Stroked with Color(0xFF0A1A2A) at 80% alpha — barely-there blueprint ink.
//
// Bar anatomy:
//   • Outer rect stroke: ArenaBlue 1.8px
//   • Corner ticks: 5px inset L-marks at each corner (military drafting feel)
//   • Bloom halo: ArenaBlue 6px wide at 12% alpha (glow without fill)
//   • Inner highlight line: left edge of bar, ArenaBlue 60% alpha
//   • Top value text: ArenaBlue, 9sp monospace, above bar
//
// Sizing:
//   Bar width: 68% of column slot, centred
//   Padding: 16dp chart inset, 24dp bottom for labels, 16dp top for values
// ─────────────────────────────────────────────────────────────

// ── Colors ────────────────────────────────────────────────────

private val ArenaBlue      = Color(0xFF00B4FF)
private val ChartBg        = Color(0xFF050D14)
private val ChartBorder    = Color(0xFF0D2030)
private val GridLineColor  = Color(0xFF0A1A2A)
private val GridLabelColor = Color(0xFF1E4060)
private val LabelColor     = Color(0xFF2A6080)
private val ValueColor     = ArenaBlue.copy(alpha = 0.85f)

// ── Layout constants ──────────────────────────────────────────

private const val GRID_LINES      = 4
private const val BAR_WIDTH_FRAC  = 0.62f    // fraction of column slot
private const val ANIM_BASE_MS    = 700
private const val ANIM_STAGGER_MS = 60
private const val CORNER_TICK_PX  = 10f      // corner tick arm length
private const val BAR_STROKE_PX   = 1.8f
private const val HALO_STROKE_PX  = 7f
private const val GRID_STROKE_PX  = 0.8f

// ── Internal drawing ──────────────────────────────────────────

private fun DrawScope.drawGridLines(
    chartLeft:   Float,
    chartRight:  Float,
    chartTop:    Float,
    chartBottom: Float,
    maxValue:    Float,
    measurer:    TextMeasurer,
) {
    val chartH = chartBottom - chartTop
    for (i in 1..GRID_LINES) {
        val fraction = i.toFloat() / GRID_LINES
        val y        = chartBottom - chartH * fraction

        // Grid line
        drawLine(
            color       = GridLineColor.copy(alpha = 0.80f),
            start       = Offset(chartLeft,  y),
            end         = Offset(chartRight, y),
            strokeWidth = GRID_STROKE_PX,
        )

        // Grid value label at left edge
        val gridVal  = maxValue * fraction
        val gridText = if (gridVal >= 100f) gridVal.toInt().toString()
                       else                 "%.1f".format(gridVal)

        val layout = measurer.measure(
            text     = gridText,
            style    = TextStyle(
                color      = GridLabelColor,
                fontSize   = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
            ),
            maxLines   = 1,
            overflow   = TextOverflow.Clip,
        )
        drawText(
            textLayoutResult = layout,
            topLeft          = Offset(
                x = chartLeft - layout.size.width - 6f,
                y = y - layout.size.height / 2f,
            ),
        )
    }

    // Baseline
    drawLine(
        color       = GridLineColor.copy(alpha = 0.90f),
        start       = Offset(chartLeft,  chartBottom),
        end         = Offset(chartRight, chartBottom),
        strokeWidth = GRID_STROKE_PX * 1.5f,
    )
}

private fun DrawScope.drawWireframeBar(
    left:       Float,
    top:        Float,
    right:      Float,
    bottom:     Float,
) {
    val w = right - left
    val h = bottom - top
    if (w <= 0f || h <= 0f) return

    val rect = androidx.compose.ui.geometry.Rect(left, top, right, bottom)

    // Bloom halo — wide soft stroke behind bar
    drawRect(
        color       = ArenaBlue.copy(alpha = 0.10f),
        topLeft     = Offset(rect.left,  rect.top),
        size        = Size(rect.width,   rect.height),
        style       = Stroke(width = HALO_STROKE_PX),
    )

    // Outer wireframe rect
    drawRect(
        color       = ArenaBlue.copy(alpha = 0.90f),
        topLeft     = Offset(rect.left,  rect.top),
        size        = Size(rect.width,   rect.height),
        style       = Stroke(width = BAR_STROKE_PX),
    )

    // Corner tick marks — military drafting L-shapes at all 4 corners
    val t = CORNER_TICK_PX
    // Top-left
    drawLine(ArenaBlue, Offset(rect.left,     rect.top + t), Offset(rect.left,  rect.top), BAR_STROKE_PX)
    drawLine(ArenaBlue, Offset(rect.left,     rect.top),     Offset(rect.left + t, rect.top), BAR_STROKE_PX)
    // Top-right
    drawLine(ArenaBlue, Offset(rect.right,    rect.top + t), Offset(rect.right, rect.top), BAR_STROKE_PX)
    drawLine(ArenaBlue, Offset(rect.right,    rect.top),     Offset(rect.right - t, rect.top), BAR_STROKE_PX)
    // Bottom-left
    drawLine(ArenaBlue, Offset(rect.left,     rect.bottom - t), Offset(rect.left, rect.bottom), BAR_STROKE_PX)
    drawLine(ArenaBlue, Offset(rect.left,     rect.bottom),  Offset(rect.left + t, rect.bottom), BAR_STROKE_PX)
    // Bottom-right
    drawLine(ArenaBlue, Offset(rect.right,    rect.bottom - t), Offset(rect.right, rect.bottom), BAR_STROKE_PX)
    drawLine(ArenaBlue, Offset(rect.right,    rect.bottom),  Offset(rect.right - t, rect.bottom), BAR_STROKE_PX)

    // Inner left-edge highlight — vertical line just inside left wall
    val innerX = rect.left + BAR_STROKE_PX * 2.5f
    if (innerX < rect.right) {
        drawLine(
            color       = ArenaBlue.copy(alpha = 0.35f),
            start       = Offset(innerX, rect.top    + BAR_STROKE_PX * 2f),
            end         = Offset(innerX, rect.bottom - BAR_STROKE_PX * 2f),
            strokeWidth = BAR_STROKE_PX * 0.7f,
        )
    }
}

private fun DrawScope.drawBarLabel(
    label:    String,
    centerX:  Float,
    y:        Float,
    measurer: TextMeasurer,
) {
    val layout = measurer.measure(
        text     = label.uppercase(),
        style    = TextStyle(
            color        = LabelColor,
            fontSize     = 9.sp,
            fontFamily   = FontFamily.Monospace,
            fontWeight   = FontWeight.Medium,
            textAlign    = TextAlign.Center,
            letterSpacing = 1.sp,
        ),
        maxLines   = 1,
        overflow   = TextOverflow.Ellipsis,
    )
    drawText(
        textLayoutResult = layout,
        topLeft          = Offset(centerX - layout.size.width / 2f, y),
    )
}

private fun DrawScope.drawBarValue(
    value:    Float,
    centerX:  Float,
    y:        Float,
    measurer: TextMeasurer,
) {
    val text = if (value >= 100f) value.toInt().toString() else "%.1f".format(value)
    val layout = measurer.measure(
        text     = text,
        style    = TextStyle(
            color      = ValueColor,
            fontSize   = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
    drawText(
        textLayoutResult = layout,
        topLeft          = Offset(centerX - layout.size.width / 2f, y - layout.size.height - 4f),
    )
}

// ── Public composable ─────────────────────────────────────────

/**
 * Military blueprint wireframe bar chart.
 *
 * @param data      List of (label, value) pairs. Values must be >= 0.
 * @param modifier  Layout modifier — chart fills the provided space.
 * @param maxValue  Y-axis ceiling. Null = derived from max value in [data].
 */
@Composable
fun BlueprintBarChart(
    data:      List<Pair<String, Float>>,
    modifier:  Modifier = Modifier,
    maxValue:  Float?   = null,
) {
    if (data.isEmpty()) return

    val resolvedMax = (maxValue ?: data.maxOf { it.second }).coerceAtLeast(1f)

    // Trigger: flip to true on first composition to start entry animation
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }

    // Per-bar animated progress (0 → 1), staggered by index
    val animatedFractions = data.mapIndexed { index, _ ->
        val targetFraction = if (triggered) 1f else 0f
        animateFloatAsState(
            targetValue   = targetFraction,
            animationSpec = tween(
                durationMillis = ANIM_BASE_MS,
                delayMillis    = index * ANIM_STAGGER_MS,
                easing         = EaseOutCubic,
            ),
            label = "barAnim_$index",
        )
    }

    val measurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .background(ChartBg, RoundedCornerShape(14.dp))
            .border(1.dp, ChartBorder, RoundedCornerShape(14.dp))
            .padding(0.dp)
    ) {
        val padLeftPx   = 36.dp.toPx()    // room for grid value labels
        val padRightPx  = 12.dp.toPx()
        val padTopPx    = 20.dp.toPx()    // room for bar value text
        val padBottomPx = 28.dp.toPx()    // room for bar labels

        val chartLeft   = padLeftPx
        val chartRight  = size.width  - padRightPx
        val chartTop    = padTopPx
        val chartBottom = size.height - padBottomPx
        val chartH      = chartBottom - chartTop
        val chartW      = chartRight  - chartLeft

        // Layer 1 — grid lines
        drawGridLines(chartLeft, chartRight, chartTop, chartBottom, resolvedMax, measurer)

        // Layer 2 — bars
        val slotW = chartW / data.size

        data.forEachIndexed { index, (label, rawValue) ->
            val fraction     = animatedFractions[index].value
            val normalised   = (rawValue / resolvedMax).coerceIn(0f, 1f)
            val barH         = chartH * normalised * fraction

            val slotLeft     = chartLeft + slotW * index
            val slotCenterX  = slotLeft + slotW / 2f
            val barHalfW     = slotW * BAR_WIDTH_FRAC / 2f

            val barLeft      = slotCenterX - barHalfW
            val barRight     = slotCenterX + barHalfW
            val barTop       = chartBottom - barH
            val barBottom    = chartBottom

            // Only draw if bar has meaningful height
            if (barH > BAR_STROKE_PX * 2f) {
                drawWireframeBar(barLeft, barTop, barRight, barBottom)
            }

            // Bar value — only when bar is mostly risen
            if (fraction > 0.85f) {
                drawBarValue(rawValue * fraction, slotCenterX, barTop, measurer)
            }

            // Bar label below x-axis
            drawBarLabel(label, slotCenterX, chartBottom + 8.dp.toPx(), measurer)
        }
    }
}
