# M24 — ResourcePicker + 资源详情弹窗

> 轨道 7 · Stage 24/29 | 前置: M23 | 依赖共享: `doc/share/01-data-models.md` `doc/share/02-interfaces.md` | 🟢 独占

## 执行目标

实现可复用的树形文件选择弹窗 (ResourcePicker) + 资源详情弹窗（标签编辑 + 组织模式切换）。

## 共享契约引用

- `doc/share/02-interfaces.md` — FileEntry
- `doc/share/01-data-models.md` — Tag/Resource
- `@prd/06-页面结构与交互.md` §4.5 — ResourcePicker 交互
- `@design/resource-picker.html` — 原型参考
- `@design/resource-detail.html` — 原型参考

## 子任务

### M24.1 ResourcePickerDialog

树形扫描选择弹窗：
- 根节点导航（无复选框）+ [全选子项] 按钮
- 子节点复选框（独立勾选，不级联）
- 智能识别：纯图片文件夹→合并为单节点；混合内容→展开
- 底部 "已选 N 项" + [批量添加资源] [批量打标签] [取消]

**产出物**：`ui/components/ResourcePickerDialog.kt`

### M24.2 资源详情弹窗

半屏弹窗/对话框：
- 资源名称 + 封面
- 标签列表（已关联的勾选状态）
- "添加标签" 入口 → 弹出 TagEditorDialog (M15)
- 组织模式下拉切换（CHAPTER/CHAPTER_GALLERY/FLATGRID/GALLERY）
- "保存" / "取消"

**产出物**：`ui/screens/home/components/ResourceDetailSheet.kt`（或放在 components/）

### M24.3 集成入口

- ResourcePicker：文件浏览器 "扫描入库" 入口 → 调用 ResourcePicker
- 资源详情弹窗：首页长按资源 → 弹出

## 验收标准

- [ ] ResourcePicker 树形展开/折叠正常
- [ ] 勾选不级联（父子独立）
- [ ] [全选子项] 一键勾选直接子文件夹
- [ ] 资源详情弹窗可编辑标签和组织模式
- [ ] `./gradlew build` 通过
