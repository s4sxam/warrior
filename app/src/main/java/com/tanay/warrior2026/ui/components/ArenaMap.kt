package com.tanay.warrior.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── ArenaMap.kt ───────────────────────────────────────────────
// Leaderboard Arena Map — v1.0.0
//
// 7 glowing regions displayed in a world-map-like grid layout.
// Each region shows a pulsing dot with player count and rank tint.
//
// Regions and layout:
//   Row 0 (3 cells):  NORTH · APEX  · EAST
//   Row 1 (1 cell):   ──── CENTRAL ────    (wide centre cell)
//   Row 2 (3 cells):  WEST · FRONTIER · SOUTH
//
// Rank tiers derived from player count descending:
//   Rank 1       → APEX GOLD  #FFD700
//   Rank 2–3     → STEEL BLUE #8BAFC4
//   Rank 4–5     → ARENA BLUE #00B4FF
//   Rank 6–7     → IRON RED   #B22222
//
// Pulse animation:
//   InfiniteTransition, scale 1f → 1.4f, 1200ms EaseInOutSine, Reverse.
//   Each region has a staggered pulse phase offset (index × 180ms delay)
//   so dots don't all throb in unison.
//
// Background:
//   Dark #050A10 with subtle grid-line overlay drawn on Canvas.
//   Outer card border #0D2030, inner region borders match rank color.
//
// Public API:
//   ArenaMapCard(
//       players:  List<Pair<String, Int>>,  // (playerName, streakDays)
//       modifier: Modifier = Modifier,
//   )
//
//   The composable derives region assignments automatically by
//   sorting players descending and distributing across the 7 regions.
//   Fewer than 7 players are fine — empty regions show "—" count.
// ─────────────────────────────────────────────────────────────

// ── Colors ────────────────────────────────────────────────────

private val MapBg         = Color(0xFF050A10)
private val MapBorder     = Color(0xFF0D2030)
private val GridLineColor = Color(0xFF0A1825)
private val ApexGold      = Color(0xFFFFD700)
private val SteelBlue     = Color(0xFF8BAFC4)
private val ArenaBlue     = Color(0xFF00B4FF)
private val IronRed       = Color(0xFFB22222)
private val TextDim       = Color(0xFF1E4060)
private val CentralBg     = Color(0xFF070E17)

// ── Region data ───────────────────────────────────────────────

private data class RegionConfig(
    val name:        String,
    val playerName:  String,   // top player assigned here
    val count:       Int,      // player count in this region
    val rankColor:   Color,
    val rank:        Int,
)

private val REGION_NAMES = listOf(
    "NORTH", "APEX", "EAST",
    "CENTRAL",
    "WEST", "FRONTIER", "SOUTH",
)

private fun rankColorFor(rank: Int): Color = when (rank) {
    1       -> ApexGold
    2, 3    -> SteelBlue
    4, 5    -> ArenaBlue
    else    -> IronRed
}

/**
 * Distributes [players] (sorted desc by streak) across the 7 regions.
 * Each region gets one "flagship" player and a count derived from
 * proportional bucket sizing. Fewer players than 7 → some regions empty.
 */
private fun buildRegions(players: List<Pair<String, Int>>): List<RegionConfig> {
    val sorted = players.sortedByDescending { it.second }
    return REGION_NAMES.mapIndexed { index, name ->
        val rank        = index + 1
        val flagship    = sorted.getOrNull(index)
        RegionConfig(
            name        = name,
            playerName  = flagship?.first ?: "—",
            count       = if (flagship != null) (sorted.size - index).coerceAtLeast(1) else 0,
            rankColor   = rankColorFor(rank),
            rank        = rank,
        )
    }
}

// ── Pulse dot ─────────────────────────────────────────────────

