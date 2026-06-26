package dev.wucheng.resource_viewer.ui.screens.sources

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

/**
 * 数据源列表 UI 状态。
 */
data class SourceListUiState(
    val sources: List<Source> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddSmbDialog: Boolean = false,
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
    private val smbClientWrapper: SmbClientWrapper,
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
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载数据源失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 显示添加 SMB 源对话框。
     */
    fun showAddSmbDialog() {
        _uiState.update { it.copy(showAddSmbDialog = true, smbForm = SmbFormData()) }
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
                val success = smbClientWrapper.testConnection(
                    host = form.host,
                    port = form.port,
                    username = form.username,
                    password = form.password,
                    domain = form.domain.ifBlank { null },
                    shareName = form.shareName
                )
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionSuccess = success,
                        testConnectionError = if (!success) "连接失败，请检查配置" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionSuccess = false,
                        testConnectionError = "连接失败: ${e.message}"
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
                        _uiState.update { it.copy(error = "保存失败: ${result.error.message}") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存失败: ${e.message}") }
            }
        }
    }

    /**
     * 删除数据源。
     */
    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            when (val result = sourceRepository.deleteById(sourceId)) {
                is Result.Ok -> {
                    sourceRepository.removePassword(sourceId)
                }
                is Result.Err -> {
                    _uiState.update { it.copy(error = "删除失败: ${result.error.message}") }
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
                    _uiState.update { it.copy(error = "更新失败: ${result.error.message}") }
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
}
