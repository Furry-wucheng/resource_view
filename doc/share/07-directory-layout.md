# 07 — 目录结构与文件布局

> 🔵 定义包目录结构和文件命名约定。所有 Agent 的代码产出必须遵循此布局。
> 依据：`@tech/02-架构设计.md` §3

---

## 包结构

```
app/src/main/java/dev/wucheng/resource_viewer/
├── ResourceViewerApp.kt                 # M02 @HiltAndroidApp
├── MainActivity.kt                      # M02 @AndroidEntryPoint
│
├── di/                                  # Hilt Module (独立文件，各自拥有)
│   ├── DatabaseModule.kt               # M02(骨架) → M08(完成)
│   ├── RepositoryModule.kt             # M10
│   ├── SecurePrefsModule.kt           # M10
│   ├── SmbModule.kt                   # M17 独占
│   └── CoilModule.kt                  # M23 独占
│
├── data/
│   ├── local/                           # Room 持久化
│   │   ├── AppDatabase.kt             # M08 聚合文件
│   │   ├── entity/                     # Entity 定义
│   │   │   ├── SourceEntity.kt        # M06
│   │   │   ├── ResourceEntity.kt      # M06
│   │   │   ├── TagEntity.kt           # M06
│   │   │   ├── ResourceTagEntity.kt   # M06
│   │   │   └── AppConfigEntity.kt     # M06
│   │   ├── dao/                        # DAO 接口
│   │   │   ├── SourceDao.kt           # M07
│   │   │   ├── ResourceDao.kt         # M07
│   │   │   ├── TagDao.kt             # M07
│   │   │   ├── ResourceTagDao.kt      # M07
│   │   │   └── AppConfigDao.kt        # M07
│   │   ├── converter/
│   │   │   └── Converters.kt          # M06
│   │   └── secure/
│   │       └── SecurePrefs.kt         # M10
│   │
│   ├── remote/                          # 外部数据源
│   │   ├── smb/
│   │   │   ├── SmbClientWrapper.kt     # M17
│   │   │   └── SmbDataSource.kt        # M18
│   │   ├── pdf/
│   │   │   └── PdfRenderer.kt         # M22
│   │   └── archive/
│   │       └── ArchiveDataSource.kt    # P2
│   │
│   └── repository/                      # Repository 实现
│       ├── SourceRepository.kt         # M10
│       ├── ResourceRepository.kt       # M10
│       ├── TagRepository.kt           # M10
│       ├── FilesystemRepository.kt     # M10
│       └── ThumbnailRepository.kt      # M10
│
├── domain/
│   ├── model/                           # 领域模型
│   │   ├── Source.kt                   # M09
│   │   ├── Resource.kt                # M09
│   │   ├── Tag.kt                     # M09
│   │   ├── FileEntry.kt              # M09
│   │   ├── Chapter.kt                # M09
│   │   ├── ViewerItem.kt             # M09
│   │   └── AppConfig.kt              # M09
│   └── usecase/                         # Use Case
│       ├── ScanResourcesUseCase.kt     # M27
│       ├── DetectOrganizationModeUseCase.kt  # M21
│       ├── SplitResourceUseCase.kt     # M27
│       ├── FilterResourcesByTagsUseCase.kt   # M16
│       └── BatchAddResourcesUseCase.kt # M27
│
├── ui/
│   ├── navigation/                      # 导航
│   │   ├── Screen.kt                   # M03 (聚合)
│   │   ├── AppNavGraph.kt             # M03 (聚合)
│   │   └── BottomNavBar.kt            # M03
│   ├── theme/                           # 主题
│   │   ├── Theme.kt                    # M01
│   │   ├── Color.kt                   # M01
│   │   ├── Type.kt                    # M01
│   │   └── Shape.kt                   # M01
│   ├── components/                      # 共享组件
│   │   ├── AppShell.kt                # M04
│   │   ├── FilterBar.kt              # M16
│   │   ├── ResourceGridItem.kt        # M23
│   │   ├── EmptyState.kt             # M05
│   │   ├── ErrorView.kt              # M26
│   │   └── ResourcePickerDialog.kt    # M24
│   └── screens/                         # 各功能页面
│       ├── home/
│       │   ├── HomeScreen.kt          # M05(骨架) → M23(完整)
│       │   └── HomeViewModel.kt        # M23
│       ├── sources/
│       │   ├── SourceListScreen.kt     # M05(骨架) → M17
│       │   ├── FileBrowserScreen.kt    # M13
│       │   ├── SourceListViewModel.kt  # M17
│       │   └── FileBrowserViewModel.kt # M13
│       ├── viewer/
│       │   ├── ViewerScreen.kt         # M14(骨架) → M22
│       │   ├── ChapterListScreen.kt    # M21
│       │   ├── ViewerViewModel.kt      # M14
│       │   └── components/
│       │       ├── SlideBar.kt         # M14
│       │       ├── ViewerToolbar.kt    # M14
│       │       └── VideoPlayer.kt      # M19
│       ├── tags/
│       │   ├── TagManagerScreen.kt     # M15
│       │   ├── TagEditorDialog.kt      # M15
│       │   └── TagViewModel.kt        # M15
│       └── settings/
│           ├── SettingsScreen.kt       # M05(骨架) → M25
│           └── SettingsViewModel.kt    # M25
│
└── shared/                              # 跨层接口
    ├── filesource/
    │   ├── FileSource.kt              # M11
    │   ├── LocalFileSource.kt         # M12
    │   ├── SmbFileSource.kt           # M17
    │   └── FileSourceFactory.kt       # M11
    ├── content/
    │   ├── ContentProvider.kt         # M11
    │   ├── ImageFolderProvider.kt     # M14
    │   └── PdfContentProvider.kt      # M22
    ├── organization/
    │   ├── OrganizationStrategy.kt    # M11
    │   ├── FlatGridStrategy.kt        # M20
    │   ├── GalleryStrategy.kt         # M20
    │   ├── ChapterStrategy.kt         # M21
    │   └── ChapterGalleryStrategy.kt  # M21
    └── thumbnail/
        ├── ThumbnailGenerator.kt      # M11
        ├── ImageThumbnailGenerator.kt # M23
        ├── PdfThumbnailGenerator.kt   # M22
        └── VideoThumbnailGenerator.kt # M19
```

