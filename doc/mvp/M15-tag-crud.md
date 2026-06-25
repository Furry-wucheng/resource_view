# M15 — 标签 CRUD

> 轨道 4 · Stage 15/29 | 前置: M10 | 依赖共享: `doc/share/01-data-models.md` §3.3 `doc/share/05-theme-tokens.md` | 🟢 独占

## 执行目标

实现标签管理页面（创建、编辑颜色/名称、删除）和标签编辑弹窗。

## 共享契约引用

- `doc/share/01-data-models.md` §3.3 — Tag Domain Model
- `doc/share/05-theme-tokens.md` — 12 色标签预设
- `doc/share/06-error-handling.md` — ValidationError
- `@design/tag-manager.html` — 原型参考
- `@prd/02-标签系统.md` — 交互逻辑

## 子任务

### M15.1 TagViewModel

管理标签列表（Flow 自动更新）、创建/重命名/删除（内置标签不可删/改）。

**产出物**：`ui/screens/tags/TagViewModel.kt`

### M15.2 TagManagerScreen

标签管理页面：
- 标签列表（按内置优先排序）
- 每项显示名称 + 颜色圆点 + 关联资源数
- 点击 → 编辑；长按 → 删除确认
- FAB "新建标签"

**产出物**：`ui/screens/tags/TagManagerScreen.kt`

### M15.3 TagEditorDialog

标签创建/编辑弹窗：
- 名称输入框（校验：非空、非"收藏"、不重复、≤20字符）
- 12 色预设颜色选择器（网格排列）
- 确认/取消按钮

**产出物**：`ui/screens/tags/TagEditorDialog.kt`

### M15.4 挂载到路由

在 `AppNavGraph.kt` 中确保 TagManager composable 挂载实际 Screen。

**产出物**：`ui/navigation/AppNavGraph.kt`（修改 TagManager composable）

## 验收标准

- [ ] 内置标签"收藏"不可删除/重命名
- [ ] 标签名称重复时显示 ValidationError
- [ ] 12 色预设可点选
- [ ] 删除标签时级联删除关联（不删资源）
- [ ] `./gradlew build` 通过
