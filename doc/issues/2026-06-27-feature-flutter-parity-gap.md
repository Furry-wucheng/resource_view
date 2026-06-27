1.# Flutter 功能对齐：Android 端全面差距分析与补齐计划

> **创建日期**: 2026-06-27
> **优先级**: Critical
> **类型**: feature
> **状态**: 🟢 全部完成（36/36）
> **影响范围**: 首页、数据源、文件浏览器、标签管理、查看器、章节列表、设置 — 全模块

## AI 影响分析（2026-06-27）

| 维度 | 内容 |
|------|------|
| 修改文件 | 涉及全部 UI feature、对应 ViewModel/Repository/UseCase、导航、测试与提案记录 |
| 影响 stage | M14、M17、M20、M21、M23、M25、M27 的既有产物；作为 stage 之外的独立提案迭代，不回写已完成状态 |
| 聚合文件 | 后续核心路由批次会涉及 `Screen.kt`、`AppNavGraph.kt`；需遵循末尾追加规则 |
| 当前首批 | 先处理 5.12 页面缓存、5.13 单页重试，以及用户补充的 SMB 图片首屏慢问题 |

## 分批执行计划

1. **性能止血**：当前页优先、200MB LRU、同页请求合并、顺序预取、单页重试。
2. **核心路由**：5.1/8.1 → 5.4 → 5.2/5.3 → 6.5，打通四种组织模式。
3. **阅读设置**：5.5/5.7/5.8/5.9/5.10 与 7.3，令已存储设置真正作用于 Viewer。
4. **资源管理**：Home、Source、FileBrowser、Tag 的 P1/P2 差距，逐模块 TDD 验收。
5. **体验收尾**：ChapterList、Settings、视频手势、响应式布局与全量回归。

> 本提案的 36 项不能因一次局部提交整体标为完成；每批完成后在本文末尾记录产出，全部验收后才转为 ✅。

---

## 问题背景

Android 端 (`resource_viewer`) 与 Flutter 端 (`resource_viewer_flutter`) 存在大量功能差距。经逐模块对比，发现三类问题：

1. **设置项已存储但 Viewer 未读取** — 翻页方向、双页模式、跨章节连续阅读
2. **UI 缺失但逻辑层可能已有** — 网格视图、OrgModeSwitcher、封面缩略图等
3. **功能完全空白** — 搜索、排序、多选批量删除、双页模式效果、点击翻页等

本文档按模块逐一列出差距，每条均标注 Flutter 参照文件，便于逐一补齐。

---

## 模块一：首页 (HomeScreen)

### 1.1 搜索功能

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 顶栏搜索图标 → 展开搜索胶囊框 → 实时过滤资源列表 |
| **Flutter 参照** | `lib/ui/features/home/home_page.dart` (`_openSearch`, `_closeSearch`, `_buildSearchCapsule`) |
| **Flutter 逻辑** | `lib/ui/features/home/view_models/home_view_model.dart` (`setSearchQuery`) |
| **Android 现状** | HomeScreen 顶栏只有标题"资源库"，无搜索入口 |
| **任务** | HomeScreen 添加搜索图标 + 搜索框 UI + HomeViewModel 添加 `setSearchQuery` 过滤逻辑 |

### 1.2 排序功能

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 顶栏排序按钮 → PopupMenu（添加时间正序/倒序、名称 A-Z/Z-A） |
| **Flutter 参照** | `lib/ui/features/home/home_page.dart` (`PopupMenuButton<ResourceSort>`) |
| **Flutter 逻辑** | `lib/ui/features/home/view_models/home_view_model.dart` (`setSort`, `ResourceSort` enum) |
| **Android 现状** | 无排序入口，资源按默认顺序展示 |
| **任务** | HomeScreen 添加排序 PopupMenu + HomeViewModel 添加排序状态与逻辑 |

### 1.3 多选模式与批量删除

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 顶栏 checklist 图标 → 进入多选模式（全选/取消全选） → 底栏红色"批量删除"按钮 → 确认弹窗 |
| **Flutter 参照** | `lib/ui/features/home/home_page.dart` (`_enterMultiSelect`, `_batchDeleteResources`, `_buildMultiSelectBottomBar`) |
| **Flutter 逻辑** | `lib/ui/features/home/view_models/home_view_model.dart` (`enterMultiSelectMode`, `exitMultiSelectMode`, `toggleSelectAllVisible`, `batchDeleteSelectedResources`) |
| **Android 现状** | 无多选入口，只能长按编辑单个资源 |
| **任务** | HomeScreen 添加多选模式 UI + 底栏 + HomeViewModel 添加多选状态管理 + 批量删除 UseCase |

### 1.4 收藏快捷操作

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 缺失 |
| **PRD** | 未单独提出 |
| **预期效果** | 资源卡片上显示收藏星标（⭐），点击可快速 toggle 收藏状态 |
| **Flutter 参照** | `lib/ui/features/home/widgets/resource_grid_item.dart` (收藏星标覆盖层) |
| **Flutter 逻辑** | `lib/ui/features/home/view_models/home_view_model.dart` (`toggleFavorite`, `favoriteResourceIds`) |
| **Android 现状** | ResourceGridItem 只显示缩略图+名称，无收藏标识；收藏只能通过长按详情弹窗操作 |
| **任务** | ResourceGridItem 添加收藏星标 + HomeViewModel 添加 `toggleFavorite` 快捷方法 |

