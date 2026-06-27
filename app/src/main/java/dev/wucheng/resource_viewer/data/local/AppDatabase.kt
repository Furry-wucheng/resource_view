package dev.wucheng.resource_viewer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.wucheng.resource_viewer.data.local.converter.Converters
import dev.wucheng.resource_viewer.data.local.dao.AppConfigDao
import dev.wucheng.resource_viewer.data.local.dao.ResourceDao
import dev.wucheng.resource_viewer.data.local.dao.ResourceTagDao
import dev.wucheng.resource_viewer.data.local.dao.SourceDao
import dev.wucheng.resource_viewer.data.local.dao.TagDao
import dev.wucheng.resource_viewer.data.local.entity.AppConfigEntity
import dev.wucheng.resource_viewer.data.local.entity.ResourceEntity
import dev.wucheng.resource_viewer.data.local.entity.ResourceTagEntity
import dev.wucheng.resource_viewer.data.local.entity.SourceEntity
import dev.wucheng.resource_viewer.data.local.entity.TagEntity

@Database(
    entities = [
        SourceEntity::class,
        ResourceEntity::class,
        TagEntity::class,
        ResourceTagEntity::class,
        AppConfigEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun resourceDao(): ResourceDao
    abstract fun tagDao(): TagDao
    abstract fun resourceTagDao(): ResourceTagDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        const val DATABASE_NAME = "resource_viewer.db"
        const val DATABASE_VERSION = 5

        /** Migration from version 2 to 3: Add favorited column to resources */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE resources ADD COLUMN favorited INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Migration from version 3 to 4: Add cache limit columns to app_config */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_config ADD COLUMN coverCacheLimitMB INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_config ADD COLUMN pageCacheLimitMB INTEGER NOT NULL DEFAULT 500")
                db.execSQL("ALTER TABLE app_config ADD COLUMN thumbnailCacheLimitMB INTEGER NOT NULL DEFAULT 500")
            }
        }

        /** Migration from version 4 to 5: Add showDirectoryTree column to app_config */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_config ADD COLUMN showDirectoryTree INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
