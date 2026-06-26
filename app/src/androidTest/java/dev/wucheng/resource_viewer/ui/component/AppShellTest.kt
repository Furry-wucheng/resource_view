package dev.wucheng.resource_viewer.ui.component

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.wucheng.resource_viewer.ui.components.AppShell
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class AppShellTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // === M16: 底部标签栏显示测试 ===

    @Test
    fun `should display five tabs in bottom navigation bar when compact width`() {
        composeTestRule.setContent {
            AppShell(widthSizeClass = WindowWidthSizeClass.Compact)
        }

        // Compact 时底部 NavigationBar 可见，共 5 个 Tab
        composeTestRule.onNodeWithText("首页").assertIsDisplayed()
        composeTestRule.onNodeWithText("知识").assertIsDisplayed()
        composeTestRule.onNodeWithText("工具箱").assertIsDisplayed()
        composeTestRule.onNodeWithText("我的").assertIsDisplayed()
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun `should display five tabs in bottom navigation bar when medium width`() {
        composeTestRule.setContent {
            AppShell(widthSizeClass = WindowWidthSizeClass.Medium)
        }

        // Medium 时仍用底部 NavigationBar（平板竖屏）
        composeTestRule.onNodeWithText("首页").assertIsDisplayed()
        composeTestRule.onNodeWithText("知识").assertIsDisplayed()
        composeTestRule.onNodeWithText("工具箱").assertIsDisplayed()
        composeTestRule.onNodeWithText("我的").assertIsDisplayed()
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun `should display five tabs in navigation rail when expanded width`() {
        composeTestRule.setContent {
            AppShell(widthSizeClass = WindowWidthSizeClass.Expanded)
        }

        // Expanded 时侧边 NavigationRail 可见，共 5 个 Tab
        composeTestRule.onNodeWithText("首页").assertIsDisplayed()
        composeTestRule.onNodeWithText("知识").assertIsDisplayed()
        composeTestRule.onNodeWithText("工具箱").assertIsDisplayed()
        composeTestRule.onNodeWithText("我的").assertIsDisplayed()
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    // === M16: 导航状态与路由同步测试 ===

    @Test
    fun `should navigate to knowledge screen when knowledge tab clicked`() {
        // Given
        composeTestRule.setContent {
            AppShell(widthSizeClass = WindowWidthSizeClass.Compact)
        }

        // When
        composeTestRule.onNodeWithText("知识").performClick()

        // Then
        composeTestRule.onNodeWithText("Knowledge Screen").assertIsDisplayed()
    }

    @Test
    fun `should navigate to toolbox screen when toolbox tab clicked`() {
        // Given
        composeTestRule.setContent {
            AppShell(widthSizeClass = WindowWidthSizeClass.Compact)
        }

        // When
        composeTestRule.onNodeWithText("工具箱").performClick()

        // Then
        composeTestRule.onNodeWithText("Toolbox Screen").assertIsDisplayed()
    }

    @Test
    fun `should navigate to profile screen when profile tab clicked`() {
        // Given
        composeTestRule.setContent {
            AppShell(widthSizeClass = WindowWidthSizeClass.Compact)
        }

        // When
        composeTestRule.onNodeWithText("我的").performClick()

        // Then
        composeTestRule.onNodeWithText("Profile Screen").assertIsDisplayed()
    }

    @Test
    fun `should navigate to settings screen when settings tab clicked`() {
        // Given
        composeTestRule.setContent {
            AppShell(widthSizeClass = WindowWidthSizeClass.Compact)
        }

        // When
        composeTestRule.onNodeWithText("设置").performClick()

        // Then
        composeTestRule.onNodeWithText("Settings Screen").assertIsDisplayed()
    }

    @Test
    fun `should navigate back to home when home tab clicked after navigating to other screen`() {
        // Given
        composeTestRule.setContent {
            AppShell(widthSizeClass = WindowWidthSizeClass.Compact)
        }

        // When
        composeTestRule.onNodeWithText("知识").performClick()
        composeTestRule.onNodeWithText("首页").performClick()

        // Then
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
    }
}
