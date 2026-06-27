# 文件浏览器体验优化

> 时间: 2026-06-28 | Agent: opencode | 状态: ✅ 已完成 | 前置: 无（独立提案）

## 设计决策

### D-001: 文件夹偏好持久化方案
- **背景**: 用户希望每个文件夹独立记住视图模式和排序方式，类似 Windows 资源管理器
- **选择**: 使用 DataStore Preferences + LRU 缓存，最多存储 1000 个文件夹偏好
  - 键格式: `fb_{sourceId}_{pathHash}`
  - 值格式: `{viewMode}|{sortMode}`
  - 超出限制时按最后访问时间淘汰
- **备选**: 
  - Room 数据库：过于重量级，键值对场景不需要 SQL
  - 按数据源存储全局偏好：无法满足每层文件夹独立排序的需求
- **影响文件**: `data/local/datastore/FileBrowserPrefsStore.kt`

### D-002: 目录树组件实现方式
- **背景**: 需要实现类似文件资源管理器左侧的树状目录导航
- **选择**: 扁平化列表 + 展开状态映射，使用 LazyColumn 渲染
  - 维护 `expandedPaths: Map<String, Boolean>` 跟踪展开状态
  - 维护 `childrenMap: Map<String, List<FileEntry>>` 缓存子目录
  - 使用 `updateCounter` 触发 remember 重新计算
- **备选**: 
  - 递归 Compose 组件：Compose 不支持递归组件的良好性能
  - 第三方 TreeView 库：Compose 生态没有成熟的 Tree 组件
- **影响文件**: `ui/screens/sources/DirectoryTree.kt`

### D-003: 目录树布局位置
- **背景**: 目录树应该放在哪里？如何控制显示/隐藏？
- **选择**: 目录树直接放在 content 内部的 Row 布局中
  - 宽屏：左侧 260dp 目录树 + 右侧文件列表
  - 窄屏：目录树覆盖在内容上方
  - 按钮控制显示/隐藏，返回键不控制目录树
- **备选**: 
  - ModalNavigationDrawer：会被返回键控制，不符合需求
  - 外层 Box 覆盖：代码复杂度高
- **影响文件**: `ui/screens/sources/FileBrowserScreen.kt`

### D-004: 排序方式与 UI
- **背景**: 用户需要支持多种排序方式，且排序选择应该是下拉菜单而非切换按钮
- **选择**: 使用 DropdownMenu 提供 4 种排序选项
  - 文件名 A→Z（默认）
  - 文件名 Z→A
  - 修改时间 旧→新
  - 修改时间 新→旧
  - 当前选中项显示 CheckCircle 图标
- **备选**: 切换按钮：无法直观显示所有选项
- **影响文件**: `ui/screens/sources/FileBrowserScreen.kt`

### D-005: 文件夹角标显示逻辑
- **背景**: 无缩略图的文件夹不应该显示右下角角标，因为已经有默认的文件夹 icon
- **选择**: 在 FileEntryGridItem 中直接加载缩略图，根据 bitmap 是否为 null 判断
  - 有缩略图：显示右下角黄色文件夹角标
  - 无缩略图：不显示角标
- **备选**: 通过 lambda 回调传递缩略图状态：代码复杂度高
- **影响文件**: `ui/screens/sources/FileBrowserScreen.kt`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `data/local/datastore/FileBrowserPrefsStore.kt` | 🆕 新增 | DataStore + LRU 缓存实现 |
| `ui/screens/sources/DirectoryTree.kt` | 🆕 新增 | 目录树组件（扁平化列表） |
| `di/DataStoreModule.kt` | 🆕 新增 | DataStore DI 模块 |
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 修改 | 添加排序/视图模式逻辑 |
| `ui/screens/sources/FileBrowserScreen.kt` | ✏️ 修改 | UI 重构 |
| `data/local/entity/AppConfigEntity.kt` | ✏️ 修改 | 添加 showDirectoryTree 字段 |
| `domain/model/AppConfig.kt` | ✏️ 修改 | 添加 showDirectoryTree 字段 |
| `data/local/AppDatabase.kt` | ✏️ 修改 | 添加数据库迁移 4→5 |
| `data/local/migration/DatabaseMigrator.kt` | ✏️ 修改 | 添加 MIGRATION_4_5 |
| `ui/screens/settings/SettingsViewModel.kt` | ✏️ 修改 | 添加目录树开关方法 |
| `ui/screens/settings/SettingsScreen.kt` | ✏️ 修改 | 添加目录树开关 UI |
| `di/ViewModelModule.kt` | ✏️ 修改 | 更新 FileBrowserViewModel 注入 |
| `ResourceViewerApp.kt` | ✏️ 修改 | 注册 DataStoreModule |
| `gradle/libs.versions.toml` | ✏️ 修改 | 添加 DataStore 依赖 |
| `app/build.gradle.kts` | ✏️ 修改 | 添加 DataStore 依赖 |
| `app/src/test/.../FileBrowserViewModelTest.kt` | ✏️ 修改 | 添加 prefsStore mock |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 已知问题 / TODO

- [ ] DirectoryTree 组件的懒加载性能可进一步优化（当前每次展开都重新请求）
- [ ] 窄屏模式下目录树的动画效果可以改进
- [ ] 考虑添加目录树的搜索功能
