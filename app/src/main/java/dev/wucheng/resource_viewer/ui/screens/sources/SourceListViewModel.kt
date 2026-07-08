package dev.wucheng.resource_viewer.ui.screens.sources

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.remote.smb.SmbClientWrapper
import dev.wucheng.resource_viewer.data.repository.FilesystemRepository
import dev.wucheng.resource_viewer.data.repository.SourceRepository
import dev.wucheng.resource_viewer.domain.error.DomainError
import dev.wucheng.resource_viewer.domain.error.Result
import dev.wucheng.resource_viewer.domain.model.Source
import dev.wucheng.resource_viewer.shared.filesource.FileSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * SMB 源表单数据。
 */
data class SmbFormData(
    val name: String = "",
    val host: String = "",
    val port: Int = 445,
    val username: String = "",
    val password: String = "",
    val domain: String = "",
    val shareName: String = "",
)

data class LocalFormData(
    val name: String = "",
    val rootPath: String = "",
)

/**
 * 数据源列表 UI 状态。
 */
data class SourceListUiState(
    val sources: List<Source> = emptyList(),
    val resourceCounts: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSourceTypePicker: Boolean = false,
    val showAddLocalDialog: Boolean = false,
    val showAddSmbDialog: Boolean = false,
    val showEditSmbDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val sourceToRename: Source? = null,
    val sourceToDelete: Source? = null,
    val sourceToEdit: Source? = null,
    val renameName: String = "",
    val localForm: LocalFormData = LocalFormData(),
    val smbForm: SmbFormData = SmbFormData(),
    val isTestingConnection: Boolean = false,
    val testConnectionSuccess: Boolean? = null,
    val testConnectionError: String? = null,
)

/**
 * 数据源列表 ViewModel。
 * 提供数据源的 CRUD 操作和 SMB 测试连接功能。
 *
 * 注意：此实现遵循 doc/mvp/M17-smb-file-source.md 中的 M17.5 子任务。
 */
