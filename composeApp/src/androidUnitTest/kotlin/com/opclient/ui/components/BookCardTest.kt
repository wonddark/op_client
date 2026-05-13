package com.opclient.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.opclient.ui.theme.AppTheme
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class BookCardTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun bookCard_displaysTitleAndAuthor() {
        composeTestRule.setContent {
            AppTheme {
                BookCard(title = "Middlemarch", author = "George Eliot", onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Middlemarch").assertIsDisplayed()
        composeTestRule.onNodeWithText("GEORGE ELIOT").assertIsDisplayed()
    }

    @Test
    fun bookCard_callsOnClick() {
        var clicked = false
        composeTestRule.setContent {
            AppTheme {
                BookCard(title = "Middlemarch", author = "George Eliot", onClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithText("Middlemarch").performClick()
        assertTrue(clicked)
    }
}
