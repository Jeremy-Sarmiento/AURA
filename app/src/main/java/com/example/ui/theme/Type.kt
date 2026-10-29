package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Estilos propios de AURA para no repetir .copy(fontSize = ...) en cada pantalla.
val AuraScreenTitle = TextStyle(
  fontWeight = FontWeight.Bold,
  fontSize = 18.sp,
  letterSpacing = 2.sp,
)

val AuraSectionTitle = TextStyle(
  fontWeight = FontWeight.Bold,
  fontSize = 16.sp,
  letterSpacing = 0.sp,
)

val AuraBadge = TextStyle(
  fontWeight = FontWeight.Bold,
  fontSize = 10.sp,
  letterSpacing = 0.5.sp,
)

// Set of Material typography styles to start with
val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
  )
