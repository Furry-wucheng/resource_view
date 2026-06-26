# M16 底部标签栏

## 里程碑信息

| 属性 | 值 |
|------|-----|
| 里程碑 | M16 |
| 功能名称 | 底部标签栏 |
| 优先级 | P0 |
| 分支 | M16-TabBar |
| 状态 | ✅ 已完成 |
| 完成日期 | 2026-06-26 |

## 验收标准

| 序号 | 验收标准 | 状态 |
|------|---------|------|
| 1 | BottomBar 集成 Jetpack Navigation | ✅ |
| 2 | 固定 5 个 Tab：首页/知识/工具箱/我的/设置 | ✅ |
| 3 | 选中态图标+文字颜色变化 | ✅ |
| 4 | 导航状态与路由同步 | ✅ |
| 5 | 胶囊底栏指示器动效 | ✅ |
| 6 | 导航性能优化（避免重组抖动） | ✅ |

## 实现方案

### 1. 新增 Screen 路由

**文件**：`ui/navigation/Screen.kt`

新增 3 个底部标签栏路由：

```kotlin
// === M16 创建：底部标签栏路由 ===
data object Knowledge : Screen("knowledge")
data object Toolbox : Screen("toolbox")
data object Profile : Screen("profile")
```

### 2. 更新底部标签栏组件

**文件**：`ui/components/AppShell.kt`

#### 2.1 Tab 定义

使用 `NavTab` 数据类定义 5 个 Tab：

```kotlin
private data class NavTab(
    val labelResId: Int,  // 字符串资源 ID
    val icon: ImageVector,
    val route: String,
)
```

5 个 Tab：
- 首页（Home）- `Icons.Default.Home`
- 知识（Knowledge）- `Icons.AutoMirrored.Filled.MenuBook`
- 工具箱（Toolbox）- `Icons.Default.Build`
- 我的（Profile）- `Icons.Default.Person`
- 设置（Settings）- `Icons.Default.Settings`

#### 2.2 导航性能优化

使用 `remember` 缓存导航标签列表，避免重组时重复创建：

```kotlin
@Composable
private fun rememberNavTabs(): List<NavTab> {
    return remember {
        listOf(
            NavTab(R.string.tab_home, Icons.Default.Home, Screen.Home.route),
            // ...
        )
    }
}
```

#### 2.3 导航状态与路由同步

使用 `popUpTo + saveState + restoreState` 优化导航：

```kotlin
private fun NavHostController.navigateWithState(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
```

#### 2.4 胶囊底栏指示器动效

实现平滑的指示器移动效果：

```kotlin
@Composable
private fun CapsuleIndicator(
    selectedIndex: Int,
    tabCount: Int,
    modifier: Modifier = Modifier,
) {
    val tabWidth = 100.dp / tabCount
    val indicatorWidth = 24.dp
    val offset by animateDpAsState(
        targetValue = tabWidth * selectedIndex + (tabWidth - indicatorWidth) / 2,
        animationSpec = tween(durationMillis = 300),
        label = "capsule_indicator_offset",
    )

    Box(
        modifier = modifier
            .offset(x = offset)
            .width(indicatorWidth)
            .size(3.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary),
    )
}
```

### 3. 新增占位页面

**文件**：
- `ui/screens/knowledge/KnowledgeScreen.kt`
- `ui/screens/toolbox/ToolboxScreen.kt`
- `ui/screens/profile/ProfileScreen.kt`

每个占位页面包含：
- TopAppBar（使用字符串资源）
- 居中显示的页面标识文本

### 4. 字符串资源

**中文**：`res/values/strings.xml`

```xml
<string name="tab_home">首页</string>
<string name="tab_knowledge">知识</string>
<string name="tab_toolbox">工具箱</string>
<string name="tab_profile">我的</string>
<string name="tab_settings">设置</string>
```

**英文**：`res/values-en/strings.xml`

```xml
<string name="tab_home">Home</string>
<string name="tab_knowledge">Knowledge</string>
<string name="tab_toolbox">Toolbox</string>
<string name="tab_profile">Profile</string>
<string name="tab_settings">Settings</string>
```

### 5. 更新导航图

**文件**：`ui/navigation/AppNavGraph.kt`

注册新路由：

```kotlin
composable(Screen.Knowledge.route) {
    KnowledgeScreen()
}
composable(Screen.Toolbox.route) {
    ToolboxScreen()
}
composable(Screen.Profile.route) {
    ProfileScreen()
}
```

## 测试

### 单元测试

**文件**：`test/.../ui/navigation/ScreenTest.kt`

```kotlin
@Test
fun `should have correct route for Knowledge`() {
    val screen = Screen.Knowledge
    assertEquals("knowledge", screen.route)
}

@Test
fun `should have correct route for Toolbox`() {
    val screen = Screen.Toolbox
    assertEquals("toolbox", screen.route)
}
```

### UI 测试

**文件**：`androidTest/.../ui/component/AppShellTest.kt`

测试内容：
- 5 个 Tab 在 Compact/Medium/Expanded 宽度下正确显示
- 点击 Tab 导航到对应页面
- 导航状态与路由同步

**测试结果**：✅ 全部通过

## 提交记录

```
commit e1e635a
feat(M16): 实现底部标签栏功能

- 固定 5 个 Tab：首页/知识/工具箱/我的/设置
- 新增 Screen 路由：Knowledge, Toolbox, Profile
- 实现胶囊底栏指示器动效（300ms 平滑动画）
- 导航状态与路由同步优化（popUpTo + saveState/restoreState）
- 使用 remember 缓存导航标签列表，避免重组抖动
- 添加字符串资源（中英双语）
- 新增占位页面：KnowledgeScreen, ToolboxScreen, ProfileScreen
- 更新单元测试和 UI 测试
```

## 技术要点

1. **状态管理**：使用 `collectAsState` + `remember` 管理导航状态
2. **性能优化**：使用 `remember` 缓存标签列表，避免重组抖动
3. **动画效果**：使用 `animateDpAsState` 实现胶囊指示器平滑移动
4. **响应式布局**：Compact/Medium 使用 BottomBar，Expanded 使用 NavigationRail
5. **国际化**：所有字符串使用字符串资源，支持中英双语

## 后续工作

- [ ] 实现知识页面具体内容
- [ ] 实现工具箱页面具体内容
- [ ] 实现我的页面具体内容
- [ ] 考虑是否需要第 5 个 Tab（待定）
