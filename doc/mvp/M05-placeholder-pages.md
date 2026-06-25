# M05 — 三个 Tab 占位页面

> 轨道 0 · Stage 5/29 | 前置: M03,M01 | 依赖共享: `doc/share/04-navigation-routes.md` `doc/share/05-theme-tokens.md` | 🟢 独占

## 执行目标

创建三个 Tab 的空壳页面 + EmptyState 组件。后续 M23/M25 替换为完整实现。

## 共享契约引用

- `doc/share/04-navigation-routes.md` — 路由挂载点
- `doc/share/05-theme-tokens.md` — 排版/颜色引用
- `doc/share/07-directory-layout.md` — 文件位置

## 子任务

### M05.1 HomeScreen（骨架）

显示 "资源库" 标题 + `EmptyState` 组件。

**产出物**：`ui/screens/home/HomeScreen.kt`

### M05.2 SourceListScreen（骨架）

显示 "数据源" 标题 + `EmptyState` 组件。

**产出物**：`ui/screens/sources/SourceListScreen.kt`

### M05.3 SettingsScreen（骨架）

显示 "设置" 标题。

**产出物**：`ui/screens/settings/SettingsScreen.kt`

### M05.4 EmptyState 组件

可复用的空状态组件，支持两种模式：
- 完全空白（hasResources=false）：引导去添加数据源
- 筛选结果为空（hasResources=true, isFiltered=true）：提示清除筛选

**产出物**：`ui/components/EmptyState.kt`

### M05.5 挂载到 NavHost

在 `AppNavGraph.kt` 中将三个 Screen composable 挂载到实际页面 Composable。

**产出物**：`ui/navigation/AppNavGraph.kt`（修改 composable 内容）

## 验收标准

- [ ] 三个页面正确挂载到 Navigation 对应路由
- [ ] Tab 切换时页面标题正确显示
- [ ] EmptyState 显示 "还没有资源" + "去添加数据源" 按钮
- [ ] `./gradlew test` 通过
- [ ] 应用可运行看到 3 Tab 切换的完整 App Shell