@Composable
private fun PulseDot(
    color:       Color,
    dotSizeDp:   Dp     = 7.dp,
    phaseDelay:  Int    = 0,
) {
    val inf = rememberInfiniteTransition(label = "pulseDot_$phaseDelay")

    val scale by inf.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.4f,
        animationSpec = infiniteRepeatable(
            animation   = tween(
                durationMillis = 1200,
                delayMillis    = phaseDelay,
                easing         = EaseInOutSine,
            ),
            repeatMode  = RepeatMode.Reverse,
        ),
        label = "pulseScale_$phaseDelay",
    )

    val glowAlpha by inf.animateFloat(
        initialValue  = 0.30f,
        targetValue   = 0.70f,
        animationSpec = infiniteRepeatable(
            animation   = tween(1200, delayMillis = phaseDelay, easing = EaseInOutSine),
            repeatMode  = RepeatMode.Reverse,
        ),
        label = "pulseGlow_$phaseDelay",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(dotSizeDp * 2.2f)
            .drawBehind {
                // Outer glow ring
                drawCircle(
                    color  = color.copy(alpha = glowAlpha * 0.40f),
                    radius = (dotSizeDp.toPx() / 2f) * scale * 2.0f,
                    center = Offset(this.size.width / 2f, this.size.height / 2f),
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(dotSizeDp * scale)
                .drawBehind {
                    val r  = this.size.minDimension / 2f
                    val cx = this.size.width  / 2f
                    val cy = this.size.height / 2f
                    // Soft halo
                    drawCircle(color.copy(alpha = glowAlpha * 0.35f), r * 1.8f, Offset(cx, cy))
                    // Core dot
                    drawCircle(color.copy(alpha = 0.92f), r, Offset(cx, cy))
                }
        )
    }
}

// ── Region cell ───────────────────────────────────────────────

@Composable
private fun RegionCell(
    region:      RegionConfig,
    phaseDelay:  Int,
    modifier:    Modifier = Modifier,
    isCentral:   Boolean  = false,
) {
    val borderAlpha = if (region.count > 0) 0.55f else 0.20f
    val bgAlpha     = if (isCentral) 0.07f else 0.05f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        region.rankColor.copy(alpha = bgAlpha),
                        MapBg,
                    )
                )
            )
            .border(
                width = 1.dp,
                color = region.rankColor.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // Region name
            Text(
                text          = region.name,
                fontSize      = if (isCentral) 9.sp else 8.sp,
                fontWeight    = FontWeight.ExtraBold,
                fontFamily    = FontFamily.Monospace,
                color         = region.rankColor.copy(alpha = if (region.count > 0) 0.90f else 0.30f),
                letterSpacing = 2.sp,
                textAlign     = TextAlign.Center,
            )

            // Pulse dot — only when region has players
            if (region.count > 0) {
                PulseDot(
                    color      = region.rankColor,
                    dotSizeDp  = if (isCentral) 9.dp else 7.dp,
                    phaseDelay = phaseDelay,
                )
            } else {
                Spacer(Modifier.size(if (isCentral) 9.dp else 7.dp))
            }

            // Player name
            Text(
                text          = region.playerName,
                fontSize      = if (isCentral) 10.sp else 8.sp,
                fontWeight    = if (isCentral) FontWeight.Bold else FontWeight.Medium,
                fontFamily    = FontFamily.Monospace,
                color         = region.rankColor.copy(alpha = if (region.count > 0) 0.75f else 0.20f),
                textAlign     = TextAlign.Center,
                maxLines      = 1,
            )

            // Player count badge
            if (region.count > 0) {
                Text(
                    text          = "${region.count} ACTIVE",
                    fontSize      = 7.sp,
                    fontWeight    = FontWeight.Normal,
                    fontFamily    = FontFamily.Monospace,
                    color         = region.rankColor.copy(alpha = 0.45f),
                    letterSpacing = 1.sp,
                    textAlign     = TextAlign.Center,
                )
            }
        }
    }
}

// ── Grid line canvas ──────────────────────────────────────────