### 1.5 分页加载

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能缺失 |
| **PRD** | 未单独提出 |
| **预期效果** | 滚动到底部自动加载下一页，显示加载指示器 |
| **Flutter 参照** | `lib/ui/features/home/home_page.dart` (`hasMore`, `isLoadingMore`, `onLoadMore`) |
| **Flutter 逻辑** | `lib/ui/features/home/view_models/home_view_model.dart` (`loadNextPage`) |
| **Android 现状** | 一次性加载全部资源，无分页 |
| **任务** | HomeViewModel 添加分页状态 + ResourceGrid 底部添加加载更多触发器 |

---

## 模块二：数据源 (SourceListScreen)

### 2.1 重命名数据源

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | M15 未覆盖 |
| **预期效果** | SourceCard 长按或菜单 → 重命名弹窗 → 修改名称 |
| **Flutter 参照** | `lib/ui/features/sources/source_list_page.dart` (`_showRenameDialog`) |
| **Flutter 逻辑** | `lib/ui/features/sources/view_models/source_list_view_model.dart` (`renameSource`) |
| **Android 现状** | SourceCard 无重命名入口，SourceListViewModel 无 renameSource 方法 |
| **任务** | SourceCard 添加操作菜单（重命名/删除） + SourceListViewModel 添加 renameSource |

### 2.2 编辑 SMB 凭据

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | M17 未覆盖 |
| **预期效果** | SMB 类型 SourceCard → 编辑凭据 → 复用 AddSmbDialog 编辑模式 |
| **Flutter 参照** | `lib/ui/features/sources/source_list_page.dart` (`_showEditSmbCredentialsDialog`) |
| **Flutter 逻辑** | `lib/ui/features/sources/view_models/source_list_view_model.dart` (`updateSmbCredentials`) |
| **Android 现状** | SMB 源创建后无法修改凭据 |
| **任务** | AddSmbDialog 支持编辑模式 + SourceListViewModel 添加 updateSmbCredentials |

### 2.3 数据源资源数量显示

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 缺失 |
| **PRD** | 未单独提出 |
| **预期效果** | SourceCard 底部显示"X 个资源" |
| **Flutter 参照** | `lib/ui/features/sources/source_list_page.dart` (`resourceCount` 参数) |
| **Android 现状** | SourceCard 只显示名称+路径+类型，无资源数量 |
| **任务** | SourceListViewModel 添加 `resourceCounts` 状态 + SourceCard 显示数量 |

### 2.4 删除确认弹窗

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 细节缺失 |
| **PRD** | 未单独提出 |
| **预期效果** | 删除数据源前显示确认弹窗，提示该源下有多少资源将被移除 |
| **Flutter 参照** | `lib/ui/features/sources/source_list_page.dart` (`_showDeleteDialog`) |
| **Android 现状** | `viewModel.deleteSource(source.id)` 直接删除，无确认 |
| **任务** | SourceListScreen 添加删除确认 AlertDialog |

### 2.5 添加本地文件夹自动命名

| 项目 | 说明 |
|------|------|
| **差距类型** | 体验差异 |
| **PRD** | M15 未明确 |
| **预期效果** | 选择文件夹后自动使用文件夹名作为源名称，用户可修改 |
| **Flutter 参照** | `lib/ui/features/sources/source_list_page.dart` (`_pickLocalFolder` 中 `p.basename`) |
| **Android 现状** | AddLocalDialog 要求手动填写名称，URI 路径显示在 rootPath 字段 |
| **任务** | AddLocalDialog 打开时自动从 URI 提取文件夹名填入 name 字段 |

---

## 模块三：文件浏览器 (FileBrowserScreen)

### 3.1 网格视图与列表/网格切换

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | M15 未覆盖 |
| **预期效果** | 文件浏览器支持列表/网格两种视图，顶部切换按钮 |
| **Flutter 参照** | `lib/ui/features/sources/widgets/file_grid_view.dart` (网格组件) |
| **Flutter 逻辑** | `lib/ui/features/sources/file_browser_page.dart` (视图切换) |
| **Android 现状** | 只有 LazyColumn 列表视图，无网格选项 |
| **任务** | 新建 FileGridItem 组件 + FileBrowserScreen 添加视图切换 + FileBrowserViewModel 添加 viewMode 状态 |

