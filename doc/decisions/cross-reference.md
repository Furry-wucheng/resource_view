# 决策日志 · 反向索引

> 按组件/决策主题倒查，快速定位设计意图。

---

## 按包/组件

| 组件 | 涉及 Stage | 决策 ID |
|------|-----------|---------|
| `ui/theme/Color.kt` | M01 | — |
| `ui/theme/Theme.kt` | M01 | — |
| `ui/navigation/Screen.kt` | M03 + 多个 | D-001 |
| `ui/navigation/AppNavGraph.kt` | M03 + 多个 | D-001 |
| `ui/navigation/BottomNavBar.kt` | M03 | D-002 |
| `data/local/AppDatabase.kt` | M08 | — |
| `data/local/converter/Converters.kt` | M06 | D-001, D-002 |
| `data/local/entity/SourceEntity.kt` | M06, M09 | — |
| `data/local/entity/ResourceEntity.kt` | M06, M09 | D-002 |
| `data/local/entity/TagEntity.kt` | M06, M09 | D-003 |
| `data/local/entity/ResourceTagEntity.kt` | M06 | — |
| `data/local/entity/AppConfigEntity.kt` | M06, M09 | — |
| `domain/model/Source.kt` | M09 | — |
| `domain/model/Resource.kt` | M09 | D-002 |
| `domain/model/Tag.kt` | M09 | D-003 |
| `domain/model/FileEntry.kt` | M09 | — |
| `domain/model/Chapter.kt` | M09 | — |
| `domain/model/ViewerItem.kt` | M09 → M19 | D-001, D-002 |
| `domain/model/AppConfig.kt` | M09 | — |
| `data/local/dao/SourceDao.kt` | M07 | D-003 |
| `data/local/dao/ResourceDao.kt` | M07 | D-001, D-002, D-003 |
| `data/local/dao/TagDao.kt` | M07 | D-003, D-004 |
| `data/local/dao/ResourceTagDao.kt` | M07 | — |
| `data/local/dao/AppConfigDao.kt` | M07 | D-003 |
| `data/repository/SourceRepository.kt` | M10 | D-001 |
| `data/repository/ResourceRepository.kt` | M10 | D-001, D-003 |
| `data/repository/TagRepository.kt` | M10 | D-001, D-004 |
| `data/repository/FilesystemRepository.kt` | M10 | D-001, D-005 |
| `data/repository/ThumbnailRepository.kt` | M10 | D-001, D-006 |
| `data/local/secure/SecurePrefs.kt` | M10, M12 | D-002 |
| `ui/components/PrivacyConsentDialog.kt` | M12 | D-002 |
| `ui/screens/settings/SettingsViewModel.kt` | M12 | D-003 |
| `di/ViewModelModule.kt` | M12 | — |
| `shared/filesource/FileSource.kt` | M11 | — |
| `shared/filesource/LocalFileSource.kt` | M12 | — |
| `shared/filesource/SmbFileSource.kt` | M17 → M30 | — |
| `shared/content/ContentProvider.kt` | M11 | — |
| `shared/content/ImageFolderProvider.kt` | M14 | D-001, D-002 |
| `shared/content/PdfContentProvider.kt` | M22 | — |
| `shared/organization/OrganizationStrategy.kt` | M11 | — |
| `shared/organization/FlatGridStrategy.kt` | M20 | — |
| `shared/organization/GalleryStrategy.kt` | M20 | — |
| `shared/organization/ChapterStrategy.kt` | M21 | — |
| `shared/organization/ChapterGalleryStrategy.kt` | M21 | — |
| `shared/thumbnail/ThumbnailGenerator.kt` | M11 | — |
| `shared/thumbnail/ImageThumbnailGenerator.kt` | M23 | — |
| `shared/thumbnail/PdfThumbnailGenerator.kt` | M22 | — |
| `shared/thumbnail/VideoThumbnailGenerator.kt` | M19 | — |
| `ui/screens/viewer/VideoPlayerViewModel.kt` | M19 | D-001, D-005 |
| `ui/screens/viewer/VideoPlayerViewModelTest.kt` | M19 | — |
| `ui/screens/viewer/components/VideoPlayer.kt` | M19 | D-004 |
| `ui/screens/home/HomeScreen.kt` | M05 → M23 | D-003 |
| `ui/screens/home/HomeViewModel.kt` | M23 | D-001 |
| `ui/components/FilterBar.kt` | M23 | D-002 |
| `ui/components/ResourceGridItem.kt` | M23 | — |
| `ui/screens/sources/SourceListScreen.kt` | M05 → M17 → M30 | D-003 |
| `ui/screens/sources/SourceListViewModel.kt` | M17 → M30 | — |
| `ui/screens/sources/FileBrowserScreen.kt` | M13 | — |
| `ui/screens/sources/FileBrowserViewModel.kt` | M13 | — |
| `ui/screens/viewer/ViewerScreen.kt` | M14 → M19 → M22 | D-003, D-004 |
| `ui/screens/viewer/ViewerViewModel.kt` | M14 → M19 | D-003 |
| `ui/screens/viewer/components/SlideBar.kt` | M14 | D-005 |
| `ui/screens/viewer/components/ViewerToolbar.kt` | M14 | — |
| `ui/screens/viewer/ChapterListScreen.kt` | M21 | — |
| `ui/screens/tags/TagManagerScreen.kt` | M15 | D-003, D-004 |
| `ui/screens/tags/TagViewModel.kt` | M15 | D-001, D-002 |
| `ui/screens/tags/TagEditorDialog.kt` | M15 | D-003 |
| `di/ViewModelModule.kt` | M15 | D-005 |
| `ui/screens/settings/SettingsScreen.kt` | M05 → M25 | D-003 |
| `ui/screens/settings/SettingsViewModel.kt` | M25 | — |
| `ui/components/FilterBar.kt` | M16 | — |
| `ui/components/ResourceGridItem.kt` | M23 | — |
| `ui/components/ResourcePickerDialog.kt` | M24 | — |
| `ui/components/ErrorView.kt` | M26 | — |
| `ui/components/EmptyState.kt` | M05 | — |
| `ui/components/AppShell.kt` | M04 → M30 | — |
| `data/remote/smb/SmbClientWrapper.kt` | M17 → M30 | — |
| `data/remote/smb/SmbDataSource.kt` | M18 | — |
| `data/remote/pdf/PdfRenderer.kt` | M22 | — |
| `domain/usecase/FilterResourcesByTagsUseCase.kt` | M16 | — |
| `domain/usecase/DetectOrganizationModeUseCase.kt` | M20 | — |
| `domain/usecase/ScanResourcesUseCase.kt` | M27 | D-001, D-002, D-003 |
| `domain/usecase/BatchAddResourcesUseCase.kt` | M27 | D-001, D-002, D-003 |
| `domain/usecase/SplitResourceUseCase.kt` | M27 | D-001, D-004 |
| `di/DatabaseModule.kt` | M02 → M08 | — |
| `di/RepositoryModule.kt` | M02 → M10 | — |
| `di/SecurePrefsModule.kt` | M10 | — |
| `di/SmbModule.kt` | M17 | — |
| `di/CoilModule.kt` | M23 | — |
| `ui/screens/viewer/ViewerSpread.kt` | fix | D-001 |
| `shared/content/MixedFolderProvider.kt` | fix | D-001 |
| `shared/media/MediaFormats.kt` | fix | D-001 |
| `shared/content/PageBitmapLoader.kt` | fix | D-001 |
| `shared/thumbnail/FileEntryThumbnailLoader.kt` | fix | D-002 |
| `shared/thumbnail/FileBrowserThumbnailDiskCache.kt` | fix | D-002 |
| `shared/thumbnail/ThumbnailTaskPool.kt` | fix | D-002 |

