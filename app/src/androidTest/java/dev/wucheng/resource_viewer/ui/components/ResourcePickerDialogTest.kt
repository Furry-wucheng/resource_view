package dev.wucheng.resource_viewer.ui.components

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.wucheng.resource_viewer.domain.model.TreeFileNode
import org.junit.Rule
import org.junit.Test

class ResourcePickerDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testTreeNodes = listOf(
        TreeFileNode(
            name = "漫画A",
            relativePath = "/root/漫画A",
            isDirectory = true,
            isExpandable = true,
            children = listOf(
                TreeFileNode(
                    name = "卷01",
                    relativePath = "/root/漫画A/卷01",
                    isDirectory = true,
                    isExpandable = false,
                ),
                TreeFileNode(
                    name = "卷02",
                    relativePath = "/root/漫画A/卷02",
                    isDirectory = true,
                    isExpandable = false,
                ),
            ),
            isExpanded = true,
        ),
        TreeFileNode(
            name = "photo.jpg",
            relativePath = "/root/photo.jpg",
            isDirectory = false,
        ),
    )

    @Test
    fun `should display root name in title`() {
        composeTestRule.setContent {
            ResourcePickerDialog(
                rootName = "全部漫画",
                treeNodes = testTreeNodes,
                selectedCount = 0,
                uiState = ResourcePickerUiState.Ready,
                onToggleExpand = {},
                onToggleCheck = {},
                onSelectAllChildren = {},
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("扫描入库：全部漫画").assertIsDisplayed()
    }

    @Test
    fun `should display tree node names`() {
        composeTestRule.setContent {
            ResourcePickerDialog(
                rootName = "全部漫画",
                treeNodes = testTreeNodes,
                selectedCount = 0,
                uiState = ResourcePickerUiState.Ready,
                onToggleExpand = {},
                onToggleCheck = {},
                onSelectAllChildren = {},
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("漫画A").assertIsDisplayed()
        composeTestRule.onNodeWithText("photo.jpg").assertIsDisplayed()
    }

    @Test
    fun `should display selected count`() {
        composeTestRule.setContent {
            ResourcePickerDialog(
                rootName = "全部漫画",
                treeNodes = testTreeNodes,
                selectedCount = 3,
                uiState = ResourcePickerUiState.Ready,
                onToggleExpand = {},
                onToggleCheck = {},
                onSelectAllChildren = {},
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("已选 3 项").assertIsDisplayed()
    }

    @Test
    fun `should display select all button for expandable directory`() {
        composeTestRule.setContent {
            ResourcePickerDialog(
                rootName = "全部漫画",
                treeNodes = testTreeNodes,
                selectedCount = 0,
                uiState = ResourcePickerUiState.Ready,
                onToggleExpand = {},
                onToggleCheck = {},
                onSelectAllChildren = {},
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("全选子项 (2)").assertIsDisplayed()
    }

    @Test
    fun `should display loading state`() {
        composeTestRule.setContent {
            ResourcePickerDialog(
                rootName = "全部漫画",
                treeNodes = emptyList(),
                selectedCount = 0,
                uiState = ResourcePickerUiState.Loading,
                onToggleExpand = {},
                onToggleCheck = {},
                onSelectAllChildren = {},
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("加载中...").assertIsDisplayed()
    }

    @Test
    fun `should display error state`() {
        composeTestRule.setContent {
            ResourcePickerDialog(
                rootName = "全部漫画",
                treeNodes = emptyList(),
                selectedCount = 0,
                uiState = ResourcePickerUiState.Error("加载失败"),
                onToggleExpand = {},
                onToggleCheck = {},
                onSelectAllChildren = {},
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("加载失败").assertIsDisplayed()
    }

    @Test
    fun `should show import badge but hide checkbox for imported node`() {
        val importedNode = TreeFileNode(
            name = "已入库文件夹",
            relativePath = "/root/imported",
            isDirectory = true,
            isImported = true,
        )
        composeTestRule.setContent {
            ResourcePickerDialog(
                rootName = "全部漫画",
                treeNodes = listOf(importedNode),
                selectedCount = 0,
                uiState = ResourcePickerUiState.Ready,
                onToggleExpand = {},
                onToggleCheck = {},
                onSelectAllChildren = {},
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("已入库文件夹").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("已入库").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("勾选 已入库文件夹").assertDoesNotExist()
    }

    @Test
    fun `should call onDismiss when cancel clicked`() {
        var dismissed = false
        composeTestRule.setContent {
            ResourcePickerDialog(
                rootName = "全部漫画",
                treeNodes = testTreeNodes,
                selectedCount = 0,
                uiState = ResourcePickerUiState.Ready,
                onToggleExpand = {},
                onToggleCheck = {},
                onSelectAllChildren = {},
                onConfirm = {},
                onDismiss = { dismissed = true },
            )
        }
        composeTestRule.onNodeWithText("取消").performClick()
        assert(dismissed)
    }
}