### 3.2 点击文件打开查看器

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | M15 未覆盖 |
| **预期效果** | 点击文件（图片/视频）→ 打开 FileSequenceViewerPage → 自动加载同目录所有兼容媒体 → 可翻页浏览 |
| **Flutter 参照** | `lib/ui/features/viewer/file_sequence_viewer_page.dart` (文件序列查看器) |
| **Flutter 适配层** | `lib/ui/features/viewer/file_viewer_page.dart` (FileViewerPage — 文件浏览器→查看器的适配层) |
| **Android 现状** | 点击文件夹 = 进入，点击文件 = 仅切换选中状态，无法查看内容 |
| **任务** | 新建 FileSequenceViewerScreen + FileViewerScreen + FileBrowserScreen 添加文件点击→导航逻辑 |

### 3.3 批量添加弹窗 (BatchAddResourcesDialog)

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 缺失 |
| **PRD** | M27 已实现 UseCase，但 UI 未对接 |
| **预期效果** | 选中文件后点击"添加入库" → 弹出统一弹窗（组织模式选择+智能判定开关+标签选择+新建标签） |
| **Flutter 参照** | `lib/ui/features/sources/widgets/batch_add_resources_dialog.dart` (完整弹窗) |
| **Flutter 逻辑** | `lib/ui/features/sources/widgets/batch_add_resources_dialog.dart` (`BatchAddDialogResult` — organizationMode + tagIds) |
| **Android 现状** | FileBrowserScreen 底栏"添加入库"按钮直接调用 `batchAddResourcesUseCase`，无弹窗，无法选择标签和组织模式 |
| **任务** | 新建 BatchAddResourcesDialog + FileBrowserScreen 底栏按钮改为先弹窗再调用 UseCase（传入 orgMode + tagIds） |

### 3.4 目录树侧边栏

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 缺失 |
| **PRD** | 未单独提出 |
| **预期效果** | 宽屏布局下左侧显示目录树，可快速跳转到任意层级 |
| **Flutter 参照** | `lib/ui/features/sources/widgets/directory_tree.dart` |
| **Android 现状** | 只能通过逐级进入+返回上级导航 |
| **任务** | 新建 DirectoryTree 组件（可后续迭代，优先级较低） |

---

## 模块四：标签管理 (TagManagerScreen)

### 4.1 点击标签跳转首页筛选

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 点击标签 → 跳转首页并预置该标签的筛选条件 |
| **Flutter 参照** | `lib/ui/features/tags/tag_manager_page.dart` (`_onTagTap` → `context.go('/home?filterTag=${tag.id}')`) |
| **Android 现状** | TagListItem 点击打开编辑弹窗，无法跳转首页 |
| **任务** | TagListItem 添加"查看资源"操作（跳转首页+预设筛选） |

### 4.2 内置/自定义分区标题

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 细节缺失 |
| **PRD** | 未单独提出 |
| **预期效果** | 标签列表分为"内置标签"和"自定义标签"两个区域，各有标题 |
| **Flutter 参照** | `lib/ui/features/tags/tag_manager_page.dart` (`_buildSectionTitle`) |
| **Android 现状** | 所有标签混排（`sortedByDescending { it.isBuiltIn }`），无分区标题 |
| **任务** | TagManagerScreen 添加分区标题 + 分组显示 |

### 4.3 标签资源计数来源

| 项目 | 说明 |
|------|------|
| **差距类型** | 数据来源可能不一致 |
| **PRD** | 未明确 |
| **预期效果** | 每个标签显示关联的资源数量 |
| **Flutter 参照** | `lib/ui/features/tags/tag_manager_page.dart` (`_loadResourceCounts` → `tagRepo.tagResourceCounts()`) — 通过 ResourceTag 关联表实时查询 |
| **Android 现状** | `tag.resourceCount` — 来源需确认是否为 TagEntity 上的冗余字段 |
| **任务** | 确认 `resourceCount` 数据来源是否正确；如不正确，改为通过关联表查询 |

---

## 模块五：查看器 (ViewerScreen) — 差距最大

### 5.1 组织模式路由分发 (ResourceViewerPage)

| 项目 | 说明 |
|------|------|
| **差距类型** | 架构缺失 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 点击资源 → 根据 organizationMode 自动路由到对应页面：chapter/chapterGallery → ChapterListScreen，flatgrid → FlatGridScreen，gallery → GalleryScreen，direct → ViewerScreen |
| **Flutter 参照** | `lib/ui/features/viewer/resource_viewer_page.dart` (`_loadAndNavigate` 中的 switch) |
| **Android 现状** | 所有资源都直接进入 ViewerScreen，不区分组织模式 |
| **任务** | 新建 ResourceViewerScreen 路由分发器 + 修改 HomeScreen 的 onNavigateToViewer 路由目标 |

### 5.2 FlatGridScreen（平铺网格页）

| 项目 | 说明 |
|------|------|
| **差距类型** | 页面完全缺失 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 显示资源目录下所有文件的网格视图，支持文件夹下钻，点击文件打开查看器 |
| **Flutter 参照** | `lib/ui/features/viewer/flat_grid_page.dart` (完整页面) |
| **Flutter ViewModel** | `lib/ui/features/viewer/view_models/flat_grid_view_model.dart` |
| **Android 现状** | 无对应页面 |
| **任务** | 新建 FlatGridScreen + FlatGridViewModel + 复用 FileGridItem 组件 |

