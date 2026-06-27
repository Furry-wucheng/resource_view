# Flutter 功能对齐：实现状态详细报告

> **创建日期**: 2026-06-27
> **关联文档**: `doc/issues/2026-06-27-feature-flutter-parity-gap.md`
> **状态**: ✅ 36/36 全部完成

---

## 总览

| 状态 | 数量 | 说明 |
|------|------|------|
| ✅ 完成 | 36 | 功能已实现并通过 `./gradlew test build` 验收 |

---

## 逐项实现状态

### 模块一：首页 (HomeScreen) — 5/5 ✅

| # | 任务 | 状态 | 批次 | 实现位置 |
|---|------|------|------|----------|
| 1.1 | 搜索功能 | ✅ | 批次 3 | `HomeScreen.kt` 搜索图标 + OutlinedTextField；`HomeViewModel.kt` `setSearchQuery` + `combine` 过滤 |
| 1.2 | 排序功能 | ✅ | 批次 3 | `HomeScreen.kt` Sort 图标循环切换；`HomeViewModel.kt` `ResourceSort` enum（四种排序） |
| 1.3 | 多选模式与批量删除 | ✅ | 批次 3 | `HomeScreen.kt` Checklist 图标进入多选、全选、底栏批量删除；`HomeViewModel.kt` `enterMultiSelectMode`/`batchDeleteSelectedResources` |
| 1.4 | 收藏快捷操作 | ✅ | 批次 4 | `ResourceGridItem.kt` 收藏星标（Star/StarBorder）；`HomeViewModel.kt` `toggleFavorite`；DB v2→v3 迁移 `favorited` 字段 |
| 1.5 | 分页加载 | ✅ | 批次 5 | `HomeViewModel.kt` `_displayCount` + `PAGE_SIZE=20` + `loadMore()`；`HomeScreen.kt` LazyVerticalGrid 底部 `LaunchedEffect` 触发加载 |

### 模块二：数据源 (SourceListScreen) — 5/5 ✅

| # | 任务 | 状态 | 批次 | 实现位置 |
|---|------|------|------|----------|
| 2.1 | 重命名数据源 | ✅ | 批次 4 | `SourceListScreen.kt` Edit 图标 + 重命名 AlertDialog；`SourceListViewModel.kt` `showRenameDialog`/`renameSource` |
| 2.2 | 编辑 SMB 凭据 | ✅ | 批次 5 | `SourceListScreen.kt` Settings 图标（SMB 专用）+ 编辑弹窗；`SourceListViewModel.kt` `showEditSmbDialog`/`updateSmbSource`；`AddSmbDialog.kt` 新增 `title`/`confirmText` 参数复用 |
| 2.3 | 资源数量显示 | ✅ | 批次 4 | `SourceListViewModel.kt` `resourceCounts` Map；`SourceListScreen.kt` SourceCard 显示 "X 个资源"；`SourceDao.kt` `getResourceCount` 查询 |
| 2.4 | 删除确认弹窗 | ✅ | 批次 4 | `SourceListScreen.kt` Delete 图标 + 删除确认 AlertDialog；`SourceListViewModel.kt` `showDeleteConfirmDialog`/`confirmDeleteSource` |
| 2.5 | 添加本地文件夹自动命名 | ✅ | 批次 4 | `SourceListViewModel.kt` `extractFolderName()` 从 URI 提取 `lastPathSegment`；`updateLocalForm` 自动填入 name |

### 模块三：文件浏览器 (FileBrowserScreen) — 4/4 ✅

