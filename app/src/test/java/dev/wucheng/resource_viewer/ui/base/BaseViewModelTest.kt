package dev.wucheng.resource_viewer.ui.base

import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.MediaType
import dev.wucheng.resource_viewer.domain.error.Result
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BaseViewModelTest {

    private lateinit var viewModel: TestViewModel

    /** 用于测试的具体 ViewModel 子类 */
    private class TestViewModel : BaseViewModel() {
        fun testSetLoading() = setLoading()
        fun testSetIdle() = setIdle()
        fun <T> testHandleResult(result: Result<T>) = handleResult(result)
    }

    @Before
    fun setup() {
        viewModel = TestViewModel()
    }

    @Test
    fun `should have IDLE state when initialized`() {
        assertEquals(UiState.IDLE, viewModel.uiState.value)
        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.lastError.value)
    }

    @Test
    fun `should have LOADING state when setLoading called`() {
        viewModel.testSetLoading()
        assertEquals(UiState.LOADING, viewModel.uiState.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `should have SUCCESS state when handleResult with Ok`() = runTest {
        viewModel.testHandleResult(Result.Ok("data"))
        assertEquals(UiState.SUCCESS, viewModel.uiState.value)
        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.lastError.value)
    }

    @Test
    fun `should have ERROR state when handleResult with Err`() = runTest {
        val error = DomainError.NetworkTimeoutError("timeout")
        viewModel.testHandleResult(Result.Err(error))
        assertEquals(UiState.ERROR, viewModel.uiState.value)
        assertEquals("连接超时，请稍后重试", viewModel.errorMessage.value)
        assertEquals(error, viewModel.lastError.value)
    }

    @Test
    fun `should reset state when setIdle called after error`() = runTest {
        viewModel.testHandleResult(Result.Err(DomainError.FileNotFoundError("not found")))
        viewModel.testSetIdle()
        assertEquals(UiState.IDLE, viewModel.uiState.value)
        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.lastError.value)
    }

    @Test
    fun `should allow retry for SourceUnreachableError`() = runTest {
        viewModel.testHandleResult(Result.Err(DomainError.SourceUnreachableError("unreachable")))
        assertTrue(viewModel.canRetry)
    }

    @Test
    fun `should allow retry for NetworkTimeoutError`() = runTest {
        viewModel.testHandleResult(Result.Err(DomainError.NetworkTimeoutError("timeout")))
        assertTrue(viewModel.canRetry)
    }

    @Test
    fun `should allow retry for FileNotFoundError`() = runTest {
        viewModel.testHandleResult(Result.Err(DomainError.FileNotFoundError("not found")))
        assertTrue(viewModel.canRetry)
    }

    @Test
    fun `should allow retry for MediaLoadError`() = runTest {
        viewModel.testHandleResult(Result.Err(DomainError.MediaLoadError(MediaType.IMAGE, "load failed")))
        assertTrue(viewModel.canRetry)
    }

    @Test
    fun `should not allow retry for SourceAuthError`() = runTest {
        viewModel.testHandleResult(Result.Err(DomainError.SourceAuthError("auth failed")))
        assertFalse(viewModel.canRetry)
    }

    @Test
    fun `should not allow retry for ValidationError`() = runTest {
        viewModel.testHandleResult(Result.Err(DomainError.ValidationError("invalid")))
        assertFalse(viewModel.canRetry)
    }

    @Test
    fun `should not allow retry for DatabaseError`() = runTest {
        viewModel.testHandleResult(Result.Err(DomainError.DatabaseError("db error")))
        assertFalse(viewModel.canRetry)
    }

    @Test
    fun `should not allow retry when no error`() {
        assertFalse(viewModel.canRetry)
    }

    @Test
    fun `should map all DomainError types to user messages`() = runTest {
        val errors = listOf(
            DomainError.SourceUnreachableError("s") to "数据源不可达，请检查网络连接",
            DomainError.SourceAuthError("s") to "认证失败，请检查用户名和密码",
            DomainError.FileNotFoundError("s") to "文件或路径不存在",
            DomainError.FileAccessDeniedError("s") to "权限不足",
            DomainError.NetworkTimeoutError("s") to "连接超时，请稍后重试",
            DomainError.MediaLoadError(MediaType.IMAGE, "s") to "图片加载失败",
            DomainError.MediaLoadError(MediaType.PDF, "s") to "PDF 加载失败",
            DomainError.MediaLoadError(MediaType.VIDEO, "s") to "视频加载失败",
            DomainError.MediaLoadError(MediaType.ARCHIVE, "s") to "压缩包读取失败",
            DomainError.MediaEncryptedError(MediaType.PDF, "s") to "加密 PDF 暂不支持",
            DomainError.MediaStreamError(MediaType.VIDEO, "s") to "媒体流中断",
            DomainError.ValidationError("custom msg") to "custom msg",
            DomainError.OperationCancelledError() to "操作已取消",
            DomainError.InsufficientStorageError("s") to "存储空间不足",
            DomainError.UnsupportedFormatError("xyz", "s") to "不支持的格式: xyz",
            DomainError.DatabaseError("s") to "数据库错误",
        )

        errors.forEach { (error, expectedMessage) ->
            viewModel.testHandleResult(Result.Err(error))
            assertEquals("For ${error::class.simpleName}", expectedMessage, viewModel.errorMessage.value)
        }
    }

    @Test
    fun `should clear error when handleResult Ok after Err`() = runTest {
        viewModel.testHandleResult(Result.Err(DomainError.NetworkTimeoutError("timeout")))
        assertEquals(UiState.ERROR, viewModel.uiState.value)

        viewModel.testHandleResult(Result.Ok("data"))
        assertEquals(UiState.SUCCESS, viewModel.uiState.value)
        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.lastError.value)
    }
}
