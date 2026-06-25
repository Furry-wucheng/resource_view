package dev.wucheng.resource_viewer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 1,
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
        const val DATABASE_VERSION = 1
    }
}
