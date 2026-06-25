# 01 — 数据模型

> 🔵 所有 Agent 共享，不得在 stage 开发中修改。变更需 PR review。
> 依据：`@prd/05-数据模型.md` `@tech/04-数据库设计.md`

---

## 1. Room Entity 定义

### 1.1 SourceEntity

```kotlin
@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,                    // UUID
    val name: String,                              // 用户自定义名称
    val type: SourceType,                          // LOCAL / SMB / FTP / WEBDAV
    val rootPath: String,                          // D:\Comics 或 smb://192.168.1.100/share
    val host: String? = null,
    val port: Int? = null,
    val username: String? = null,
    val passwordStored: Boolean = false,            // 密码在 EncryptedSharedPreferences
    val domain: String? = null,
    val enabled: Boolean = true,
    val isAvailable: Boolean = false,
    val lastCheckAt: Long? = null,                 // epoch ms
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
```

### 1.2 ResourceEntity

```kotlin
@Entity(
    tableName = "resources",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["sourceId", "relativePath"], unique = true),
        Index(value = ["createdAt", "id"]),
        Index(value = ["name", "id"]),
    ],
)
data class ResourceEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val name: String,
    val type: ResourceType,
    val organizationMode: OrganizationMode? = null, // null = 未判定
    val relativePath: String,
    val thumbnailPath: String? = null,
    val fileCount: Int? = null,
    val fileSize: Long? = null,
    val isAvailable: Boolean = true,
    val lastScannedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
```

### 1.3 TagEntity

```kotlin
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,                              // #RRGGBB
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
```

### 1.4 ResourceTagEntity

```kotlin
@Entity(
    tableName = "resource_tags",
    primaryKeys = ["resourceId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = ResourceEntity::class, parentColumns = ["id"], childColumns = ["resourceId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["resourceId", "tagId"]),
        Index(value = ["tagId", "resourceId"]),
    ],
)
data class ResourceTagEntity(
    val resourceId: String,
    val tagId: String,
    val createdAt: Long = System.currentTimeMillis(),
)
```

### 1.5 AppConfigEntity

```kotlin
@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1,                    // 单例，固定为 1
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pageDirection: PageDirection = PageDirection.RIGHT_TO_LEFT,
    val doublePageMode: DoublePageMode = DoublePageMode.AUTO,
    val crossChapter: Boolean = true,
    val cacheLimitMB: Int = 500,
    val thumbnailConcurrency: Int = 4,
    val autoSyncInterval: AutoSyncInterval? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
```

---

## 2. Enum 定义

```kotlin
enum class SourceType { LOCAL, SMB, FTP, WEBDAV }
enum class ResourceType { FOLDER, PDF, ARCHIVE, VIDEO }
enum class OrganizationMode { CHAPTER, CHAPTER_GALLERY, FLATGRID, GALLERY }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class PageDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL }
enum class DoublePageMode { AUTO, SINGLE, DOUBLE }
enum class AutoSyncInterval { OFF, MINUTES_15, MINUTES_30, HOUR_1 }
```

---

## 3. Domain Model 定义

> Domain Model 是 Repository 对外暴露的类型，Entity 仅在 Data 层内部使用。

```kotlin
data class Source(
    val id: String,
    val name: String,
    val type: SourceType,
    val rootPath: String,
    val host: String? = null,
    val port: Int? = null,
    val username: String? = null,
    val passwordStored: Boolean = false,
    val domain: String? = null,
    val enabled: Boolean = true,
    val isAvailable: Boolean = false,
    val lastCheckAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Resource(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val name: String,
    val type: ResourceType,
    val organizationMode: OrganizationMode?,
    val relativePath: String,
    val thumbnailPath: String?,
    val fileCount: Int?,
    val fileSize: Long?,
    val isAvailable: Boolean,
    val lastScannedAt: Long?,
    val tags: List<Tag> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
)

data class Tag(
    val id: String,
    val name: String,
    val color: String,
    val isBuiltIn: Boolean = false,
    val resourceCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

data class AppConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pageDirection: PageDirection = PageDirection.RIGHT_TO_LEFT,
    val doublePageMode: DoublePageMode = DoublePageMode.AUTO,
    val crossChapter: Boolean = true,
    val cacheLimitMB: Int = 500,
    val thumbnailConcurrency: Int = 4,
    val autoSyncInterval: AutoSyncInterval? = null,
)
```

---

## 4. ViewerItem 联合类型

```kotlin
sealed class ViewerItem {
    abstract val title: String

    /**
     * 图片/PDF 页。
     * 不存储 lambda，ViewModel 通过 contentProvider.loadPage(pageIndex, w, h) 加载。
     */
    data class ImagePage(
        override val title: String,
        val pageIndex: Int,
        /** ContentProvider 实例标识，ViewModel 用于查找对应 Provider 加载 */
        val providerKey: String = "",
    ) : ViewerItem()

    data class Video(
        override val title: String,
        val videoSource: VideoMediaSource,
    ) : ViewerItem()
}

sealed class VideoMediaSource {
    data class LocalFile(val path: String) : VideoMediaSource()
    data class SmbFile(
        val fileSource: FileSource,
        val relativePath: String,
        val fileSize: Long,
    ) : VideoMediaSource()
}
```

---

## 5. 内置标签常量

```kotlin
object BuiltInTags {
    const val FAVORITES_ID = "00000000-0000-0000-0000-000000000001"
    const val FAVORITES_NAME = "收藏"
    const val FAVORITES_COLOR = "#FFC107"
}
```

---

## 6. 实体映射约定

- Entity → Domain: 在 Repository 中进行（可写扩展函数 `toDomain()`）
- Entity 仅在 `data/local/entity/` 中定义
- Domain Model 在 `domain/model/` 中定义
- 跨层传递只使用 Domain Model，不泄漏 Entity

---

## 7. 注意事项

1. **UUID 使用 String 存储**，key set 分页时需配合时间戳
2. **密码不入库**：`passwordStored` 仅标记存在性，实际密码在 `EncryptedSharedPreferences`
3. **`organizationMode = null`** 表示未判定，由 `DetectOrganizationModeUseCase` 异步填充
4. **级联删除**：删除 Source → 级联删除所有 Resource、ResourceTag；删除 Tag → 级联删除 ResourceTag
