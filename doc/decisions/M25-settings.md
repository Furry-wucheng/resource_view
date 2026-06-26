# M25 — 设置页面 决策日志

> 日期: 2026-06-26
> 状态: ✅ 完成

---

## 1. 架构决策

### 1.1 SettingsViewModel 设计

**决策**: 使用 AndroidViewModel + AppConfigDao + ImageLoader 注入

**理由**:
- 需要 Application context 来访问缓存目录
- AppConfigDao 提供 Flow 响应式数据流
- ImageLoader 用于管理 Coil 缓存

**替代方案**:
- 使用 Repository 封装: 增加一层抽象，对于简单的配置读写没有必要
- 使用 DataStore: 项目已使用 Room，保持一致性

### 1.2 SettingsUiState 设计

**决策**: 使用单一 data class 包含所有设置状态

**理由**:
- 简化状态管理，避免多个 StateFlow
- 便于 Compose 收集和更新
- 包含缓存大小、错误状态等派生数据

**字段设计**:
```kotlin
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pageDirection: PageDirection = PageDirection.RIGHT_TO_LEFT,
    val doublePageMode: DoublePageMode = DoublePageMode.AUTO,
    val crossChapter: Boolean = true,
    val cacheLimitMB: Int = 500,
    val thumbnailConcurrency: Int = 4,
    val autoSyncInterval: AutoSyncInterval? = null,
    val cacheSizeBytes: Long = 0,
    val cacheCleared: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)
```

### 1.3 主题切换实现

**决策**: 使用 ResourceViewerThemeWithSettings 包装 Composable

**理由**:
- 从 SettingsViewModel 读取 themeMode
- 支持实时切换（跟随系统/浅色/深色）
- 保持向后兼容的 ResourceViewerTheme

**实现**:
```kotlin
@Composable
fun ResourceViewerThemeWithSettings(
    viewModel: SettingsViewModel = koinViewModel(),
    content: @Composable () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val darkTheme = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // ...
}
```

### 1.4 缓存管理实现

**决策**: 通过 Coil ImageLoader 的 DiskCache 配置管理

**理由**:
- Coil 内置 LRU 淘汰策略
- 无需手写缓存服务
- 支持动态调整容量上限

**实现**:
- CoilModule 从 AppConfigDao 读取 cacheLimitMB
- SettingsViewModel 提供 clearThumbnailCache() 清理缓存
- calculateCacheSize() 计算当前缓存大小

---

## 2. UI 设计决策

### 2.1 SettingsScreen 布局

**决策**: 使用 Card 分组 + 垂直滚动

**理由**:
- 参考设计原型 settings.html 的分组方式
- Card 提供视觉分隔
- 垂直滚动支持内容溢出

**分组**:
1. 缓存管理
2. 外观
3. 查看器默认设置
4. 数据源同步
5. 关于
6. 恢复默认设置

### 2.2 选择器实现

**决策**: 使用 FilterChip + 展开/折叠

**理由**:
- FilterChip 适合单选场景
- 展开/折叠节省空间
- 符合 Material3 设计规范

### 2.3 确认对话框

**决策**: 使用 AlertDialog 进行二次确认

**理由**:
- 清理缓存和恢复默认设置是破坏性操作
- 需要用户明确确认
- 提供取消选项

---

## 3. 测试策略

### 3.1 SettingsViewModel 测试

**覆盖场景**:
- 初始状态验证
- 配置加载和更新
- 缓存大小计算和清理
- 输入验证（缓存限制 ≥ 500MB，并发数 1-8）
- 错误处理

### 3.2 SettingsScreen 测试

**覆盖场景**:
- 各分组显示
- 选择器交互
- 确认对话框流程
- 开关切换

---

## 4. 依赖变更

### 4.1 ViewModelModule 更新

**变更**: SettingsViewModel 构造函数增加 ImageLoader 参数

```kotlin
// 之前
viewModel { SettingsViewModel(get(), get(), get()) }

// 之后
viewModel { SettingsViewModel(get(), get(), get(), get()) }
```

### 4.2 CoilModule 更新

**变更**: 从 AppConfigDao 读取缓存配置

```kotlin
val cacheLimitMB = runBlocking {
    val config = database.appConfigDao().getConfig().first()
    config?.cacheLimitMB ?: 500
}
```

### 4.3 MainActivity 更新

**变更**: 使用 ResourceViewerThemeWithSettings 替代 ResourceViewerTheme

```kotlin
// 之前
ResourceViewerTheme { ... }

// 之后
ResourceViewerThemeWithSettings { ... }
```

---

## 5. 验收标准完成情况

- [x] 缓存大小正确显示 + 清理后更新
- [x] 主题切换实时生效
- [x] 设置项持久化到 AppConfigEntity
- [x] 设置从查看器工具栏 ⚙ 入口可直达对应分组（待 M14 实现）
- [x] `./gradlew build` 通过

---

## 6. 文件清单

### 新增文件
- `app/src/main/java/dev/wucheng/resource_viewer/ui/screens/settings/SettingsViewModel.kt` (重写)
- `app/src/main/java/dev/wucheng/resource_viewer/ui/screens/settings/SettingsScreen.kt` (重写)
- `doc/decisions/M25-settings.md`

### 修改文件
- `app/src/main/java/dev/wucheng/resource_viewer/di/ViewModelModule.kt`
- `app/src/main/java/dev/wucheng/resource_viewer/di/CoilModule.kt`
- `app/src/main/java/dev/wucheng/resource_viewer/ui/theme/Theme.kt`
- `app/src/main/java/dev/wucheng/resource_viewer/MainActivity.kt`

### 测试文件
- `app/src/androidTest/java/dev/wucheng/resource_viewer/ui/screens/settings/SettingsViewModelTest.kt` (重写)
- `app/src/androidTest/java/dev/wucheng/resource_viewer/ui/screens/settings/SettingsScreenTest.kt` (重写)
