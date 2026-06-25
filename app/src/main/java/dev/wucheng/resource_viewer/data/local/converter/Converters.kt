package dev.wucheng.resource_viewer.data.local.converter

import androidx.room.TypeConverter

enum class SourceType { LOCAL, SMB, FTP, WEBDAV }
enum class ResourceType { FOLDER, PDF, ARCHIVE, VIDEO }
enum class OrganizationMode { CHAPTER, CHAPTER_GALLERY, FLATGRID, GALLERY }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class PageDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL }
enum class DoublePageMode { AUTO, SINGLE, DOUBLE }
enum class AutoSyncInterval { OFF, MINUTES_15, MINUTES_30, HOUR_1 }

class Converters {
    // SourceType
    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String?): SourceType? =
        value?.let { SourceType.entries.find { e -> e.name == it } }

    // ResourceType
    @TypeConverter
    fun fromResourceType(value: ResourceType): String = value.name

    @TypeConverter
    fun toResourceType(value: String?): ResourceType? =
        value?.let { ResourceType.entries.find { e -> e.name == it } }

    // OrganizationMode
    @TypeConverter
    fun fromOrganizationMode(value: OrganizationMode): String = value.name

    @TypeConverter
    fun toOrganizationMode(value: String?): OrganizationMode? =
        value?.let { OrganizationMode.entries.find { e -> e.name == it } }

    // ThemeMode
    @TypeConverter
    fun fromThemeMode(value: ThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String?): ThemeMode? =
        value?.let { ThemeMode.entries.find { e -> e.name == it } }

    // PageDirection
    @TypeConverter
    fun fromPageDirection(value: PageDirection): String = value.name

    @TypeConverter
    fun toPageDirection(value: String?): PageDirection? =
        value?.let { PageDirection.entries.find { e -> e.name == it } }

    // DoublePageMode
    @TypeConverter
    fun fromDoublePageMode(value: DoublePageMode): String = value.name

    @TypeConverter
    fun toDoublePageMode(value: String?): DoublePageMode? =
        value?.let { DoublePageMode.entries.find { e -> e.name == it } }

    // AutoSyncInterval
    @TypeConverter
    fun fromAutoSyncInterval(value: AutoSyncInterval): String = value.name

    @TypeConverter
    fun toAutoSyncInterval(value: String?): AutoSyncInterval? =
        value?.let { AutoSyncInterval.entries.find { e -> e.name == it } }
}
