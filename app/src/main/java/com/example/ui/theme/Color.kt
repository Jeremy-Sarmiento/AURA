package com.example.ui.theme

/**
 * ARCHIVO: Color.kt
 * FUNCIÓN: Paleta de colores de AURA (fondos oscuros, morado primario, acentos y colores de género).
 *          Consumida por Theme.kt y por todas las pantallas/componentes.
 */

import androidx.compose.ui.graphics.Color

// Fondos y superficies del tema oscuro.
val AuraDarkBackground = Color(0xFF101229)
val AuraDarkSurface = Color(0xFF181B38)
val AuraDarkSurfaceVariant = Color(0xFF222640)
val AuraCardBorder = Color(0xFF2E335E)

// Marca y acentos principales.
// #7C3AED da 5.7:1 con texto blanco (AA); #9155FD solo daba 4.27:1.
val AuraPrimaryPurple = Color(0xFF7C3AED)
val AuraPrimaryPurpleLight = Color(0xFFAE80FD)
val AuraSecondaryBlue = Color(0xFF5B78EE)
val AuraAccentTeal = Color(0xFF2EC4B6)
val AuraAccentPink = Color(0xFFE85D9E)
val AuraAccentGold = Color(0xFFFFB703)
val AuraDanger = Color(0xFFB3261E)

// Colores de texto.
val AuraTextPrimary = Color(0xFFFFFFFF)
val AuraTextSecondary = Color(0xFF9098C0)
// Aclarado desde #5B638D (3.19:1) para pasar AA en texto secundario: 4.85:1 sobre surfaceVariant.
val AuraTextMuted = Color(0xFF8A93BB)

// Mockup Genre Card Colors
// Colores de las tarjetas de género de la pantalla de búsqueda.
val GenrePop = Color(0xFFD6589F)
val GenreRock = Color(0xFF4C7DE6)
val GenreElectro = Color(0xFF2BBFA8)
val GenreIndie = Color(0xFF4FA882)
val GenreFocus = Color(0xFF8B7DE8)
val GenreParty = Color(0xFFE65D79)