### 5.3 GalleryScreen（画廊页）

| 项目 | 说明 |
|------|------|
| **差距类型** | 页面完全缺失 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 递归展开所有子文件夹的全部兼容文件到一个大网格，点击打开查看器 |
| **Flutter 参照** | `lib/ui/features/viewer/gallery_page.dart` (完整页面) |
| **Flutter ViewModel** | `lib/ui/features/viewer/view_models/gallery_view_model.dart` |
| **Android 现状** | 无对应页面 |
| **任务** | 新建 GalleryScreen + GalleryViewModel |

### 5.4 OrgModeSwitcher（组织模式切换器）

| 项目 | 说明 |
|------|------|
| **差距类型** | 组件完全缺失 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 并排模式按钮（章节/章廊/平铺/画廊），选中项白色凸起，未选中项灰色扁平；在 ChapterListScreen、FlatGridScreen、GalleryScreen 顶部显示 |
| **Flutter 参照** | `lib/ui/features/viewer/widgets/org_mode_switcher.dart` (完整组件) |
| **Android 现状** | 无此组件，组织模式只能在首页长按详情弹窗中修改 |
| **任务** | 新建 OrgModeSwitcher 组件 + 在各查看页面顶栏集成 |

### 5.5 点击区域翻页

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 屏幕左 25% 点击 = 上一页，右 25% = 下一页，中间 50% = 切换工具栏 |
| **Flutter 参照** | `lib/ui/features/viewer/viewer_page.dart` (`_handlePointerUp` — 坐标判断 + 方向感知) |
| **Android 现状** | 整个页面 clickable 只切换工具栏可见性 |
| **任务** | ViewerScreen 添加 PointerInput 处理 → 根据点击位置和阅读方向决定翻页/切换工具栏 |

### 5.6 双击缩放

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 双击图片 → 放大到 2x → 再次双击还原；放大后支持拖动查看细节 |
| **Flutter 参照** | `lib/ui/features/viewer/viewer_page.dart` (`_handleDoubleTap`, `InteractiveViewer`, `_zoomedPages`) |
| **Android 现状** | 无缩放功能 |
| **任务** | PageContent 中集成 `detectTapGestures(onDoubleTap)` + `Modifier.graphicsLayer(scale)` + 拖动支持 |

### 5.7 双页模式实现

| 项目 | 说明 |
|------|------|
| **差距类型** | 设置已存储，Viewer 未实现 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 根据设置（auto/single/double）和窗口宽度，将两页合并为一个 spread 显示；宽图（aspectRatio ≥ 1.2）不参与配对 |
| **Flutter 参照** | `lib/ui/features/viewer/viewer_page.dart` (`_doublePageRequested`, `_viewerPositions`, `_ViewerPosition`, `_canPairPage`) — 约 200 行逻辑 |
| **Flutter ViewModel** | `lib/ui/features/viewer/view_models/viewer_view_model.dart` (`DoublePageMode`, `applyDoublePageMode`) |
| **Android 现状** | SettingsScreen 有双页模式选项，SettingsViewModel 读写 AppConfig.doublePageMode，但 ViewerScreen 的 HorizontalPager 始终单页 |
| **任务** | ViewerViewModel 读取 AppConfig → ViewerScreen HorizontalPager 根据模式渲染 spread（Row 两页）→ 宽图判断 → 拖动中冻结翻转 |

### 5.8 翻页方向 (RTL/LTR)

| 项目 | 说明 |
|------|------|
| **差距类型** | 设置已存储，Viewer 未实现 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | RTL 模式下：视觉位置→逻辑页码反转、键盘左右键反转、点击区域反转、SlideBar 方向反转 |
| **Flutter 参照** | `lib/ui/features/viewer/viewer_page.dart` (`_logicalPositionForVisual`, `_visualPositionForLogical`, `_handleKeyEvent`, `_handlePointerUp`) |
| **Flutter ViewModel** | `lib/ui/features/viewer/view_models/viewer_view_model.dart` (`PageDirection`, `applyPageDirection`) |
| **Android 现状** | SettingsScreen 有翻页方向选项（RTL/LTR/垂直），ViewerScreen 的 HorizontalPager 始终 LTR |
| **任务** | ViewerViewModel 读取 AppConfig → HorizontalPager reverse 映射 → 点击区域方向感知 → 键盘方向感知 → SlideBar RTL 支持 |

### 5.9 跨章节连续阅读

| 项目 | 说明 |
|------|------|
| **差距类型** | 设置已存储，Viewer 未实现 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 章节末页继续滑动 → 自动切换到下一章 → 顶部显示过渡提示（"下一章: XXX"）→ pushReplacement 替换当前查看器 |
| **Flutter 参照** | `lib/ui/features/viewer/viewer_page.dart` (`_tryCrossChapter`, `_displayChapterHint`) |
| **Flutter ViewModel** | `lib/ui/features/viewer/view_models/viewer_view_model.dart` (`chapters`, `currentChapterIndex`, `onNavigateChapter`, `getNextChapterName`, `getPrevChapterName`) |
| **Android 现状** | SettingsScreen 有跨章节开关，但 ViewerScreen 无 chapters 数据、无跨章节导航逻辑 |
| **任务** | ViewerViewModel 添加 chapters + currentChapterIndex + onNavigateChapter → ViewerScreen 章节末尾检测 + 过渡提示 + 章节切换 |

