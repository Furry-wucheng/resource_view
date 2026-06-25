package dev.wucheng.resource_viewer.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class AppNavGraphTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should start at home screen`() {
        // Given
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }

        // Then
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
    }

    @Test
    fun `should navigate to sources when sources tab clicked`() {
        // Given
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }

        // When
        composeTestRule.onNodeWithText("数据源").performClick()

        // Then
        composeTestRule.onNodeWithText("Sources Screen").assertIsDisplayed()
    }

    @Test
    fun `should navigate to settings when settings tab clicked`() {
        // Given
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }

        // When
        composeTestRule.onNodeWithText("设置").performClick()

        // Then
        composeTestRule.onNodeWithText("Settings Screen").assertIsDisplayed()
    }

    @Test
    fun `should navigate back to home when home tab clicked after navigating to sources`() {
        // Given
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }

        // When
        composeTestRule.onNodeWithText("数据源").performClick()
        composeTestRule.onNodeWithText("首页库").performClick()

        // Then
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
    }
}
