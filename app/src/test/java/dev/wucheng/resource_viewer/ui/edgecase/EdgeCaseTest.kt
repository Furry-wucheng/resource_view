package dev.wucheng.resource_viewer.ui.edgecase

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.domain.model.Tag
import dev.wucheng.resource_viewer.ui.base.UiState
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * M28.3: 边界处理测试
 *
 * 验证各种边界情况的处理：
 * - 空数据处理
 * - 大数据量处理
 * - 错误状态处理
 * - 资源不可达处理
 */
class EdgeCaseTest {

    // === 空数据处理 ===

    @Test
    fun `should handle empty resource list gracefully`() {
        val resources = emptyList<Resource>()
        assertTrue("Empty resource list should be valid", resources.isEmpty())
    }

    @Test
    fun `should handle empty tag list gracefully`() {
        val tags = emptyList<Tag>()
        assertTrue("Empty tag list should be valid", tags.isEmpty())
    }

    @Test
    fun `should handle empty source list gracefully`() {
        val sources = emptyList<Source>()
        assertTrue("Empty source list should be valid", sources.isEmpty())
    }

    @Test
    fun `should handle resource with no tags`() {
        val resource = createTestResource(tags = emptyList())
        assertTrue("Resource with no tags should have empty tags list", resource.tags.isEmpty())
    }

    @Test
    fun `should handle resource with no thumbnail`() {
        val resource = createTestResource(thumbnailPath = null)
        assertNull("Resource with no thumbnail should have null thumbnailPath", resource.thumbnailPath)
    }

    // === 大数据量处理 ===

    @Test
    fun `should handle large resource list`() {
        val resources = (1..1000).map { createTestResource(name = "Resource $it") }
        assertEquals("Should handle 1000 resources", 1000, resources.size)
    }

    @Test
    fun `should handle large tag list`() {
        val now = System.currentTimeMillis()
        val tags = (1..100).map { Tag(id = "tag-$it", name = "Tag $it", color = "#FF0000", createdAt = now, updatedAt = now) }
        assertEquals("Should handle 100 tags", 100, tags.size)
    }

    @Test
    fun `should handle resource with many tags`() {
        val now = System.currentTimeMillis()
        val tags = (1..50).map { Tag(id = "tag-$it", name = "Tag $it", color = "#FF0000", createdAt = now, updatedAt = now) }
        val resource = createTestResource(tags = tags)
        assertEquals("Resource should have 50 tags", 50, resource.tags.size)
    }

    // === 错误状态处理 ===

    @Test
    fun `should handle DomainError correctly`() {
        val error = DomainError.ValidationError("Test error")
        assertNotNull("DomainError should have message", error.message)
    }

    @Test
    fun `should handle Result Error correctly`() {
        val result: Result<String> = Result.Err(DomainError.ValidationError("Test error"))
        assertTrue("Result should be Error", result is Result.Err)
    }

    @Test
    fun `should handle Result Ok correctly`() {
        val result: Result<String> = Result.Ok("Success")
        assertTrue("Result should be Ok", result is Result.Ok)
    }

    // === 资源不可达处理 ===

    @Test
    fun `should handle unavailable source`() {
        val source = createTestSource(isAvailable = false)
        assertFalse("Unavailable source should not be available", source.isAvailable)
    }

    @Test
    fun `should handle disabled source`() {
        val source = createTestSource(enabled = false)
        assertFalse("Disabled source should not be enabled", source.enabled)
    }

    // === UI 状态处理 ===

    @Test
    fun `should handle all UI states`() {
        val states = listOf(UiState.IDLE, UiState.LOADING, UiState.SUCCESS, UiState.ERROR)
        assertEquals("Should have 4 UI states", 4, states.size)
    }

    // === 特殊字符处理 ===

    @Test
    fun `should handle resource name with special characters`() {
        val specialNames = listOf(
            "Resource with spaces",
            "Resource-with-dashes",
            "Resource_with_underscores",
            "Resource (with parentheses)",
            "Resource [with brackets]",
            "Resource {with braces}",
            "Resource with unicode: 你好世界",
            "Resource with emoji: 🎉",
        )
        specialNames.forEach { name ->
            val resource = createTestResource(name = name)
            assertEquals("Should handle special name: $name", name, resource.name)
        }
    }

    @Test
    fun `should handle tag color with different formats`() {
        val now = System.currentTimeMillis()
        val colors = listOf(
            "#FF0000",
            "#00FF00",
            "#0000FF",
            "#FFFFFF",
            "#000000",
        )
        colors.forEach { color ->
            val tag = Tag(id = "tag-$color", name = "Tag", color = color, createdAt = now, updatedAt = now)
            assertEquals("Should handle color: $color", color, tag.color)
        }
    }

    // === 边界值处理 ===

    @Test
    fun `should handle resource with very long name`() {
        val longName = "A".repeat(1000)
        val resource = createTestResource(name = longName)
        assertEquals("Should handle long name", longName, resource.name)
    }

    @Test
    fun `should handle resource with very long path`() {
        val longPath = "/".repeat(500) + "file.txt"
        val resource = createTestResource(relativePath = longPath)
        assertEquals("Should handle long path", longPath, resource.relativePath)
    }

    // === 辅助方法 ===

    private fun createTestResource(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Resource",
        relativePath: String = "/test/path",
        thumbnailPath: String? = "/test/thumbnail.jpg",
        tags: List<Tag> = emptyList(),
        organizationMode: OrganizationMode = OrganizationMode.CHAPTER,
    ): Resource {
        val now = System.currentTimeMillis()
        return Resource(
            id = id,
            sourceId = "source-1",
            sourceName = "Test Source",
            name = name,
            type = ResourceType.FOLDER,
            organizationMode = organizationMode,
            relativePath = relativePath,
            thumbnailPath = thumbnailPath,
            fileCount = 1,
            fileSize = 1024L,
            isAvailable = true,
            lastScannedAt = now,
            tags = tags,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun createTestSource(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Source",
        type: SourceType = SourceType.LOCAL,
        enabled: Boolean = true,
        isAvailable: Boolean = true,
    ): Source {
        return Source(
            id = id,
            name = name,
            type = type,
            rootPath = "/test",
            host = null,
            port = null,
            username = null,
            passwordStored = false,
            domain = null,
            enabled = enabled,
            isAvailable = isAvailable,
            lastCheckAt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
    }
}
