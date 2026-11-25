package com.realityai.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RealityDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF041C10),
    primaryContainer = NeonGreenMuted,
    onPrimaryContainer = NeonGreen,
    secondary = CyberCyan,
    onSecondary = Color(0xFF021922),
    secondaryContainer = CyberCyanMuted,
    onSecondaryContainer = CyberCyan,
    tertiary = AmberWarning,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedCharcoal,
    onSurfaceVariant = TextSecondary,
    outline = HairlineBorder,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun RealityAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RealityDarkColorScheme,
        typography = RealityTypography,
        content = content
    )
}
