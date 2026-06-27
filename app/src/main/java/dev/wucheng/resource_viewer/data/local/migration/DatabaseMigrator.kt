package dev.wucheng.resource_viewer.data.local.migration

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.wucheng.resource_viewer.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * 数据库迁移管理器
 * 提供数据库迁移管理和 Schema 校验功能
 */
class DatabaseMigrator(
    private val context: Context,
) {
    /**
     * 获取所有已定义的迁移
     * 在此添加从 v1 开始的迁移
     */
    fun getMigrations(): Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        // 后续迁移在此追加
    )

    /**
     * 获取当前数据库版本
     */
    fun getCurrentVersion(): Int = AppDatabase.DATABASE_VERSION

    /**
     * 读取导出的 Schema JSON 文件
     * @param version 数据库版本号
     * @return Schema JSON 内容，如果文件不存在返回 null
     */
    fun readExportedSchema(version: Int): String? {
        val schemaFile = File(
            context.filesDir.parentFile?.parentFile,
            "schemas/$version/${AppDatabase.DATABASE_NAME}/$version.json"
        )
        return if (schemaFile.exists()) schemaFile.readText() else null
    }

    /**
     * 校验 Schema 完整性
     * @param version 要校验的版本号
     * @return 校验结果
     */
    suspend fun validateSchema(version: Int): SchemaValidationResult = withContext(Dispatchers.IO) {
        try {
            val schemaJson = readExportedSchema(version)
                ?: return@withContext SchemaValidationResult.Error(
                    "Schema 文件不存在: version $version"
                )

            val schema = JSONObject(schemaJson)
            val tables = schema.getJSONArray("database").let { db ->
                (0 until db.length()).map { db.getJSONObject(it) }
            }

            // 验证必需的表是否存在
            val requiredTables = setOf(
                "sources", "resources", "tags", "resource_tags", "app_config"
            )

            val existingTables = tables.map { it.getString("tableName") }.toSet()
            val missingTables = requiredTables - existingTables

            if (missingTables.isNotEmpty()) {
                return@withContext SchemaValidationResult.Error(
                    "缺少必需的表: $missingTables"
                )
            }

            // 验证每个表的列信息
            val tableDetails = tables.associate { table ->
                val tableName = table.getString("tableName")
                val columns = table.getJSONArray("columns").let { cols ->
                    (0 until cols.length()).map { cols.getJSONObject(it) }
                }
                tableName to columns.map { it.getString("name") }
            }

            SchemaValidationResult.Success(
                version = version,
                tables = existingTables,
                tableDetails = tableDetails,
            )
        } catch (e: Exception) {
            SchemaValidationResult.Error("Schema 校验失败: ${e.message}")
        }
    }

    /**
     * 获取数据库统计信息
     * @param database 数据库实例
     * @return 数据库统计信息
     */
    suspend fun getDatabaseStats(database: AppDatabase): DatabaseStats =
        withContext(Dispatchers.IO) {
            val db = database.openHelper.readableDatabase

            val sourceCount = db.query("SELECT COUNT(*) FROM sources").use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0
            }

            val resourceCount = db.query("SELECT COUNT(*) FROM resources").use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0
            }

            val tagCount = db.query("SELECT COUNT(*) FROM tags").use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0
            }

            val resourceTagCount = db.query("SELECT COUNT(*) FROM resource_tags").use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0
            }

            DatabaseStats(
                sourceCount = sourceCount,
                resourceCount = resourceCount,
                tagCount = tagCount,
                resourceTagCount = resourceTagCount,
            )
        }

    companion object {
        /**
         * Migration 1 -> 2
         * M12: 添加隐私政策同意字段
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_config ADD COLUMN hasAcceptedPrivacy INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Migration 2 -> 3
         * 添加收藏字段到 resources 表
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE resources ADD COLUMN favorited INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}

/**
 * Schema 校验结果
 */
sealed class SchemaValidationResult {
    data class Success(
        val version: Int,
        val tables: Set<String>,
        val tableDetails: Map<String, List<String>>,
    ) : SchemaValidationResult()

    data class Error(val message: String) : SchemaValidationResult()
}

/**
 * 数据库统计信息
 */
data class DatabaseStats(
    val sourceCount: Long,
    val resourceCount: Long,
    val tagCount: Long,
    val resourceTagCount: Long,
)