## 测试目录

```
app/src/test/java/dev/wucheng/resource_viewer/       # 纯单元测试（无 Android 依赖）
├── domain/model/         # Domain Model 测试
├── shared/               # 纯逻辑接口测试
└── helpers/              # TestData, TestDatabase

app/src/androidTest/java/dev/wucheng/resource_viewer/ # 需要 Android 框架的测试
├── data/repository/      # Room in-memory 测试
├── data/usecase/         # UseCase 测试
├── ui/viewmodel/         # ViewModel 测试 (runTest)
├── ui/component/         # Compose UI 测试 (createComposeRule)
└── database/             # 迁移测试
```

## 命名约定

| 类型 | 模式 | 示例 |
|------|------|------|
| Entity | `${Name}Entity` | `SourceEntity` |
| DAO | `${Name}Dao` | `SourceDao` |
| Domain Model | `${Name}` | `Source` |
| Repository | `${Name}Repository` | `SourceRepository` |
| UseCase | `${Verb}${Noun}UseCase` | `FilterResourcesByTagsUseCase` |
| ViewModel | `${Screen}ViewModel` | `HomeViewModel` |
| Screen | `${Screen}Screen` | `HomeScreen` |
| Component | `${ComponentName}` | `FilterBar` |
| Strategy | `${Mode}Strategy` | `ChapterStrategy` |
| Module | `${Feature}Module` | `SmbModule` |
