package com.tanay.warrior.ui.components

// ──────────────────────────────────────────────────────────────────────────────
// Chart.kt  —  Kotlin/Compose equivalent of the TypeScript chart system
//              (ChartContainer, ChartTooltip, ChartLegend, ChartStyle)
//
// Mirrors the TS/Recharts API shape as closely as possible in Compose:
//   • ChartConfig  — maps series keys → label, icon, color
//   • ChartContainer — provides config via CompositionLocal, wraps chart content
//   • ChartTooltip / ChartTooltipContent — floating tooltip on data-point tap
//   • ChartLegend / ChartLegendContent — labelled color dots
//   • Indicator styles: dot | line | dashed  (matches TS `indicator` prop)
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
// Mirrors:  export type ChartConfig = { [k in string]: { label?, icon?, color? | theme? } }

data class ChartSeriesConfig(
    val label: String = "",
    val color: Color  = Color.Gray
)

typealias ChartConfig = Map<String, ChartSeriesConfig>

// ─── CompositionLocal — equivalent of ChartContext ────────────────────────────

val LocalChartConfig = compositionLocalOf<ChartConfig> { emptyMap() }

// Helper (mirrors useChart())
@Composable
fun useChart(): ChartConfig = LocalChartConfig.current

// ─── ChartContainer ───────────────────────────────────────────────────────────
//
// Mirrors:
//   const ChartContainer = forwardRef(({ id, className, children, config, ...props }, ref) => (
//     <ChartContext.Provider value={{ config }}>
//       <div data-chart={chartId} ...>
//         <ChartStyle id={chartId} config={config} />
//         <ResponsiveContainer>{children}</ResponsiveContainer>
//       </div>
//     </ChartContext.Provider>
//   ))

@Composable
fun ChartContainer(
    config:   ChartConfig,
    modifier: Modifier = Modifier,
    content:  @Composable BoxScope.() -> Unit
) {
    CompositionLocalProvider(LocalChartConfig provides config) {
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

// ─── Single legend item ───────────────────────────────────────────────────────

@Composable
fun ChartLegendItem(
    color:     Color,
    label:     String,
    indicator: ChartIndicator = ChartIndicator.DOT,
    hideIcon:  Boolean        = false
) {
    Row(
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!hideIcon) {
            when (indicator) {
                ChartIndicator.DOT    -> Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                ChartIndicator.LINE   -> Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(2.dp)
                        .background(color)
                )
                ChartIndicator.DASHED -> Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(2.dp)
                                .background(color)
                        )
                    }
                }
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
//
// Mirrors ChartLegendContent — auto-renders all entries from ChartConfig

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
                hideIcon = hideIcon
            )
        }
    }
}

enum class LegendAlign { TOP, BOTTOM }

// ─── Tooltip payload item — mirrors Recharts payload entry ───────────────────

data class TooltipPayloadItem(
    val key:   String,
    val name:  String,
    val value: Number,
    val color: Color
)

// ─── ChartTooltipContent ─────────────────────────────────────────────────────
//
// Mirrors ChartTooltipContent forwardRef — shows a styled card with per-series rows.
// In Compose, call this inside a Popup/Box when your chart state says a point is selected.

@Composable
fun ChartTooltipContent(
    label:         String                  = "",
    payload:       List<TooltipPayloadItem> = emptyList(),
    config:        ChartConfig             = useChart(),
    indicator:     ChartIndicator          = ChartIndicator.DOT,
    hideLabel:     Boolean                 = false,
    hideIndicator: Boolean                 = false,
    modifier:      Modifier                = Modifier
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
        // Label row
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
                // Indicator
                if (!hideIndicator) {
                    when (indicator) {
                        ChartIndicator.DOT    -> Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(displayColor)
                        )
                        ChartIndicator.LINE   -> Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .background(displayColor)
                        )
                        ChartIndicator.DASHED -> Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = displayColor,
                                    shape = RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }

                // Name
                Text(
                    text      = seriesConfig?.label ?: item.name,
                    fontSize  = 11.sp,
                    color     = Color(0xFFAAAAAA),
                    modifier  = Modifier.weight(1f)
                )

                // Value
                Text(
                    text       = item.value.toDouble().let {
                        if (it == it.toLong().toDouble()) it.toLong().toString()
                        else "%.1f".format(it)
                    },
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
            }
        }
    }
}