| # | 任务 | 状态 | 批次 | 实现位置 |
|---|------|------|------|----------|
| 3.1 | 网格视图与列表/网格切换 | ✅ | 批次 5 | `FileBrowserViewModel.kt` `FileViewMode` enum + `toggleViewMode()`；`FileBrowserScreen.kt` GridView/List 图标 + LazyColumn/LazyVerticalGrid 双视图 + `FileEntryGridItem` |
| 3.2 | 点击文件打开查看器 | ✅ | 批次 6 | `FileBrowserScreen.kt` `FilePreviewOverlay` composable：可预览文件（图片/视频/PDF）点击后全屏预览，支持缩放/平移；`FileBrowserViewModel.kt` `openFilePreview()`/`closeFilePreview()`/`loadPreviewBitmap()` |
| 3.3 | 批量添加弹窗 | ✅ | 批次 6 | `BatchAddResourcesDialog.kt` 新组件：组织模式选择（自动/章节/平铺）+ 标签勾选；`BatchAddResourcesUseCase` 扩展 `organizationMode`/`tagIds` 参数；`FileBrowserViewModel.kt` `showBatchAddDialog()`/`confirmBatchAdd()`；`ResourceRepository.setResourceTags()` |
| 3.4 | 目录树侧边栏 | ✅ | 批次 7 | `FileBrowserScreen.kt` `DirectoryTreePanel` composable：路径层级导航，点击跳转任意层级；宽屏（≥900dp）左侧 persistent drawer 240dp；窄屏覆盖层 280dp + 背景点击关闭；`FileBrowserViewModel.kt` `toggleDirectoryTree()`/`navigateToPathSegment()` |

### 模块四：标签管理 (TagManagerScreen) — 3/3 ✅

| # | 任务 | 状态 | 批次 | 实现位置 |
|---|------|------|------|----------|
| 4.1 | 点击标签跳转首页筛选 | ✅ | 批次 4 | `TagManagerScreen.kt` `onTagClick` 回调 → `AppNavGraph.kt` `popBackStack` + `NavBackStackEntry.savedStateHandle` 传递 `filterTagId`；`HomeViewModel.kt` `selectTag` |
| 4.2 | 内置/自定义分区标题 | ✅ | 批次 4 | `TagManagerScreen.kt` `SectionTitle` composable + 分组 LazyColumn（内置标签 / 自定义标签） |
| 4.3 | 标签资源计数来源 | ✅ | 已有 | `TagRepository.kt` `getAllTags()` 使用 `combine(tagDao.getAllTags(), tagDao.getTagResourceCounts())` 从关联表实时查询 |

### 模块五：查看器 (ViewerScreen) — 13/13 ✅

| # | 任务 | 状态 | 批次 | 实现位置 |
|---|------|------|------|----------|
| 5.1 | 组织模式路由分发 | ✅ | 批次 2A/2B | `AppNavGraph.kt` `resolveResourceDestination()` 纯函数路由决策；四模式分别导航到 ChapterList/FlatGrid/Gallery/Viewer |
| 5.2 | FlatGridScreen | ✅ | 批次 2B | `ContentGridScreen.kt` + `ContentGridViewModel.kt`（mode=FLAT_GRID）；支持文件夹下钻、返回上级 |
| 5.3 | GalleryScreen | ✅ | 批次 2B | `ContentGridScreen.kt` + `ContentGridViewModel.kt`（mode=GALLERY）；递归展开全部子目录图片 |
| 5.4 | OrgModeSwitcher | ✅ | 批次 4 | `OrgModeSwitcher.kt` 新组件（章节/章廊/平铺/画廊）；集成到 ChapterListScreen 和 ContentGridScreen |
| 5.5 | 点击区域翻页 | ✅ | 批次 3 | `ViewerNavigation.kt` `resolveTapAction()` 三区域判断 + 方向感知；`ViewerScreen.kt` `pointerInput` + `detectTapGestures` |
| 5.6 | 双击缩放 | ✅ | 批次 5 | `ViewerScreen.kt` `PageContent` 中 `detectTapGestures(onDoubleTap)` 切换 1x/2x + `detectTransformGestures` 拖动 + `graphicsLayer` 动画 |
| 5.7 | 双页模式实现 | ✅ | 批次 3 | `ViewerScreen.kt` `useDoublePage` 逻辑 + `ViewerPagerContent` Row 两页 spread；`ViewerViewModel.kt` `DoublePageMode` 读写 + 工具栏循环切换 |
| 5.8 | 翻页方向 (RTL/LTR) | ✅ | 批次 3 | `ViewerScreen.kt` `HorizontalPager(reverseLayout=RTL)` + `resolveTapAction` 方向感知；`SlideBar.kt` RTL 支持；`ViewerViewModel.kt` `PageDirection` 读写 |
| 5.9 | 跨章节连续阅读 | ✅ | 批次 4 | `ViewerViewModel.kt` `chapters`/`currentChapterIndex`/`navigateToNextChapter`/`navigateToPrevChapter`；`ViewerScreen.kt` 末页检测 + `chapterHint` 过渡提示 |
| 5.10 | ViewerToolbar 扩展 | ✅ | 批次 3+4 | `ViewerToolbar.kt` 收藏按钮（Star/StarBorder）+ 翻页方向文字按钮（LTR/RTL/纵向）+ 双页模式文字按钮（自动/单页/双页） |
| 5.11 | VideoSeekGestureArea | ✅ | 批次 5 | `VideoPlayer.kt` `VideoSeekGestureArea` composable：底部 20% 水平拖动 seek + 预览时间显示 |
| 5.12 | 页面缓存策略 | ✅ | 批次 1 | `PageLoader.kt` 200MB LRU 缓存 + 同页请求合并 + 当前页优先 + 前后 2 页顺序预取 |
| 5.13 | 单页错误重试 | ✅ | 批次 1 | `ViewerScreen.kt` `PageContent` 中 `PageBitmapState.Error` + "重试"按钮 + `retryCount` 触发重新加载 |

