package com.opclient.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.opclient.ui.theme.AppTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class BottomNavBarTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun bottomNavBar_showsAllDestinationLabels() {
        composeTestRule.setContent {
            AppTheme {
                BottomNavBar(selected = Destination.SEARCH, onSelect = {})
            }
        }
        composeTestRule.onNodeWithText("SEARCH").assertIsDisplayed()
        composeTestRule.onNodeWithText("BROWSE").assertIsDisplayed()
        composeTestRule.onNodeWithText("LIBRARY").assertIsDisplayed()
    }

    @Test
    fun bottomNavBar_callsOnSelect_withCorrectDestination() {
        var selected: Destination? = null
        composeTestRule.setContent {
            AppTheme {
                BottomNavBar(selected = Destination.SEARCH, onSelect = { selected = it })
            }
        }
        composeTestRule.onNodeWithText("BROWSE").performClick()
        assertEquals(Destination.BROWSE, selected)
    }
}
