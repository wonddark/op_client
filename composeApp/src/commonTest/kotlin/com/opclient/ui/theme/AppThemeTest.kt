package com.opclient.ui.theme

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppThemeTest {

    @Test
    fun appTheme_providesLightColors_whenDarkFalse() = runComposeUiTest {
        var captured: AppColors? = null
        setContent {
            AppTheme(darkTheme = false) {
                captured = LocalAppColors.current
            }
        }
        assertEquals(LightColors.accent, captured?.accent)
        assertEquals(LightColors.background, captured?.background)
    }

    @Test
    fun appTheme_providesDarkColors_whenDarkTrue() = runComposeUiTest {
        var captured: AppColors? = null
        setContent {
            AppTheme(darkTheme = true) {
                captured = LocalAppColors.current
            }
        }
        assertEquals(DarkColors.accent, captured?.accent)
        assertEquals(DarkColors.background, captured?.background)
    }
}