### 5.10 ViewerToolbar 扩展功能

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 缺失 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 工具栏除返回+页面信息外，还包含：收藏按钮、翻页方向切换按钮、双页模式切换按钮 |
| **Flutter 参照** | `lib/ui/features/viewer/widgets/viewer_toolbar.dart` (`isFavorited`, `onFavoriteTap`, `pageDirection`, `doublePageMode`, `onPageDirectionChanged`, `onDoublePageModeChanged`) |
| **Android 现状** | ViewerToolbar 只有返回箭头 + resourceName + pageInfo |
| **任务** | ViewerToolbar 添加收藏按钮 + 翻页方向按钮 + 双页模式按钮（后两者可先做 UI，待 5.7/5.8 完成后对接） |

### 5.11 VideoSeekGestureArea

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 缺失 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 视频播放时底部大范围水平拖动热区 → 拖动 seek（与 PageView 横向滚动竞争，内层优先） |
| **Flutter 参照** | `lib/ui/features/viewer/widgets/video_seek_gesture_area.dart` (完整组件) |
| **Android 现状** | VideoPlayer 组件内无独立的 seek 手势热区 |
| **任务** | 新建 VideoSeekGestureArea 组件 + 集成到 VideoPlayer |

### 5.12 页面缓存策略

| 项目 | 说明 |
|------|------|
| **差距类型** | 性能差距 |
| **PRD** | 未单独提出 |
| **预期效果** | 200MB LRU 页面缓存，当前页同步加载 ±3 页异步预加载 |
| **Flutter 参照** | `lib/ui/features/viewer/view_models/viewer_view_model.dart` (`_pageCache`, `_maxCacheBytes`, `_preloadPages`, `_evictIfNeeded`) |
| **Android 现状** | 每次翻页都重新加载 bitmap，无缓存 |
| **任务** | ViewerViewModel 添加 LRU 缓存 + 预加载逻辑 |

### 5.13 单页错误重试

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 缺失 |
| **PRD** | 未单独提出 |
| **预期效果** | 单页加载失败时显示"重试"按钮，不影响其他页面 |
| **Flutter 参照** | `lib/ui/features/viewer/viewer_page.dart` (`_buildPageError`, `_retryPage`) |
| **Android 现状** | 只有全局错误状态，无单页重试 |
| **任务** | PageContent 添加单页错误状态 + 重试按钮 |

---

## 模块六：章节列表 (ChapterListScreen)

### 6.1 封面缩略图

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 缺失 |
| **PRD** | M21 未覆盖 |
| **预期效果** | 章节卡片左侧/上方显示实际封面缩略图（从 ThumbnailRepository 加载） |
| **Flutter 参照** | `lib/ui/features/viewer/chapter_list_page.dart` (`_loadChapterThumbnail` → `thumbnailRepo.preview`) |
| **Android 现状** | ChapterItem 左侧显示灰色占位符 + Folder 图标 |
| **任务** | ChapterItem 改为加载实际缩略图（复用 ThumbnailRepository） |

### 6.2 网格/列表视图切换

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | 未单独提出，但 Flutter 已实现 |
| **预期效果** | 顶栏 grid/list toggle 按钮，切换网格和列表两种视图 |
| **Flutter 参照** | `lib/ui/features/viewer/chapter_list_page.dart` (`_buildViewModeToggle`, `_buildChapterGrid`, `_buildChapterList`) |
| **Android 现状** | 只有列表视图 |
| **任务** | ChapterListScreen 添加视图切换 + ChapterListViewModel 添加 viewMode 状态 |

### 6.3 响应式双栏布局

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 缺失 |
| **PRD** | 未单独提出 |
| **预期效果** | 宽屏（≥900dp）：左侧封面面板 + 右侧章节列表；窄屏：上下布局 |
| **Flutter 参照** | `lib/ui/features/viewer/chapter_list_page.dart` (`_buildWideLayout`, `_buildNarrowLayout`, `_buildCoverPanel`, `_buildCoverPanelCompact`) |
| **Android 现状** | 只有单一纵向列表布局 |
| **任务** | ChapterListScreen 添加 BoxWithConstraints + 宽窄布局分支 |

### 6.4 散落文件 (Loose Files)

| 项目 | 说明 |
|------|------|
| **差距类型** | 功能完全空白 |
| **PRD** | 未单独提出 |
| **预期效果** | 章节列表底部显示资源根目录下不属于任何子目录的独立文件（图片/视频/PDF），点击可查看 |
| **Flutter 参照** | `lib/ui/features/viewer/chapter_list_page.dart` (`looseFiles`, `_buildLooseFileCard`, `_buildLooseFileListItem`, `_openLooseFile`) |
| **Flutter 逻辑** | `lib/ui/features/viewer/view_models/chapter_list_view_model.dart` (`looseFiles` — 从目录条目中过滤非目录文件) |
| **Android 现状** | ChapterListScreen 只显示章节，不显示散落文件 |
| **任务** | ChapterListViewModel 添加 looseFiles 状态 + ChapterListScreen 底部添加散落文件区域 |

