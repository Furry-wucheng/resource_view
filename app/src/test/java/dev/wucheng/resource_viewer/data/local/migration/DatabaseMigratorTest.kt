package dev.wucheng.resource_viewer.data.local.migration

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DatabaseMigratorTest {
    private lateinit var context: Context
    private lateinit var migrator: DatabaseMigrator

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        migrator = DatabaseMigrator(context)
    }

    @Test
    fun `getMigrations should return non-empty array`() {
        val migrations = migrator.getMigrations()

        assertNotNull(migrations)
        assertTrue(migrations.isNotEmpty())
    }

    @Test
    fun `getCurrentVersion should return positive version`() {
        val version = migrator.getCurrentVersion()

        assertTrue(version > 0)
    }

    @Test
    fun `validateSchema should return Error for non-existent schema`() = runTest {
        // 当没有导出的 schema 文件时，应该返回 Error
        val result = migrator.validateSchema(1)

        // 由于在单元测试环境中没有实际的 schema 文件，应该返回 Error
        assertTrue(result is SchemaValidationResult.Error)
    }

    @Test
    fun `Migration 1_2 should be defined`() {
        val migrations = migrator.getMigrations()
        val migration1to2 = migrations.firstOrNull { it.startVersion == 1 && it.endVersion == 2 }

        assertNotNull(migration1to2)
    }
}
