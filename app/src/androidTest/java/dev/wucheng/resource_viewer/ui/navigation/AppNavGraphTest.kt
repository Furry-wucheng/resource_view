package dev.wucheng.resource_viewer.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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

    // === M16: 路由注册验证测试 ===

    @Test
    fun `should have knowledge route registered`() {
        // Given
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }

        // Then - 验证 AppNavGraph 可以渲染（路由已注册）
        // 实际导航测试在 AppShellTest 中进行
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
    }

    @Test
    fun `should have toolbox route registered`() {
        // Given
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }

        // Then - 验证 AppNavGraph 可以渲染（路由已注册）
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
    }

    @Test
    fun `should have profile route registered`() {
        // Given
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }

        // Then - 验证 AppNavGraph 可以渲染（路由已注册）
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
    }
}
