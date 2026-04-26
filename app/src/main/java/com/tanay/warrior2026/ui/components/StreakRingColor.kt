package com.tanay.warrior.ui.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// ── StreakRingColor.kt ────────────────────────────────────────
// Streak Ring Color Evolution — v1.0.0
//
// Animates the streak ring color through three milestone tiers with
// smooth interpolation. No new libraries — pure Compose animation.
//
// Tiers:
//   Day 0–29  → Rusty Iron    Color(0xFF5C4A32)  warm dark brown
//   Day 30–89 → Polished Steel Color(0xFF4A90C4) steel blue
//   Day 90+   → Radiant Gold  Color(0xFF00B4FF)  ArenaBlue / Gold
//
// Between tiers the color is linearly interpolated so there are no
// harsh jumps. The animated target is reached over 1200ms EaseOutCubic.
//
// Public API:
//
//   val ringColor by rememberStreakRingColor(streak = state.streak)
//
//   // Then pass to drawArc:
//   drawArc(color = ringColor, ...)
//
// Or use the convenience function:
//
//   val ringColor = streakRingColorTarget(streak)   // instant, no animation
//
// Architecture:
//   rememberStreakRingColor returns an animatable State<Color> via
//   animateColorAsState. It is a @Composable function so it must be called
//   from a composable context (not inside a Canvas draw block). Pass the
//   resulting Color down to the Canvas drawArc call.
//
// Integration — replace the hardcoded ringColor in DashboardScreen.kt:
//
//   // OLD (in StreakHero):
//   val ringColor = when {
//       streak == 0        -> Color(0xFF2A2A2A)
//       streak >= best     -> Gold
//       streak >= best / 2 -> VictoryGreen
//       else               -> WarriorRed
//   }
//
//   // NEW:
//   val ringColor by rememberStreakRingColor(streak = streak)
//
// ─────────────────────────────────────────────────────────────

// ── Milestone colors ──────────────────────────────────────────

/** Day 0 — no streak, cold dead iron. */
val RingColorDead      = Color(0xFF252525)

/** Day 1–29 — rusty iron. Effort beginning, not yet polished. */
val RingColorIron      = Color(0xFF7A5C3A)

/** Day 30–89 — polished steel blue. Discipline showing. */
val RingColorSteel     = Color(0xFF3A7FC1)

/** Day 90+ — radiant ArenaBlue / Gold. Legend tier. */
val RingColorGold      = Color(0xFF00B4FF)

// ── Intermediate accent for glow decoration ───────────────────

/** Warm amber used as halo tint on steel tier. */
val RingGlowAmber  = Color(0xFFFF8C00)

/** Blue-white core glow on gold tier. */
val RingGlowWhite  = Color(0xFFCCEEFF)

// ── Target color (instant, no animation) ─────────────────────

/**
 * Returns the target ring color for [streak] with smooth interpolation
 * between milestone tiers. No animation — useful for static previews
 * or when you need the raw value for a non-composable context.
 *
 * @param streak Current streak day count.
 * @return Interpolated [Color].
 */
fun streakRingColorTarget(streak: Int): Color {
    return when {
        streak <= 0  -> RingColorDead
        streak < 1   -> RingColorDead    // guard
        streak < 30  -> {
            // Iron: ramps from dim dead → full rusty iron over Day 1–29
            val t = (streak / 29f).coerceIn(0f, 1f)
            lerp(RingColorDead, RingColorIron, t)
        }
        streak < 90  -> {
            // Steel: ramps from iron → steel blue over Day 30–89
            val t = ((streak - 30f) / 60f).coerceIn(0f, 1f)
            lerp(RingColorIron, RingColorSteel, t)
        }
        else         -> {
            // Gold: ramps from steel → radiant gold over Day 90–180
            val t = ((streak - 90f) / 90f).coerceIn(0f, 1f)
            lerp(RingColorSteel, RingColorGold, t)
        }
    }
}

/**
 * Returns the glow/shadow color that should be used around the ring
 * to complement the ring color tier.
 *
 * @param streak Current streak day count.
 * @return Tint color for the ring's outer glow effect.
 */
fun streakRingGlowTarget(streak: Int): Color {
    return when {
        streak <= 0  -> Color.Transparent
        streak < 30  -> RingColorIron.copy(alpha = 0.3f)
        streak < 90  -> RingGlowAmber.copy(alpha = 0.25f)
        else         -> RingGlowWhite.copy(alpha = 0.35f)
    }
}

// ── Animated composable ───────────────────────────────────────

/**
 * Animates the streak ring color smoothly across milestone tiers.
 *
 * Usage (inside any @Composable):
 * ```kotlin
 * val ringColor by rememberStreakRingColor(streak = streak)
 * ```
 *
 * The animation duration is 1200ms with EaseOutCubic — long enough to
 * feel like an evolving material rather than an instant swap.
 *
 * @param streak Current streak day count (typically from WarriorState).
 * @return Animated [State]<[Color]> — delegate with `by`.
 */
@Composable
fun rememberStreakRingColor(streak: Int): State<Color> {
    val target = streakRingColorTarget(streak)
    return animateColorAsState(
        targetValue   = target,
        animationSpec = tween(
            durationMillis = 1200,
            easing         = EaseOutCubic,
        ),
        label = "streakRingColor",
    )
}

/**
 * Animates the ring glow color in sync with the ring color.
 *
 * @param streak Current streak day count.
 * @return Animated [State]<[Color]>.
 */
@Composable
fun rememberStreakRingGlow(streak: Int): State<Color> {
    val target = streakRingGlowTarget(streak)
    return animateColorAsState(
        targetValue   = target,
        animationSpec = tween(
            durationMillis = 1200,
            easing         = EaseOutCubic,
        ),
        label = "streakRingGlow",
    )
}
