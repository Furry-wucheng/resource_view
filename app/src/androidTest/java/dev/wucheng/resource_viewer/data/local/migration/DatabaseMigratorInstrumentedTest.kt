package dev.wucheng.resource_viewer.data.local.migration

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.wucheng.resource_viewer.data.local.AppDatabase
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.local.converter.SourceType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigratorInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var migrator: DatabaseMigrator

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        migrator = DatabaseMigrator(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getCurrentVersion_shouldReturnPositiveVersion() {
        val version = migrator.getCurrentVersion()
        assertTrue(version > 0)
    }

    @Test
    fun getMigrations_shouldReturnNonEmptyArray() {
        val migrations = migrator.getMigrations()
        assertTrue(migrations.isNotEmpty())
    }

    @Test
    fun getDatabaseStats_shouldReturnCorrectCounts() = runTest {
        // 插入测试数据
        val source = SourceEntity(
            id = "test-source-1",
            name = "Test Source",
            type = SourceType.LOCAL,
            rootPath = "/test/path"
        )
        database.sourceDao().insert(source)

        // 获取统计信息
        val stats = migrator.getDatabaseStats(database)

        assertEquals(1L, stats.sourceCount)
        assertEquals(0L, stats.resourceCount)
        assertEquals(0L, stats.tagCount)
        assertEquals(0L, stats.resourceTagCount)
    }

    @Test
    fun validateSchema_shouldReturnErrorForNonExistentSchema() = runTest {
        // 在测试环境中，没有导出的 schema 文件
        val result = migrator.validateSchema(1)
        assertTrue(result is SchemaValidationResult.Error)
    }
}
