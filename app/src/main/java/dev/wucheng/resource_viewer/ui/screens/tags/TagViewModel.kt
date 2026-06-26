package dev.wucheng.resource_viewer.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.local.entity.TagEntity
import dev.wucheng.resource_viewer.data.repository.TagRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 标签管理 UI 状态
 */
data class TagManagerUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * 标签编辑 UI 状态
 */
data class TagEditorUiState(
    val isVisible: Boolean = false,
    val editingTag: Tag? = null,
    val name: String = "",
    val color: String = "",
    val nameError: String? = null,
)

/**
 * 标签管理 ViewModel。
 * 管理标签列表（Flow 自动更新）、创建/重命名/删除（内置标签不可删/改）。
 *
 * 注意：此实现遵循 doc/mvp/M15-tag-crud.md 中的 M15.1 子任务。
 */
class TagViewModel(
    private val tagRepository: TagRepository,
) : ViewModel() {
    /**
     * 标签列表（Flow 自动更新）
     */
    val tags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * 标签管理 UI 状态
     */
    private val _uiState = MutableStateFlow(TagManagerUiState())
    val uiState: StateFlow<TagManagerUiState> = _uiState.asStateFlow()

    /**
     * 标签编辑 UI 状态
     */
    private val _editorState = MutableStateFlow(TagEditorUiState())
    val editorState: StateFlow<TagEditorUiState> = _editorState.asStateFlow()

    /**
     * 打开创建标签弹窗
     */
    fun showCreateDialog() {
        _editorState.value = TagEditorUiState(
            isVisible = true,
            editingTag = null,
            name = "",
            color = "",
            nameError = null,
        )
    }

    /**
     * 打开编辑标签弹窗
     */
    fun showEditDialog(tag: Tag) {
        _editorState.value = TagEditorUiState(
            isVisible = true,
            editingTag = tag,
            name = tag.name,
            color = tag.color,
            nameError = null,
        )
    }

    /**
     * 关闭编辑弹窗
     */
    fun dismissEditor() {
        _editorState.value = TagEditorUiState()
    }

    /**
     * 更新编辑中的标签名称
     */
    fun updateEditorName(name: String) {
        _editorState.value = _editorState.value.copy(
            name = name,
            nameError = null,
        )
    }

    /**
     * 更新编辑中的标签颜色
     */
    fun updateEditorColor(color: String) {
        _editorState.value = _editorState.value.copy(color = color)
    }

    /**
     * 验证并保存标签
     */
    fun saveTag() {
        val state = _editorState.value
        val name = state.name.trim()

        // 校验：非空
        if (name.isEmpty()) {
            _editorState.value = state.copy(nameError = "标签名称不能为空")
            return
        }

        // 校验：非"收藏"
        if (name == "收藏") {
            _editorState.value = state.copy(nameError = "不能使用内置标签名称")
            return
        }

        // 校验：≤20字符
        if (name.length > 20) {
            _editorState.value = state.copy(nameError = "标签名称不能超过20个字符")
            return
        }

        // 校验：不重复（排除自身）
        val existingTag = tags.value.find { it.name == name && it.id != state.editingTag?.id }
        if (existingTag != null) {
            _editorState.value = state.copy(nameError = "标签名称已存在")
            return
        }

        // 校验：颜色已选择
        if (state.color.isEmpty()) {
            _editorState.value = state.copy(nameError = "请选择标签颜色")
            return
        }

        viewModelScope.launch {
            if (state.editingTag != null) {
                // 编辑模式
                val entity = TagEntity(
                    id = state.editingTag.id,
                    name = name,
                    color = state.color,
                    isBuiltIn = state.editingTag.isBuiltIn,
                    createdAt = state.editingTag.createdAt,
                    updatedAt = System.currentTimeMillis(),
                )
                when (val result = tagRepository.update(entity)) {
                    is Result.Ok -> dismissEditor()
                    is Result.Err -> {
                        _uiState.value = _uiState.value.copy(
                            error = (result.error as? DomainError.ValidationError)?.message
                                ?: "更新标签失败"
                        )
                    }
                }
            } else {
                // 创建模式
                val entity = TagEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    color = state.color,
                    isBuiltIn = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
                when (val result = tagRepository.insert(entity)) {
                    is Result.Ok -> dismissEditor()
                    is Result.Err -> {
                        _uiState.value = _uiState.value.copy(
                            error = (result.error as? DomainError.DatabaseError)?.message
                                ?: "创建标签失败"
                        )
                    }
                }
            }
        }
    }

    /**
     * 删除标签
     */
    fun deleteTag(tag: Tag) {
        if (tag.isBuiltIn) {
            _uiState.value = _uiState.value.copy(error = "内置标签不能删除")
            return
        }

        viewModelScope.launch {
            when (val result = tagRepository.deleteById(tag.id)) {
                is Result.Ok -> { /* 删除成功，Flow 自动更新列表 */ }
                is Result.Err -> {
                    _uiState.value = _uiState.value.copy(
                        error = (result.error as? DomainError.ValidationError)?.message
                            ?: "删除标签失败"
                    )
                }
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
