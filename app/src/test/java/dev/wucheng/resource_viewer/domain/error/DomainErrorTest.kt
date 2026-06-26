package dev.wucheng.resource_viewer.domain.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainErrorTest {

    @Test
    fun `should create SourceUnreachableError with message and cause`() {
        // Given
        val cause = RuntimeException("connection failed")

        // When
        val error = DomainError.SourceUnreachableError("unreachable", cause)

        // Then
        assertEquals("unreachable", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `should create SourceAuthError with message`() {
        // When
        val error = DomainError.SourceAuthError("auth failed")

        // Then
        assertEquals("auth failed", error.message)
        assertNull(error.cause)
    }

    @Test
    fun `should create FileNotFoundError with message`() {
        // When
        val error = DomainError.FileNotFoundError("not found")

        // Then
        assertEquals("not found", error.message)
    }

    @Test
    fun `should create FileAccessDeniedError with message`() {
        // When
        val error = DomainError.FileAccessDeniedError("denied")

        // Then
        assertEquals("denied", error.message)
    }

    @Test
    fun `should create NetworkTimeoutError with message and cause`() {
        // Given
        val cause = RuntimeException("timeout")

        // When
        val error = DomainError.NetworkTimeoutError("timed out", cause)

        // Then
        assertEquals("timed out", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `should create MediaLoadError with mediaType and message`() {
        // When
        val error = DomainError.MediaLoadError(MediaType.IMAGE, "load failed")

        // Then
        assertEquals("load failed", error.message)
        assertEquals(MediaType.IMAGE, error.mediaType)
    }

    @Test
    fun `should create MediaEncryptedError with mediaType and message`() {
        // When
        val error = DomainError.MediaEncryptedError(MediaType.PDF, "encrypted")

        // Then
        assertEquals("encrypted", error.message)
        assertEquals(MediaType.PDF, error.mediaType)
    }

    @Test
    fun `should create MediaStreamError with mediaType and message`() {
        // When
        val error = DomainError.MediaStreamError(MediaType.VIDEO, "stream failed")

        // Then
        assertEquals("stream failed", error.message)
        assertEquals(MediaType.VIDEO, error.mediaType)
    }

    @Test
    fun `should create ValidationError with message`() {
        // When
        val error = DomainError.ValidationError("invalid input")

        // Then
        assertEquals("invalid input", error.message)
        assertNull(error.cause)
    }

    @Test
    fun `should create OperationCancelledError with default message`() {
        // When
        val error = DomainError.OperationCancelledError()

        // Then
        assertEquals("操作已取消", error.message)
    }

    @Test
    fun `should create OperationCancelledError with custom message`() {
        // When
        val error = DomainError.OperationCancelledError("custom cancel")

        // Then
        assertEquals("custom cancel", error.message)
    }

    @Test
    fun `should create InsufficientStorageError with message`() {
        // When
        val error = DomainError.InsufficientStorageError("no space")

        // Then
        assertEquals("no space", error.message)
    }

    @Test
    fun `should create UnsupportedFormatError with format and message`() {
        // When
        val error = DomainError.UnsupportedFormatError("xyz", "unsupported format")

        // Then
        assertEquals("unsupported format", error.message)
        assertEquals("xyz", error.format)
    }

    @Test
    fun `should create DatabaseError with message and cause`() {
        // Given
        val cause = RuntimeException("db connection failed")

        // When
        val error = DomainError.DatabaseError("database error", cause)

        // Then
        assertEquals("database error", error.message)
        assertEquals(cause, error.cause)
    }

    // Tests for toUserMessage extension
    @Test
    fun `should return correct user message for SourceUnreachableError`() {
        val error = DomainError.SourceUnreachableError("test")
        assertEquals("数据源不可达，请检查网络连接", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for SourceAuthError`() {
        val error = DomainError.SourceAuthError("test")
        assertEquals("认证失败，请检查用户名和密码", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for FileNotFoundError`() {
        val error = DomainError.FileNotFoundError("test")
        assertEquals("文件或路径不存在", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for FileAccessDeniedError`() {
        val error = DomainError.FileAccessDeniedError("test")
        assertEquals("权限不足", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for NetworkTimeoutError`() {
        val error = DomainError.NetworkTimeoutError("test")
        assertEquals("连接超时，请稍后重试", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for MediaLoadError with IMAGE`() {
        val error = DomainError.MediaLoadError(MediaType.IMAGE, "test")
        assertEquals("图片加载失败", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for MediaLoadError with PDF`() {
        val error = DomainError.MediaLoadError(MediaType.PDF, "test")
        assertEquals("PDF 加载失败", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for MediaLoadError with VIDEO`() {
        val error = DomainError.MediaLoadError(MediaType.VIDEO, "test")
        assertEquals("视频加载失败", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for MediaLoadError with ARCHIVE`() {
        val error = DomainError.MediaLoadError(MediaType.ARCHIVE, "test")
        assertEquals("压缩包读取失败", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for MediaEncryptedError with PDF`() {
        val error = DomainError.MediaEncryptedError(MediaType.PDF, "test")
        assertEquals("加密 PDF 暂不支持", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for MediaEncryptedError with non-PDF`() {
        val error = DomainError.MediaEncryptedError(MediaType.IMAGE, "test")
        assertEquals("加密文件暂不支持", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for MediaStreamError`() {
        val error = DomainError.MediaStreamError(MediaType.VIDEO, "test")
        assertEquals("媒体流中断", error.toUserMessage())
    }

    @Test
    fun `should return message directly for ValidationError`() {
        val error = DomainError.ValidationError("custom message")
        assertEquals("custom message", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for OperationCancelledError`() {
        val error = DomainError.OperationCancelledError()
        assertEquals("操作已取消", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for InsufficientStorageError`() {
        val error = DomainError.InsufficientStorageError("test")
        assertEquals("存储空间不足", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for UnsupportedFormatError`() {
        val error = DomainError.UnsupportedFormatError("xyz", "test")
        assertEquals("不支持的格式: xyz", error.toUserMessage())
    }

    @Test
    fun `should return correct user message for DatabaseError`() {
        val error = DomainError.DatabaseError("test")
        assertEquals("数据库错误", error.toUserMessage())
    }

    // Tests for canRetry extension
    @Test
    fun `should allow retry for SourceUnreachableError`() {
        val error = DomainError.SourceUnreachableError("test")
        assertTrue(error.canRetry)
    }

    @Test
    fun `should allow retry for FileNotFoundError`() {
        val error = DomainError.FileNotFoundError("test")
        assertTrue(error.canRetry)
    }

    @Test
    fun `should allow retry for NetworkTimeoutError`() {
        val error = DomainError.NetworkTimeoutError("test")
        assertTrue(error.canRetry)
    }

    @Test
    fun `should allow retry for MediaLoadError`() {
        val error = DomainError.MediaLoadError(MediaType.IMAGE, "test")
        assertTrue(error.canRetry)
    }

    @Test
    fun `should not allow retry for SourceAuthError`() {
        val error = DomainError.SourceAuthError("test")
        assertFalse(error.canRetry)
    }

    @Test
    fun `should not allow retry for FileAccessDeniedError`() {
        val error = DomainError.FileAccessDeniedError("test")
        assertFalse(error.canRetry)
    }

    @Test
    fun `should not allow retry for MediaEncryptedError`() {
        val error = DomainError.MediaEncryptedError(MediaType.PDF, "test")
        assertFalse(error.canRetry)
    }

    @Test
    fun `should not allow retry for MediaStreamError`() {
        val error = DomainError.MediaStreamError(MediaType.VIDEO, "test")
        assertFalse(error.canRetry)
    }

    @Test
    fun `should not allow retry for ValidationError`() {
        val error = DomainError.ValidationError("test")
        assertFalse(error.canRetry)
    }

    @Test
    fun `should not allow retry for OperationCancelledError`() {
        val error = DomainError.OperationCancelledError()
        assertFalse(error.canRetry)
    }

    @Test
    fun `should not allow retry for InsufficientStorageError`() {
        val error = DomainError.InsufficientStorageError("test")
        assertFalse(error.canRetry)
    }

    @Test
    fun `should not allow retry for UnsupportedFormatError`() {
        val error = DomainError.UnsupportedFormatError("xyz", "test")
        assertFalse(error.canRetry)
    }

    @Test
    fun `should not allow retry for DatabaseError`() {
        val error = DomainError.DatabaseError("test")
        assertFalse(error.canRetry)
    }
}
