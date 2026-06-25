# M13 — 文件浏览器

> 轨道 3 · Stage 13/29 | 前置: M12 | 依赖共享: `doc/share/02-interfaces.md` `doc/share/04-navigation-routes.md` | 🟡 聚合(Screen.kt + AppNavGraph.kt 追加路由)

## 执行目标

实现文件浏览器页面：双视图（列表/网格）+ 多选 + 导航面包屑。

## 共享契约引用

- `doc/share/02-interfaces.md` — FileEntry 定义
- `doc/share/04-navigation-routes.md` — 追加 FileBrowser 路由
- `doc/share/05-theme-tokens.md` — 图标/排版
- `@design/file-browser.html` — 原型参考

## 子任务

### M13.1 FileBrowserViewModel

管理目录导航栈、当前文件列表、多选状态、视图切换（列表/网格）。

**产出物**：`ui/screens/sources/FileBrowserViewModel.kt`

### M13.2 FileBrowserScreen

双视图布局：
- 顶部面包屑导航
- 列表视图 / 网格视图切换按钮
- 底部多选操作栏（添加资源/批量打标签）

**产出物**：`ui/screens/sources/FileBrowserScreen.kt`

### M13.3 FileListView / FileGridView

列表和网格两个子 Composable。

**产出物**：`ui/screens/sources/components/FileListView.kt`、`FileGridView.kt`

### M13.4 追加路由

在 `Screen.kt` 追加 `FileBrowser` 路由。在 `AppNavGraph.kt` 追加 composable。

**产出物**：`ui/navigation/Screen.kt`（追加）、`ui/navigation/AppNavGraph.kt`（追加）

## 验收标准

- [ ] 点击 SourceCard → 导航到文件浏览器，显示根目录内容
- [ ] 文件夹优先排序
- [ ] 列表/网格切换正常
- [ ] 面包屑导航可点击返回上级
- [ ] 多选后底部栏出现
- [ ] `./gradlew build` 通过