### 6.5 章节点击行为修正

| 项目 | 说明 |
|------|------|
| **差距类型** | 逻辑缺陷 |
| **PRD** | M21 未明确 |
| **预期效果** | 点击章节 → 打开该章节的独立查看器（仅该章节图片），并传递 chapters 列表和当前索引用于跨章节导航 |
| **Flutter 参照** | `lib/ui/features/viewer/chapter_list_page.dart` (`_openChapter` → `_buildImageChapterViewer` → `ViewerPage(chapters:, currentChapterIndex:, onNavigateChapter:)`) |
| **Android 现状** | `onNavigateToViewer(resourceId)` — 点击任意章节都导航到同一个 resourceId 的 ViewerScreen，无法区分具体章节 |
| **任务** | ChapterListScreen 点击章节时传递 chapterIndex → ViewerViewModel 根据 index 加载对应章节的 ContentProvider |

---

## 模块七：设置 (SettingsScreen)

### 7.1 自定义缓存容量

| 项目 | 说明 |
|------|------|
| **差距类型** | UI 缺失 |
| **PRD** | 未单独提出 |
| **预期效果** | 容量选择区域除 500/1000/1500/2000 外，还有"自定义"按钮 → 弹窗输入数字 |
| **Flutter 参照** | `lib/ui/features/settings/settings_page.dart` (`_buildCustomCapacityChip`, `_showCustomCapacityDialog`) |
| **Android 现状** | 只有 4 个固定选项 |
| **任务** | SettingsScreen 添加"自定义" FilterChip + 输入弹窗 |

### 7.2 缓存路径显示

| 项目 | 说明 |
|------|------|
| **差距类型** | 逻辑缺陷 |
| **PRD** | 未明确 |
| **预期效果** | 显示实际缓存目录路径 |
| **Flutter 参照** | `lib/ui/features/settings/view_models/settings_view_model.dart` (`_cacheDirectory` → `thumbnailCacheService.getCacheDirectory()`) |
| **Android 现状** | 硬编码 `"/data/cache/thumbnails/"`，不反映实际路径 |
| **任务** | SettingsViewModel 从 ThumbnailRepository 获取实际缓存路径 |

### 7.3 垂直滚动翻页方向选项

| 项目 | 说明 |
|------|------|
| **差距类型** | 设置有选项但 Viewer 未实现 |
| **PRD** | 未明确 |
| **预期效果** | 选择"垂直滚动"后 Viewer 使用 VerticalPager |
| **Android 现状** | SettingsScreen 有 VERTICAL 选项，但 ViewerScreen 始终使用 HorizontalPager |
| **任务** | ViewerScreen 根据 pageDirection 选择 HorizontalPager / VerticalPager |

---

## 模块八：导航与架构

### 8.1 ChapterListScreen 导航集成

| 项目 | 说明 |
|------|------|
| **差距类型** | 集成缺失 |
| **PRD** | M21 已创建路由但未完整集成 |
| **预期效果** | 资源为 chapter/chapterGallery 模式时，点击资源 → ChapterListScreen（而非 ViewerScreen） |
| **Flutter 参照** | `lib/ui/features/viewer/resource_viewer_page.dart` (`_pushChapterList`) |
| **Android 现状** | AppNavGraph 有 ChapterList 路由，但 HomeScreen 的 `onNavigateToViewer` 始终导航到 ViewerScreen |
| **任务** | 与 5.1 一起实现 ResourceViewerScreen 路由分发 |

---

## 任务汇总

### 按优先级排列

#### P0 — 核心查看链路（不修则核心流程断裂）

| # | 任务 | 模块 | 预估 |
|---|------|------|------|
| 1 | ResourceViewerScreen 路由分发（5.1 + 8.1） | Viewer | L |
| 2 | FlatGridScreen 平铺网格页（5.2） | Viewer | M |
| 3 | GalleryScreen 画廊页（5.3） | M |
| 4 | OrgModeSwitcher 组件（5.4） | Viewer | S |
| 5 | 点击区域翻页（5.5） | Viewer | S |
| 6 | 双页模式实现（5.7） | Viewer | L |
| 7 | 翻页方向实现（5.8） | Viewer | M |
| 8 | 跨章节连续阅读（5.9） | Viewer | M |
| 9 | 章节点击行为修正（6.5） | ChapterList | S |

#### P1 — 重要功能补全

| # | 任务 | 模块 | 预估 |
|---|------|------|------|
| 10 | 搜索功能（1.1） | Home | M |
| 11 | 排序功能（1.2） | Home | S |
| 12 | 多选模式与批量删除（1.3） | Home | M |
| 13 | 点击文件打开查看器（3.2） | FileBrowser | L |
| 14 | 批量添加弹窗（3.3） | FileBrowser | M |
| 15 | ViewerToolbar 扩展（5.10） | Viewer | S |
| 16 | 双击缩放（5.6） | Viewer | M |
| 17 | 封面缩略图（6.1） | ChapterList | S |
| 18 | 散落文件（6.4） | ChapterList | S |

