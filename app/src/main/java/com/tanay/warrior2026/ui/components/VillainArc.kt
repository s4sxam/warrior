package com.tanay.warrior.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// ── VillainArc.kt ─────────────────────────────────────────────
// Villain Arc Mode — v1.0.0
//
// Activates when relapseCount >= 3.
// Layers three effects on top of whatever is behind it:
//
//   1. VIGNETTE
//      Radial gradient overlay — transparent center, blood-red edges.
//      Alpha 0.15f — dark but not blocking UI interaction reading.
//
//   2. STATIC NOISE
//      Canvas-drawn random pixel scatter. Each frame a new seed
//      produces ~200 tiny rects scattered across the screen.
//      Driven by withFrameMillis — same frame-clock pattern as
//      the existing particle system. Low alpha (0.02–0.06) so it
//      reads as corrupt CRT static without overwhelming the UI.
//      Noise density scales with relapseCount (capped at 5) so
//      it gets progressively worse with each additional relapse.
//
//   3. TAUNTING QUOTE
//      Four quotes cycle every 4 seconds with a crossfade via
//      animateFloatAsState. Centered in the bottom third of the
//      screen to avoid covering key UI elements.
//      Quote index increments in a LaunchedEffect with a 4 000ms delay.
//
// Renders nothing when relapseCount < 3.
// No new libraries — pure Compose + coroutines.
//
// Usage:
//   VillainArcOverlay(relapseCount = relapseCount)
//   Place as the FIRST overlay in root Box so it sits beneath the
//   action overlays (Glitch, Shatter, Slash) but above the content.
// ─────────────────────────────────────────────────────────────

// ── Constants ─────────────────────────────────────────────────

private val TAUNT_QUOTES = listOf(
    "You broke again.\nPathetic.",
    "The weak always\nfind excuses.",
    "Your old self is\nlaughing at you.",
    "Pain is your teacher.\nYou keep failing class.",
)

private val BloodRed      = Color(0xFF8B0000)
private val TauntColor    = Color(0xFFCC2222)
private val TauntSub      = Color(0xFF661111)

// Noise rect side in px — small enough to look like pixels
private const val NOISE_RECT_PX = 2f

// ── Noise drawer ──────────────────────────────────────────────

/**
 * Draws random pixel scatter on [this] DrawScope.
 * [seed] changes every frame to produce flicker.
 * [density] controls how many rects are drawn (100–350 range).
 */
private fun DrawScope.drawStaticNoise(seed: Int, density: Int) {
    val rng    = Random(seed)
    val w      = size.width
    val h      = size.height
    val count  = density.coerceIn(100, 350)

    repeat(count) {
        val x     = rng.nextFloat() * w
        val y     = rng.nextFloat() * h
        val alpha = rng.nextFloat() * 0.04f + 0.02f   // 0.02–0.06
        val grey  = rng.nextFloat()                    // white ↔ dark noise
        drawRect(
            color   = Color(grey, grey, grey, alpha),
            topLeft = Offset(x, y),
            size    = Size(NOISE_RECT_PX, NOISE_RECT_PX),
        )
    }
}

// ── Public composable ─────────────────────────────────────────

/**
 * Villain Arc overlay. Renders nothing when [relapseCount] < 3.
 *
 * @param relapseCount  Total consecutive relapses. Drive from WarriorState.
 */
@Composable
fun VillainArcOverlay(relapseCount: Int) {
    if (relapseCount < 3) return

    // Noise density scales with severity — each extra relapse beyond 3
    // adds 50 more pixels, capped at relapseCount 5 (density 300).
    val noiseDensity = 150 + ((relapseCount - 3).coerceIn(0, 2) * 50)

    // ── Noise frame seed — increments every frame ──────────────
    var noiseSeed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { noiseSeed = (noiseSeed + 1) and Int.MAX_VALUE }
        }
    }

    // ── Quote cycling ──────────────────────────────────────────
    var quoteIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4_000L)
            quoteIndex = (quoteIndex + 1) % TAUNT_QUOTES.size
        }
    }

    // Crossfade: reset alpha to 0 when index changes, animate back to 1
    val quoteAlpha by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(durationMillis = 600, easing = EaseInOutSine),
        label         = "villainQuoteFade",
    )
    // Force recompose on index change so animateFloatAsState re-triggers from 0
    // We achieve this by keying a wrapper on quoteIndex.
    val currentQuote = TAUNT_QUOTES[quoteIndex]

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Layer 1: Vignette ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            BloodRed.copy(alpha = 0.08f),
                            BloodRed.copy(alpha = 0.15f),
                        ),
                        // Center slightly above mid so vignette is heavier on top
                        center = Offset.Unspecified,
                        radius = Float.MAX_VALUE,
                    )
                )
        )

        // ── Layer 2: Static noise ──────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawStaticNoise(seed = noiseSeed, density = noiseDensity)
        }

        // ── Layer 3: Taunting quote — bottom third ─────────────
        key(quoteIndex) {
            val alpha by animateFloatAsState(
                targetValue   = 1f,
                animationSpec = tween(durationMillis = 700, easing = EaseOutCubic),
                label         = "quoteAlpha_$quoteIndex",
            )

            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier            = Modifier.padding(horizontal = 32.dp),
                ) {
                    // Decorative rule above quote
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(1.dp)
                            .background(TauntSub.copy(alpha = alpha * 0.6f))
                    )

                    Text(
                        text          = currentQuote,
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.Black,
                        color         = TauntColor.copy(alpha = alpha * 0.85f),
                        textAlign     = TextAlign.Center,
                        lineHeight    = 20.sp,
                        letterSpacing = 1.sp,
                    )

                    Text(
                        text          = "— YOUR VILLAIN ARC",
                        fontSize      = 8.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = TauntSub.copy(alpha = alpha * 0.55f),
                        textAlign     = TextAlign.Center,
                        letterSpacing = 3.sp,
                    )

                    // Decorative rule below
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(1.dp)
                            .background(TauntSub.copy(alpha = alpha * 0.6f))
                    )
                }
            }
        }
    }
}
