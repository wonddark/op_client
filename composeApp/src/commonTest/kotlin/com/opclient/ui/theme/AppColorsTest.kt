package com.opclient.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class AppColorsTest {

    @Test
    fun lightColors_accent_isCorrect() {
        assertEquals(Color(0xFF3A7D44), LightColors.accent)
    }

    @Test
    fun lightColors_background_isCorrect() {
        assertEquals(Color(0xFFF7F8F6), LightColors.background)
    }

    @Test
    fun darkColors_accent_isCorrect() {
        assertEquals(Color(0xFF6AB874), DarkColors.accent)
    }

    @Test
    fun darkColors_background_isCorrect() {
        assertEquals(Color(0xFF111C10), DarkColors.background)
    }
}