#### P2 — 体验优化

| # | 任务 | 模块 | 预估 |
|---|------|------|------|
| 19 | 收藏快捷操作（1.4） | Home | S |
| 20 | 分页加载（1.5） | Home | M |
| 21 | 重命名数据源（2.1） | Sources | S |
| 22 | 编辑 SMB 凭据（2.2） | Sources | M |
| 23 | 资源数量显示（2.3 + 2.4） | Sources | S |
| 24 | 添加本地文件夹自动命名（2.5） | Sources | S |
| 25 | 网格视图与切换（3.1） | FileBrowser | M |
| 26 | 标签跳转首页筛选（4.1） | Tags | S |
| 27 | 内置/自定义分区标题（4.2） | Tags | S |
| 28 | 网格/列表视图切换（6.2） | ChapterList | S |
| 29 | 响应式双栏布局（6.3） | ChapterList | M |
| 30 | 自定义缓存容量（7.1） | Settings | S |
| 31 | 缓存路径显示（7.2） | Settings | S |

#### P3 — 性能与增强

| # | 任务 | 模块 | 预估 |
|---|------|------|------|
| 32 | VideoSeekGestureArea（5.11） | Viewer | S |
| 33 | 页面缓存策略（5.12） | Viewer | M |
| 34 | 单页错误重试（5.13） | Viewer | S |
| 35 | 目录树侧边栏（3.4） | FileBrowser | L |
| 36 | 垂直滚动选项（7.3） | Settings+Viewer | M |

### 按预估工作量统计

| 预估 | 数量 | 说明 |
|------|------|------|
| **S (≤1天)** | 18 | 简单 UI 组件或小逻辑 |
| **M (1-3天)** | 14 | 中等页面或复杂逻辑 |
| **L (3-5天)** | 4 | 完整新页面或架构变更 |

### 依赖关系

```
5.1 路由分发 ──→ 5.2 FlatGrid / 5.3 Gallery / 6.5 章节点击
5.4 OrgModeSwitcher ──→ 5.2 / 5.3 / 6.2
5.7 双页模式 ──→ 5.8 翻页方向（共享 ViewerPosition 逻辑）
5.8 翻页方向 ──→ 5.5 点击翻页（方向感知）
5.9 跨章节 ──→ 6.5 章节点击（需要 chapters 数据传递）
3.2 文件查看器 ──→ 5.2 FlatGrid / 5.3 Gallery（共享 FileSequenceViewer）
```

### 建议执行顺序

**第一批**（核心链路打通）：5.1 → 5.4 → 5.2 → 5.3 → 6.5 → 6.1
**第二批**（Viewer 高级功能）：5.5 → 5.7 → 5.8 → 5.9 → 5.10
**第二批并行**（Home 补全）：1.1 → 1.2 → 1.3
**第三批**（FileBrowser + ChapterList）：3.2 → 3.3 → 6.2 → 6.4
**第四批**（体验优化）：其余 P2 任务

---

## 相关文档

- Flutter 源码: `resource_viewer_flutter/lib/`
- Android 源码: `app/src/main/java/dev/wucheng/resource_viewer/`
- Issue 模板: `doc/issues/ISSUE_TEMPLATE.md`

---

## 分批产出记录

### 2026-06-27 — 批次 1：SMB 查看性能与页面容错 ✅

- 完成 5.12：200MB LRU 页面缓存、同页请求合并、当前页优先、前后各 2 页顺序预取。
- 完成 5.13：单页错误状态支持独立重试，失败页面不写入缓存。
- 修复 Pager 首屏并发读取最多 5 张 SMB 原图的问题，详见 `2026-06-27-perf-smb-image-viewer-slow.md`。
- 修复 JVM 测试基线：Android `Log` 默认值与 `SourceListViewModel` 可注入 I/O dispatcher。
- 验收：`./gradlew test lint build` 全部通过（423 tests）。

### 2026-06-27 — 批次 2A：章节资源路由与选章阅读 ✅

- 部分完成 5.1/8.1：主页按组织模式分发；`CHAPTER`、`CHAPTER_GALLERY` 正确进入章节列表。FlatGrid/Gallery 专用页面留待批次 2B。
- 完成 6.5 的基础链路：章节点击携带 URL 编码后的 `chapterPath`，Viewer 只加载所选章节。
- 修正章节画廊 Provider：递归读取章内子目录图片，不再只显示章节根层图片。
- 新增纯函数路由决策测试、章节路由编码测试、递归 Provider 测试。

### 2026-06-27 — 批次 2B：FlatGrid 与 Gallery 核心页面 ✅

