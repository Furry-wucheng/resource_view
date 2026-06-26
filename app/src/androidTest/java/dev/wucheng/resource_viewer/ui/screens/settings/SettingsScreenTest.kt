package dev.wucheng.resource_viewer.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should display settings title`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun `should display data management section`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("数据管理").assertIsDisplayed()
    }

    @Test
    fun `should display clear all data option`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("清除所有数据").assertIsDisplayed()
        composeTestRule.onNodeWithText("删除所有数据源、资源、标签和密码").assertIsDisplayed()
    }

    @Test
    fun `should display about section`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("关于").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resource Viewer").assertIsDisplayed()
        composeTestRule.onNodeWithText("版本 1.0.0").assertIsDisplayed()
    }

    @Test
    fun `should display local storage notice`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("所有数据仅存储在本地设备").assertIsDisplayed()
    }

    @Test
    fun `should show confirmation dialog when clear data clicked`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("清除所有数据").performClick()

        composeTestRule.onNodeWithText("确认清除所有数据").assertIsDisplayed()
        composeTestRule.onNodeWithText("确认清除").assertIsDisplayed()
        composeTestRule.onNodeWithText("取消").assertIsDisplayed()
    }

    @Test
    fun `should display warning message in confirmation dialog`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("清除所有数据").performClick()

        composeTestRule.onNodeWithText("此操作将永久删除：").assertIsDisplayed()
        composeTestRule.onNodeWithText("此操作不可撤销！").assertIsDisplayed()
    }

    @Test
    fun `should close dialog when cancel clicked`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("清除所有数据").performClick()
        composeTestRule.onNodeWithText("取消").performClick()

        // 对话框应该关闭，确认按钮应该不存在
        composeTestRule.onNodeWithText("确认清除").assertDoesNotExist()
    }
}
