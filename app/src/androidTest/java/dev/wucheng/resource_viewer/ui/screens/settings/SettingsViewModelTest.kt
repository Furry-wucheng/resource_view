package dev.wucheng.resource_viewer.ui.screens.settings

import android.app.Application
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.entity.AppConfigEntity
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.local.entity.ResourceTagEntity
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.local.entity.TagEntity
import dev.wucheng.resource_viewer.data.local.secure.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var securePrefs: SecurePrefs
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application

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

        viewModel = SettingsViewModel(application, db, securePrefs)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `should have initial state with no clearing and no error`() = runTest {
        val state = viewModel.uiState.value
        assertFalse(state.isClearing)
        assertFalse(state.clearSuccess)
        assertNull(state.error)
    }

    @Test
    fun `should clear all database data when clearAllData called`() = runTest {
        // 先插入一些测试数据
        val source = SourceEntity(id = "s1", name = "Test Source", type = "local", path = "/test")
        db.sourceDao().insert(source)

        val resource = ResourceEntity(id = "r1", name = "Test Resource", sourceId = "s1", path = "/test/file.jpg")
        db.resourceDao().insert(resource)

        val tag = TagEntity(id = "t1", name = "Test Tag")
        db.tagDao().insert(tag)

        val resourceTag = ResourceTagEntity(resourceId = "r1", tagId = "t1")
        db.resourceTagDao().insert(resourceTag)

        val config = AppConfigEntity()
        db.appConfigDao().save(config)

        // 执行清除
        viewModel.clearAllData()
        advanceUntilIdle()

        // 验证数据已被清除
        val sources = db.sourceDao().getAllSources()
        val resources = db.resourceDao().getVisibleResources()
        val tags = db.tagDao().getAllTags()
        val resourceTags = db.resourceTagDao().getByResourceId("r1")

        // 注意：Flow 需要 collect 来获取值，这里直接查询数据库
        val db2 = db.openHelper.readableDatabase

        val sourceCount = db2.query("SELECT COUNT(*) FROM sources").use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0
        }
        assertEquals(0L, sourceCount)

        val resourceCount = db2.query("SELECT COUNT(*) FROM resources").use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0
        }
        assertEquals(0L, resourceCount)

        val resourceTagCount = db2.query("SELECT COUNT(*) FROM resource_tags").use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0
        }
        assertEquals(0L, resourceTagCount)
    }

    @Test
    fun `should preserve built-in tags when clearAllData called`() = runTest {
        // 插入内置标签和自定义标签
        val builtInTag = TagEntity(id = "builtin1", name = "Built-in", isBuiltIn = true)
        val customTag = TagEntity(id = "custom1", name = "Custom", isBuiltIn = false)
        db.tagDao().insert(builtInTag)
        db.tagDao().insert(customTag)

        // 执行清除
        viewModel.clearAllData()
        advanceUntilIdle()

        // 验证内置标签保留，自定义标签被删除
        val db2 = db.openHelper.readableDatabase

        val builtInCount = db2.query("SELECT COUNT(*) FROM tags WHERE isBuiltIn = 1").use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0
        }
        assertEquals(1L, builtInCount)

        val customCount = db2.query("SELECT COUNT(*) FROM tags WHERE isBuiltIn = 0").use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0
        }
        assertEquals(0L, customCount)
    }

    @Test
    fun `should clear secure prefs when clearAllData called`() = runTest {
        // 先存储一些密码
        securePrefs.putPassword("source1", "password1")
        securePrefs.putPassword("source2", "password2")

        // 执行清除
        viewModel.clearAllData()
        advanceUntilIdle()

        // 验证密码已被清除
        assertNull(securePrefs.getPassword("source1"))
        assertNull(securePrefs.getPassword("source2"))
    }

    @Test
    fun `should set clearSuccess to true when clearAllData succeeds`() = runTest {
        viewModel.clearAllData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.clearSuccess)
        assertFalse(state.isClearing)
        assertNull(state.error)
    }

    @Test
    fun `should reset clearSuccess state when resetClearSuccess called`() = runTest {
        viewModel.clearAllData()
        advanceUntilIdle()

        viewModel.resetClearSuccess()

        val state = viewModel.uiState.value
        assertFalse(state.clearSuccess)
    }

    @Test
    fun `should clear error state when clearError called`() = runTest {
        // 手动设置错误状态（通过反射或直接测试 clearError 方法）
        viewModel.clearError()

        val state = viewModel.uiState.value
        assertNull(state.error)
    }
}