- 完成 5.1/8.1 的四模式主页分发：章节、章节画廊、平铺网格、画廊均进入对应页面。
- 完成 5.2 核心链路：FlatGrid 展示图片与文件夹、支持逐层下钻和返回上级。
- 完成 5.3 核心链路：Gallery 递归扁平资源下全部图片。
- 点击网格图片按正确初始索引进入 Viewer，并可在当前序列连续翻页。
- Viewer 新增 `contentPath + initialPage` 参数，Gallery Provider 使用递归模式。
- SMB 网格暂用轻量类型卡片，避免未经调度的并发原图缩略图请求；实际缩略图与 OrgModeSwitcher 后续补齐。

### 2026-06-27 — 批次 3：Viewer 交互与 Home 管理 ✅

- 完成 5.5、5.7、5.8、7.3：点击区域翻页、单/双/自动 spread、RTL/LTR、垂直 Pager 与 RTL SlideBar。
- Viewer 从 Room 读取方向和双页设置，工具栏可循环切换并持久化。
- 完成 1.1、1.2、1.3 核心功能：首页实时搜索、四种排序、多选/全选、批量删除。
- 新增方向点击区域单元测试与 Home 搜索/排序测试。

### 2026-06-27 — 批次 4：跨章节阅读 + 收藏 + 数据源管理 + 设置 ✅

- 完成 5.4：OrgModeSwitcher 组件（章节/章廊/平铺/画廊四模式切换），集成到 ChapterListScreen 和 ContentGridScreen。
- 完成 5.9：跨章节连续阅读，Viewer 加载章节列表并在末页自动切换，顶部显示过渡提示。
- 完成 5.10 部分：ViewerToolbar 添加收藏按钮（Star/StarBorder）。
- 完成 1.4：资源收藏功能，ResourceEntity 新增 `favorited` 字段（DB v2→v3 迁移），首页网格可切换收藏。
- 完成 6.1：ChapterItem 使用 Coil AsyncImage 加载实际封面缩略图。
- 完成 6.4：ChapterListScreen 底部显示散落文件区域（图片/视频/PDF）。
- 完成 2.1/2.3/2.4/2.5：SourceListScreen 增加重命名、删除（含确认弹窗）、资源数量显示、本地文件夹自动命名。
- 完成 4.1/4.2：TagManagerScreen 内置/自定义分区标题，点击标签返回首页筛选。
- 完成 7.1/7.2：SettingsScreen 自定义缓存容量弹窗、实际缓存路径显示。
- 修复 SourceListViewModelTest 编译错误（新增 resourceRepository 参数）。
- 验收：`./gradlew test lint build` 全部通过。

### 2026-06-27 — 批次 5：Viewer 高级交互 + FileBrowser + ChapterList 增强 + 分页 ✅

- 完成 5.6：双击缩放（2x），支持拖动查看细节，双击还原。
- 完成 5.11：VideoSeekGestureArea，底部 20% 区域水平拖动 seek，显示预览时间。
- 完成 2.2：编辑 SMB 凭据，SourceListScreen 增加编辑按钮和弹窗，复用 AddSmbDialog。
- 完成 3.1：FileBrowserScreen 网格/列表视图切换。
- 完成 6.2：ChapterListScreen 网格/列表视图切换，新增 ChapterGridItem。
- 完成 1.5：HomeScreen 分页加载（键集分页），滚动到底部自动加载更多。
- 验收：`./gradlew test lint build` 全部通过。

### 2026-06-27 — 批次 6：批量添加 + 文件预览 + 双栏布局 ✅

- 完成 3.3：批量添加弹窗（BatchAddResourcesDialog），支持组织模式选择和标签勾选。
  - 新增 `BatchAddResourcesDialog` composable，含自动检测/章节/平铺三种组织模式。
  - 扩展 `BatchAddResourcesUseCase` 支持 `organizationMode` 和 `tagIds` 参数。
  - `ResourceRepository.setResourceTags()` 批量设置资源标签。
  - `TagDao.getAllTagsSnapshot()` / `TagRepository.getAllTagsOnce()` 非 Flow 查询。
  - `FileBrowserViewModel` 新增 `showBatchAddDialog()` / `confirmBatchAdd()` 方法。
- 完成 3.2：文件预览（FilePreviewOverlay），支持图片缩放/平移。
- 完成 6.3：ChapterListScreen 双栏布局（BoxWithConstraints ≥900dp 阈值，左侧封面面板 + 右侧章节列表）。
- 验收：`./gradlew test build` 全部通过。

### 2026-06-27 — 批次 7：目录树侧边栏 ✅

- 完成 3.4：文件浏览器目录树侧边栏。
  - `DirectoryTreePanel` composable：展示当前路径层级，点击可跳转任意层级。
  - 宽屏（≥900dp）：左侧 persistent drawer，240dp 宽。
  - 窄屏：覆盖层模式，280dp 宽，点击背景关闭。
  - `FileBrowserViewModel` 新增 `toggleDirectoryTree()` / `hideDirectoryTree()` / `navigateToPathSegment()` 方法。
- 验收：`./gradlew test build` 全部通过。
- **全部 36/36 项差距已补齐。**
