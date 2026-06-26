package dev.wucheng.resource_viewer.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.secure.SecurePrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * M12: 设置页 ViewModel
 *
 * 处理设置页的业务逻辑，包括数据清除功能。
 */
class SettingsViewModel(
    application: Application,
    private val database: AppDatabase,
    private val securePrefs: SecurePrefs,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * 清除所有应用数据
     *
     * 清除内容包括：
     * - 数据库所有表数据
     * - SecurePrefs 中的密码
     * - 缓存文件
     */
    fun clearAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClearing = true)

            try {
                // 1. 清除数据库数据
                clearDatabase()

                // 2. 清除 SecurePrefs
                clearSecurePrefs()

                // 3. 清除缓存
                clearCache()

                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    clearSuccess = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    error = "清除数据失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 清除数据库所有表数据
     */
    private suspend fun clearDatabase() {
        val db = database.openHelper.writableDatabase

        // 按照外键依赖顺序删除
        db.execSQL("DELETE FROM resource_tags")
        db.execSQL("DELETE FROM resources")
        db.execSQL("DELETE FROM tags WHERE isBuiltIn = 0") // 保留内置标签
        db.execSQL("DELETE FROM sources")

        // 重置自增 ID（如果需要）
        // db.execSQL("DELETE FROM sqlite_sequence")
    }

    /**
     * 清除 SecurePrefs 中的所有密码
     */
    private fun clearSecurePrefs() {
        val prefs = securePrefs.prefs
        prefs.edit().clear().apply()
    }

    /**
     * 清除应用缓存
     */
    private fun clearCache() {
        val context = getApplication<Application>()
        context.cacheDir?.let { deleteRecursively(it) }
    }

    /**
     * 递归删除文件/目录
     */
    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 重置清除成功状态
     */
    fun resetClearSuccess() {
        _uiState.value = _uiState.value.copy(clearSuccess = false)
    }
}

/**
 * 设置页 UI 状态
 */
data class SettingsUiState(
    val isClearing: Boolean = false,
    val clearSuccess: Boolean = false,
    val error: String? = null,
)
