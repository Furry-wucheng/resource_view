package dev.wucheng.resource_viewer.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dev.wucheng.resource_viewer.data.cache.CacheManager
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.converter.AutoSyncInterval
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import dev.wucheng.resource_viewer.data.local.converter.ThemeMode
import dev.wucheng.resource_viewer.data.local.entity.AppConfigEntity
import dev.wucheng.resource_viewer.data.local.secure.SecurePrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * M25: 设置页 ViewModel
 *
 * 处理设置页的业务逻辑，包括：
 * - 读取/更新 AppConfig
 * - 缓存管理：显示当前缓存大小、手动清理（分类管理）
 * - 外观切换：system / light / dark（实时生效）
 */
class SettingsViewModel(
    application: Application,
    private val database: AppDatabase,
    private val securePrefs: SecurePrefs,
    private val imageLoader: ImageLoader,
    private val cacheManager: CacheManager,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
        calculateCacheSize()
        loadCachePath()
    }

    /**
     * 从数据库加载配置
     */
    private fun loadConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val config = database.appConfigDao().getConfig().first()
                if (config != null) {
                    _uiState.value = _uiState.value.copy(
                        themeMode = config.themeMode,
                        pageDirection = config.pageDirection,
                        doublePageMode = config.doublePageMode,
                        crossChapter = config.crossChapter,
                        thumbnailConcurrency = config.thumbnailConcurrency,
                        autoSyncInterval = config.autoSyncInterval,
                        coverCacheLimitMB = config.coverCacheLimitMB,
                        pageCacheLimitMB = config.pageCacheLimitMB,
                        thumbnailCacheLimitMB = config.thumbnailCacheLimitMB,
                        showDirectoryTree = config.showDirectoryTree,
                        isLoading = false,
                    )
                } else {
                    // 如果没有配置，使用默认值并保存
                    val defaultConfig = AppConfigEntity()
                    database.appConfigDao().save(defaultConfig)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载配置失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 更新主题模式
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            try {
                val currentConfig = database.appConfigDao().getConfig().first()
                val updatedConfig = (currentConfig ?: AppConfigEntity()).copy(
                    themeMode = themeMode,
                    updatedAt = System.currentTimeMillis(),
                )
                database.appConfigDao().save(updatedConfig)
                _uiState.value = _uiState.value.copy(themeMode = themeMode)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新主题模式失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 更新翻页方向
     */
    fun updatePageDirection(pageDirection: PageDirection) {
        viewModelScope.launch {
            try {
                val currentConfig = database.appConfigDao().getConfig().first()
                val updatedConfig = (currentConfig ?: AppConfigEntity()).copy(
                    pageDirection = pageDirection,
                    updatedAt = System.currentTimeMillis(),
                )
                database.appConfigDao().save(updatedConfig)
                _uiState.value = _uiState.value.copy(pageDirection = pageDirection)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新翻页方向失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 更新双页模式
     */
    fun updateDoublePageMode(doublePageMode: DoublePageMode) {
        viewModelScope.launch {
            try {
                val currentConfig = database.appConfigDao().getConfig().first()
                val updatedConfig = (currentConfig ?: AppConfigEntity()).copy(
                    doublePageMode = doublePageMode,
                    updatedAt = System.currentTimeMillis(),
                )
                database.appConfigDao().save(updatedConfig)
                _uiState.value = _uiState.value.copy(doublePageMode = doublePageMode)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新双页模式失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 更新跨章节阅读设置
     */
    fun updateCrossChapter(crossChapter: Boolean) {
        viewModelScope.launch {
            try {
                val currentConfig = database.appConfigDao().getConfig().first()
                val updatedConfig = (currentConfig ?: AppConfigEntity()).copy(
                    crossChapter = crossChapter,
                    updatedAt = System.currentTimeMillis(),
                )
                database.appConfigDao().save(updatedConfig)
                _uiState.value = _uiState.value.copy(crossChapter = crossChapter)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新跨章节设置失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 更新封面缓存容量（0 表示无限制）
     */
    fun updateCoverCacheLimit(limitMB: Int) {
        if (limitMB < 0) {
            _uiState.value = _uiState.value.copy(error = "容量不能为负数")
            return
        }

        viewModelScope.launch {
            try {
                val currentConfig = database.appConfigDao().getConfig().first()
                val updatedConfig = (currentConfig ?: AppConfigEntity()).copy(
                    coverCacheLimitMB = limitMB,
                    updatedAt = System.currentTimeMillis(),
                )
                database.appConfigDao().save(updatedConfig)
                _uiState.value = _uiState.value.copy(coverCacheLimitMB = limitMB)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新封面缓存容量失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 更新页面缓存容量
     */
    fun updatePageCacheLimit(limitMB: Int) {
        if (limitMB < 100) {
            _uiState.value = _uiState.value.copy(error = "最小容量为 100 MB")
            return
        }

        viewModelScope.launch {
            try {
                val currentConfig = database.appConfigDao().getConfig().first()
                val updatedConfig = (currentConfig ?: AppConfigEntity()).copy(
                    pageCacheLimitMB = limitMB,
                    updatedAt = System.currentTimeMillis(),
                )
                database.appConfigDao().save(updatedConfig)
                _uiState.value = _uiState.value.copy(pageCacheLimitMB = limitMB)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新页面缓存容量失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 更新缩略图缓存容量
     */
    fun updateThumbnailCacheLimit(limitMB: Int) {
        if (limitMB < 100) {
            _uiState.value = _uiState.value.copy(error = "最小容量为 100 MB")
            return
        }

        viewModelScope.launch {
            try {
                val currentConfig = database.appConfigDao().getConfig().first()
                val updatedConfig = (currentConfig ?: AppConfigEntity()).copy(
                    thumbnailCacheLimitMB = limitMB,
                    updatedAt = System.currentTimeMillis(),
                )
                database.appConfigDao().save(updatedConfig)
                _uiState.value = _uiState.value.copy(thumbnailCacheLimitMB = limitMB)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新缩略图缓存容量失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 更新缩略图并发数
     */
    fun updateThumbnailConcurrency(concurrency: Int) {
        if (concurrency < 1 || concurrency > 8) {
            _uiState.value = _uiState.value.copy(
                error = "并发数必须在 1-8 之间",
            )
            return
        }

        viewModelScope.launch {
            try {
                val currentConfig = database.appConfigDao().getConfig().first()
                val updatedConfig = (currentConfig ?: AppConfigEntity()).copy(
                    thumbnailConcurrency = concurrency,
                    updatedAt = System.currentTimeMillis(),
                )
                database.appConfigDao().save(updatedConfig)
                _uiState.value = _uiState.value.copy(thumbnailConcurrency = concurrency)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新并发数失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 更新自动同步间隔
     */
    fun updateAutoSyncInterval(interval: AutoSyncInterval?) {
        viewModelScope.launch {
            try {
                val currentConfig = database.appConfigDao().getConfig().first()
                val updatedConfig = (currentConfig ?: AppConfigEntity()).copy(
                    autoSyncInterval = interval,
                    updatedAt = System.currentTimeMillis(),
                )
                database.appConfigDao().save(updatedConfig)
                _uiState.value = _uiState.value.copy(autoSyncInterval = interval)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新同步间隔失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 计算缓存大小（使用 CacheManager 获取分类大小）
     */
    fun calculateCacheSize() {
        viewModelScope.launch {
            try {
                val cacheSizes = cacheManager.getCacheSizes()
                _uiState.value = _uiState.value.copy(
                    coverCacheSizeBytes = cacheSizes.coverCacheBytes,
                    pageCacheSizeBytes = cacheSizes.pageCacheBytes,
                    thumbnailCacheSizeBytes = cacheSizes.thumbnailCacheBytes,
                )
            } catch (e: Exception) {
                // 缓存大小计算失败不影响主功能
            }
        }
    }

    /**
     * 加载缓存路径
     */
    private fun loadCachePath() {
        val context = getApplication<Application>()
        val cacheDir = context.cacheDir
        _uiState.value = _uiState.value.copy(cachePath = cacheDir.absolutePath)
    }

    /**
     * 清理封面缓存
     */
    fun clearCoverCache() {
        viewModelScope.launch {
            try {
                cacheManager.clearCoverCache()
                calculateCacheSize()
                _uiState.value = _uiState.value.copy(cacheCleared = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "清理封面缓存失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 清理页面缓存
     */
    fun clearPageCache() {
        viewModelScope.launch {
            try {
                cacheManager.clearPageCache()
                calculateCacheSize()
                _uiState.value = _uiState.value.copy(cacheCleared = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "清理页面缓存失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 清理缩略图缓存
     */
    fun clearThumbnailCache() {
        viewModelScope.launch {
            try {
                cacheManager.clearThumbnailCache()
                calculateCacheSize()
                _uiState.value = _uiState.value.copy(cacheCleared = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "清理缩略图缓存失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 重置缓存清理状态
     */
    fun resetCacheCleared() {
        _uiState.value = _uiState.value.copy(cacheCleared = false)
    }

    /**
     * 恢复默认设置
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                val defaultConfig = AppConfigEntity()
                database.appConfigDao().save(defaultConfig)
                _uiState.value = _uiState.value.copy(
                    themeMode = defaultConfig.themeMode,
                    pageDirection = defaultConfig.pageDirection,
                    doublePageMode = defaultConfig.doublePageMode,
                    crossChapter = defaultConfig.crossChapter,
                    thumbnailConcurrency = defaultConfig.thumbnailConcurrency,
                    autoSyncInterval = defaultConfig.autoSyncInterval,
                    coverCacheLimitMB = defaultConfig.coverCacheLimitMB,
                    pageCacheLimitMB = defaultConfig.pageCacheLimitMB,
                    thumbnailCacheLimitMB = defaultConfig.thumbnailCacheLimitMB,
                    showDirectoryTree = defaultConfig.showDirectoryTree,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "恢复默认设置失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 更新目录树显示设置
     */
    fun updateShowDirectoryTree(show: Boolean) {
        viewModelScope.launch {
            try {
                val currentConfig = database.appConfigDao().getConfig().first()
                val updatedConfig = (currentConfig ?: AppConfigEntity()).copy(
                    showDirectoryTree = show,
                    updatedAt = System.currentTimeMillis(),
                )
                database.appConfigDao().save(updatedConfig)
                _uiState.value = _uiState.value.copy(showDirectoryTree = show)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新目录树设置失败: ${e.message}",
                )
            }
        }
    }
}

/**
 * 设置页 UI 状态
 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pageDirection: PageDirection = PageDirection.RIGHT_TO_LEFT,
    val doublePageMode: DoublePageMode = DoublePageMode.AUTO,
    val crossChapter: Boolean = true,
    val thumbnailConcurrency: Int = 4,
    val autoSyncInterval: AutoSyncInterval? = null,
    // 缓存大小
    val coverCacheSizeBytes: Long = 0,
    val pageCacheSizeBytes: Long = 0,
    val thumbnailCacheSizeBytes: Long = 0,
    // 缓存容量设置（0 表示无限制）
    val coverCacheLimitMB: Int = 0,
    val pageCacheLimitMB: Int = 500,
    val thumbnailCacheLimitMB: Int = 500,
    val cachePath: String = "",
    val cacheCleared: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    // 文件浏览器设置
    val showDirectoryTree: Boolean = true,
)