### M25 — 设置页面

| 代码组件 / 决策主题 | Stage | 决策 ID |
|---------------------|-------|---------|
| SettingsViewModel | M25 | — |
| SettingsScreen | M25 | — |
| ResourceViewerThemeWithSettings | M25 | — |
| SettingsViewModel 状态管理 | M25 | D-001 |
| 缓存管理策略 (Coil DiskCache) | M25 | D-002 |
| 主题切换实时生效 | M25 | D-003 |
| 设置项输入验证 | M25 | D-004 |
| CoilModule 动态缓存配置 | M25 | D-005 |

---

## 按决策主题

| 主题 | 涉及 Stage | 决策 ID |
|------|-----------|---------|
| 依赖版本选型 | M00 | — |
| BouncyCastle Android 冲突处理 | M00, M17 | — |
| SMB 视频流式读取策略 | M18 | — |
| 缩略图 LRU 淘汰算法 | M23, M25 | — |
| 标签交集查询 SQL 方案 | M07 | D-002 |
| 键集分页实现 | M07 | D-001 |
| 外键约束 + 级联删除 | M06, M08 | — |
| 枚举序列化方案 (name vs ordinal) | M06 | D-001 |
| pdfium-android 加密支持 | M22 | — |
| Coil vs Glide 选型 | M00 | — |
| Room vs SQLDelight 选型 | M00 | — |
| 密码安全存储方案 | M10 | D-002 |
| Repository Result 包装策略 | M10 | D-001 |
| Flow 组合查询策略 | M10 | D-003 |
| 内置标签保护机制 | M10 | D-004 |
| FileSource 动态创建策略 | M10 | D-005 |
| ThumbnailGenerator 策略集合 | M10 | D-006 |
| Navigation 路由设计 | M03 | D-001, D-002 |
| 权限请求时机与方式 | M12 | D-001 |
| 隐私政策存储方式 | M12 | D-002 |
| 数据清除策略 | M12 | D-003 |
| 数据库迁移策略 | M12 | D-004 |
| ViewModel StateFlow 状态管理 | M15 | D-001 |
| 标签名称校验策略 | M15 | D-002 |
| 颜色选择器布局方案 | M15 | D-003 |
| 删除确认交互方案 | M15 | D-004 |
| Koin ViewModel 注入 | M15 | D-005 |
| ImageFolderProvider 初始化策略 | M14 | D-001 |
| 图片文件识别方式 | M14 | D-002 |
| ViewerViewModel 状态管理 | M14 | D-003 |
| HorizontalPager 配置 | M14 | D-004 |
| SlideBar 实现方式 | M14 | D-005 |
| VideoPlayerViewModel 架构 | M19 | D-001 |
| VideoMediaSource.SmbFile 设计 | M19 | D-002 |
| 视频资源检测策略 | M19 | D-003 |
| 视频手势处理方案 | M19 | D-004 |
| ExoPlayer 生命周期管理 | M19 | D-005 |
| VideoThumbnailGenerator 实现 | M19 | D-006 |