### 模块六：章节列表 (ChapterListScreen) — 5/5 ✅

| # | 任务 | 状态 | 批次 | 实现位置 |
|---|------|------|------|----------|
| 6.1 | 封面缩略图 | ✅ | 批次 4 | `ChapterListScreen.kt` `ChapterItem` 使用 Coil `AsyncImage` 加载 `chapter.coverPath` |
| 6.2 | 网格/列表视图切换 | ✅ | 批次 5 | `ChapterListViewModel.kt` `ChapterViewMode` enum + `toggleViewMode()`；`ChapterListScreen.kt` GridView/List 图标 + `ChapterGridItem`/`LooseFileGridItem` |
| 6.3 | 响应式双栏布局 | ✅ | 批次 6 | `ChapterListScreen.kt` `BoxWithConstraints` ≥900dp 阈值；`WideChapterLayout` 左侧 280dp 封面面板 + 右侧章节列表；`NarrowChapterLayout` 标准纵向布局 |
| 6.4 | 散落文件 | ✅ | 批次 4 | `ChapterListViewModel.kt` `getLooseFiles()` 过滤非目录文件；`ChapterListScreen.kt` 底部 "散落文件" 区域 + `LooseFileItem` |
| 6.5 | 章节点击行为修正 | ✅ | 批次 2A | `ChapterListScreen.kt` 点击传递 `chapter.relativePath` → `ChapterViewer` 路由（URL 编码）→ Viewer 仅加载所选章节 |

### 模块七：设置 (SettingsScreen) — 3/3 ✅

| # | 任务 | 状态 | 批次 | 实现位置 |
|---|------|------|------|----------|
| 7.1 | 自定义缓存容量 | ✅ | 批次 4 | `SettingsScreen.kt` "自定义" FilterChip + AlertDialog + OutlinedTextField 输入；`SettingsViewModel.kt` `showCustomCapacityDialog` |
| 7.2 | 缓存路径显示 | ✅ | 批次 4 | `SettingsViewModel.kt` `loadCachePath()` 从 ThumbnailRepository 获取实际路径；`SettingsScreen.kt` 显示 `uiState.cachePath` |
| 7.3 | 垂直滚动翻页方向选项 | ✅ | 批次 3 | `ViewerScreen.kt` `if (pageDirection == VERTICAL) VerticalPager else HorizontalPager` |

### 模块八：导航与架构 — 1/1 ✅

| # | 任务 | 状态 | 批次 | 实现位置 |
|---|------|------|------|----------|
| 8.1 | ChapterListScreen 导航集成 | ✅ | 批次 2A | `AppNavGraph.kt` `resolveResourceDestination()` + `HomeScreen.kt` `onNavigateToViewer` 按组织模式分发路由 |

