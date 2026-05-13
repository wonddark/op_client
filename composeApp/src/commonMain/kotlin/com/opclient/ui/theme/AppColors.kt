package com.opclient.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentLight: Color,
    val onAccent: Color,
)

val LightColors = AppColors(
    background    = Color(0xFFF7F8F6),
    surface       = Color(0xFFFFFFFF),
    surface2      = Color(0xFFEEF1EB),
    border        = Color(0xFFDDE4DA),
    textPrimary   = Color(0xFF1F2B1E),
    textSecondary = Color(0xFF7A8C76),
    accent        = Color(0xFF3A7D44),
    accentLight   = Color(0xFFEEF1EB),
    onAccent      = Color.White,
)

val DarkColors = AppColors(
    background    = Color(0xFF111C10),
    surface       = Color(0xFF1A2A18),
    surface2      = Color(0xFF243322),
    border        = Color(0xFF2E4A2C),
    textPrimary   = Color(0xFFE4EDE2),
    textSecondary = Color(0xFF7A9C76),
    accent        = Color(0xFF6AB874),
    accentLight   = Color(0xFF1E3A1C),
    onAccent      = Color(0xFF111C10),
)

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors provided — wrap content in AppTheme")
}
