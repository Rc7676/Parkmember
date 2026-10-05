package com.parkmember

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Brand: confident blue for the app, green for "parked", amber for hints.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1A56DB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE5FF),
    onPrimaryContainer = Color(0xFF00194D),
    secondary = Color(0xFF15803D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F5DD),
    onSecondaryContainer = Color(0xFF00391A),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8CC),
    onTertiaryContainer = Color(0xFF3D1C00),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF1A1C22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFE6E8F0),
    onSurfaceVariant = Color(0xFF5A5F6E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9FAFD),
    surfaceContainer = Color(0xFFF1F3F8),
    surfaceContainerHigh = Color(0xFFECEEF4),
    surfaceContainerHighest = Color(0xFFE6E8F0),
    outline = Color(0xFF8A8F9C),
    outlineVariant = Color(0xFFD5D8E2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB3C6FF),
    onPrimary = Color(0xFF002A78),
    primaryContainer = Color(0xFF1F45A8),
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = Color(0xFF86D9A0),
    onSecondary = Color(0xFF00391A),
    secondaryContainer = Color(0xFF0F5230),
    onSecondaryContainer = Color(0xFFD3F5DD),
    tertiary = Color(0xFFFFB870),
    onTertiary = Color(0xFF4A2500),
    tertiaryContainer = Color(0xFF6B3A00),
    onTertiaryContainer = Color(0xFFFFE8CC),
    background = Color(0xFF0F1117),
    onBackground = Color(0xFFE3E5EC),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFE3E5EC),
    surfaceVariant = Color(0xFF2A2E38),
    onSurfaceVariant = Color(0xFFB9BDC9),
    surfaceContainerLowest = Color(0xFF0B0D12),
    surfaceContainerLow = Color(0xFF15181F),
    surfaceContainer = Color(0xFF1A1D25),
    surfaceContainerHigh = Color(0xFF242832),
    surfaceContainerHighest = Color(0xFF2E333E),
    outline = Color(0xFF7C8190),
    outlineVariant = Color(0xFF3A3F4B),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun ParkmemberTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
