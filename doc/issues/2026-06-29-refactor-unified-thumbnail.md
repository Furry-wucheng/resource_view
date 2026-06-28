# refactor: 统一缩略图加载管理器和卡片组件

> 日期: 2026-06-29 | 类型: refactor | 状态: ✅ 已完成

## 描述

FileBrowser、ContentGrid、ChapterList 三个 ViewModel 各自维护了一份完全相同的缩略图加载逻辑（内存 LRU + 磁盘缓存 + 并发池），UI 层也各自实现了视觉完全一致的缩略图卡片。需要提取共享组件消除重复。

## 验收标准

- [x] `ThumbnailLoadManager` 统一三处 ViewModel 的缓存+并发+磁盘+配置读取逻辑
- [x] `FileThumbnailCard` 统一 FileBrowser 和 ContentGrid 的网格卡片 UI
- [x] `fileTypeColor`/`fileTypeIcon` 提取为公共工具函数
- [x] ContentGrid/ChapterList 的并发数从硬编码改为读取 AppConfigDao 配置
- [x] 移除三处 copy-paste 代码

## 影响分析

| 维度 | 内容 |
|------|------|
| 重复代码 | 三份 ViewModel 缓存逻辑（~80 行 ×3）、两份卡片 UI（~150 行 ×2） |
| 根因 | 三个 Stage（M13/M20/M21）各自独立开发，互相不可见 |
| 修改文件 | 见产出清单 |
| 影响 stage | M13(FileBrowser), M20(ContentGrid), M21(ChapterList) |

## 是否需合并回原文档

[x] 否 → 独立提案，不回溯

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `shared/thumbnail/ThumbnailLoadManager.kt` | 🆕 新增 | 统一内存 LRU + misses 记录 + 磁盘缓存 + 并发控制 |
| `ui/components/FileThumbnailCard.kt` | 🆕 新增 | 统一 3:4 Card + 渐变遮罩 + fileTypeColor/Icon 工具函数 |
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 重构 | 替换缓存层为 ThumbnailLoadManager，保留 inFlight dedup |
| `ui/screens/viewer/ContentGridViewModel.kt` | ✏️ 重构 | 替换缓存层为 ThumbnailLoadManager，注入 AppConfigDao |
| `ui/screens/viewer/ChapterListViewModel.kt` | ✏️ 重构 | 替换缓存层为 ThumbnailLoadManager，注入 AppConfigDao |
| `ui/screens/sources/FileBrowserScreen.kt` | ✏️ 重构 | FileEntryGridItem → FileThumbnailCard |
| `ui/screens/viewer/ContentGridScreen.kt` | ✏️ 重构 | GridEntryCard → FileThumbnailCard |
| `di/ViewModelModule.kt` | ✏️ 修改 | ChapterListVM/ContentGridVM 注入 AppConfigDao |
