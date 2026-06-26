package dev.wucheng.resource_viewer.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Tag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResourceGridItemTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testResource = Resource(
        id = "res1",
        sourceId = "src1",
        sourceName = "本地",
        name = "漫画A - 长标题测试超过两行应该被截断显示省略号",
        type = ResourceType.FOLDER,
        organizationMode = OrganizationMode.GALLERY,
        relativePath = "/comics/a",
        thumbnailPath = null,
        fileCount = 10,
        fileSize = 1000L,
        isAvailable = true,
        lastScannedAt = null,
        tags = listOf(
            Tag("tag1", "热血", "#E53935", false, 2, 1000L, 1000L),
            Tag("tag2", "冒险", "#1E88E5", false, 1, 2000L, 2000L),
            Tag("tag3", "搞笑", "#43A047", false, 1, 3000L, 3000L),
            Tag("tag4", "日常", "#FB8C00", false, 1, 4000L, 4000L),
        ),
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    @Test
    fun `should display resource name`() {
        composeTestRule.setContent {
            ResourceGridItem(
                resource = testResource,
                onClick = {},
            )
        }
        composeTestRule.onNodeWithText("漫画A - 长标题测试超过两行应该被截断显示省略号").assertIsDisplayed()
    }

    @Test
    fun `should display at most 3 tag dots`() {
        composeTestRule.setContent {
            ResourceGridItem(
                resource = testResource,
                onClick = {},
            )
        }
        // 验证最多显示 3 个标签点（通过 contentDescription）
        composeTestRule.onNodeWithContentDescription("标签: 热血").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("标签: 冒险").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("标签: 搞笑").assertIsDisplayed()
        // 第 4 个标签不应显示
        composeTestRule.onNodeWithContentDescription("标签: 日常").assertDoesNotExist()
    }

    @Test
    fun `should call onClick when clicked`() {
        var clicked = false
        composeTestRule.setContent {
            ResourceGridItem(
                resource = testResource,
                onClick = { clicked = true },
            )
        }
        composeTestRule.onNodeWithText("漫画A - 长标题测试超过两行应该被截断显示省略号").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `should display resource without tags`() {
        val noTagResource = testResource.copy(tags = emptyList())
        composeTestRule.setContent {
            ResourceGridItem(
                resource = noTagResource,
                onClick = {},
            )
        }
        composeTestRule.onNodeWithText("漫画A - 长标题测试超过两行应该被截断显示省略号").assertIsDisplayed()
    }
}
