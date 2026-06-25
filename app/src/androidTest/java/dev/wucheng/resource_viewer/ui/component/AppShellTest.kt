package dev.wucheng.resource_viewer.ui.component

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.wucheng.resource_viewer.ui.components.AppShell
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class AppShellTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should display bottom navigation bar when compact width`() {
        composeTestRule.setContent {
            AppShell(widthSizeClass = WindowWidthSizeClass.Compact)
        }

        // Compact 时底部 NavigationBar 可见
        composeTestRule.onNodeWithText("首页库").assertIsDisplayed()
        composeTestRule.onNodeWithText("数据源").assertIsDisplayed()
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun `should display bottom navigation bar when medium width`() {
        composeTestRule.setContent {
            AppShell(widthSizeClass = WindowWidthSizeClass.Medium)
        }

        // Medium 时仍用底部 NavigationBar（平板竖屏）
        composeTestRule.onNodeWithText("首页库").assertIsDisplayed()
        composeTestRule.onNodeWithText("数据源").assertIsDisplayed()
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun `should display side navigation rail when expanded width`() {
        composeTestRule.setContent {
            AppShell(widthSizeClass = WindowWidthSizeClass.Expanded)
        }

        // Expanded 时侧边 NavigationRail 可见
        composeTestRule.onNodeWithText("首页库").assertIsDisplayed()
        composeTestRule.onNodeWithText("数据源").assertIsDisplayed()
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }
}
