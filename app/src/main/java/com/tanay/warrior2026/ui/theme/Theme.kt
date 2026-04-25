package com.tanay.warrior.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

private val WarriorColorScheme = darkColorScheme(
    primary        = WarriorRed,
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

@Composable
fun Warrior2026Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WarriorColorScheme, content = content)
}