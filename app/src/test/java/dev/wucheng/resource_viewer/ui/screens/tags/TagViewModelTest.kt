package dev.wucheng.resource_viewer.ui.screens.tags

import dev.wucheng.resource_viewer.data.local.entity.TagEntity
import dev.wucheng.resource_viewer.data.repository.TagRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.Tag
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * TagViewModel 单元测试。
 * 测试标签 CRUD 操作、表单校验和状态管理。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TagViewModelTest {

    private lateinit var mockTagRepository: TagRepository
    private lateinit var viewModel: TagViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testTag = Tag(
        id = "tag-1",
        name = "风景",
        color = "#4CAF50",
        isBuiltIn = false,
        resourceCount = 5,
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    private val builtInTag = Tag(
        id = "tag-builtin",
        name = "收藏",
        color = "#FF9800",
        isBuiltIn = true,
        resourceCount = 10,
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockTagRepository = mockk(relaxed = true)
        every { mockTagRepository.getAllTags() } returns flowOf(listOf(testTag, builtInTag))
        viewModel = TagViewModel(mockTagRepository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ===== 编辑器状态管理 =====

    @Test
    fun `showCreateDialog should set editor visible with empty fields`() = runTest {
        // When
        viewModel.showCreateDialog()

        // Then
        val state = viewModel.editorState.value
        assertTrue(state.isVisible)
        assertNull(state.editingTag)
        assertEquals("", state.name)
        assertEquals("", state.color)
        assertNull(state.nameError)
    }

    @Test
    fun `showEditDialog should populate editor with tag data`() = runTest {
        // When
        viewModel.showEditDialog(testTag)

        // Then
        val state = viewModel.editorState.value
        assertTrue(state.isVisible)
        assertEquals(testTag, state.editingTag)
        assertEquals("风景", state.name)
        assertEquals("#4CAF50", state.color)
        assertNull(state.nameError)
    }

    @Test
    fun `dismissEditor should reset editor state`() = runTest {
        // Given
        viewModel.showCreateDialog()
        viewModel.updateEditorName("test")

        // When
        viewModel.dismissEditor()

        // Then
        val state = viewModel.editorState.value
        assertFalse(state.isVisible)
        assertEquals("", state.name)
        assertEquals("", state.color)
    }

    @Test
    fun `updateEditorName should update name and clear error`() = runTest {
        // Given
        viewModel.showCreateDialog()

        // When
        viewModel.updateEditorName("新标签")

        // Then
        assertEquals("新标签", viewModel.editorState.value.name)
        assertNull(viewModel.editorState.value.nameError)
    }

    @Test
    fun `updateEditorColor should update color`() = runTest {
        // Given
        viewModel.showCreateDialog()

        // When
        viewModel.updateEditorColor("#FF0000")

        // Then
        assertEquals("#FF0000", viewModel.editorState.value.color)
    }

    // ===== saveTag 校验 =====

    @Test
    fun `saveTag should reject empty name`() = runTest {
        // Given
        viewModel.showCreateDialog()
        viewModel.updateEditorColor("#FF0000")

        // When
        viewModel.saveTag()

        // Then
        assertEquals("标签名称不能为空", viewModel.editorState.value.nameError)
        coVerify(exactly = 0) { mockTagRepository.insert(any()) }
    }

    @Test
    fun `saveTag should reject whitespace-only name`() = runTest {
        // Given
        viewModel.showCreateDialog()
        viewModel.updateEditorName("   ")
        viewModel.updateEditorColor("#FF0000")

        // When
        viewModel.saveTag()

        // Then
        assertEquals("标签名称不能为空", viewModel.editorState.value.nameError)
    }

    @Test
    fun `saveTag should reject built-in name '收藏'`() = runTest {
        // Given
        viewModel.showCreateDialog()
        viewModel.updateEditorName("收藏")
        viewModel.updateEditorColor("#FF0000")

        // When
        viewModel.saveTag()

        // Then
        assertEquals("不能使用内置标签名称", viewModel.editorState.value.nameError)
    }

    @Test
    fun `saveTag should reject name longer than 20 characters`() = runTest {
        // Given
        viewModel.showCreateDialog()
        viewModel.updateEditorName("这是一个超过二十个字符的非常非常长的标签名称")
        viewModel.updateEditorColor("#FF0000")

        // When
        viewModel.saveTag()

        // Then
        assertEquals("标签名称不能超过20个字符", viewModel.editorState.value.nameError)
    }

    @Test
    fun `saveTag should reject duplicate name`() = runTest {
        // Given - 先触发 tags flow 收集
        val collectJob = launch { viewModel.tags.collect {} }
        advanceUntilIdle()

        viewModel.showCreateDialog()
        viewModel.updateEditorName("风景") // 已存在的标签名
        viewModel.updateEditorColor("#FF0000")

        // When
        viewModel.saveTag()

        // Then
        assertEquals("标签名称已存在", viewModel.editorState.value.nameError)
        collectJob.cancel()
    }

    @Test
    fun `saveTag should reject empty color`() = runTest {
        // Given
        viewModel.showCreateDialog()
        viewModel.updateEditorName("新标签名")

        // When
        viewModel.saveTag()

        // Then
        assertEquals("请选择标签颜色", viewModel.editorState.value.nameError)
    }

    // ===== saveTag 创建模式 =====

    @Test
    fun `saveTag should create new tag when form is valid`() = runTest {
        // Given
        viewModel.showCreateDialog()
        viewModel.updateEditorName("全新标签")
        viewModel.updateEditorColor("#2196F3")
        coEvery { mockTagRepository.insert(any()) } returns Result.Ok(Unit)

        // When
        viewModel.saveTag()
        advanceUntilIdle()

        // Then
        coVerify {
            mockTagRepository.insert(match { entity ->
                entity.name == "全新标签" &&
                entity.color == "#2196F3" &&
                !entity.isBuiltIn
            })
        }
        // Editor should be dismissed
        assertFalse(viewModel.editorState.value.isVisible)
    }

    @Test
    fun `saveTag should set error when insert fails`() = runTest {
        // Given
        viewModel.showCreateDialog()
        viewModel.updateEditorName("新标签")
        viewModel.updateEditorColor("#FF0000")
        coEvery { mockTagRepository.insert(any()) } returns Result.Err(
            DomainError.DatabaseError("Insert failed")
        )

        // When
        viewModel.saveTag()
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value.error)
    }

    // ===== saveTag 编辑模式 =====

    @Test
    fun `saveTag should update existing tag when editing`() = runTest {
        // Given
        viewModel.showEditDialog(testTag)
        viewModel.updateEditorName("新名称")
        viewModel.updateEditorColor("#FF5722")
        coEvery { mockTagRepository.update(any()) } returns Result.Ok(Unit)

        // When
        viewModel.saveTag()
        advanceUntilIdle()

        // Then
        coVerify {
            mockTagRepository.update(match { entity ->
                entity.id == testTag.id &&
                entity.name == "新名称" &&
                entity.color == "#FF5722"
            })
        }
        assertFalse(viewModel.editorState.value.isVisible)
    }

    @Test
    fun `saveTag should allow same name when editing the same tag`() = runTest {
        // Given - 编辑标签但不改名
        viewModel.showEditDialog(testTag)
        // name is already "风景", color is already "#4CAF50"
        coEvery { mockTagRepository.update(any()) } returns Result.Ok(Unit)

        // When - 不修改名称，应能保存
        viewModel.saveTag()
        advanceUntilIdle()

        // Then
        coVerify { mockTagRepository.update(any()) }
    }

    @Test
    fun `saveTag should set error when update fails`() = runTest {
        // Given
        viewModel.showEditDialog(testTag)
        viewModel.updateEditorName("新名称")
        viewModel.updateEditorColor("#FF0000")
        coEvery { mockTagRepository.update(any()) } returns Result.Err(
            DomainError.DatabaseError("Update failed")
        )

        // When
        viewModel.saveTag()
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value.error)
    }

    // ===== deleteTag =====

    @Test
    fun `deleteTag should reject built-in tag`() = runTest {
        // When
        viewModel.deleteTag(builtInTag)

        // Then
        assertEquals("内置标签不能删除", viewModel.uiState.value.error)
        coVerify(exactly = 0) { mockTagRepository.deleteById(any()) }
    }

    @Test
    fun `deleteTag should delete non-built-in tag`() = runTest {
        // Given
        coEvery { mockTagRepository.deleteById(testTag.id) } returns Result.Ok(Unit)

        // When
        viewModel.deleteTag(testTag)
        advanceUntilIdle()

        // Then
        coVerify { mockTagRepository.deleteById(testTag.id) }
    }

    @Test
    fun `deleteTag should set error when delete fails`() = runTest {
        // Given
        coEvery { mockTagRepository.deleteById(testTag.id) } returns Result.Err(
            DomainError.DatabaseError("Delete failed")
        )

        // When
        viewModel.deleteTag(testTag)
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value.error)
    }

    // ===== clearError =====

    @Test
    fun `clearError should reset error state`() = runTest {
        // Given - 先触发一个错误
        viewModel.deleteTag(builtInTag)
        assertNotNull(viewModel.uiState.value.error)

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }
}
