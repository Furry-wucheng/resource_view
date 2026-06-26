# 03 — Koin DI 契约

> 🔵 定义谁提供什么、谁消费什么。所有 Agent 以此为集成契约。
> 决策：Kotlin 2.4 当前与 Hilt/KSP 组合存在兼容风险，本项目实际采用 Koin。

---

## 核心原则

1. **集中注册 Module**：`ResourceViewerApp` 在 `startKoin` 中加载所有模块。
2. **`single` 作用域**：数据库、Repository、缓存、UseCase 等全局共享实例。
3. **`viewModel` DSL**：ViewModel 由 Koin 创建，带路由参数的 ViewModel 使用 `parametersOf(...)`。

---

## Module 文件清单

| Module 文件 | Owner Stage | 提供内容 |
|------------|-------------|---------|
| `di/DatabaseModule.kt` | M02 → M08 | AppDatabase, 各 DAO, DatabaseBackupManager |
| `di/RepositoryModule.kt` | M10+ | 各 Repository, UseCase, ThumbnailRepository |
| `di/SecurePrefsModule.kt` | M10 | SecurePrefs |
| `di/SmbModule.kt` | M17 | SmbClientWrapper |
| `di/ViewModelModule.kt` | M03+ | 所有 ViewModel |
| `di/CoilModule.kt` | M23 | ImageLoader |

---

## DatabaseModule 契约

```kotlin
val databaseModule = module {
    single<AppDatabase> { Room.databaseBuilder(get(), AppDatabase::class.java, AppDatabase.DATABASE_NAME).build() }
    single { get<AppDatabase>().sourceDao() }
    single { get<AppDatabase>().resourceDao() }
    single { get<AppDatabase>().tagDao() }
    single { get<AppDatabase>().resourceTagDao() }
    single { get<AppDatabase>().appConfigDao() }
    single { DatabaseBackupManager(get(), get()) }
}
```

## RepositoryModule 契约

```kotlin
val repositoryModule = module {
    single { SourceRepository(get(), get()) }
    single { ResourceRepository(get(), get(), get()) }
    single { TagRepository(get(), get()) }
    single { FilesystemRepository(get(), get()) }
    single { DetectOrganizationModeUseCase() }
    single { BatchAddResourcesUseCase(get(), get()) }
    single { ScanResourcesUseCase(get(), get()) }
    single { SplitResourceUseCase(get()) }
    single { ThumbnailRepository(setOf(VideoThumbnailGenerator(), PdfThumbnailGenerator(get()), ImageThumbnailGenerator(get()))) }
}
```

## ViewModelModule 契约

```kotlin
val viewModelModule = module {
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
    viewModel { TagViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { SourceListViewModel(get(), get(), get()) }
    viewModel { (sourceId: String) -> FileBrowserViewModel(sourceId, get(), get()) }
    viewModel { (resourceId: String) -> ViewerViewModel(resourceId, get(), get(), get()) }
    viewModel { (resourceId: String) -> ChapterListViewModel(resourceId, get(), get()) }
}
```

---

## Consumer 清单

| Consumer (ViewModel) | 消费的 Repository / UseCase | Stage |
|---------------------|------------------------------|-------|
| `HomeViewModel` | ResourceRepository, TagRepository, ResourceTagDao | M23 |
| `SourceListViewModel` | SourceRepository, FilesystemRepository, SmbClientWrapper | M17 |
| `FileBrowserViewModel` | FilesystemRepository, BatchAddResourcesUseCase | M13/M27 |
| `ViewerViewModel` | ResourceRepository, FilesystemRepository | M14/M22 |
| `TagViewModel` | TagRepository | M15 |
| `SettingsViewModel` | AppDatabase, SecurePrefs, ImageLoader | M25 |
| `VideoPlayerViewModel` | ExoPlayer | M19 |
