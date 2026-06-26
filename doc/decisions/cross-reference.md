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
| `domain/model/ViewerItem.kt` | M09 | D-001 |
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
| `shared/filesource/SmbFileSource.kt` | M17 | — |
| `shared/content/ContentProvider.kt` | M11 | — |
| `shared/content/ImageFolderProvider.kt` | M14 | — |
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
| `ui/screens/home/HomeScreen.kt` | M05 → M23 | D-003 |
| `ui/screens/home/HomeViewModel.kt` | M23 | — |
| `ui/screens/sources/SourceListScreen.kt` | M05 → M17 | D-003 |
| `ui/screens/sources/SourceListViewModel.kt` | M17 | — |
| `ui/screens/sources/FileBrowserScreen.kt` | M13 | — |
| `ui/screens/sources/FileBrowserViewModel.kt` | M13 | — |
| `ui/screens/viewer/ViewerScreen.kt` | M14 → M19 → M22 | D-003 |
| `ui/screens/viewer/ViewerViewModel.kt` | M14 | — |
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
| `ui/components/AppShell.kt` | M04 | — |
| `data/remote/smb/SmbClientWrapper.kt` | M17 | — |
| `data/remote/smb/SmbDataSource.kt` | M18 | — |
| `data/remote/pdf/PdfRenderer.kt` | M22 | — |
| `domain/usecase/FilterResourcesByTagsUseCase.kt` | M16 | — |
| `domain/usecase/DetectOrganizationModeUseCase.kt` | M20 | — |
| `domain/usecase/ScanResourcesUseCase.kt` | M27 | — |
| `domain/usecase/BatchAddResourcesUseCase.kt` | M27 | — |
| `domain/usecase/SplitResourceUseCase.kt` | M27 | — |
| `di/DatabaseModule.kt` | M02 → M08 | — |
| `di/RepositoryModule.kt` | M02 → M10 | — |
| `di/SecurePrefsModule.kt` | M10 | — |
| `di/SmbModule.kt` | M17 | — |
| `di/CoilModule.kt` | M23 | — |

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

---

> ⚠️ 各 stage 完成后，Agent 需在此文件中新增自己 stage 涉及的组件行和决策主题行。
> 已有行的决策 ID 列留空（`—`），待对应 stage 完成后由 Agent 填写。
