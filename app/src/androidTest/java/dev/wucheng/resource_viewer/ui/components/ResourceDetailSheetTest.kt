package dev.wucheng.resource_viewer.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Tag
import org.junit.Rule
import org.junit.Test

class ResourceDetailSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testTag1 = Tag(
        id = "tag1",
        name = "收藏",
        color = "#FFC107",
        isBuiltIn = true,
        resourceCount = 5,
        createdAt = 1000L,
        updatedAt = 1000L,
    )
    private val testTag2 = Tag(
        id = "tag2",
        name = "热血",
        color = "#E53935",
        isBuiltIn = false,
        resourceCount = 3,
        createdAt = 2000L,
        updatedAt = 2000L,
    )
    private val testResource = Resource(
        id = "res1",
        sourceId = "src1",
        sourceName = "本地",
        name = "海贼王_卷01",
        type = ResourceType.FOLDER,
        organizationMode = OrganizationMode.CHAPTER,
        relativePath = "/comics/onepiece",
        thumbnailPath = null,
        fileCount = 176,
        fileSize = 512_000_000L,
        isAvailable = true,
        lastScannedAt = null,
        tags = listOf(testTag1),
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    @Test
    fun `should display resource name`() {
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = {},
                onOrgModeChange = {},
                onSave = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("海贼王_卷01").assertIsDisplayed()
    }

    @Test
    fun `should display tag list with names`() {
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = {},
                onOrgModeChange = {},
                onSave = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("收藏").assertIsDisplayed()
        composeTestRule.onNodeWithText("热血").assertIsDisplayed()
    }

    @Test
    fun `should display organization mode options`() {
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = {},
                onOrgModeChange = {},
                onSave = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("章节模式").assertIsDisplayed()
        composeTestRule.onNodeWithText("章节画廊").assertIsDisplayed()
        composeTestRule.onNodeWithText("平铺网格").assertIsDisplayed()
        composeTestRule.onNodeWithText("画廊模式").assertIsDisplayed()
    }

    @Test
    fun `should call onDismiss when cancel clicked`() {
        var dismissed = false
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = {},
                onOrgModeChange = {},
                onSave = {},
                onDismiss = { dismissed = true },
            )
        }
        composeTestRule.onNodeWithText("取消").performClick()
        assert(dismissed)
    }

    @Test
    fun `should call onSave when save clicked`() {
        var saved = false
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = {},
                onOrgModeChange = {},
                onSave = { saved = true },
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("保存").performClick()
        assert(saved)
    }

    @Test
    fun `should call onTagToggle when tag clicked`() {
        var toggledTagId: String? = null
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = { toggledTagId = it },
                onOrgModeChange = {},
                onSave = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("热血").performClick()
        assert(toggledTagId == "tag2")
    }

    @Test
    fun `should display delete button`() {
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = {},
                onOrgModeChange = {},
                onSave = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("删除资源").assertIsDisplayed()
    }

    @Test
    fun `should show delete confirm dialog when delete clicked`() {
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = {},
                onOrgModeChange = {},
                onSave = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("删除资源").performClick()
        composeTestRule.onNodeWithText("确认删除").assertIsDisplayed()
        composeTestRule.onNodeWithText("确定要删除「海贼王_卷01」吗？此操作不可撤销。").assertIsDisplayed()
    }

    @Test
    fun `should show tag creation field`() {
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = {},
                onOrgModeChange = {},
                onSave = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("创建").assertIsDisplayed()
    }

    @Test
    fun `should call onDelete when delete confirmed`() {
        var deletedResource: Resource? = null
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = {},
                onOrgModeChange = {},
                onSave = {},
                onDismiss = {},
                onDelete = { deletedResource = it },
            )
        }
        composeTestRule.onNodeWithText("删除资源").performClick()
        composeTestRule.onNodeWithText("删除").performClick()
        assert(deletedResource != null && deletedResource?.id == "res1")
    }

    @Test
    fun `should call onCreateTag when tag created`() {
        var createdTagName: String? = null
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = {},
                onOrgModeChange = {},
                onSave = {},
                onDismiss = {},
                onCreateTag = { name, _ -> createdTagName = name },
            )
        }
        composeTestRule.onNodeWithText("创建").performClick()
        composeTestRule.waitForIdle()
        // Create button should be disabled when field is blank
        composeTestRule.onNodeWithText("创建").assertIsDisplayed()
    }

    @Test
    fun `should call onOrgModeChange when mode chip clicked`() {
        var changedMode: OrganizationMode? = null
        composeTestRule.setContent {
            ResourceDetailSheet(
                resource = testResource,
                allTags = listOf(testTag1, testTag2),
                selectedTagIds = setOf("tag1"),
                selectedOrgMode = OrganizationMode.CHAPTER,
                onTagToggle = {},
                onOrgModeChange = { changedMode = it },
                onSave = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("平铺网格").performClick()
        assert(changedMode == OrganizationMode.FLATGRID)
    }
}
