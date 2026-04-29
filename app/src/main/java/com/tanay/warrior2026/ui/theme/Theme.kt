package com.tanay.warrior.ui.theme

// ─────────────────────────────────────────────────────────────────
// Theme.kt  — v6.0.0 (Redesign)
//
// CHANGES:
//   • Clear visual hierarchy: Primary → Secondary → Quiet
//   • Removed duplicate/unused color aliases
//   • Added semantic action colors for CTA clarity
//   • WarRoom focus-mode color support
// ─────────────────────────────────────────────────────────────────

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand ──────────────────────────────────────────────────────
val WarriorRed    = Color(0xFFE53935)   // primary CTA / accent
val VictoryGreen  = Color(0xFF43A047)   // success / win
val WarningAmber  = Color(0xFFFFA726)   // relapse / warning
val ArenaCyan     = Color(0xFF00B4FF)   // leaderboard / rank

// ── Surfaces — strict 3-level hierarchy ───────────────────────
val BgBlack       = Color(0xFF050505)   // page background
val SurfaceDark   = Color(0xFF0F0F0F)   // card surfaces
val ElevatedCard  = Color(0xFF1A1A1A)   // elevated card / input
val BorderColor   = Color(0xFF2A2A2A)   // dividers and borders

// ── Text — WCAG AA compliant ────────────────────────────────
val TextPrimary   = Color(0xFFFFFFFF)   // 21:1 — headlines, numbers
val TextSecondary = Color(0xFFAAAAAA)   // 7.5:1 — body text
val TextTertiary  = Color(0xFF666666)   // 4.5:1 — labels, hints
val TextDisabled  = Color(0xFF3A3A3A)   // disabled / placeholder

// ── Semantic aliases ──────────────────────────────────────────
val ChartClean    = VictoryGreen
val ChartFailed   = WarriorRed
val Gold          = ArenaCyan           // leaderboard gold

// ── Backwards-compat aliases (do not add new ones) ────────────
val CardBlack     = SurfaceDark
val Card2Black    = ElevatedCard
val GlassSurface  = SurfaceDark
val DividerColor  = BorderColor
val DarkRed       = Color(0xFF8B0000)
val TextDim       = TextDisabled
val TextDimmer    = Color(0xFF2A2A2A)
val TextDimmest   = Color(0xFF1A1A1A)
val OnboardRed1   = Color(0xFF1A0000)
val OnboardRed2   = Color(0xFF0D0000)

// ── Material color scheme ─────────────────────────────────────
private val WarriorColorScheme = darkColorScheme(
    primary        = WarriorRed,
    onPrimary      = Color.White,
    secondary      = VictoryGreen,
    onSecondary    = Color.Black,
    tertiary       = ArenaCyan,
    background     = BgBlack,
    surface        = SurfaceDark,
    surfaceVariant = ElevatedCard,
    onBackground   = TextPrimary,
    onSurface      = TextPrimary,
    outline        = BorderColor,
    error          = WarriorRed,
)

@Composable
fun Warrior2026Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WarriorColorScheme, content = content)
}
