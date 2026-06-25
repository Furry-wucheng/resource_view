# M04 — AppShell 响应式布局

> 轨道 0 · Stage 4/29 | 前置: M03 | 依赖共享: `doc/share/04-navigation-routes.md` | 🟢 独占

## 执行目标

根据屏幕宽度自适应切换 `NavigationBar`（手机）和 `NavigationRail`（平板/折叠屏）。

## 共享契约引用

- `doc/share/04-navigation-routes.md` — 底部导航栏组件定义

## 子任务

### M04.1 AppShell.kt

使用 `WindowSizeClass` 判定屏幕宽度：
- `< 900dp`：`NavigationBar`（底部 Tab）
- `≥ 900dp`：`NavigationRail`（侧边导航栏）
- `NavHost` 内嵌在 Scaffold 中

**产出物**：`ui/components/AppShell.kt`

### M04.2 集成到 MainActivity

更新 `MainActivity.kt`，将 `setContent` 的内容替换为 `AppShell`。

**产出物**：`MainActivity.kt`（修改）

## 验收标准

- [ ] 手机：底部 `NavigationBar`，3 Tab
- [ ] 平板/折叠屏：`NavigationRail`（侧边导航栏）
- [ ] 窗口 resize 时导航组件实时切换
- [ ] `./gradlew build` 通过
