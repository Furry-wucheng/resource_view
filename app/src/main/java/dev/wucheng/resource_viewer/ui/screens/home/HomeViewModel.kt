package dev.wucheng.resource_viewer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.dao.ResourceTagDao
import dev.wucheng.resource_viewer.data.local.entity.ResourceTagEntity
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel。
 * 管理资源列表、标签筛选、UI 状态和资源详情编辑。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val resourceRepository: ResourceRepository,
    private val tagRepository: TagRepository,
    private val resourceTagDao: ResourceTagDao,
) : ViewModel() {
    enum class ResourceSort { ADDED_DESC, ADDED_ASC, NAME_ASC, NAME_DESC }

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
     * 当前选中的标签 ID 集合（首页筛选）
     */
    private val _selectedTagIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagIds: StateFlow<Set<String>> = _selectedTagIds.asStateFlow()

    /**
     * 资源列表：根据选中标签自动切换数据源
     */
    private val baseResources = _selectedTagIds
        .flatMapLatest { tagIds ->
            if (tagIds.isEmpty()) {
                resourceRepository.getVisibleResources()
            } else {
                resourceRepository.filterByTags(tagIds.toList())
            }
        }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _sort = MutableStateFlow(ResourceSort.ADDED_ASC)
    val sort = _sort.asStateFlow()
    private val _isMultiSelect = MutableStateFlow(false)
    val isMultiSelect = _isMultiSelect.asStateFlow()
    private val _selectedResourceIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedResourceIds = _selectedResourceIds.asStateFlow()

    val resources: StateFlow<List<Resource>> = combine(baseResources, _searchQuery, _sort) { resources, query, sort ->
        val filtered = if (query.isBlank()) resources else resources.filter { it.name.contains(query, ignoreCase = true) }
        when (sort) {
            ResourceSort.ADDED_DESC -> filtered.sortedByDescending { it.createdAt }
            ResourceSort.ADDED_ASC -> filtered.sortedBy { it.createdAt }
            ResourceSort.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            ResourceSort.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
        }
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSort(sort: ResourceSort) { _sort.value = sort }
    fun enterMultiSelectMode() { _isMultiSelect.value = true }
    fun exitMultiSelectMode() { _isMultiSelect.value = false; _selectedResourceIds.value = emptySet() }
    fun toggleResourceSelection(id: String) {
        _selectedResourceIds.value = _selectedResourceIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }
    fun toggleSelectAllVisible() {
        val visibleIds = resources.value.map { it.id }.toSet()
        _selectedResourceIds.value = if (_selectedResourceIds.value.containsAll(visibleIds)) emptySet() else visibleIds
    }
    fun batchDeleteSelectedResources() {
        val ids = _selectedResourceIds.value
        viewModelScope.launch {
            ids.forEach { resourceRepository.deleteById(it) }
            exitMultiSelectMode()
        }
    }

    /**
     * UI 状态
     */
    private val _uiState = MutableStateFlow(UiState.IDLE)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // === 资源详情弹窗状态 ===

    /**
     * 当前查看详情的资源
     */
    private val _detailResource = MutableStateFlow<Resource?>(null)
    val detailResource: StateFlow<Resource?> = _detailResource.asStateFlow()

    /**
     * 详情弹窗中选中的标签 ID 集合
     */
    private val _detailTagIds = MutableStateFlow<Set<String>>(emptySet())
    val detailTagIds: StateFlow<Set<String>> = _detailTagIds.asStateFlow()

    /**
     * 详情弹窗中选中的组织模式
     */
    private val _detailOrgMode = MutableStateFlow(OrganizationMode.CHAPTER)
    val detailOrgMode: StateFlow<OrganizationMode> = _detailOrgMode.asStateFlow()

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
            viewModelScope.launch {
                stateFlow.collect { state ->
                    _uiState.value = state
                }
            }
        }
    }

    /**
     * 切换标签选中状态（首页筛选）。
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

    // === 资源详情弹窗方法 ===

    /**
     * 打开资源详情弹窗。
     */
    fun openResourceDetail(resource: Resource) {
        _detailResource.value = resource
        _detailTagIds.value = resource.tags.map { it.id }.toSet()
        _detailOrgMode.value = resource.organizationMode ?: OrganizationMode.CHAPTER
    }

    /**
     * 关闭资源详情弹窗。
     */
    fun closeResourceDetail() {
        _detailResource.value = null
        _detailTagIds.value = emptySet()
    }

    /**
     * 切换详情弹窗中的标签勾选状态。
     */
    fun toggleDetailTag(tagId: String) {
        val current = _detailTagIds.value.toMutableSet()
        if (current.contains(tagId)) {
            current.remove(tagId)
        } else {
            current.add(tagId)
        }
        _detailTagIds.value = current
    }

    /**
     * 设置详情弹窗中的组织模式。
     */
    fun setDetailOrgMode(mode: OrganizationMode) {
        _detailOrgMode.value = mode
    }

    /**
     * 保存资源详情（标签 + 组织模式）。
     */
    fun saveResourceDetail() {
        val resource = _detailResource.value ?: return
        val newTagIds = _detailTagIds.value
        val newOrgMode = _detailOrgMode.value

        viewModelScope.launch {
            resourceRepository.updateOrganizationMode(resource.id, newOrgMode)

            // 更新标签关联
            // 先删除旧的
            resourceTagDao.deleteByResourceId(resource.id)
            // 再插入新的
            val newTags = newTagIds.map { tagId ->
                ResourceTagEntity(
                    resourceId = resource.id,
                    tagId = tagId,
                )
            }
            if (newTags.isNotEmpty()) {
                resourceTagDao.insertAll(newTags)
            }

            // 关闭弹窗
            closeResourceDetail()
        }
    }
}
