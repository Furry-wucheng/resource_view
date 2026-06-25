# 03 — Hilt DI 契约

> 🔵 定义谁提供什么、谁消费什么。所有 Agent 以此为集成契约。
> 依据：`@tech/02-架构设计.md` §6

---

## 核心原则

1. **每个 feature 使用独立的 Hilt Module 文件**，Hilt 编译时自动聚合所有 `@Module`，无需修改中心化 DI 文件
2. **`@Singleton` 作用域**：数据库、Repository、缓存等全局共享实例
3. **`@HiltViewModel`**：ViewModel 由 Hilt 自动注入，不手动创建

---

## Module 文件清单

| Module 文件 | Owner Stage | 提供内容 |
|------------|-------------|---------|
| `di/DatabaseModule.kt` | M02 → M08 | AppDatabase, 各 DAO |
| `di/RepositoryModule.kt` | M10 | 各 Repository |
| `di/SecurePrefsModule.kt` | M10 | SecurePrefs |
| `di/LocalSourceModule.kt` | M12 | LocalFileSource |
| `di/SmbModule.kt` | M17 | SmbClientWrapper, SmbFileSource |
| `di/MediaModule.kt` | M19 | ExoPlayer (可选按需) |
| `di/CoilModule.kt` | M23 | ImageLoader (单例) |

---

## DatabaseModule 契约

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase

    @Provides fun provideSourceDao(db: AppDatabase): SourceDao
    @Provides fun provideResourceDao(db: AppDatabase): ResourceDao
    @Provides fun provideTagDao(db: AppDatabase): TagDao
    @Provides fun provideResourceTagDao(db: AppDatabase): ResourceTagDao
    @Provides fun provideAppConfigDao(db: AppDatabase): AppConfigDao
}
```

## RepositoryModule 契约

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides @Singleton
    fun provideSourceRepository(sourceDao: SourceDao, securePrefs: SecurePrefs): SourceRepository
    @Provides @Singleton
    fun provideResourceRepository(resourceDao: ResourceDao, tagDao: TagDao, resourceTagDao: ResourceTagDao): ResourceRepository
    @Provides @Singleton
    fun provideTagRepository(tagDao: TagDao, resourceTagDao: ResourceTagDao): TagRepository
    @Provides @Singleton
    fun provideFilesystemRepository(sourceDao: SourceDao, securePrefs: SecurePrefs): FilesystemRepository
    @Provides @Singleton
    fun provideThumbnailRepository(thumbnailGenerators: Set<ThumbnailGenerator>): ThumbnailRepository
}
```

## SecurePrefsModule 契约

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SecurePrefsModule {
    @Provides @Singleton
    fun provideSecurePrefs(@ApplicationContext context: Context): SecurePrefs {
        return SecurePrefs(EncryptedSharedPreferences.create(...))
    }
}
```

## Feature Module 范例

```kotlin
// 每个 feature 自己管理自己的 Module
@Module
@InstallIn(SingletonComponent::class)
object SmbModule {
    @Provides @Singleton
    fun provideSmbClientWrapper(): SmbClientWrapper
}

// ViewModel 注入范例
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val filesystemRepository: FilesystemRepository,
    private val sourceRepository: SourceRepository,
) : ViewModel()
```

---

## Consumer 清单

| Consumer (ViewModel) | 消费的 Repository | Stage |
|---------------------|-------------------|-------|
| `HomeViewModel` | ResourceRepository, TagRepository | M23 |
| `SourceListViewModel` | SourceRepository | M05(骨架) → M17 |
| `FileBrowserViewModel` | FilesystemRepository, SourceRepository | M13 |
| `ViewerViewModel` | ResourceRepository, FileSourceFactory | M14 |
| `TagViewModel` | TagRepository, ResourceTagDao | M15 |
| `SettingsViewModel` | AppConfigDao, ThumbnailRepository | M25 |
| `VideoPlayerViewModel` | FileSourceFactory (for SMB) | M19 |
