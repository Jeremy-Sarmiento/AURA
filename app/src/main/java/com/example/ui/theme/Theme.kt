package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuraColorScheme = darkColorScheme(
    primary = AuraPrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = AuraDarkSurfaceVariant,
    onPrimaryContainer = Color.White,
    secondary = AuraSecondaryBlue,
    onSecondary = Color.White,
    tertiary = AuraAccentTeal,
    background = AuraDarkBackground,
    onBackground = AuraTextPrimary,
    surface = AuraDarkSurface,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraDarkSurfaceVariant,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraCardBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraColorScheme,
        typography = Typography,
        content = content
    )
}
