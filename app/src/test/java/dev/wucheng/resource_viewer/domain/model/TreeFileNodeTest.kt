package dev.wucheng.resource_viewer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeFileNodeTest {

    @Test
    fun `checkedLeafNodes should return empty when nothing checked`() {
        val node = TreeFileNode(
            name = "root",
            relativePath = "",
            isDirectory = true,
            children = listOf(
                TreeFileNode(name = "file1.jpg", relativePath = "file1.jpg", isDirectory = false),
                TreeFileNode(name = "file2.jpg", relativePath = "file2.jpg", isDirectory = false),
            ),
        )
        assertTrue(node.checkedLeafNodes.isEmpty())
    }

    @Test
    fun `checkedLeafNodes should return checked leaf files`() {
        val node = TreeFileNode(
            name = "root",
            relativePath = "",
            isDirectory = true,
            children = listOf(
                TreeFileNode(name = "file1.jpg", relativePath = "file1.jpg", isDirectory = false, isChecked = true),
                TreeFileNode(name = "file2.jpg", relativePath = "file2.jpg", isDirectory = false),
            ),
        )
        val checked = node.checkedLeafNodes
        assertEquals(1, checked.size)
        assertEquals("file1.jpg", checked[0].name)
    }

    @Test
    fun `checkedLeafNodes should return checked non-expandable directory`() {
        val node = TreeFileNode(
            name = "root",
            relativePath = "",
            isDirectory = true,
            children = listOf(
                TreeFileNode(
                    name = "folder",
                    relativePath = "folder",
                    isDirectory = true,
                    isChecked = true,
                    isExpandable = false,
                ),
            ),
        )
        val checked = node.checkedLeafNodes
        assertEquals(1, checked.size)
        assertEquals("folder", checked[0].name)
    }

    @Test
    fun `checkedLeafNodes should return checked expandable directory`() {
        val node = TreeFileNode(
            name = "root",
            relativePath = "",
            isDirectory = true,
            children = listOf(
                TreeFileNode(
                    name = "folder",
                    relativePath = "folder",
                    isDirectory = true,
                    isChecked = true,
                    isExpandable = true,
                ),
            ),
        )
        val checked = node.checkedLeafNodes
        assertEquals(1, checked.size)
        assertEquals("folder", checked[0].name)
    }

    @Test
    fun `checkedLeafNodes should collect from nested children`() {
        val node = TreeFileNode(
            name = "root",
            relativePath = "",
            isDirectory = true,
            children = listOf(
                TreeFileNode(
                    name = "sub",
                    relativePath = "sub",
                    isDirectory = true,
                    isExpandable = true,
                    children = listOf(
                        TreeFileNode(name = "deep.jpg", relativePath = "sub/deep.jpg", isDirectory = false, isChecked = true),
                    ),
                ),
            ),
        )
        val checked = node.checkedLeafNodes
        assertEquals(1, checked.size)
        assertEquals("deep.jpg", checked[0].name)
    }

    @Test
    fun `areAllChildrenChecked should return true when all children checked`() {
        val node = TreeFileNode(
            name = "root",
            relativePath = "",
            isDirectory = true,
            children = listOf(
                TreeFileNode(name = "a.jpg", relativePath = "a.jpg", isDirectory = false, isChecked = true),
                TreeFileNode(name = "b.jpg", relativePath = "b.jpg", isDirectory = false, isChecked = true),
            ),
        )
        assertTrue(node.areAllChildrenChecked)
    }

    @Test
    fun `areAllChildrenChecked should return false when some children unchecked`() {
        val node = TreeFileNode(
            name = "root",
            relativePath = "",
            isDirectory = true,
            children = listOf(
                TreeFileNode(name = "a.jpg", relativePath = "a.jpg", isDirectory = false, isChecked = true),
                TreeFileNode(name = "b.jpg", relativePath = "b.jpg", isDirectory = false),
            ),
        )
        assertFalse(node.areAllChildrenChecked)
    }

    @Test
    fun `areAllChildrenChecked should return false when no children`() {
        val node = TreeFileNode(
            name = "root",
            relativePath = "",
            isDirectory = true,
        )
        assertFalse(node.areAllChildrenChecked)
    }

    @Test
    fun `directChildCount should return children size`() {
        val node = TreeFileNode(
            name = "root",
            relativePath = "",
            isDirectory = true,
            children = listOf(
                TreeFileNode(name = "a", relativePath = "a", isDirectory = true),
                TreeFileNode(name = "b", relativePath = "b", isDirectory = false),
                TreeFileNode(name = "c", relativePath = "c", isDirectory = false),
            ),
        )
        assertEquals(3, node.directChildCount)
    }
}
