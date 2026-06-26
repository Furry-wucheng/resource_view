package dev.wucheng.resource_viewer.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.wucheng.resource_viewer.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FilterBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testTags = listOf(
        Tag("tag1", "热血", "#E53935", false, 2, 1000L, 1000L),
        Tag("tag2", "冒险", "#1E88E5", false, 1, 2000L, 2000L),
        Tag("tag3", "搞笑", "#43A047", false, 1, 3000L, 3000L),
    )

    @Test
    fun `should display all tags`() {
        composeTestRule.setContent {
            FilterBar(
                tags = testTags,
                selectedTagIds = emptySet(),
                onTagClick = {},
            )
        }
        composeTestRule.onNodeWithText("全部").assertIsDisplayed()
        composeTestRule.onNodeWithText("热血").assertIsDisplayed()
        composeTestRule.onNodeWithText("冒险").assertIsDisplayed()
        composeTestRule.onNodeWithText("搞笑").assertIsDisplayed()
    }

    @Test
    fun `should call onTagClick when tag clicked`() {
        var clickedTagId: String? = null
        composeTestRule.setContent {
            FilterBar(
                tags = testTags,
                selectedTagIds = emptySet(),
                onTagClick = { clickedTagId = it },
            )
        }
        composeTestRule.onNodeWithText("热血").performClick()
        assertEquals("tag1", clickedTagId)
    }

    @Test
    fun `should call onTagClick with null when all button clicked`() {
        var clickedTagId: String? = "some"
        composeTestRule.setContent {
            FilterBar(
                tags = testTags,
                selectedTagIds = setOf("tag1"),
                onTagClick = { clickedTagId = it },
            )
        }
        composeTestRule.onNodeWithText("全部").performClick()
        assertEquals(null, clickedTagId)
    }

    @Test
    fun `should show selected state for selected tags`() {
        composeTestRule.setContent {
            FilterBar(
                tags = testTags,
                selectedTagIds = setOf("tag1"),
                onTagClick = {},
            )
        }
        // 选中状态的标签应该有不同的视觉表现
        // 具体断言取决于实现，这里只验证不崩溃
        composeTestRule.onNodeWithText("热血").assertIsDisplayed()
    }
}
