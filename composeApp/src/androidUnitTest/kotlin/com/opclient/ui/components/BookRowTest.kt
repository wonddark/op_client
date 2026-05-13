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
class BookRowTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun bookRow_displaysTitle() {
        composeTestRule.setContent {
            AppTheme {
                BookRow(title = "Middlemarch", author = "George Eliot", onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Middlemarch").assertIsDisplayed()
    }

    @Test
    fun bookRow_displaysAuthorUppercased() {
        composeTestRule.setContent {
            AppTheme {
                BookRow(title = "Middlemarch", author = "George Eliot", onClick = {})
            }
        }
        composeTestRule.onNodeWithText("GEORGE ELIOT").assertIsDisplayed()
    }

    @Test
    fun bookRow_callsOnClick_whenTapped() {
        var clicked = false
        composeTestRule.setContent {
            AppTheme {
                BookRow(title = "Middlemarch", author = "George Eliot", onClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithText("Middlemarch").performClick()
        assertTrue(clicked)
    }
}
