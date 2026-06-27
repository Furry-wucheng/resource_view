package dev.wucheng.resource_viewer.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * ViewerItem 测试。
 * 测试 ViewerItem.ImagePage 的 isAnimated 属性。
 */
class ViewerItemTest {

    @Test
    fun `should return true for gif extension`() {
        // Arrange
        val imagePage = ViewerItem.ImagePage(
            title = "test.gif",
            pageIndex = 0,
            extension = "gif",
        )

        // Act & Assert
        assertTrue(imagePage.isAnimated)
    }

    @Test
    fun `should return true for uppercase gif extension`() {
        // Arrange
        val imagePage = ViewerItem.ImagePage(
            title = "test.GIF",
            pageIndex = 0,
            extension = "GIF",
        )

        // Act & Assert
        assertTrue(imagePage.isAnimated)
    }

    @Test
    fun `should return true for webp extension`() {
        // Arrange
        val imagePage = ViewerItem.ImagePage(
            title = "test.webp",
            pageIndex = 0,
            extension = "webp",
        )

        // Act & Assert
        assertTrue(imagePage.isAnimated)
    }

    @Test
    fun `should return false for jpg extension`() {
        // Arrange
        val imagePage = ViewerItem.ImagePage(
            title = "test.jpg",
            pageIndex = 0,
            extension = "jpg",
        )

        // Act & Assert
        assertFalse(imagePage.isAnimated)
    }

    @Test
    fun `should return false for png extension`() {
        // Arrange
        val imagePage = ViewerItem.ImagePage(
            title = "test.png",
            pageIndex = 0,
            extension = "png",
        )

        // Act & Assert
        assertFalse(imagePage.isAnimated)
    }

    @Test
    fun `should return false for empty extension`() {
        // Arrange
        val imagePage = ViewerItem.ImagePage(
            title = "test",
            pageIndex = 0,
            extension = "",
        )

        // Act & Assert
        assertFalse(imagePage.isAnimated)
    }

    @Test
    fun `should return false for default extension`() {
        // Arrange
        val imagePage = ViewerItem.ImagePage(
            title = "test.jpg",
            pageIndex = 0,
        )

        // Act & Assert
        assertFalse(imagePage.isAnimated)
    }
}
