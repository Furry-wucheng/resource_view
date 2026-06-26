package dev.wucheng.resource_viewer.ui.screens.settings

import android.app.Application
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.disk.DiskCache
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.converter.AutoSyncInterval
import dev.wucheng.resource_viewer.data.local.converter.DoublePageMode
import dev.wucheng.resource_viewer.data.local.converter.PageDirection
import dev.wucheng.resource_viewer.data.local.converter.ThemeMode
import dev.wucheng.resource_viewer.data.local.entity.AppConfigEntity
import dev.wucheng.resource_viewer.data.local.secure.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var securePrefs: SecurePrefs
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var imageLoader: ImageLoader
    private lateinit var diskCacheDir: File

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        application = context.applicationContext as Application

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val prefs = context.getSharedPreferences("test_secure_prefs", 0)
        securePrefs = SecurePrefs(prefs)

        // 创建测试用的 DiskCache
        diskCacheDir = File(context.cacheDir, "test_thumbnails")
        diskCacheDir.mkdirs()

        imageLoader = ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    .directory(diskCacheDir.toOkioPath())
                    .maxSizeBytes(500L * 1024 * 1024) // 500MB
                    .build()
            }
            .build()

        viewModel = SettingsViewModel(application, db, securePrefs, imageLoader)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
        diskCacheDir.deleteRecursively()
    }

    // ========== 初始状态测试 ==========

    @Test
    fun `should have initial state with default config`() = runTest {
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(PageDirection.RIGHT_TO_LEFT, state.pageDirection)
        assertEquals(DoublePageMode.AUTO, state.doublePageMode)
        assertTrue(state.crossChapter)
        assertEquals(500, state.cacheLimitMB)
        assertEquals(4, state.thumbnailConcurrency)
        assertNull(state.autoSyncInterval)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `should load config from database on init`() = runTest {
        // 预先插入配置
        val existingConfig = AppConfigEntity(
            themeMode = ThemeMode.DARK,
            pageDirection = PageDirection.LEFT_TO_RIGHT,
            doublePageMode = DoublePageMode.DOUBLE,
            crossChapter = false,
            cacheLimitMB = 1000,
            thumbnailConcurrency = 8,
            autoSyncInterval = AutoSyncInterval.HOUR_1
        )
        db.appConfigDao().save(existingConfig)

        // 创建新的 ViewModel 来测试初始化加载
        val newViewModel = SettingsViewModel(application, db, securePrefs, imageLoader)
        advanceUntilIdle()

        val state = newViewModel.uiState.value
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(PageDirection.LEFT_TO_RIGHT, state.pageDirection)
        assertEquals(DoublePageMode.DOUBLE, state.doublePageMode)
        assertFalse(state.crossChapter)
        assertEquals(1000, state.cacheLimitMB)
        assertEquals(8, state.thumbnailConcurrency)
        assertEquals(AutoSyncInterval.HOUR_1, state.autoSyncInterval)
    }

    // ========== 主题模式测试 ==========

    @Test
    fun `should update theme mode`() = runTest {
        viewModel.updateThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ThemeMode.DARK, state.themeMode)

        // 验证数据库已更新
        val savedConfig = db.appConfigDao().getConfig().first()
        assertNotNull(savedConfig)
        assertEquals(ThemeMode.DARK, savedConfig!!.themeMode)
    }

    @Test
    fun `should update theme mode to light`() = runTest {
        viewModel.updateThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ThemeMode.LIGHT, state.themeMode)
    }

    @Test
    fun `should update theme mode to system`() = runTest {
        viewModel.updateThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        viewModel.updateThemeMode(ThemeMode.SYSTEM)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
    }

    // ========== 翻页方向测试 ==========

    @Test
    fun `should update page direction`() = runTest {
        viewModel.updatePageDirection(PageDirection.LEFT_TO_RIGHT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PageDirection.LEFT_TO_RIGHT, state.pageDirection)

        // 验证数据库已更新
        val savedConfig = db.appConfigDao().getConfig().first()
        assertNotNull(savedConfig)
        assertEquals(PageDirection.LEFT_TO_RIGHT, savedConfig!!.pageDirection)
    }

    @Test
    fun `should update page direction to vertical`() = runTest {
        viewModel.updatePageDirection(PageDirection.VERTICAL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PageDirection.VERTICAL, state.pageDirection)
    }

    // ========== 双页模式测试 ==========

    @Test
    fun `should update double page mode`() = runTest {
        viewModel.updateDoublePageMode(DoublePageMode.SINGLE)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DoublePageMode.SINGLE, state.doublePageMode)

        // 验证数据库已更新
        val savedConfig = db.appConfigDao().getConfig().first()
        assertNotNull(savedConfig)
        assertEquals(DoublePageMode.SINGLE, savedConfig!!.doublePageMode)
    }

    @Test
    fun `should update double page mode to double`() = runTest {
        viewModel.updateDoublePageMode(DoublePageMode.DOUBLE)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DoublePageMode.DOUBLE, state.doublePageMode)
    }

    // ========== 跨章节阅读测试 ==========

    @Test
    fun `should update cross chapter setting`() = runTest {
        viewModel.updateCrossChapter(false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.crossChapter)

        // 验证数据库已更新
        val savedConfig = db.appConfigDao().getConfig().first()
        assertNotNull(savedConfig)
        assertFalse(savedConfig!!.crossChapter)
    }

    @Test
    fun `should enable cross chapter reading`() = runTest {
        viewModel.updateCrossChapter(false)
        advanceUntilIdle()

        viewModel.updateCrossChapter(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.crossChapter)
    }

    // ========== 缓存限制测试 ==========

    @Test
    fun `should update cache limit`() = runTest {
        viewModel.updateCacheLimit(1000)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1000, state.cacheLimitMB)

        // 验证数据库已更新
        val savedConfig = db.appConfigDao().getConfig().first()
        assertNotNull(savedConfig)
        assertEquals(1000, savedConfig!!.cacheLimitMB)
    }

    @Test
    fun `should reject cache limit less than 500`() = runTest {
        viewModel.updateCacheLimit(100)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 应该保持原值
        assertEquals(500, state.cacheLimitMB)
        assertNotNull(state.error)
    }

    // ========== 缩略图并发数测试 ==========

    @Test
    fun `should update thumbnail concurrency`() = runTest {
        viewModel.updateThumbnailConcurrency(8)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(8, state.thumbnailConcurrency)

        // 验证数据库已更新
        val savedConfig = db.appConfigDao().getConfig().first()
        assertNotNull(savedConfig)
        assertEquals(8, savedConfig!!.thumbnailConcurrency)
    }

    @Test
    fun `should reject thumbnail concurrency out of range`() = runTest {
        viewModel.updateThumbnailConcurrency(10)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 应该保持原值
        assertEquals(4, state.thumbnailConcurrency)
        assertNotNull(state.error)
    }

    @Test
    fun `should reject thumbnail concurrency less than 1`() = runTest {
        viewModel.updateThumbnailConcurrency(0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 应该保持原值
        assertEquals(4, state.thumbnailConcurrency)
        assertNotNull(state.error)
    }

    // ========== 自动同步间隔测试 ==========

    @Test
    fun `should update auto sync interval`() = runTest {
        viewModel.updateAutoSyncInterval(AutoSyncInterval.MINUTES_15)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AutoSyncInterval.MINUTES_15, state.autoSyncInterval)

        // 验证数据库已更新
        val savedConfig = db.appConfigDao().getConfig().first()
        assertNotNull(savedConfig)
        assertEquals(AutoSyncInterval.MINUTES_15, savedConfig!!.autoSyncInterval)
    }

    @Test
    fun `should disable auto sync`() = runTest {
        viewModel.updateAutoSyncInterval(AutoSyncInterval.MINUTES_15)
        advanceUntilIdle()

        viewModel.updateAutoSyncInterval(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.autoSyncInterval)
    }

    // ========== 缓存管理测试 ==========

    @Test
    fun `should calculate cache size`() = runTest {
        // 创建一些测试缓存文件
        val cacheFile1 = File(diskCacheDir, "test1.jpg")
        val cacheFile2 = File(diskCacheDir, "test2.jpg")
        cacheFile1.writeBytes(ByteArray(1024 * 1024)) // 1MB
        cacheFile2.writeBytes(ByteArray(1024 * 1024)) // 1MB

        viewModel.calculateCacheSize()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.cacheSizeBytes > 0)
    }

    @Test
    fun `should clear thumbnail cache`() = runTest {
        // 创建一些测试缓存文件
        val cacheFile1 = File(diskCacheDir, "test1.jpg")
        val cacheFile2 = File(diskCacheDir, "test2.jpg")
        cacheFile1.writeBytes(ByteArray(1024 * 1024)) // 1MB
        cacheFile2.writeBytes(ByteArray(1024 * 1024)) // 1MB

        viewModel.clearThumbnailCache()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0L, state.cacheSizeBytes)
        assertTrue(state.cacheCleared)

        // 验证文件已删除
        assertFalse(cacheFile1.exists())
        assertFalse(cacheFile2.exists())
    }

    @Test
    fun `should reset cache cleared state`() = runTest {
        viewModel.clearThumbnailCache()
        advanceUntilIdle()

        viewModel.resetCacheCleared()

        val state = viewModel.uiState.value
        assertFalse(state.cacheCleared)
    }

    // ========== 恢复默认设置测试 ==========

    @Test
    fun `should reset all settings to default`() = runTest {
        // 先修改一些设置
        viewModel.updateThemeMode(ThemeMode.DARK)
        viewModel.updatePageDirection(PageDirection.LEFT_TO_RIGHT)
        viewModel.updateCacheLimit(1000)
        advanceUntilIdle()

        // 恢复默认
        viewModel.resetToDefaults()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(PageDirection.RIGHT_TO_LEFT, state.pageDirection)
        assertEquals(DoublePageMode.AUTO, state.doublePageMode)
        assertTrue(state.crossChapter)
        assertEquals(500, state.cacheLimitMB)
        assertEquals(4, state.thumbnailConcurrency)
        assertNull(state.autoSyncInterval)
    }

    // ========== 错误处理测试 ==========

    @Test
    fun `should clear error state`() = runTest {
        viewModel.updateCacheLimit(100) // 触发错误
        advanceUntilIdle()

        viewModel.clearError()

        val state = viewModel.uiState.value
        assertNull(state.error)
    }
}
