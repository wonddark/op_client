package com.opclient.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.opclient.ui.theme.AppTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class SearchInputTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun searchInput_showsPlaceholder_whenValueEmpty() {
        composeTestRule.setContent {
            AppTheme { SearchInput(value = "", onValueChange = {}) }
        }
        composeTestRule.onNodeWithText("FIND BOOKS, AUTHORS…").assertIsDisplayed()
    }

    @Test
    fun searchInput_callsOnValueChange_whenTextEntered() {
        var captured = ""
        composeTestRule.setContent {
            AppTheme { SearchInput(value = "", onValueChange = { captured = it }) }
        }
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Middlemarch")
        assertEquals("Middlemarch", captured)
    }

    @Test
    fun searchInput_hidesPlaceholder_whenValueNonEmpty() {
        composeTestRule.setContent {
            AppTheme { SearchInput(value = "Middlemarch", onValueChange = {}) }
        }
        composeTestRule.onNodeWithText("FIND BOOKS, AUTHORS…").assertDoesNotExist()
    }
}
