# filebrowser-scroll-position — 文件浏览器目录滚动位置记忆

> 时间: 2026-06-30 | Agent: opencode | 状态: ✅ 已完成 | 前置: M13, file-browser-ux

## 设计决策

### D-001: 滚动位置缓存策略 — 内存 Map vs 持久化 vs 独立路由

- **背景**: FileBrowserScreen 使用单 ViewModel 单 Screen 管理所有目录层级，目录切换只是修改 `currentPath`，因此 Compose LazyList/LazyGrid 的滚动状态没有按目录隔离。用户反馈从子目录返回时丢失滚动位置。
- **选择**: 采用 **ViewModel 内存 Map** 方案。在 `FileBrowserViewModel` 中维护 `scrollPositions: Map<String, Pair<Int, Int>>`（path → firstVisibleItemIndex + scrollOffset）。在 UI 层通过显式 `LazyListState`/`LazyGridState` 实现：
  1. 离开目录前通过统一封装（`performOpenDirectory`、`performGoUp` 等）保存当前滚动位置
  2. 进入目录后通过 `LaunchedEffect(currentPath, viewMode)` 读取缓存并 `scrollToItem`
- **备选 A — DataStore 持久化**: 考虑过将滚动位置写进 `FileBrowserPrefsStore`，但写入频率高（每次切换目录都写磁盘），且用户明确需求是"临时的"，故放弃。
- **备选 B — 每层目录独立路由**: 将 `FileBrowser` 改为带 `path` 参数的路由，利用 Navigation Compose 自动保存/恢复所有状态。但改动量涉及 `Screen.kt`、`AppNavGraph.kt`、`BreadCrumb`、`DirectoryTree` 等多处交互入口，属于架构调整而非最小修复，放弃。
- **影响文件**:
  - `ui/screens/sources/FileBrowserViewModel.kt:64-70`
  - `ui/screens/sources/FileBrowserScreen.kt:99-129, 272-300`
- **被依赖**: 无额外依赖，纯 UI 状态增强

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 修改 | 新增 `scrollPositions` 内存缓存及 `saveScrollPosition`/`getScrollPosition` |
| `ui/screens/sources/FileBrowserScreen.kt` | ✏️ 修改 | 显式 `LazyListState`/`LazyGridState` + 统一目录切换封装 + `LaunchedEffect` 恢复位置 |
| `ui/screens/sources/FileBrowserViewModelTest.kt` | ✏️ 修改 | 补充 2 条滚动位置单元测试 |

## 已知问题 / TODO

- 无
