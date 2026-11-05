package com.example.ui.theme

/**
 * ARCHIVO: Theme.kt
 * FUNCIÓN: Tema Material 3 de AURA. Define el esquema de color oscuro y lo aplica junto con la
 *          tipografía a toda la app mediante MyApplicationTheme.
 * NAVEGACIÓN: MainActivity envuelve AuraApp con MyApplicationTheme.
 */

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Esquema de color oscuro basado en la paleta de Color.kt.
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
/**
 * MyApplicationTheme: aplica colorScheme + Typography al contenido Compose.
 * darkTheme/dynamicColor se mantienen por compatibilidad, pero AURA usa siempre su tema oscuro.
 */
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraColorScheme,
        typography = Typography,
        content = content
    )
}