### M20 — Gallery + FlatGrid 策略

| 代码组件 / 决策主题 | Stage | 决策 ID |
|---------------------|-------|---------|
| FlatGridStrategy | M20 | — |
| GalleryStrategy | M20 | — |
| DetectOrganizationModeUseCase | M20 | — |
| FlatGrid 和 Gallery 策略共享文件获取逻辑 | M20 | D-001 |
| DetectOrganizationModeUseCase 仅检查一级子目录 | M20 | D-002 |
| 混合文件类型默认使用 FLATGRID | M20 | D-003 |

### M22 — PDF 查看器

| 代码组件 / 决策主题 | Stage | 决策 ID |
|---------------------|-------|---------|
| PdfRenderer | M22 | — |
| PdfContentProvider | M22 | — |
| PdfThumbnailGenerator | M22 | — |
| pdfium-android 库选择 | M22 | D-001 |
| PdfRenderer 封装方式 | M22 | D-002 |
| PdfContentProvider 初始化策略 | M22 | D-003 |
| 加密 PDF 处理方式 | M22 | D-004 |
| PdfThumbnailGenerator 缩放策略 | M22 | D-005 |
| ViewerViewModel Context 依赖 | M22 | D-006 |

### M23 — 首页网格完整实现

| 代码组件 / 决策主题 | Stage | 决策 ID |
|---------------------|-------|---------|
| HomeViewModel | M23 | D-001 |
| FilterBar | M23 | D-002 |
| CoilModule | M23 | D-003 |
| ImageThumbnailGenerator | M23 | D-004 |
| ResourceGridItem | M23 | — |
| HomeViewModel 状态管理 (flatMapLatest) | M23 | D-001 |
| FilterBar "全部"按钮传 null | M23 | D-002 |
| Coil 缓存策略 (内存25%/磁盘2%) | M23 | D-003 |
| ImageThumbnailGenerator 取第一张图片 | M23 | D-004 |

