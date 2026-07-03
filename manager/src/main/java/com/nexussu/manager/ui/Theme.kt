package com.nexussu.manager.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

val MonoFont = FontFamily.Monospace

enum class AccentTheme(val accent: Color, val accent2: Color) {
    Nexus(Color(0xFF5B7FFF), Color(0xFF9955FF)),
    Mint(Color(0xFF00C853), Color(0xFF64FFDA)),
    Rose(Color(0xFFFF4081), Color(0xFFFF80AB))
}

data class NexusPalette(
    val ink: Color,
    val dim: Color,
    val void: Color,
    val voidGradientEnd: Color,
    val glassFill: Color,
    val glassEdge: Color,
    val accent: Color,
    val accent2: Color
)

val LocalNexusPalette = compositionLocalOf<NexusPalette> { error("No palette provided") }
val LocalHazeState = compositionLocalOf<dev.chrisbanes.haze.HazeState?> { null }

@Composable
fun NexusSUTheme(darkTheme: Boolean, accentTheme: AccentTheme, content: @Composable () -> Unit) {
    val palette = if (darkTheme) {
        NexusPalette(
            ink = Color(0xFFFFFFFF),       // Pure white for striking contrast
            dim = Color(0xFFA5ADB8),       // Bright, legible silver for secondary text
            void = Color(0xFF090B10),      // Deep space black background
            voidGradientEnd = Color(0xFF141720), 
            glassFill = Color(0x4D000000), // Dark translucent tint to make text POP
            glassEdge = Color(0x26FFFFFF), // Subtle white border reflection
            accent = accentTheme.accent,
            accent2 = accentTheme.accent2
        )
    } else {
        NexusPalette(
            ink = Color(0xFF0F141E),
            dim = Color(0xFF636A75),
            void = Color(0xFFF3F5F9),
            voidGradientEnd = Color(0xFFE2E6EE),
            glassFill = Color(0x99FFFFFF), // Bright frosted glass for light mode
            glassEdge = Color(0x4DFFFFFF),
            accent = accentTheme.accent,
            accent2 = accentTheme.accent2
        )
    }

    CompositionLocalProvider(LocalNexusPalette provides palette, content = content)
}
