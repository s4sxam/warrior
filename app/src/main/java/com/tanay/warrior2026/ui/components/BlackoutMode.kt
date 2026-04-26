package com.tanay.warrior.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// ── BlackoutMode.kt ───────────────────────────────────────────
// Blackout Overlay — v1.0.0
//
// Concept:
//   Day 0 feels locked and degraded — a heavy black veil smothers
//   the UI. Every streak day earned peels back the darkness linearly
//   until Day 30, where the overlay vanishes entirely and the app
//   is fully revealed.
//
// Public API:
//   BlackoutOverlay(streak: Int, modifier: Modifier = Modifier)
//
// Behaviour:
//   streak == 0   → overlay alpha 0.55f  (maximum darkness)
//   streak 1–29   → alpha lerps from 0.55f → 0f  (linear per day)
//   streak >= 30  → composable returns immediately, draws nothing
//
// Animation:
//   animateFloatAsState with tween(600ms) so any streak increment
//   smoothly lifts the veil rather than snapping.
//
// Placement:
//   Drop as the LAST child in your root Box so it sits above all
//   content without blocking touch events (pointerInput not consumed).
//
//   Example:
//     Box(Modifier.fillMaxSize()) {
//         DashboardContent(...)
//         BlackoutOverlay(streak = streakAnim)
//     }
// ─────────────────────────────────────────────────────────────

private const val MAX_ALPHA   = 0.55f   // darkness at Day 0
private const val CLEAR_DAY   = 30      // streak at which overlay is fully gone
private const val ANIM_MS     = 600     // ms for each alpha transition

/**
 * Renders a full-screen black overlay whose opacity is driven by [streak].
 *
 * Place this as the topmost child in your root [Box] so it overlays all
 * content. It draws nothing and exits early once [streak] reaches 30,
 * adding zero cost to the composition past that point.
 *
 * @param streak  Current streak day count (use streakAnim for smooth feel).
 * @param modifier Optional modifier — defaults to [Modifier.fillMaxSize].
 */
@Composable
fun BlackoutOverlay(
    streak:   Int,
    modifier: Modifier = Modifier,
) {
    // Nothing to render once the warrior has earned the light
    if (streak >= CLEAR_DAY) return

    // Target alpha: linear decay from MAX_ALPHA at day 0 to 0f at day 30
    val targetAlpha = MAX_ALPHA * (1f - streak.coerceIn(0, CLEAR_DAY).toFloat() / CLEAR_DAY)

    val animatedAlpha by animateFloatAsState(
        targetValue   = targetAlpha,
        animationSpec = tween(durationMillis = ANIM_MS),
        label         = "blackoutAlpha",
    )

    // Skip draw if alpha has animated all the way to zero
    if (animatedAlpha <= 0f) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = animatedAlpha))
    )
}
