# M03 — 导航系统

> 时间: 2026-06-26 | Agent: opencode/clever-wolf | 状态: ✅ 已完成 | 前置: M02

## 设计决策

### D-001: 路由密封类设计
- **背景**: 需要定义应用的路由结构，支持底部 Tab 和全屏页面
- **选择**: 使用 Kotlin sealed class 定义路由，每个路由是一个 data object，包含 route 字符串
- **备选**: 使用枚举类或字符串常量，放弃原因：sealed class 更类型安全，支持参数化路由
- **影响文件**: `ui/navigation/Screen.kt:1-20`
- **被依赖**: M04, M05, M13, M14, M15, M21, M24 消费此路由定义

### D-002: 底部导航栏状态管理
- **背景**: Tab 切换需要保持各 Tab 的页面状态
- **选择**: 使用 Navigation Compose 的 saveState/restoreState 机制，配合 popUpTo 避免栈堆积
- **备选**: 手动管理 Fragment 状态，放弃原因：Compose 推荐使用 Navigation 组件
- **影响文件**: `ui/navigation/BottomNavBar.kt:25-60`
- **被依赖**: M04 AppShell 集成此组件

### D-003: 占位屏幕组件
- **背景**: M03 需要创建导航骨架，但实际屏幕内容由后续 stage 实现
- **选择**: 创建简单的占位屏幕，只显示文本标识，便于测试导航行为
- **备选**: 使用空 Composable，放弃原因：占位文本便于调试和测试
- **影响文件**: `ui/screens/home/HomeScreen.kt`, `ui/screens/sources/SourceListScreen.kt`, `ui/screens/settings/SettingsScreen.kt`, `ui/screens/viewer/ViewerScreen.kt`, `ui/screens/tags/TagManagerScreen.kt`
- **被依赖**: M05, M13, M14, M15 将替换这些占位组件

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/navigation/Screen.kt` | 🆕 新增 | 路由密封类，包含 5 个路由 |
| `ui/navigation/BottomNavBar.kt` | 🆕 新增 | 底部导航栏组件 |
| `ui/navigation/AppNavGraph.kt` | 🆕 新增 | NavHost 骨架 |
| `ui/screens/home/HomeScreen.kt` | 🆕 新增 | 首页占位屏幕 |
| `ui/screens/sources/SourceListScreen.kt` | 🆕 新增 | 数据源列表占位屏幕 |
| `ui/screens/settings/SettingsScreen.kt` | 🆕 新增 | 设置占位屏幕 |
| `ui/screens/viewer/ViewerScreen.kt` | 🆕 新增 | 查看器占位屏幕 |
| `ui/screens/tags/TagManagerScreen.kt` | 🆕 新增 | 标签管理占位屏幕 |
| `app/src/test/java/.../ScreenTest.kt` | 🆕 新增 | Screen 路由单元测试 |
| `app/src/androidTest/java/.../BottomNavBarTest.kt` | 🆕 新增 | BottomNavBar UI 测试 |
| `app/src/androidTest/java/.../AppNavGraphTest.kt` | 🆕 新增 | AppNavGraph 导航测试 |
| `gradle/libs.versions.toml` | ✏️ 修改 | 添加 material-icons-extended 依赖 |
| `app/build.gradle.kts` | ✏️ 修改 | 添加 material-icons-extended 依赖 |
| `MainActivity.kt` | ✏️ 修改 | 集成 AppNavGraph 和 BottomNavBar |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 已知问题 / TODO

- [ ] 占位屏幕组件需要在后续 stage 中替换为实际实现
- [x] ~~BottomNavBar 的选中状态需要与当前路由同步~~ → M04 AppShell 中通过 `currentBackStackEntryAsState` 实现
- [x] ~~BottomNavBar.kt 与 AppShell 功能重复~~ → M04 中删除，逻辑统一到 AppShell
- [ ] 全屏路由（Viewer, TagManager）需要隐藏底部导航栏（后续 stage 实现）
