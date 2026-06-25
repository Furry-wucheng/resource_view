# M04 — AppShell 响应式布局

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M03

## 设计决策

### D-001: 使用 WindowWidthSizeClass 而非固定 dp 阈值

- **背景**: M04 规范文档写的是 `< 900dp` / `≥ 900dp` 作为分界，但 Material3 提供了 `WindowWidthSizeClass` 标准分类
- **选择**: 采用 `WindowWidthSizeClass`，`Compact` 用底部栏，`Medium`/`Expanded` 用侧边栏
- **备选**: 固定 900dp 阈值 — 需要自行计算 dp，且与 Material3 设计规范不一致
- **影响文件**: `ui/components/AppShell.kt:30`

### D-002: 导航逻辑内聚到 AppShell

- **背景**: 最初实现将 `NavigationBar` 和 `NavigationRail` 分别放在 `BottomNavBar.kt` 和 `NavigationSideBar.kt`
- **选择**: 合并到 `AppShell.kt` 内部的 `private` 组件，`MainActivity` 只需调用 `AppShell`
- **备选**: 保留独立文件 — 但 M04 规范要求 `AppShell.kt` 作为单一入口
- **影响文件**: `ui/components/AppShell.kt:45-130`

### D-003: material3-window-size-class 依赖别名

- **背景**: Gradle version catalog 中 `class` 是保留字，不能用作 alias
- **选择**: 使用 `androidx-compose-material3-windowsize` 作为别名
- **备选**: 其他命名如 `material3-wsc` — 语义不够清晰
- **影响文件**: `gradle/libs.versions.toml:42`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/components/AppShell.kt` | 🆕 新增 | 响应式 Shell，含 NavigationBar + NavigationRail |
| `ui/navigation/NavigationSideBar.kt` | 🗑️ 删除 | 逻辑合入 AppShell |
| `MainActivity.kt` | ✏️ 修改 | 使用 AppShell 替代直接 Scaffold |
| `gradle/libs.versions.toml` | ✏️ 修改 | 追加 material3-window-size-class |
| `app/build.gradle.kts` | ✏️ 修改 | 追加依赖 |
| `ui/component/AppShellTest.kt` | 🆕 新增 | Compose UI 测试（Compact/Medium/Expanded） |
| `doc/issues/2026-06-26-feature-responsive-navigation.md` | 🆕 新增 | 提案记录 |

### D-004: Expanded 布局必须有 Scaffold

- **背景**: 最初 Expanded 布局用 `Row(NavigationRail + AppNavGraph)`，没有 Scaffold，导致内容区无主题背景色（白色）
- **选择**: Expanded 布局改为 `Row(NavigationRail + Scaffold)`，Scaffold 自动应用 Material3 主题背景色
- **备选**: 手动设置 Surface 背景色 — 不如 Scaffold 统一，且丢失 FAB/Snackbar 等槽位
- **影响文件**: `ui/components/AppShell.kt:34-42`

### D-005: 移除 enableEdgeToEdge 的 DisposableEffect

- **背景**: 最初用 `DisposableEffect` + `SystemBarStyle.auto()` 动态更新系统栏，代码复杂且多余
- **选择**: 只保留 `enableEdgeToEdge()` 一次调用，Material3 Scaffold 会自动根据主题设置系统栏颜色
- **备选**: 保留 DisposableEffect — 过度工程化，增加维护负担
- **影响文件**: `MainActivity.kt:19`

### D-006: 删除 BottomNavBar.kt，导航逻辑统一到 AppShell

- **背景**: M03 创建的 `BottomNavBar.kt` 与 AppShell 内的 `AppNavigationBar` 功能重复，且 `selected = false` 硬编码
- **选择**: 删除 `BottomNavBar.kt` 及其测试，在 AppShell 中用 `currentBackStackEntryAsState` 跟踪路由状态
- **备选**: 保留 BottomNavBar 并修复 selected — 增加维护成本，两处导航逻辑
- **影响文件**: `ui/components/AppShell.kt:68-110`、`ui/navigation/BottomNavBar.kt`(删除)

### D-007: 修复 04-navigation-routes.md 语法错误

- **背景**: `doc/share/04-navigation-routes.md` 中 Screen 密封类末尾有多余的 `}`
- **选择**: 用户授权修改只读共享文件，删除多余闭合括号
- **影响文件**: `doc/share/04-navigation-routes.md:37`

## 已知问题 / TODO

- 无
