# M16 — 底部标签栏

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M03

## 设计决策

### D-001: 固定 5 个 Tab 使用 remember 缓存

- **背景**: 底部标签栏需要固定 5 个 Tab（首页/知识/工具箱/我的/设置），每次重组时不应重复创建列表
- **选择**: 使用 `rememberNavTabs()` 函数配合 `remember` 缓存 `NavTab` 列表，Tab 定义使用字符串资源 ID 而非硬编码字符串
- **备选**: 直接在 `NAV_TABS` 常量中定义 — 放弃原因：无法使用 `stringResource()` 获取国际化字符串
- **影响文件**: `ui/components/AppShell.kt:58-74`
- **被依赖**: AppNavigationBar、AppNavigationRail 消费此列表

### D-002: 胶囊指示器使用 animateDpAsState 实现动效

- **背景**: 底部标签栏需要胶囊指示器动效，选中 Tab 时指示器平滑移动
- **选择**: 使用 `animateDpAsState` + `tween(300ms)` 动画，根据 `selectedIndex` 计算偏移量，指示器宽度固定 24dp
- **备选**: 使用 `AnimatedVisibility` 或自定义 `Canvas` 绘制 — 放弃原因：`animateDpAsState` 最简洁，性能好
- **影响文件**: `ui/components/AppShell.kt:145-172`
- **被依赖**: 无

### D-003: 导航状态使用 popUpTo + saveState/restoreState 优化

- **背景**: Tab 切换时需要避免重组抖动，同时保留各 Tab 的滚动位置等状态
- **选择**: `navigateWithState()` 使用 `popUpTo(Screen.Home.route) { saveState = true }` + `launchSingleTop = true` + `restoreState = true`
- **备选**: 直接 `navigate(route)` — 放弃原因：会导致回退栈堆积，Tab 状态丢失
- **影响文件**: `ui/components/AppShell.kt:130-143`
- **被依赖**: 无

### D-004: 新增 3 个占位页面使用统一结构

- **背景**: Knowledge、Toolbox、Profile 三个新 Tab 需要对应页面
- **选择**: 创建统一的占位页面结构：TopAppBar + 居中文本标识，使用 `stringResource` 设置标题
- **备选**: 使用单一 `PlaceholderScreen` 通过参数区分 — 放弃原因：后续各页面逻辑会分化，独立文件更清晰
- **影响文件**: `ui/screens/knowledge/KnowledgeScreen.kt`, `ui/screens/toolbox/ToolboxScreen.kt`, `ui/screens/profile/ProfileScreen.kt`
- **被依赖**: AppNavGraph 引用这三个页面

### D-005: 字符串资源中英双语

- **背景**: 所有用户可见字符串必须使用字符串资源，支持中英双语
- **选择**: 在 `res/values/strings.xml` 添加中文，在 `res/values-en/strings.xml` 添加英文
- **备选**: 使用硬编码中文 — 放弃原因：违反项目规范
- **影响文件**: `res/values/strings.xml`, `res/values-en/strings.xml`
- **被依赖**: 所有使用 Tab 名称的地方

## 实现思路

### 整体结构

```
AppShell (入口)
    ├─ rememberNavTabs() → 5 个 NavTab
    ├─ WindowWidthSizeClass 判断
    │   ├─ Compact/Medium → AppNavigationBar (底部)
    │   │   ├─ NavigationBar + 5 个 NavigationBarItem
    │   │   └─ CapsuleIndicator (胶囊动效)
    │   └─ Expanded → AppNavigationRail (侧边)
    └─ AppNavGraph (路由)
        ├─ HomeScreen
        ├─ KnowledgeScreen (新增)
        ├─ ToolboxScreen (新增)
        ├─ ProfileScreen (新增)
        └─ SettingsScreen
```

### 关键实现

1. **Tab 定义**: `NavTab(labelResId, icon, route)` 数据类，使用字符串资源 ID
2. **胶囊指示器**: `animateDpAsState` 计算偏移，`tween(300ms)` 平滑动画
3. **导航优化**: `popUpTo + saveState + restoreState` 保留 Tab 状态
4. **响应式布局**: Compact/Medium 使用 BottomBar，Expanded 使用 NavigationRail

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/navigation/Screen.kt` | ✏️ 修改 | 新增 Knowledge、Toolbox、Profile 路由 |
| `ui/components/AppShell.kt` | ✏️ 修改 | 重构为 5 个 Tab + 胶囊指示器 + remember 缓存 |
| `ui/navigation/AppNavGraph.kt` | ✏️ 修改 | 注册新路由 |
| `ui/screens/knowledge/KnowledgeScreen.kt` | 🆕 新增 | 知识页面占位 |
| `ui/screens/toolbox/ToolboxScreen.kt` | 🆕 新增 | 工具箱页面占位 |
| `ui/screens/profile/ProfileScreen.kt` | 🆕 新增 | 我的页面占位 |
| `res/values/strings.xml` | ✏️ 修改 | 添加中文 Tab 名称 |
| `res/values-en/strings.xml` | 🆕 新增 | 添加英文 Tab 名称 |
| `test/.../ScreenTest.kt` | ✏️ 修改 | 新增 Knowledge、Toolbox 路由测试 |
| `androidTest/.../AppShellTest.kt` | ✏️ 修改 | 新增 5 Tab 显示和导航测试 |
| `androidTest/.../AppNavGraphTest.kt` | ✏️ 修改 | 更新路由注册验证 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 测试清单

| 测试内容 | 状态 |
|---------|------|
| Screen.Knowledge 路由为 "knowledge" | ✅ |
| Screen.Toolbox 路由为 "toolbox" | ✅ |
| Compact 宽度显示 5 个 Tab | ✅ |
| Medium 宽度显示 5 个 Tab | ✅ |
| Expanded 宽度显示 5 个 Tab（NavigationRail） | ✅ |
| 点击 Tab 导航到对应页面 | ✅ |
| 导航状态与路由同步 | ✅ |
| `./gradlew testDebugUnitTest` 通过 | ✅ |

## 已知问题 / TODO

- [ ] KnowledgeScreen、ToolboxScreen、ProfileScreen 目前是占位页面，需要后续实现具体内容
- [ ] 胶囊指示器位置计算基于固定 100dp 宽度，可能需要根据实际屏幕宽度调整
- [ ] 第 5 个 Tab（我的/Profile）的具体功能待定
