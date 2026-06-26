package dev.wucheng.resource_viewer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.data.repository.TagRepository
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Tag
import dev.wucheng.resource_viewer.ui.base.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel。
 * 管理资源列表、标签筛选和 UI 状态。
 *
 * 注意：此实现遵循 doc/mvp/M23-home-grid.md 中的 M23.1 子任务。
 */
class HomeViewModel(
    private val resourceRepository: ResourceRepository,
    tagRepository: TagRepository,
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
     * 当前选中的标签 ID 集合
     */
    private val _selectedTagIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagIds: StateFlow<Set<String>> = _selectedTagIds.asStateFlow()

    /**
     * 资源列表：根据选中标签自动切换数据源
     */
    val resources: StateFlow<List<Resource>> = _selectedTagIds
        .flatMapLatest { tagIds ->
            if (tagIds.isEmpty()) {
                resourceRepository.getVisibleResources()
            } else {
                resourceRepository.filterByTags(tagIds.toList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * UI 状态
     */
    private val _uiState = MutableStateFlow(UiState.IDLE)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // 监听资源变化，自动更新 UI 状态
        combine(resources, _selectedTagIds) { res, _ ->
            if (res.isNotEmpty() || _selectedTagIds.value.isEmpty()) {
                UiState.SUCCESS
            } else {
                UiState.SUCCESS // 筛选结果为空也是成功状态
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.IDLE,
        ).let { stateFlow ->
            // 收集并更新 _uiState
            viewModelScope.launch {
                stateFlow.collect { state ->
                    _uiState.value = state
                }
            }
        }
    }

    /**
     * 切换标签选中状态。
     * 如果已选中则取消，否则添加。
     */
    fun selectTag(tagId: String) {
        val current = _selectedTagIds.value.toMutableSet()
        if (current.contains(tagId)) {
            current.remove(tagId)
        } else {
            current.add(tagId)
        }
        _selectedTagIds.value = current
    }

    /**
     * 清除所有筛选
     */
    fun clearFilter() {
        _selectedTagIds.value = emptySet()
    }
}
