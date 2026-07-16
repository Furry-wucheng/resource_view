package dev.wucheng.resource_viewer.ui.screens.sources

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResponsiveFieldPairTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should place fields on separate rows when stacked`() {
        composeTestRule.setContent {
            MaterialTheme {
                ResponsiveFieldPair(
                    stacked = true,
                    first = { Text("First", modifier = Modifier.width(120.dp)) },
                    second = { Text("Second", modifier = Modifier.width(120.dp)) },
                )
            }
        }

        val firstY = composeTestRule.onNodeWithText("First").fetchSemanticsNode().positionInRoot.y
        val secondY = composeTestRule.onNodeWithText("Second").fetchSemanticsNode().positionInRoot.y
        assertTrue(secondY > firstY)
    }

    @Test
    fun `should place fields on same row when not stacked`() {
        composeTestRule.setContent {
            MaterialTheme {
                ResponsiveFieldPair(
                    stacked = false,
                    first = { Text("First") },
                    second = { Text("Second") },
                )
            }
        }

        val firstY = composeTestRule.onNodeWithText("First").fetchSemanticsNode().positionInRoot.y
        val secondY = composeTestRule.onNodeWithText("Second").fetchSemanticsNode().positionInRoot.y
        assertEquals(firstY, secondY, 0.5f)
    }
}
