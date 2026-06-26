package dev.wucheng.resource_viewer.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.wucheng.resource_viewer.data.local.converter.AutoSyncInterval
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import dev.wucheng.resource_viewer.data.local.converter.ThemeMode
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // ========== 基础布局测试 ==========

    @Test
    fun `should display settings title`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun `should display cache management section`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("缓存管理").assertIsDisplayed()
    }

    @Test
    fun `should display appearance section`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("外观").assertIsDisplayed()
    }

    @Test
    fun `should display viewer section`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("查看器默认设置").assertIsDisplayed()
    }

    @Test
    fun `should display data source section`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("数据源同步").assertIsDisplayed()
    }

    @Test
    fun `should display about section`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("关于").assertIsDisplayed()
    }

    // ========== 缓存管理测试 ==========

    @Test
    fun `should display cache size info`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("缩略图缓存").assertIsDisplayed()
    }

    @Test
    fun `should display cache limit options`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("容量上限").assertIsDisplayed()
        composeTestRule.onNodeWithText("500 MB").assertIsDisplayed()
        composeTestRule.onNodeWithText("1000 MB").assertIsDisplayed()
        composeTestRule.onNodeWithText("1500 MB").assertIsDisplayed()
        composeTestRule.onNodeWithText("2000 MB").assertIsDisplayed()
    }

    @Test
    fun `should display clear cache button`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("清理缩略图缓存").assertIsDisplayed()
    }

    @Test
    fun `should show confirmation dialog when clear cache clicked`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("清理缩略图缓存").performClick()

        composeTestRule.onNodeWithText("确认清理缓存").assertIsDisplayed()
        composeTestRule.onNodeWithText("确认清理").assertIsDisplayed()
        composeTestRule.onNodeWithText("取消").assertIsDisplayed()
    }

    @Test
    fun `should close dialog when cancel clicked`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("清理缩略图缓存").performClick()
        composeTestRule.onNodeWithText("取消").performClick()

        composeTestRule.onNodeWithText("确认清理").assertDoesNotExist()
    }

    // ========== 外观设置测试 ==========

    @Test
    fun `should display theme mode label`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("深色模式").assertIsDisplayed()
    }

    @Test
    fun `should display theme options`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("跟随系统").assertIsDisplayed()
        composeTestRule.onNodeWithText("深色").assertIsDisplayed()
        composeTestRule.onNodeWithText("浅色").assertIsDisplayed()
    }

    // ========== 查看器设置测试 ==========

    @Test
    fun `should display page direction setting`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("默认翻页方向").assertIsDisplayed()
    }

    @Test
    fun `should display page direction options`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("右→左").assertIsDisplayed()
        composeTestRule.onNodeWithText("左→右").assertIsDisplayed()
        composeTestRule.onNodeWithText("垂直滚动").assertIsDisplayed()
    }

    @Test
    fun `should display double page mode setting`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("双页显示").assertIsDisplayed()
    }

    @Test
    fun `should display double page mode options`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("自动").assertIsDisplayed()
        composeTestRule.onNodeWithText("始终单页").assertIsDisplayed()
        composeTestRule.onNodeWithText("始终双页").assertIsDisplayed()
    }

    @Test
    fun `should display cross chapter setting`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("跨章节连续阅读").assertIsDisplayed()
    }

    @Test
    fun `should display thumbnail concurrency setting`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("缩略图并发加载").assertIsDisplayed()
    }

    // ========== 数据源同步测试 ==========

    @Test
    fun `should display auto sync interval setting`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("自动同步间隔").assertIsDisplayed()
    }

    @Test
    fun `should display sync interval options`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("关闭").assertIsDisplayed()
        composeTestRule.onNodeWithText("每 15 分钟").assertIsDisplayed()
        composeTestRule.onNodeWithText("每 30 分钟").assertIsDisplayed()
        composeTestRule.onNodeWithText("每小时").assertIsDisplayed()
    }

    // ========== 关于测试 ==========

    @Test
    fun `should display app name`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("Resource Viewer").assertIsDisplayed()
    }

    @Test
    fun `should display version`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("版本 0.1.0").assertIsDisplayed()
    }

    @Test
    fun `should display reset defaults button`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("恢复默认设置").assertIsDisplayed()
    }

    @Test
    fun `should show confirmation dialog when reset defaults clicked`() {
        composeTestRule.setContent {
            SettingsScreen()
        }

        composeTestRule.onNodeWithText("恢复默认设置").performClick()

        composeTestRule.onNodeWithText("确认恢复默认设置").assertIsDisplayed()
        composeTestRule.onNodeWithText("确认恢复").assertIsDisplayed()
        composeTestRule.onNodeWithText("取消").assertIsDisplayed()
    }
}
