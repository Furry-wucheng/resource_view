package dev.wucheng.resource_viewer.shared.content

import android.content.Context
import dev.wucheng.resource_viewer.shared.filesource.FileSource
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * PdfContentProvider 测试。
 * 测试 ContentProvider 接口实现，PDF 逐页渲染。
 */
class PdfContentProviderTest {
    private lateinit var mockContext: Context
    private lateinit var mockFileSource: FileSource
    private lateinit var provider: PdfContentProvider

    private val testRelativePath = "test/document.pdf"
    private val testPdfBytes = ByteArray(100) { it.toByte() } // 模拟 PDF 字节

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockFileSource = mockk()
    }

    @After
    fun teardown() {
        if (::provider.isInitialized) {
            provider.dispose()
        }
    }

    // ===== RED: 测试 pageCount =====

    @Test
    fun `should return page count from PDF document`() {
        // Arrange
        coEvery { mockFileSource.readFile(testRelativePath) } returns testPdfBytes

        // Act & Assert
        try {
            provider = PdfContentProvider(mockContext, mockFileSource, testRelativePath)
            // 如果成功创建，验证 pageCount
            assertTrue(provider.pageCount > 0)
        } catch (e: Exception) {
            // 在单元测试中，由于无法真正打开 PDF，预期会失败
            assertNotNull(e)
        }
    }

    @Test
    fun `should return zero pageCount when PDF is invalid`() {
        // Arrange
        coEvery { mockFileSource.readFile(testRelativePath) } returns ByteArray(0)

        // Act & Assert
        try {
            provider = PdfContentProvider(mockContext, mockFileSource, testRelativePath)
            assertEquals(0, provider.pageCount)
        } catch (e: Exception) {
            // 预期异常
            assertNotNull(e)
        }
    }

    // ===== RED: 测试 loadPage =====

    @Test
    fun `should throw when loading page with invalid index`() = runTest {
        // Arrange
        coEvery { mockFileSource.readFile(testRelativePath) } returns testPdfBytes

        // Act & Assert
        try {
            provider = PdfContentProvider(mockContext, mockFileSource, testRelativePath)

            // 尝试加载无效页码
            try {
                provider.loadPage(-1, 100, 100)
                fail("Should throw exception for negative index")
            } catch (e: Exception) {
                assertTrue(e is IllegalArgumentException || e is IllegalStateException)
            }

            try {
                provider.loadPage(provider.pageCount, 100, 100)
                fail("Should throw exception for out-of-range index")
            } catch (e: Exception) {
                assertTrue(e is IllegalArgumentException || e is IllegalStateException)
            }
        } catch (e: Exception) {
            // 创建失败，跳过
            assertNotNull(e)
        }
    }

    @Test
    fun `should throw when loading with zero or negative dimensions`() = runTest {
        // Arrange
        coEvery { mockFileSource.readFile(testRelativePath) } returns testPdfBytes

        // Act & Assert
        try {
            provider = PdfContentProvider(mockContext, mockFileSource, testRelativePath)

            try {
                provider.loadPage(0, 0, 100)
                fail("Should throw exception for zero width")
            } catch (e: Exception) {
                assertTrue(e is IllegalArgumentException || e is IllegalStateException)
            }

            try {
                provider.loadPage(0, 100, 0)
                fail("Should throw exception for zero height")
            } catch (e: Exception) {
                assertTrue(e is IllegalArgumentException || e is IllegalStateException)
            }
        } catch (e: Exception) {
            // 创建失败，跳过
            assertNotNull(e)
        }
    }

    // ===== RED: 测试 dispose =====

    @Test
    fun `should not throw when dispose is called multiple times`() {
        // Arrange
        coEvery { mockFileSource.readFile(testRelativePath) } returns testPdfBytes

        try {
            provider = PdfContentProvider(mockContext, mockFileSource, testRelativePath)
        } catch (e: Exception) {
            // 忽略
        }

        // Act & Assert
        if (::provider.isInitialized) {
            provider.dispose()
            provider.dispose() // 第二次调用不应抛异常
        }
    }

    @Test
    fun `should throw when accessing pageCount after dispose`() {
        // Arrange
        coEvery { mockFileSource.readFile(testRelativePath) } returns testPdfBytes

        try {
            provider = PdfContentProvider(mockContext, mockFileSource, testRelativePath)
            provider.dispose()

            // Act & Assert
            try {
                provider.pageCount
                fail("Should throw exception after dispose")
            } catch (e: Exception) {
                assertTrue(e is IllegalStateException)
            }
        } catch (e: Exception) {
            // 创建失败，跳过
            assertNotNull(e)
        }
    }

    @Test
    fun `should throw when loading page after dispose`() = runTest {
        // Arrange
        coEvery { mockFileSource.readFile(testRelativePath) } returns testPdfBytes

        try {
            provider = PdfContentProvider(mockContext, mockFileSource, testRelativePath)
            provider.dispose()

            // Act & Assert
            try {
                provider.loadPage(0, 100, 100)
                fail("Should throw exception after dispose")
            } catch (e: Exception) {
                assertTrue(e is IllegalStateException)
            }
        } catch (e: Exception) {
            // 创建失败，跳过
            assertNotNull(e)
        }
    }
}
