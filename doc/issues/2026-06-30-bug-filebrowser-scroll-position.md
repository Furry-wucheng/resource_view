# bug: 文件浏览器 — 返回父目录时丢失滚动位置

> 日期: 2026-06-30 | 类型: bug | 状态: ✅ 已完成

## 现象

在 FileBrowserScreen 中浏览文件夹时，如果滚动到某个位置后进入子目录，再返回父目录，列表会自动回到顶部（index 0），无法记住之前离开时的滚动位置。

## 复现步骤

1. 打开文件浏览器
2. 向下滚动列表/网格到某个位置
3. 点击进入一个子文件夹
4. 按返回键或面包屑返回到父目录
5. 观察到列表已回到顶部，而非离开时的位置

## 期望效果

返回父目录时，应自动恢复到离开前的滚动位置（包括列表和网格两种视图模式）。

## 影响分析

| 维度 | 内容 |
|------|------|
| 根因 | FileBrowser 使用单 ViewModel/单 Screen 管理所有层级目录；LazyColumn/LazyVerticalGrid 的滚动状态未按目录隔离保存，导致目录切换后滚动状态丢失 |
| 修改文件 | `ui/screens/sources/FileBrowserScreen.kt`、`ui/screens/sources/FileBrowserViewModel.kt` |
| 影响 stage | 无，属于已有阶段（M13 文件浏览、file-browser-ux）的 bug 修复 |

## 执行计划

1. ViewModel 层：新增内存滚动位置缓存 Map<String, Pair<Int, Int>>
2. UI 层：显式创建 LazyListState/LazyGridState，在目录切换前保存当前位置，进入目录后恢复之前保存的位置
3. 测试：补充滚动位置保存/恢复的单元测试

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 修改 | 新增 `scrollPositions` Map 及 `saveScrollPosition`/`getScrollPosition` |
| `ui/screens/sources/FileBrowserScreen.kt` | ✏️ 修改 | 显式状态管理 + 统一目录切换封装 + 恢复逻辑 |
| `ui/screens/sources/FileBrowserViewModelTest.kt` | ✏️ 修改 | 补充 2 条滚动位置测试 |
