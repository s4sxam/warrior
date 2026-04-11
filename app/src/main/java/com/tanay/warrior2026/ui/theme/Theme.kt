package com.tanay.warrior2026.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val WarriorRed = Color(0xFFFF3131)
val DarkRed = Color(0xFF8B0000)
val BgBlack = Color(0xFF000000)
val SurfaceBlack = Color(0xFF0A0A0A)
val CardBlack = Color(0xFF0D0D0D)
val GlassSurface = Color(0x08FFFFFF)
val BorderColor = Color(0x0DFFFFFF)
val VictoryGreen = Color(0xFF1DB954)
val TextDim = Color(0xFF555555)
val TextDimmer = Color(0xFF333333)
val TextDimmest = Color(0xFF222222)

private val WarriorColorScheme = darkColorScheme(
    primary = WarriorRed,
    onPrimary = Color.White,
    background = BgBlack,
    surface = SurfaceBlack,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun Warrior2026Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarriorColorScheme,
        content = content
    )
}