### M27 — 批量添加 + 拆分 UseCase

| 代码组件 / 决策主题 | Stage | 决策 ID |
|---------------------|-------|---------|
| ScanResourcesUseCase | M27 | D-001, D-002, D-003 |
| BatchAddResourcesUseCase | M27 | D-001, D-002, D-003 |
| SplitResourceUseCase | M27 | D-001, D-004 |
| UseCase 返回类型选择 (Flow vs Result) | M27 | D-001 |
| 文件类型过滤策略 | M27 | D-002 |
| 批量插入失败处理 | M27 | D-003 |
| 拆分后父资源状态 | M27 | D-004 |

### M28 — 打磨完善

| 代码组件 / 决策主题 | Stage | 决策 ID |
|---------------------|-------|---------|
| `app/proguard-rules.pro` | M28 | D-001 |
| `app/build.gradle.kts` (R8 配置) | M28 | D-001 |
| `app/src/test/.../ProGuardRulesTest.kt` | M28 | — |
| `app/src/test/.../DarkModeTest.kt` | M28 | D-003 |
| `app/src/test/.../EdgeCaseTest.kt` | M28 | — |
| ProGuard 规则策略 | M28 | D-001 |
| mbassy javax.el 兼容处理 | M28 | D-002 |
| 深色模式验证策略 | M28 | D-003 |

### M30 — 修复底栏导航 + SMB 线程与权限

| 代码组件 / 决策主题 | Stage | 决策 ID |
|---------------------|-------|---------|
| `ui/components/AppShell.kt` | M30 | D-001 |
| `ui/navigation/Screen.kt` | M30 | D-001 |
| `ui/navigation/AppNavGraph.kt` | M30 | D-001 |
| `ui/screens/sources/SourceListScreen.kt` | M30 | D-002 |
| `ui/screens/sources/SourceListViewModel.kt` | M30 | D-002, D-005 |
| `shared/filesource/SmbFileSource.kt` | M30 | D-003 |
| `data/remote/smb/SmbClientWrapper.kt` | M30 | D-003, D-005 |
| `AndroidManifest.xml` | M30 | D-004 |
| 底栏 Tab 精简 | M30 | D-001 |
| 添加数据源统一类型选择 | M30 | D-002 |
| SMB 线程调度 | M30 | D-003 |
| 网络权限配置 | M30 | D-004 |
| 错误信息分层 | M30 | D-005 |

### fix — 文件浏览、混合查看器、缩略图、标签与 SMB 回归修复

| 代码组件 / 决策主题 | Stage | 决策 ID |
|---------------------|-------|---------|
| `ui/components/AppShell.kt` | fix | D-004 |
| `ui/screens/sources/FileBrowserScreen.kt` | fix | D-004 |
| `ui/screens/viewer/ViewerSpread.kt` | fix | D-001 |
| `ui/screens/viewer/ViewerScreen.kt` | fix | D-001 |
| `ui/screens/viewer/VideoPlayerViewModel.kt` | fix | D-001 |
| `shared/content/MixedFolderProvider.kt` | fix | D-001 |
| `shared/media/MediaFormats.kt` | fix | D-001 |
| `shared/content/PageBitmapLoader.kt` | fix | D-001 |
| `shared/thumbnail/FileEntryThumbnailLoader.kt` | fix | D-002 |
| `shared/thumbnail/ImageThumbnailGenerator.kt` | fix | D-002 |
| `shared/thumbnail/FileBrowserThumbnailDiskCache.kt` | fix | D-002 |
| `shared/thumbnail/ThumbnailTaskPool.kt` | fix | D-002 |
| `ui/screens/sources/FileBrowserViewModel.kt` | fix | D-002 |
| `ui/screens/sources/BatchAddResourcesDialog.kt` | fix | D-003 |
| `shared/filesource/FileSourceFactory.kt` | fix | D-003 |
| `shared/filesource/SmbFileSource.kt` | fix | D-003 |
| `data/remote/smb/SmbClientWrapper.kt` | fix | D-003 |
| 混合查看器视觉页模型重构 | fix | D-001 |
| 文件/文件夹缩略图解析与回退卡片 | fix | D-002 |
| SMB 会话复用与批量添加标签选择 | fix | D-003 |
| 系统返回键逐级返回 | fix | D-004 |