@Composable
private fun MapGridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stepX = size.width  / 6f
        val stepY = size.height / 6f
        for (i in 1..5) {
            drawLine(
                color       = GridLineColor,
                start       = Offset(stepX * i, 0f),
                end         = Offset(stepX * i, size.height),
                strokeWidth = 0.6f,
            )
            drawLine(
                color       = GridLineColor,
                start       = Offset(0f, stepY * i),
                end         = Offset(size.width, stepY * i),
                strokeWidth = 0.6f,
            )
        }
        // Outer frame
        drawRect(
            color       = MapBorder,
            style       = Stroke(strokeWidth = 1.2f),
        )
    }
}

// ── Public composable ─────────────────────────────────────────

/**
 * Leaderboard Arena Map — 7 glowing regions with pulsing player indicators.
 *
 * @param players  List of (playerName, streakDays). Sorted internally.
 *                 Supports 0–N players; regions with no player show dim/empty.
 * @param modifier Layout modifier for the outer card.
 *
 * Usage:
 * ```
 * ArenaMapCard(
 *     players = listOf(
 *         "ATLAS"   to 90,
 *         "KIRA"    to 62,
 *         "VANCE"   to 45,
 *         "DREV"    to 30,
 *         "MIRA"    to 18,
 *         "COLE"    to 9,
 *         "ECHO"    to 3,
 *     ),
 *     modifier = Modifier.fillMaxWidth(),
 * )
 * ```
 */
@Composable
fun ArenaMapCard(
    players:  List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
) {
    val regions = remember(players) { buildRegions(players) }

    // regions index layout:
    //   0=NORTH  1=APEX   2=EAST
    //   3=CENTRAL (spans full width)
    //   4=WEST   5=FRONTIER  6=SOUTH

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MapBg)
            .border(1.dp, MapBorder, RoundedCornerShape(18.dp))
    ) {
        // Background grid lines
        MapGridBackground(Modifier.matchParentSize())

        Column(
            modifier            = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {

            // ── Header ─────────────────────────────────────────
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically,
            ) {
                Text(
                    text          = "ARENA MAP",
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    fontFamily    = FontFamily.Monospace,
                    color         = TextDim,
                    letterSpacing = 3.sp,
                )
                Text(
                    text          = "${players.size} WARRIORS",
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Medium,
                    fontFamily    = FontFamily.Monospace,
                    color         = TextDim.copy(alpha = 0.60f),
                    letterSpacing = 1.sp,
                )
            }

            // ── Row 0: NORTH · APEX · EAST ─────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RegionCell(
                    region     = regions[0],   // NORTH
                    phaseDelay = 0,
                    modifier   = Modifier.weight(1f).height(86.dp),
                )
                RegionCell(
                    region     = regions[1],   // APEX
                    phaseDelay = 180,
                    modifier   = Modifier.weight(1f).height(86.dp),
                )
                RegionCell(
                    region     = regions[2],   // EAST
                    phaseDelay = 360,
                    modifier   = Modifier.weight(1f).height(86.dp),
                )
            }

            // ── Row 1: CENTRAL (full width) ─────────────────────
            RegionCell(
                region     = regions[3],   // CENTRAL
                phaseDelay = 540,
                modifier   = Modifier.fillMaxWidth().height(72.dp),
                isCentral  = true,
            )

            // ── Row 2: WEST · FRONTIER · SOUTH ──────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RegionCell(
                    region     = regions[4],   // WEST
                    phaseDelay = 720,
                    modifier   = Modifier.weight(1f).height(86.dp),
                )
                RegionCell(
                    region     = regions[5],   // FRONTIER
                    phaseDelay = 900,
                    modifier   = Modifier.weight(1f).height(86.dp),
                )
                RegionCell(
                    region     = regions[6],   // SOUTH
                    phaseDelay = 1080,
                    modifier   = Modifier.weight(1f).height(86.dp),
                )
            }
        }
    }
}
