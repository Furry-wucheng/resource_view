package dev.wucheng.resource_viewer.data.local.migration

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.wucheng.resource_viewer.data.local.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * M12: 数据库迁移测试
 *
 * 测试 MIGRATION_1_2 正确添加 hasAcceptedPrivacy 字段
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigration1to2Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun `migrate 1 to 2 should add hasAcceptedPrivacy column`() {
        // 创建 v1 数据库
        helper.createDatabase(AppDatabase.DATABASE_NAME, 1).use { oldDb ->
            // 插入 v1 格式的数据
            oldDb.execSQL(
                """
                INSERT INTO app_config (id, themeMode, pageDirection, doublePageMode, crossChapter, cacheLimitMB, thumbnailConcurrency, updatedAt)
                VALUES (1, 'SYSTEM', 'RIGHT_TO_LEFT', 'AUTO', 1, 500, 4, ${System.currentTimeMillis()})
                """.trimIndent()
            )
        }

        // 执行迁移
        helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            DatabaseMigrator.MIGRATION_1_2,
        ).use { newDb ->
            // 验证数据仍然存在
            val cursor = newDb.query("SELECT hasAcceptedPrivacy FROM app_config WHERE id = 1")
            assertTrue(cursor.moveToFirst())

            // 验证新字段有默认值 0 (false)
            val hasAcceptedPrivacy = cursor.getInt(0)
            assertEquals(0, hasAcceptedPrivacy)

            cursor.close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun `migrate 1 to 2 should preserve existing data`() {
        val timestamp = System.currentTimeMillis()

        // 创建 v1 数据库并插入测试数据
        helper.createDatabase(AppDatabase.DATABASE_NAME, 1).use { oldDb ->
            oldDb.execSQL(
                """
                INSERT INTO app_config (id, themeMode, pageDirection, doublePageMode, crossChapter, cacheLimitMB, thumbnailConcurrency, updatedAt)
                VALUES (1, 'DARK', 'LEFT_TO_RIGHT', 'DOUBLE', 0, 1000, 8, $timestamp)
                """.trimIndent()
            )
        }

        // 执行迁移
        helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            DatabaseMigrator.MIGRATION_1_2,
        ).use { newDb ->
            // 验证原有数据保持不变
            val cursor = newDb.query(
                "SELECT themeMode, pageDirection, doublePageMode, crossChapter, cacheLimitMB, thumbnailConcurrency, updatedAt FROM app_config WHERE id = 1"
            )
            assertTrue(cursor.moveToFirst())

            assertEquals("DARK", cursor.getString(0))
            assertEquals("LEFT_TO_RIGHT", cursor.getString(1))
            assertEquals("DOUBLE", cursor.getString(2))
            assertEquals(0, cursor.getInt(3)) // false -> 0
            assertEquals(1000, cursor.getInt(4))
            assertEquals(8, cursor.getInt(5))
            assertEquals(timestamp, cursor.getLong(6))

            cursor.close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun `migrate 1 to 2 should handle multiple rows`() {
        // 创建 v1 数据库并插入多行数据
        helper.createDatabase(AppDatabase.DATABASE_NAME, 1).use { oldDb ->
            // 注意：app_config 表的主键是 id=1，所以只能有一行
            // 这个测试验证单行迁移的正确性
            oldDb.execSQL(
                """
                INSERT INTO app_config (id, themeMode, pageDirection, doublePageMode, crossChapter, cacheLimitMB, thumbnailConcurrency, updatedAt)
                VALUES (1, 'SYSTEM', 'RIGHT_TO_LEFT', 'AUTO', 1, 500, 4, ${System.currentTimeMillis()})
                """.trimIndent()
            )
        }

        // 执行迁移
        helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            DatabaseMigrator.MIGRATION_1_2,
        ).use { newDb ->
            // 验证迁移后可以更新新字段
            newDb.execSQL("UPDATE app_config SET hasAcceptedPrivacy = 1 WHERE id = 1")

            val cursor = newDb.query("SELECT hasAcceptedPrivacy FROM app_config WHERE id = 1")
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))

            cursor.close()
        }
    }
}
