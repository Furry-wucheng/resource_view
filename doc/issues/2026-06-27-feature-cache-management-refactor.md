# feature: 缓存管理重构 — 独立容量控制 + 预加载优化 + 缩略图复用

> 日期: 2026-06-27 | 类型: feature | 状态: ✅ 已完成

## 描述

对查看器预加载策略、缩略图缓存系统和设置页面缓存管理进行全面重构：

1. **预加载策略优化**：将查看器预加载范围从 ±2 页改为 +3 -1 页（后 3 页 + 前 1 页），提升正向阅读流畅度
2. **缩略图尺寸统一**：封面生成器尺寸从 300px 统一为 320px
3. **缩略图缓存复用**：Generator 生成前先检查 `FileBrowserThumbnailDiskCache`，命中则直接复用，避免重复 I/O
4. **封面并发生成**：`BatchAddResourcesUseCase` 使用 `ThumbnailTaskPool` + `coroutineScope` 并发生成，遵循设置中的 `thumbnailConcurrency` 配置
5. **缓存管理重构**：三个独立缓存（封面/页面/缩略图）分别管理容量，设置页面显示进度条 + 容量选择器

## 验收标准

- [x] 查看器预加载范围为 +3 -1（后 3 页 + 前 1 页）
- [x] 所有缩略图生成器尺寸统一为 320px
- [x] Generator 生成前检查 `FileBrowserThumbnailDiskCache`，命中则复用
- [x] 批量添加资源时并发生成缩略图，遵循 `thumbnailConcurrency` 配置
- [x] 封面缓存目录移出 `image_cache/` 到 `thumbnails/resources/`
- [x] 设置页面显示三个独立缓存条目，各有进度条和容量选择器
- [x] 封面缓存默认无限制（永久），可设置容量
- [x] 页面缓存和缩略图缓存独立设置容量
- [x] 自定义容量内嵌输入框，非弹窗
- [x] build/test passed

## 影响分析

| 维度 | 内容 |
|------|------|
| 修改文件 | ViewerViewModel, ImageThumbnailGenerator, PdfThumbnailGenerator, VideoThumbnailGenerator, BatchAddResourcesUseCase, CacheManager, SettingsViewModel, SettingsScreen, AppConfigEntity, AppDatabase 等 |
| 影响 stage | M14 (查看器), M23 (缩略图), M25 (设置), M27 (批量添加) |
| 聚合文件 | AppDatabase.kt (v3→v4 迁移) |
| 数据库变更 | app_config 表新增 coverCacheLimitMB, pageCacheLimitMB, thumbnailCacheLimitMB 字段 |

## 是否需合并回原文档

- [x] 是 → 需更新 doc/share/ 02-interfaces.md (ThumbnailGenerator 接口变化)

## 执行计划

1. **Step 1**：预加载策略修改（+3 -1）— ViewerViewModel.kt
2. **Step 2**：缩略图尺寸统一（300→320）— ImageThumbnailGenerator + PdfThumbnailGenerator
3. **Step 3**：Generator 注入 diskCache + 复用检测
4. **Step 4**：BatchAddResourcesUseCase 并发生成
5. **Step 5**：CacheManager 统一管理类
6. **Step 6**：数据库升级（v3→v4）
7. **Step 7**：设置页面缓存管理 UI 重构

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/screens/viewer/ViewerViewModel.kt` | ✏️ 修改 | 预加载 +3 -1，使用 pageCacheLimitMB |
| `shared/thumbnail/ImageThumbnailGenerator.kt` | ✏️ 修改 | 尺寸 320 + 复用检测 + 注入 diskCache |
| `shared/thumbnail/PdfThumbnailGenerator.kt` | ✏️ 修改 | 尺寸 320 + 复用检测 + 注入 diskCache |
| `shared/thumbnail/VideoThumbnailGenerator.kt` | ✏️ 修改 | 复用检测 + 注入 diskCache |
| `domain/usecase/BatchAddResourcesUseCase.kt` | ✏️ 修改 | 并发生成 + ThumbnailTaskPool |
| `data/cache/CacheManager.kt` | 🆕 新增 | 统一缓存管理 |
| `di/CacheModule.kt` | 🆕 新增 | 注册 CacheManager |
| `data/local/entity/AppConfigEntity.kt` | ✏️ 修改 | 新增三个缓存容量字段 |
| `domain/model/AppConfig.kt` | ✏️ 修改 | 同步更新 |
| `data/local/AppDatabase.kt` | ✏️ 修改 | v3→v4 迁移 |
| `data/local/migration/DatabaseMigrator.kt` | ✏️ 修改 | 注册迁移 |
| `ui/screens/settings/SettingsViewModel.kt` | ✏️ 修改 | 三个独立缓存容量管理 |
| `ui/screens/settings/SettingsScreen.kt` | ✏️ 修改 | 三个独立缓存设置条目 |
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 修改 | 使用 thumbnailCacheLimitMB |
| `di/CoilModule.kt` | ✏️ 修改 | 使用 thumbnailCacheLimitMB |
| `di/RepositoryModule.kt` | ✏️ 修改 | 注入 diskCache + database |
| `di/ViewModelModule.kt` | ✏️ 修改 | 注入 CacheManager |
| `ResourceViewerApp.kt` | ✏️ 修改 | 注册 cacheModule |
