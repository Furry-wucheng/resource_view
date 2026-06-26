# M24 — ResourcePicker + 资源详情弹窗

> 时间: 2026-06-25 | Agent: claude | 状态: ✅ 已完成 | 前置: M23

## 设计决策

### D-001: TreeFileNode 数据模型设计
- **背景**: ResourcePicker 需要树形结构来展示文件目录，需要一个数据模型来表示节点状态（展开/折叠、勾选）
- **选择**: 使用不可变 `data class TreeFileNode`，所有状态通过 `copy()` 更新。节点包含 `children` 列表实现递归树结构。
- **备选**: 使用可变的 `MutableList` 子节点 + `var` 状态字段。放弃原因：不可变数据更易于测试和状态管理。
- **影响文件**: `domain/model/TreeFileNode.kt`

### D-002: 树节点路径策略
- **背景**: 树节点需要唯一标识，且需要与 FileSource API 的相对路径对应
- **选择**: 使用完整的相对路径（如 `/root/folder1/file.jpg`）作为节点的 `relativePath`，同时用于标识和 FileSource 调用
- **备选**: 使用 UUID 作为节点 ID，单独存储路径。放弃原因：增加不必要的复杂度
- **影响文件**: `ui/components/ResourcePickerViewModel.kt:179-192`

### D-003: ResourcePickerViewModel 状态管理
- **背景**: ResourcePicker 弹窗需要管理树结构、展开/折叠、勾选等多种状态
- **选择**: 使用 `MutableStateFlow<List<TreeFileNode>>` 存储整个树，通过递归函数 `updateNodeAtPath` 更新指定路径的节点
- **备选**: 使用 Compose 的 `mutableStateListOf`。放弃原因：ViewModel 层应使用 StateFlow
- **影响文件**: `ui/components/ResourcePickerViewModel.kt`

### D-004: ResourceDetailSheet 使用 ModalBottomSheet
- **背景**: 资源详情弹窗需要半屏展示，支持标签编辑和组织模式切换
- **选择**: 使用 Material3 的 `ModalBottomSheet` 组件，底部弹出形式
- **备选**: 使用 `AlertDialog`。放弃原因：底部弹窗更适合移动端交互，且能显示更多内容
- **影响文件**: `ui/components/ResourceDetailSheet.kt`

### D-005: HomeViewModel 扩展支持详情编辑
- **背景**: 首页需要支持长按资源打开详情弹窗，并保存标签和组织模式的修改
- **选择**: 在 HomeViewModel 中添加详情弹窗状态（`detailResource`、`detailTagIds`、`detailOrgMode`），通过 `openResourceDetail` / `closeResourceDetail` 管理生命周期
- **备选**: 创建独立的 `ResourceDetailViewModel`。放弃原因：增加 DI 复杂度，且详情状态与首页资源列表紧密关联
- **影响文件**: `ui/screens/home/HomeViewModel.kt`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `domain/model/TreeFileNode.kt` | 🆕 新增 | 树形文件节点数据模型 |
| `ui/components/ResourcePickerViewModel.kt` | 🆕 新增 | ResourcePicker 弹窗 ViewModel |
| `ui/components/ResourcePickerDialog.kt` | 🆕 新增 | ResourcePicker 弹窗 Composable |
| `ui/components/ResourceDetailSheet.kt` | 🆕 新增 | 资源详情底部弹窗 |
| `ui/screens/home/HomeViewModel.kt` | ✏️ 修改 | 添加详情弹窗状态管理 |
| `ui/screens/home/HomeScreen.kt` | ✏️ 修改 | 长按打开详情弹窗 |
| `ui/components/ResourceGridItem.kt` | ✏️ 修改 | 添加长按支持 |
| `di/ViewModelModule.kt` | ✏️ 修改 | HomeViewModel 添加 ResourceTagDao 依赖 |
| `test/.../TreeFileNodeTest.kt` | 🆕 新增 | TreeFileNode 单元测试 |
| `test/.../ResourcePickerViewModelTest.kt` | 🆕 新增 | ResourcePickerViewModel 单元测试 |
| `test/.../HomeViewModelTest.kt` | ✏️ 修改 | 添加详情弹窗相关测试 |
| `androidTest/.../ResourcePickerDialogTest.kt` | 🆕 新增 | ResourcePickerDialog UI 测试 |
| `androidTest/.../ResourceDetailSheetTest.kt` | 🆕 新增 | ResourceDetailSheet UI 测试 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 已知问题 / TODO

- [ ] ResourcePicker 的"智能识别"逻辑（纯图片文件夹→不可展开）尚未实现，当前所有目录默认可展开
- [ ] ResourceDetailSheet 中的"添加标签"入口（弹出 TagEditorDialog）尚未集成
- [ ] 文件浏览器中的"扫描入库"入口尚未集成 ResourcePickerDialog