### cache-management-refactor — 缓存管理重构

| 代码组件 / 决策主题 | Stage | 决策 ID |
|---------------------|-------|---------|
| `ui/screens/viewer/ViewerViewModel.kt` | cache-refactor | D-001, D-005 |
| `shared/thumbnail/ImageThumbnailGenerator.kt` | cache-refactor | D-002, D-003 |
| `shared/thumbnail/PdfThumbnailGenerator.kt` | cache-refactor | D-002, D-003 |
| `shared/thumbnail/VideoThumbnailGenerator.kt` | cache-refactor | D-003 |
| `domain/usecase/BatchAddResourcesUseCase.kt` | cache-refactor | D-004, D-005 |
| `data/cache/CacheManager.kt` | cache-refactor | D-006 |
| `di/CacheModule.kt` | cache-refactor | D-006 |
| `data/local/entity/AppConfigEntity.kt` | cache-refactor | D-006 |
| `domain/model/AppConfig.kt` | cache-refactor | D-006 |
| `data/local/AppDatabase.kt` | cache-refactor | D-006 |
| `data/local/migration/DatabaseMigrator.kt` | cache-refactor | D-006 |
| `ui/screens/settings/SettingsViewModel.kt` | cache-refactor | D-006, D-007 |
| `ui/screens/settings/SettingsScreen.kt` | cache-refactor | D-006, D-007 |
| `ui/screens/sources/FileBrowserViewModel.kt` | cache-refactor | D-006 |
| `di/CoilModule.kt` | cache-refactor | D-006 |
| `di/RepositoryModule.kt` | cache-refactor | D-003 |
| 预加载策略 +3 -1 | cache-refactor | D-001 |
| 缩略图尺寸统一 320px | cache-refactor | D-002 |
| Generator 缓存复用 | cache-refactor | D-003 |
| 封面缓存目录独立 | cache-refactor | D-004 |
| 批量并发生成缩略图 | cache-refactor | D-005 |
| 三个独立缓存容量设置 | cache-refactor | D-006 |
| 自定义容量内嵌输入框 | cache-refactor | D-007 |

### file-browser-ux — 文件浏览器体验优化

| 代码组件 / 决策主题 | Stage | 决策 ID |
|---------------------|-------|---------|
| `data/local/datastore/FileBrowserPrefsStore.kt` | file-browser-ux | D-001 |
| `ui/screens/sources/DirectoryTree.kt` | file-browser-ux | D-002 |
| `ui/screens/sources/FileBrowserScreen.kt` | file-browser-ux | D-003, D-004, D-005 |
| `ui/screens/sources/FileBrowserViewModel.kt` | file-browser-ux | D-001 |
| `di/DataStoreModule.kt` | file-browser-ux | — |
| `data/local/entity/AppConfigEntity.kt` | file-browser-ux | — |
| `domain/model/AppConfig.kt` | file-browser-ux | — |
| `data/local/AppDatabase.kt` | file-browser-ux | — |
| `data/local/migration/DatabaseMigrator.kt` | file-browser-ux | — |
| `ui/screens/settings/SettingsViewModel.kt` | file-browser-ux | — |
| `ui/screens/settings/SettingsScreen.kt` | file-browser-ux | — |
| 文件夹偏好持久化方案 (DataStore + LRU) | file-browser-ux | D-001 |
| 目录树组件实现 (扁平化列表) | file-browser-ux | D-002 |
| 目录树布局位置 (content 内部) | file-browser-ux | D-003 |
| 排序方式与 UI (DropdownMenu) | file-browser-ux | D-004 |
| 文件夹角标显示逻辑 | file-browser-ux | D-005 |

---

> ⚠️ 各 stage 完成后，Agent 需在此文件中新增自己 stage 涉及的组件行和决策主题行。
> 已有行的决策 ID 列留空（`—`），待对应 stage 完成后由 Agent 填写。