class SourceListViewModel(
    private val sourceRepository: SourceRepository,
    private val filesystemRepository: FilesystemRepository,
    private val resourceRepository: dev.wucheng.resource_viewer.data.repository.ResourceRepository,
    private val smbClientWrapper: SmbClientWrapper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SourceListUiState())
    val uiState: StateFlow<SourceListUiState> = _uiState.asStateFlow()

    /**
     * 加载所有数据源。
     */
    fun loadSources() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                sourceRepository.getAllSources().collect { sources ->
                    _uiState.update { it.copy(sources = sources, isLoading = false) }
                    loadResourceCounts(sources)
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载数据源失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载数据源失败"
                    )
                }
            }
        }
    }

    /**
     * 加载每个数据源的资源数量。
     */
    private fun loadResourceCounts(sources: List<Source>) {
        viewModelScope.launch {
            val counts = mutableMapOf<String, Int>()
            sources.forEach { source ->
                try {
                    val count = sourceRepository.getResourceCount(source.id)
                    counts[source.id] = count
                } catch (e: Exception) {
                    Log.e(TAG, "加载资源数量失败: ${source.id}", e)
                }
            }
            _uiState.update { it.copy(resourceCounts = counts) }
        }
    }

    /**
     * 显示数据源类型选择器。
     */
    fun showSourceTypePicker() {
        _uiState.update { it.copy(showSourceTypePicker = true) }
    }

    /**
     * 隐藏数据源类型选择器。
     */
    fun hideSourceTypePicker() {
        _uiState.update { it.copy(showSourceTypePicker = false) }
    }

    /**
     * 显示添加 SMB 源对话框。
     */
    fun showAddSmbDialog() {
        _uiState.update { it.copy(showSourceTypePicker = false, showAddSmbDialog = true, smbForm = SmbFormData()) }
    }

    fun showAddLocalDialog() {
        _uiState.update { it.copy(showSourceTypePicker = false, showAddLocalDialog = true, localForm = LocalFormData()) }
    }

    /**
     * 隐藏添加 SMB 源对话框。
     */
    fun hideAddSmbDialog() {
        _uiState.update {
            it.copy(
                showAddSmbDialog = false,
                smbForm = SmbFormData(),
                testConnectionSuccess = null,
                testConnectionError = null
            )
        }
    }

    fun hideAddLocalDialog() {
        _uiState.update {
            it.copy(
                showAddLocalDialog = false,
                localForm = LocalFormData(),
            )
        }
    }

    /**
     * 显示编辑 SMB 源对话框。
     */
    fun showEditSmbDialog(source: Source) {
        viewModelScope.launch {
            // 获取存储的密码
            val password = sourceRepository.getPassword(source.id) ?: ""
            _uiState.update {
                it.copy(
                    showEditSmbDialog = true,
                    sourceToEdit = source,
                    smbForm = SmbFormData(
                        name = source.name,
                        host = source.host ?: "",
                        port = source.port ?: 445,
                        username = source.username ?: "",
                        password = password,
                        domain = source.domain ?: "",
                        shareName = source.rootPath.removePrefix("/"),
                    ),
                    testConnectionSuccess = null,
                    testConnectionError = null,
                )
            }
        }
    }

    /**
     * 隐藏编辑 SMB 源对话框。
     */
    fun hideEditSmbDialog() {
        _uiState.update {
            it.copy(
                showEditSmbDialog = false,
                sourceToEdit = null,
                smbForm = SmbFormData(),
                testConnectionSuccess = null,
                testConnectionError = null,
            )
        }
    }

    /**
     * 更新 SMB 源。
     */
    fun updateSmbSource() {
        val source = _uiState.value.sourceToEdit ?: return
        val form = _uiState.value.smbForm

        if (form.name.isBlank()) {
            _uiState.update { it.copy(error = "源名称不能为空") }
            return
        }
        if (form.host.isBlank()) {
            _uiState.update { it.copy(error = "主机地址不能为空") }
            return
        }
        if (form.shareName.isBlank()) {
            _uiState.update { it.copy(error = "共享名称不能为空") }
            return
        }

        viewModelScope.launch {
            try {
                val entity = SourceEntity(
                    id = source.id,
                    name = form.name,
                    type = SourceType.SMB,
                    rootPath = "/${form.shareName}",
                    host = form.host,
                    port = form.port,
                    username = form.username.ifBlank { null },
                    passwordStored = form.password.isNotBlank(),
                    domain = form.domain.ifBlank { null },
                    enabled = source.enabled,
                    isAvailable = source.isAvailable,
                    lastCheckAt = source.lastCheckAt,
                    createdAt = source.createdAt,
                    updatedAt = System.currentTimeMillis(),
                )

                when (val result = sourceRepository.update(entity)) {
                    is Result.Ok -> {
                        // 更新密码
                        if (form.password.isNotBlank()) {
                            sourceRepository.putPassword(source.id, form.password)
                        } else {
                            sourceRepository.removePassword(source.id)
                        }
                        FileSourceFactory.evict(source.id)
                        hideEditSmbDialog()
                    }
                    is Result.Err -> {
                        _uiState.update { it.copy(error = "更新失败") }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新 SMB 源失败", e)
                _uiState.update { it.copy(error = "更新失败") }
            }
        }
    }

    fun updateLocalForm(
        name: String? = null,
        rootPath: String? = null,
    ) {
        _uiState.update { state ->
            val newRootPath = rootPath ?: state.localForm.rootPath
            val newName = name ?: if (rootPath != null && state.localForm.name.isBlank()) {
                // 自动从 URI 提取文件夹名
                extractFolderName(rootPath)
            } else {
                state.localForm.name
            }
            state.copy(
                localForm = state.localForm.copy(
                    name = newName,
                    rootPath = newRootPath,
                )
            )
        }
    }

    /**
     * 从 URI 或路径中提取文件夹名。
     */
    private fun extractFolderName(path: String): String {
        return try {
            val uri = android.net.Uri.parse(path)
            uri.lastPathSegment ?: path.substringAfterLast("/").ifBlank { path }
        } catch (e: Exception) {
            path.substringAfterLast("/").ifBlank { path }
        }
    }

    /**
     * 更新 SMB 表单数据。
     */
    fun updateSmbForm(
        name: String? = null,
        host: String? = null,
        port: Int? = null,
        username: String? = null,
        password: String? = null,
        domain: String? = null,
        shareName: String? = null,
    ) {
        _uiState.update { state ->
            state.copy(
                smbForm = state.smbForm.copy(
                    name = name ?: state.smbForm.name,
                    host = host ?: state.smbForm.host,
                    port = port ?: state.smbForm.port,
                    username = username ?: state.smbForm.username,
                    password = password ?: state.smbForm.password,
                    domain = domain ?: state.smbForm.domain,
                    shareName = shareName ?: state.smbForm.shareName,
                )
            )
        }
    }

    /**
     * 测试 SMB 连接。
     */
    fun testSmbConnection() {
        val form = _uiState.value.smbForm
        if (form.host.isBlank() || form.shareName.isBlank()) {
            _uiState.update {
                it.copy(testConnectionSuccess = false, testConnectionError = "主机地址和共享名称不能为空")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testConnectionSuccess = null, testConnectionError = null) }
            try {
                withContext(ioDispatcher) {
                    smbClientWrapper.testConnection(
                        host = form.host,
                        port = form.port,
                        username = form.username,
                        password = form.password,
                        domain = form.domain.ifBlank { null },
                        shareName = form.shareName
                    )
                }
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionSuccess = true,
                        testConnectionError = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "SMB 测试连接失败", e)
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionSuccess = false,
                        testConnectionError = "连接失败，请检查地址、凭据和共享名称"
                    )
                }
            }
        }
    }

    /**
     * 添加 SMB 源。
     */
    fun addSmbSource() {
        val form = _uiState.value.smbForm

        // 验证表单
        if (form.name.isBlank()) {
            _uiState.update { it.copy(error = "源名称不能为空") }
            return
        }
        if (form.host.isBlank()) {
            _uiState.update { it.copy(error = "主机地址不能为空") }
            return
        }
        if (form.shareName.isBlank()) {
            _uiState.update { it.copy(error = "共享名称不能为空") }
            return
        }

        viewModelScope.launch {
            try {
                val sourceId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val rootPath = "/${form.shareName}"

                val entity = SourceEntity(
                    id = sourceId,
                    name = form.name,
                    type = SourceType.SMB,
                    rootPath = rootPath,
                    host = form.host,
                    port = form.port,
                    username = form.username.ifBlank { null },
                    passwordStored = form.password.isNotBlank(),
                    domain = form.domain.ifBlank { null },
                    enabled = true,
                    isAvailable = false,
                    createdAt = now,
                    updatedAt = now
                )

                when (val result = sourceRepository.insert(entity)) {
                    is Result.Ok -> {
                        // 存储密码到安全存储
                        if (form.password.isNotBlank()) {
                            sourceRepository.putPassword(sourceId, form.password)
                        }
                        hideAddSmbDialog()
                    }
                    is Result.Err -> {
                        _uiState.update { it.copy(error = "保存失败") }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "添加 SMB 源失败", e)
                _uiState.update { it.copy(error = "保存失败") }
            }
        }
    }

    fun addLocalSource() {
        val form = _uiState.value.localForm

        if (form.name.isBlank()) {
            _uiState.update { it.copy(error = "源名称不能为空") }
            return
        }
        if (form.rootPath.isBlank()) {
            _uiState.update { it.copy(error = "本地路径不能为空") }
            return
        }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val entity = SourceEntity(
                    id = UUID.randomUUID().toString(),
                    name = form.name,
                    type = SourceType.LOCAL,
                    rootPath = form.rootPath.trim(),
                    enabled = true,
                    isAvailable = true,
                    lastCheckAt = now,
                    createdAt = now,
                    updatedAt = now,
                )

                when (val result = sourceRepository.insert(entity)) {
                    is Result.Ok -> hideAddLocalDialog()
                    is Result.Err -> _uiState.update { it.copy(error = "保存失败") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "添加本地源失败", e)
                _uiState.update { it.copy(error = "保存失败") }
            }
        }
    }

    /**
     * 显示重命名对话框。
     */
    fun showRenameDialog(source: Source) {
        _uiState.update {
            it.copy(
                showRenameDialog = true,
                sourceToRename = source,
                renameName = source.name,
            )
        }
    }

    /**
     * 隐藏重命名对话框。
     */
    fun hideRenameDialog() {
        _uiState.update {
            it.copy(
                showRenameDialog = false,
                sourceToRename = null,
                renameName = "",
            )
        }
    }

    /**
     * 更新重命名名称。
     */
    fun updateRenameName(name: String) {
        _uiState.update { it.copy(renameName = name) }
    }

    /**
     * 重命名数据源。
     */
    fun renameSource() {
        val source = _uiState.value.sourceToRename ?: return
        val newName = _uiState.value.renameName.trim()
        if (newName.isBlank()) {
            _uiState.update { it.copy(error = "名称不能为空") }
            return
        }

        viewModelScope.launch {
            try {
                val entity = SourceEntity(
                    id = source.id,
                    name = newName,
                    type = source.type,
                    rootPath = source.rootPath,
                    host = source.host,
                    port = source.port,
                    username = source.username,
                    passwordStored = source.passwordStored,
                    domain = source.domain,
                    enabled = source.enabled,
                    isAvailable = source.isAvailable,
                    lastCheckAt = source.lastCheckAt,
                    createdAt = source.createdAt,
                    updatedAt = System.currentTimeMillis(),
                )
                when (val result = sourceRepository.update(entity)) {
                    is Result.Ok -> hideRenameDialog()
                    is Result.Err -> _uiState.update { it.copy(error = "重命名失败") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "重命名数据源失败", e)
                _uiState.update { it.copy(error = "重命名失败") }
            }
        }
    }

    /**
     * 显示删除确认对话框。
     */
    fun showDeleteConfirmDialog(source: Source) {
        _uiState.update {
            it.copy(
                showDeleteConfirmDialog = true,
                sourceToDelete = source,
            )
        }
    }

    /**
     * 隐藏删除确认对话框。
     */
    fun hideDeleteConfirmDialog() {
        _uiState.update {
            it.copy(
                showDeleteConfirmDialog = false,
                sourceToDelete = null,
            )
        }
    }

    /**
     * 确认删除数据源。
     */
    fun confirmDeleteSource() {
        val source = _uiState.value.sourceToDelete ?: return
        viewModelScope.launch {
            when (val result = sourceRepository.deleteById(source.id)) {
                is Result.Ok -> {
                    sourceRepository.removePassword(source.id)
                    FileSourceFactory.evict(source.id)
                    hideDeleteConfirmDialog()
                }
                is Result.Err -> {
                    _uiState.update { it.copy(error = "删除失败") }
                }
            }
        }
    }

    /**
     * 删除数据源（直接删除，无确认）。
     */
    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            when (val result = sourceRepository.deleteById(sourceId)) {
                is Result.Ok -> {
                    sourceRepository.removePassword(sourceId)
                    FileSourceFactory.evict(sourceId)
                }
                is Result.Err -> {
                    _uiState.update { it.copy(error = "删除失败") }
                }
            }
        }
    }

    /**
     * 切换数据源启用状态。
     */
    fun toggleSourceEnabled(source: Source) {
        viewModelScope.launch {
            val entity = SourceEntity(
                id = source.id,
                name = source.name,
                type = source.type,
                rootPath = source.rootPath,
                host = source.host,
                port = source.port,
                username = source.username,
                passwordStored = source.passwordStored,
                domain = source.domain,
                enabled = !source.enabled,
                isAvailable = source.isAvailable,
                lastCheckAt = source.lastCheckAt,
                createdAt = source.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            when (val result = sourceRepository.update(entity)) {
                is Result.Ok -> { /* Success */ }
                is Result.Err -> {
                    _uiState.update { it.copy(error = "更新失败") }
                }
            }
        }
    }

    /**
     * 清除错误状态。
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        private const val TAG = "SourceListViewModel"
    }
}
