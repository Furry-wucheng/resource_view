# feature: 导航栏响应式布局 — 平板使用侧边 NavigationRail

> 日期: 2026-06-26 | 类型: feature | 状态: ✅ 已完成

## 描述

当前导航栏在平板设备上仍显示为底部 `NavigationBar`，未按响应式规范切换为侧边 `NavigationRail`。
需要根据屏幕宽度自适应切换导航组件位置。

## 验收标准

- [x] 手机（Compact）：底部 `NavigationBar`，3 Tab
- [x] 平板/折叠屏（Medium/Expanded）：侧边 `NavigationRail`
- [x] 窗口 resize 时导航组件实时切换
- [x] `./gradlew build` 通过
- [x] `./gradlew test` 通过

## 影响分析

| 维度 | 内容 |
|------|------|
| 修改文件 | `ui/components/AppShell.kt`(新增)、`MainActivity.kt`(修改)、`gradle/libs.versions.toml`(追加)、`app/build.gradle.kts`(追加) |
| 影响 stage | M04 AppShell 响应式布局（本提案即实现 M04） |
| 聚合文件 | `gradle/libs.versions.toml` 追加 material3-window-size-class 依赖 |

## 是否需合并回原文档

[x] 是 → 需更新 `doc/decisions/AGENTS.md` 和 `doc/mvp/AGENTS.md` 进度表
[ ] 否

## 执行计划

1. RED — 编写 `AppShellTest.kt`，测试手机/平板两种布局
2. GREEN — 创建 `AppShell.kt`，重构 `MainActivity.kt` 使用 AppShell
3. REFACTOR — 清理多余的 `NavigationSideBar.kt`（逻辑合入 AppShell）
4. 验证 — `./gradlew build && ./gradlew test`
5. 决策日志 — 创建 `doc/decisions/M04-app-shell.md`

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/components/AppShell.kt` | 🆕 新增 | 响应式 Shell，含 NavigationRail |
| `MainActivity.kt` | ✏️ 修改 | 使用 AppShell 替代直接 Scaffold |
| `gradle/libs.versions.toml` | ✏️ 修改 | 追加 material3-window-size-class |
| `app/build.gradle.kts` | ✏️ 修改 | 追加依赖 |
| `ui/navigation/NavigationSideBar.kt` | 🗑️ 删除 | 逻辑合入 AppShell |
| `ui/component/AppShellTest.kt` | 🆕 新增 | Compose UI 测试 |
