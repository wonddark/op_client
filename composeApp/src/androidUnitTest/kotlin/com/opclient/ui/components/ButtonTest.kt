package com.opclient.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.opclient.ui.theme.AppTheme
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ButtonTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun primaryButton_showsLabelUppercased() {
        composeTestRule.setContent {
            AppTheme { PrimaryButton(text = "save", onClick = {}) }
        }
        composeTestRule.onNodeWithText("SAVE").assertIsDisplayed()
    }

    @Test
    fun primaryButton_callsOnClick() {
        var clicked = false
        composeTestRule.setContent {
            AppTheme { PrimaryButton(text = "Save", onClick = { clicked = true }) }
        }
        composeTestRule.onNodeWithText("SAVE").performClick()
        assertTrue(clicked)
    }

    @Test
    fun primaryButton_doesNotCallOnClick_whenDisabled() {
        var clicked = false
        composeTestRule.setContent {
            AppTheme { PrimaryButton(text = "Save", onClick = { clicked = true }, enabled = false) }
        }
        composeTestRule.onNodeWithText("SAVE").performClick()
        assertFalse(clicked)
    }

    @Test
    fun secondaryButton_showsLabel() {
        composeTestRule.setContent {
            AppTheme { SecondaryButton(text = "cancel", onClick = {}) }
        }
        composeTestRule.onNodeWithText("CANCEL").assertIsDisplayed()
    }
}
