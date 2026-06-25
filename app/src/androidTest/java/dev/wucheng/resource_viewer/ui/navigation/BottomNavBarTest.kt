package dev.wucheng.resource_viewer.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class BottomNavBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should display all three tabs`() {
        // Given
        composeTestRule.setContent {
            val navController = rememberNavController()
            BottomNavBar(navController = navController)
        }

        // Then
        composeTestRule.onNodeWithText("首页库").assertIsDisplayed()
        composeTestRule.onNodeWithText("数据源").assertIsDisplayed()
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun `should navigate to home when home tab clicked`() {
        // Given
        val mockNavController = mockk<NavHostController>(relaxed = true)
        composeTestRule.setContent {
            BottomNavBar(navController = mockNavController)
        }

        // When
        composeTestRule.onNodeWithText("首页库").performClick()

        // Then
        verify { mockNavController.navigate(Screen.Home.route) }
    }

    @Test
    fun `should navigate to sources when sources tab clicked`() {
        // Given
        val mockNavController = mockk<NavHostController>(relaxed = true)
        composeTestRule.setContent {
            BottomNavBar(navController = mockNavController)
        }

        // When
        composeTestRule.onNodeWithText("数据源").performClick()

        // Then
        verify { mockNavController.navigate(Screen.Sources.route) }
    }

    @Test
    fun `should navigate to settings when settings tab clicked`() {
        // Given
        val mockNavController = mockk<NavHostController>(relaxed = true)
        composeTestRule.setContent {
            BottomNavBar(navController = mockNavController)
        }

        // When
        composeTestRule.onNodeWithText("设置").performClick()

        // Then
        verify { mockNavController.navigate(Screen.Settings.route) }
    }
}
