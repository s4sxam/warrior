package com.tanay.warrior.ui.components

// ──────────────────────────────────────────────────────────────────────────────
// Chart.kt  —  v4.0.2 update
//
// Changes vs previous version:
//   • ChartSeriesConfig gains an optional `icon` composable slot (mirrors TS
//     `icon?: React.ComponentType` in ChartConfig).
//   • ChartTooltipContent now renders the icon slot when provided (matches TS
//     `{itemConfig?.icon ? <itemConfig.icon /> : (!hideIndicator && <div …/>)}`).
//   • ChartLegendItem / ChartLegendContent use the icon slot the same way.
//   • LocalChartColors CompositionLocal added — children can read per-key colors
//     the same way TS ChartStyle injects --color-{key} CSS variables.
//   • Everything else (ChartContainer, ChartIndicator, LegendAlign, tooltip card
//     layout, value formatting) is unchanged.
// ──────────────────────────────────────────────────────────────────────────────

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── ChartConfig ─────────────────────────────────────────────────────────────
//
// Mirrors TS:
//   type ChartConfig = {
//     [k: string]: { label?, icon?: React.ComponentType, color? | theme? }
//   }

data class ChartSeriesConfig(
    val label: String                          = "",
    val color: Color                           = Color.Gray,
    // Optional icon slot — mirrors TS `icon?: React.ComponentType`
    val icon:  (@Composable () -> Unit)? = null
)

typealias ChartConfig = Map<String, ChartSeriesConfig>

// ─── CompositionLocals ───────────────────────────────────────────────────────
//
// LocalChartConfig  — mirrors ChartContext / useChart()
// LocalChartColors  — mirrors ChartStyle CSS variable injection
//                     usage: LocalChartColors.current["victories"] → Color

val LocalChartConfig = compositionLocalOf<ChartConfig> { emptyMap() }

/** Flattened color map derived from the active ChartConfig. Read like CSS vars. */
val LocalChartColors = compositionLocalOf<Map<String, Color>> { emptyMap() }

@Composable
fun useChart(): ChartConfig = LocalChartConfig.current

// ─── ChartContainer ───────────────────────────────────────────────────────────

@Composable
fun ChartContainer(
    config:   ChartConfig,
    modifier: Modifier = Modifier,
    content:  @Composable BoxScope.() -> Unit
) {
    val colors = remember(config) { config.mapValues { it.value.color } }
    CompositionLocalProvider(
        LocalChartConfig  provides config,
        LocalChartColors  provides colors
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            content = content
        )
    }
}

// ─── Indicator styles ─────────────────────────────────────────────────────────

enum class ChartIndicator { DOT, LINE, DASHED }

// ─── Indicator composable (shared by tooltip and legend) ─────────────────────

@Composable
private fun IndicatorBox(
    color:     Color,
    indicator: ChartIndicator
) {
    when (indicator) {
        ChartIndicator.DOT    -> Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        ChartIndicator.LINE   -> Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(color)
        )
        ChartIndicator.DASHED -> Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .border(1.5.dp, color, RoundedCornerShape(1.dp))
        )
    }
}

// ─── Single legend item ───────────────────────────────────────────────────────

@Composable
fun ChartLegendItem(
    color:     Color,
    label:     String,
    indicator: ChartIndicator                  = ChartIndicator.DOT,
    hideIcon:  Boolean                         = false,
    icon:      (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!hideIcon) {
            if (icon != null) {
                // Mirrors TS: `{itemConfig?.icon && !hideIcon ? <itemConfig.icon /> : <div…/>}`
                icon()
            } else {
                IndicatorBox(color = color, indicator = indicator)
            }
        }
        Text(
            text       = label,
            fontSize   = 11.sp,
            color      = Color(0xFFAAAAAA),
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── ChartLegendContent ───────────────────────────────────────────────────────

@Composable
fun ChartLegendContent(
    config:        ChartConfig,
    hideIcon:      Boolean       = false,
    verticalAlign: LegendAlign   = LegendAlign.BOTTOM,
    modifier:      Modifier      = Modifier
) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .padding(
                top    = if (verticalAlign == LegendAlign.TOP) 0.dp else 10.dp,
                bottom = if (verticalAlign == LegendAlign.TOP) 10.dp else 0.dp
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        config.entries.forEachIndexed { i, (_, series) ->
            if (i > 0) Spacer(Modifier.width(16.dp))
            ChartLegendItem(
                color    = series.color,
                label    = series.label,
                hideIcon = hideIcon,
                icon     = series.icon
            )
        }
    }
}

enum class LegendAlign { TOP, BOTTOM }

// ─── Tooltip payload item ─────────────────────────────────────────────────────

data class TooltipPayloadItem(
    val key:   String,
    val name:  String,
    val value: Number,
    val color: Color
)

// ─── ChartTooltipContent ─────────────────────────────────────────────────────
//
// Mirrors TS ChartTooltipContent — shows a styled card with per-series rows.
// Place inside a Popup or Box when a data point is selected.
//
// Icon precedence (mirrors TS exactly):
//   if (itemConfig?.icon) → render icon
//   else if (!hideIndicator) → render indicator shape

@Composable
fun ChartTooltipContent(
    label:         String                   = "",
    payload:       List<TooltipPayloadItem> = emptyList(),
    config:        ChartConfig              = useChart(),
    indicator:     ChartIndicator           = ChartIndicator.DOT,
    hideLabel:     Boolean                  = false,
    hideIndicator: Boolean                  = false,
    modifier:      Modifier                 = Modifier
) {
    if (payload.isEmpty()) return

    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF252525), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Label row — mirrors TS tooltipLabel useMemo
        if (!hideLabel && label.isNotBlank()) {
            Text(
                text       = config[label]?.label ?: label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
        }

        // Per-series rows
        payload.forEach { item ->
            val seriesConfig = config[item.key]
            val displayColor = item.color

            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Indicator / icon — mirrors TS icon-first logic
                val iconSlot = seriesConfig?.icon
                when {
                    iconSlot != null  -> iconSlot()
                    !hideIndicator    -> IndicatorBox(displayColor, indicator)
                }

                // Series label
                Text(
                    text     = seriesConfig?.label ?: item.name,
                    fontSize = 11.sp,
                    color    = Color(0xFFAAAAAA),
                    modifier = Modifier.weight(1f)
                )

                // Value — mirrors TS `item.value.toLocaleString()`
                Text(
                    text       = item.value.toDouble().let {
                        if (it == it.toLong().toDouble()) "%,d".format(it.toLong())
                        else "%,.1f".format(it)
                    },
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
            }
        }
    }
}
