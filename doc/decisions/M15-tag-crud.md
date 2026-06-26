# M15 — 标签 CRUD

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M10

## 设计决策

### D-001: TagViewModel 使用 StateFlow 管理 UI 状态

- **背景**: 标签管理页面需要同时管理标签列表、编辑弹窗状态、错误消息等多种 UI 状态
- **选择**: 使用 `StateFlow` 分离 `TagManagerUiState`（页面级状态）和 `TagEditorUiState`（弹窗状态），标签列表使用 `stateIn` 转换为热流
- **备选**: 使用单一 `UiState` 包含所有状态 — 放弃原因：弹窗状态与页面状态生命周期不同，分离更清晰
- **影响文件**: `ui/screens/tags/TagViewModel.kt`
- **被依赖**: 无（UI 层内部使用）

### D-002: 标签名称校验在 ViewModel 层执行

- **背景**: 标签名称需要多重校验（非空、非"收藏"、不重复、≤20字符）
- **选择**: 在 ViewModel 的 `saveTag()` 方法中执行所有校验，校验失败更新 `nameError` 状态
- **备选**: 在 UI 层使用 `visualTransformation` 实时校验 — 放弃原因：重复名称校验需要查询数据库，在 ViewModel 中统一处理更简洁
- **影响文件**: `ui/screens/tags/TagViewModel.kt:120-150`
- **被依赖**: 无

### D-003: 12 色预设颜色选择器使用 FlowRow 布局

- **背景**: 颜色选择器需要以网格形式展示 12 种预设颜色
- **选择**: 使用 `FlowRow` 组件自动换行布局，每个颜色项为可点击的圆形色块
- **备选**: 使用 `LazyVerticalGrid` — 放弃原因：12 个颜色项不需要懒加载，`FlowRow` 更简洁
- **影响文件**: `ui/screens/tags/TagEditorDialog.kt:80-110`
- **被依赖**: 无

### D-004: 删除确认使用 AlertDialog

- **背景**: 删除标签需要二次确认，防止误删
- **选择**: 使用 Material3 `AlertDialog` 组件，在 `TagManagerScreen` 中通过 `tagToDelete` 状态控制显示
- **备选**: 使用 `Snackbar` + Action — 放弃原因：删除是破坏性操作，AlertDialog 更醒目
- **影响文件**: `ui/screens/tags/TagManagerScreen.kt:175-195`
- **被依赖**: 无

### D-005: ViewModel 通过 Koin 注入

- **背景**: TagViewModel 需要获取 TagRepository 实例
- **选择**: 创建 `ViewModelModule`，使用 `viewModel { TagViewModel(get()) }` 注册，UI 层通过 `koinViewModel()` 获取
- **备选**: 使用 Hilt — 放弃原因：项目已统一使用 Koin 作为 DI 框架
- **影响文件**: `di/ViewModelModule.kt`, `ResourceViewerApp.kt`
- **被依赖**: 后续所有 ViewModel 都应在 `viewModelModule` 中注册

## 实现思路

### 整体架构

```
TagManagerScreen (UI)
    ↓ 通过 koinViewModel() 获取
TagViewModel
    ↓ 消费 TagRepository
TagRepository (M10)
    ↓ 消费 TagDao
Room Database (M08)
```

### 关键实现

1. **Flow 自动更新**: `tagRepository.getAllTags()` 返回 `Flow<List<Tag>>`，通过 `stateIn` 转换为 `StateFlow`
2. **内置标签保护**: ViewModel 检查 `isBuiltIn` 字段，内置标签不可编辑/删除
3. **UUID 生成**: 使用 `java.util.UUID.randomUUID()` 生成标签 ID
4. **颜色转换**: 使用 `android.graphics.Color.parseColor()` 将十六进制颜色字符串转换为 Compose `Color`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/screens/tags/TagViewModel.kt` | 🆕 新增 | 标签管理 ViewModel |
| `ui/screens/tags/TagEditorDialog.kt` | 🆕 新增 | 标签创建/编辑弹窗 |
| `ui/screens/tags/TagManagerScreen.kt` | ✏️ 修改 | 从占位符改为完整实现 |
| `di/ViewModelModule.kt` | 🆕 新增 | ViewModel Koin Module |
| `ResourceViewerApp.kt` | ✏️ 修改 | 注册 viewModelModule |
| `ui/navigation/AppNavGraph.kt` | ✏️ 修改 | 添加 onNavigateBack 回调 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 测试清单

| 测试内容 | 状态 |
|---------|------|
| 内置标签"收藏"不可删除/重命名 | ✅ |
| 标签名称重复时显示 ValidationError | ✅ |
| 12 色预设可点选 | ✅ |
| 删除标签时级联删除关联（不删资源） | ✅（数据库外键 CASCADE） |
| `./gradlew build` 通过 | ✅ |

## 已知问题 / TODO

- [ ] TagManagerScreen 目前由底部 Tab 直接导航，后续可能需要调整为从设置页面或其他入口进入
- [ ] 标签排序目前仅按内置优先，后续可扩展按名称或资源数量排序
