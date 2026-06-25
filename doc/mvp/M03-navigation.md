# M03 — 导航系统

> 轨道 0 · Stage 3/29 | 前置: M02 | 依赖共享: `doc/share/04-navigation-routes.md` | 🟡 聚合(Screen.kt + AppNavGraph.kt)

## 执行目标

搭建 3 个底部 Tab 的路由框架 + 全屏路由占位。

## 共享契约引用

- `doc/share/04-navigation-routes.md` — Screen 密封类定义、NavHost 结构、BottomNavBar 组件
- `doc/share/03-di-contracts.md` — Hilt 集成点

## 子任务

### M03.1 Screen.kt

创建路由密封类，包含三个 Tab 路由 + Viewer + TagManager 占位路由。

**产出物**：`ui/navigation/Screen.kt`
**注意**：后续 stage 在此文件末尾追加新路由。

### M03.2 BottomNavBar.kt

创建底部导航栏 Composable，三个 Tab（首页库/数据源/设置），带图标和文字。

**产出物**：`ui/navigation/BottomNavBar.kt`

### M03.3 AppNavGraph.kt

创建 NavHost 骨架，包含三个 Tab 的 composable + Viewer/TagManager 占位 composable（内容暂时为空 Text）。

**产出物**：`ui/navigation/AppNavGraph.kt`
**注意**：后续 stage 在此文件末尾追加新 composable。

## 验收标准

- [ ] 路由结构含：home、sources、sources/{id}/browser（暂空）、settings、viewer/{resourceId}、tags/manager
- [ ] Tab 切换保持各 Tab 页面状态（saveState/restoreState）
- [ ] 查看器/标签管理页面覆盖底部导航栏
- [ ] `./gradlew build` 通过
