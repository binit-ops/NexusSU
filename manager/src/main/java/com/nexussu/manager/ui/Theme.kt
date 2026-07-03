package com.nexussu.manager.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

val MonoFont = FontFamily.Monospace

enum class AccentTheme(val accent: Color, val accent2: Color) {
    Nexus(Color(0xFF5B8CFF), Color(0xFF8A5CFF)),
    Ember(Color(0xFFFF6B4A), Color(0xFFFFB454)),
    Verdant(Color(0xFF3DDC97), Color(0xFF2EC4B6)),
    Mono(Color(0xFF8891A6), Color(0xFF5B6478))
}

data class NexusPalette(
    val void: Color, val voidGradientEnd: Color,
    val ink: Color, val dim: Color,
    val glassFill: Color, val glassEdge: Color, val edgeBright: Color,
    val accent: Color, val accent2: Color
)

fun darkPalette(accent: AccentTheme) = NexusPalette(
    void = Color(0xFF0A0E14), voidGradientEnd = Color(0xFF10151F),
    ink = Color(0xFFE9EEF5), dim = Color(0xFF8B96AA),
    glassFill = Color.White.copy(alpha = 0.07f),
    glassEdge = Color.White.copy(alpha = 0.14f),
    edgeBright = Color.White.copy(alpha = 0.4f),
    accent = accent.accent, accent2 = accent.accent2
)

fun lightPalette(accent: AccentTheme) = NexusPalette(
    void = Color(0xFFEEF1F6), voidGradientEnd = Color(0xFFE4E9F2),
    ink = Color(0xFF1B2130), dim = Color(0xFF697086),
    glassFill = Color.White.copy(alpha = 0.55f),
    glassEdge = Color(0xFF141A2D).copy(alpha = 0.10f),
    edgeBright = Color.White.copy(alpha = 0.8f),
    accent = accent.accent, accent2 = accent.accent2
)

val LocalNexusPalette = staticCompositionLocalOf { darkPalette(AccentTheme.Nexus) }

@Composable
fun NexusSUTheme(darkTheme: Boolean, accent: AccentTheme, content: @Composable () -> Unit) {
    val palette = if (darkTheme) darkPalette(accent) else lightPalette(accent)
    CompositionLocalProvider(LocalNexusPalette provides palette) {
        MaterialTheme(content = content)
    }
}