---

## 批次执行记录摘要

| 批次 | 日期 | 内容 | 验收 |
|------|------|------|------|
| 1 | 2026-06-27 | 5.12 页面缓存 + 5.13 单页重试 + SMB 性能修复 | ✅ 423 tests |
| 2A | 2026-06-27 | 5.1/8.1 部分路由 + 6.5 章节点击 + 递归 Provider | ✅ |
| 2B | 2026-06-27 | 5.1/8.1 完整四模式 + 5.2 FlatGrid + 5.3 Gallery | ✅ |
| 3 | 2026-06-27 | 5.5/5.7/5.8/7.3 Viewer 交互 + 1.1/1.2/1.3 Home 管理 | ✅ |
| 4 | 2026-06-27 | 5.4/5.9/5.10/1.4/6.1/6.4/2.1-2.5/4.1-4.2/7.1-7.2 | ✅ |
| 5 | 2026-06-27 | 5.6/5.11/2.2/3.1/6.2/1.5 | ✅ |
| 6 | 2026-06-27 | 3.2 文件预览 + 3.3 批量添加弹窗 + 6.3 双栏布局 | ✅ |
| 7 | 2026-06-27 | 3.4 目录树侧边栏 | ✅ |

**总计**: 36/36 项全部完成。`./gradlew test build` 全部通过。

---

## 技术决策记录

### D1: 分页策略选择

**决策**: 使用内存分页（`_displayCount` + `take()`）而非数据库 LIMIT/OFFSET。

**原因**:
- 当前架构基于 `Flow<List<Resource>>` 自动更新，数据库分页会破坏响应式
- 资源数量通常在数百到数千级别，内存分页性能可接受
- DAO 已有 `pageAfter` 键集分页方法，未来数据量增长时可切换

### D2: 双击缩放实现方式

**决策**: 使用 `graphicsLayer` + `animateFloatAsState` 而非 `TransformableState`。

**原因**:
- `graphicsLayer` 性能最优（硬件加速，不触发 recomposition）
- 动画控制更精细（200ms tween）
- 拖动范围限制通过 `coerceIn` 实现，逻辑清晰

### D3: VideoSeekGestureArea 集成方式

**决策**: 在 PlayerView 上层叠加 Compose 透明热区，而非扩展 Android GestureDetector。

**原因**:
- Compose `detectHorizontalDragGestures` API 更简洁
- 避免与 PlayerView 内置手势冲突（`false` 不消费事件）
- 预览时间 UI 直接用 Compose Text 渲染

### D4: AddSmbDialog 复用策略

**决策**: 通过 `title` 和 `confirmText` 可选参数复用同一个 Dialog，而非新建 EditSmbDialog。

**原因**:
- 表单字段完全一致，无需重复代码
- 调用方只需传入不同标题和按钮文本
- 测试覆盖范围更集中

### D5: 文件预览策略

**决策**: 在 FileBrowserScreen 内实现 FilePreviewOverlay 全屏覆盖层，而非跳转到 ViewerScreen。

**原因**:
- 文件浏览器中的预览是"快速查看"，不需要翻页/缩放工具栏等完整功能
- 复用 `FileSource.openInputStream()` + `BitmapFactory.decodeStream()` 加载图片
- 避免 ViewerScreen 路由复杂化（需要处理 sourceId → resourceId 映射）

### D6: 目录树侧边栏响应式策略

**决策**: 宽屏（≥900dp）使用 persistent drawer，窄屏使用覆盖层 + 背景蒙层。

**原因**:
- 与 ChapterListScreen 的 `WideChapterLayout` 阈值一致（900dp）
- 覆盖层模式在窄屏上不占用常驻空间，点击背景即可关闭
- `BoxWithConstraints` 在 Compose 中是最简洁的响应式断点方案

---

## 相关文档

- Flutter 差距分析: `doc/issues/2026-06-27-feature-flutter-parity-gap.md`
- Android 源码: `app/src/main/java/dev/wucheng/resource_viewer/`
- Issue 模板: `doc/issues/ISSUE_TEMPLATE.md`
