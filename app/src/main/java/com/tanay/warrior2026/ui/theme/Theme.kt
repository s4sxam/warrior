package com.tanay.warrior.ui.theme

// [NEW] v4.2.0: Custom themes.
//
// WarriorRed and friends stay as plain top-level vals — they're referenced
// as bare identifiers at 60+ call sites across every screen file, and
// several (ChartClean, ChartFailed) are computed at object-init time from
// them, which only works with a plain val, not a @Composable accessor.
// Rewriting every call site to a function call was the alternative; instead:
//
//   • LocalAccentColor (CompositionLocal, default = WarriorRed) is the
//     user's chosen accent. Screens that want to honor a custom theme read
//     `LocalAccentColor.current` instead of `WarriorRed` directly — this is
//     opt-in and additive, so unmodified screens keep compiling and keep
//     using the fixed brand red exactly as before.
//   • LocalScreenBg (CompositionLocal, default = BgBlack) is the color each
//     screen's own root .background(...) should use. It resolves to
//     Color.Transparent when a photo theme is active, so the photo layer
//     MainActivity draws behind the Scaffold isn't hidden by each screen's
//     opaque black fill. Every screen's root background call was switched
//     from BgBlack to LocalScreenBg.current for this reason — it's the one
//     required per-screen edit; everything else about this feature is
//     additive.
//   • Warrior2026Theme() now takes the persisted ThemeSettings and provides
//     both locals + a MaterialTheme colorScheme derived from the accent, so
//     `MaterialTheme.colorScheme.primary` (used by many Material3 components
//     for default tinting) also reflects the custom color automatically.
//   • Photo-mode background is handled separately in MainActivity (drawn
//     behind the Scaffold), not here — Theme.kt only owns colors.

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.tanay.warrior.data.ThemeMode
import com.tanay.warrior.data.ThemeSettings

// ── Brand ──────────────────────────────────────────────────
val WarriorRed    = Color(0xFFFF3131)
val DarkRed       = Color(0xFF8B0000)
val VictoryGreen  = Color(0xFF1DB954)
val Gold          = Color(0xFF00B4FF)  // Arena Blue — replaces gold throughout

// ── Surfaces ───────────────────────────────────────────────
val BgBlack       = Color(0xFF000000)
val SurfaceBlack  = Color(0xFF0A0A0A)
val CardBlack     = Color(0xFF111111)
val Card2Black    = Color(0xFF161616)
val GlassSurface  = Color(0xFF0F0F0F)
val BorderColor   = Color(0xFF252525)
val DividerColor  = Color(0xFF1A1A1A)

// ── Text — all pass WCAG AA on #000 ────────────────────────
val TextPrimary   = Color(0xFFFFFFFF)  // 21:1
val TextSecondary = Color(0xFFAAAAAA)  // 7.5:1
val TextTertiary  = Color(0xFF777777)  // 4.8:1  ← min AA large text
val TextDim       = Color(0xFF555555)
val TextDimmer    = Color(0xFF333333)
val TextDimmest   = Color(0xFF222222)

// ── Charts ─────────────────────────────────────────────────
val ChartClean    = VictoryGreen
val ChartFailed   = WarriorRed
val ChartBar      = Color(0xFF1A1A1A)

// ── Onboarding ─────────────────────────────────────────────
val OnboardRed1   = Color(0xFF1A0000)
val OnboardRed2   = Color(0xFF0D0000)

// v4.2.0 — the active accent color. Defaults to the fixed brand red so any
// screen reading this without a custom theme set behaves exactly as before.
val LocalAccentColor = compositionLocalOf { WarriorRed }

// v4.2.0 — the active screen-root background. Defaults to the fixed BgBlack
// so any screen reading this without a custom theme set behaves exactly as
// before. Overridden to Color.Transparent by Warrior2026Theme when a photo
// background is active, so each screen's own opaque fill doesn't hide the
// photo layer drawn behind the Scaffold in MainActivity.
val LocalScreenBg = compositionLocalOf { BgBlack }

/** Parses a "RRGGBB" hex string (no leading '#') into a Color. Falls back to WarriorRed on bad input. */
fun parseAccentHex(hex: String): Color = runCatching {
    Color(0xFF000000L or hex.removePrefix("#").toLong(16))
}.getOrDefault(WarriorRed)

/** Reverse of parseAccentHex — used when persisting a color the user picked. */
fun Color.toAccentHex(): String {
    val r = (red   * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue  * 255).toInt().coerceIn(0, 255)
    return "%02X%02X%02X".format(r, g, b)
}

// v4.2.0 — a small curated palette shown in the theme picker. Kept here
// rather than in AboutScreen so any future screen can reuse the same set.
val ACCENT_PRESETS = listOf(
    "WARRIOR RED"   to WarriorRed,
    "VICTORY GREEN" to VictoryGreen,
    "ARENA BLUE"    to Gold,
    "AMBER"         to Color(0xFFFF9800),
    "VIOLET"        to Color(0xFF9C27B0),
    "CYAN"          to Color(0xFF00E5FF),
)

private fun colorSchemeFor(accent: Color) = darkColorScheme(
    primary        = accent,
    onPrimary      = Color.White,
    secondary      = VictoryGreen,
    onSecondary    = Color.Black,
    background     = BgBlack,
    surface        = SurfaceBlack,
    onBackground   = Color.White,
    onSurface      = Color.White,
    outline        = BorderColor,
    error          = WarriorRed,
)

/**
 * [themeSettings] is the user's persisted choice (default red, a custom
 * solid accent color, or a gallery photo background). Color-mode accents
 * flow through LocalAccentColor + MaterialTheme.colorScheme.primary. Photo
 * mode doesn't change colors here — MainActivity draws the picked photo as
 * a background layer behind everything, so this theme falls back to the
 * default red accent on top of that photo (keeps text/buttons legible
 * regardless of what the photo looks like).
 */
@Composable
fun Warrior2026Theme(
    themeSettings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val accent = when (themeSettings.mode) {
        ThemeMode.CUSTOM_COLOR -> remember(themeSettings.accentColorHex) { parseAccentHex(themeSettings.accentColorHex) }
        ThemeMode.PHOTO        -> WarriorRed
        ThemeMode.DEFAULT      -> WarriorRed
    }
    val scheme = remember(accent) { colorSchemeFor(accent) }
    // v4.2.0 — when a photo background is active, every screen's own root
    // .background(LocalScreenBg.current) resolves to transparent instead of
    // opaque black, letting the photo layer (drawn behind the Scaffold in
    // MainActivity) show through instead of being hidden underneath it.
    val screenBg = if (themeSettings.mode == ThemeMode.PHOTO && themeSettings.photoPath.isNotBlank())
        Color.Transparent else BgBlack

    CompositionLocalProvider(
        LocalAccentColor provides accent,
        LocalScreenBg    provides screenBg
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
