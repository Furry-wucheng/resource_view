# M27 — 批量添加 + 拆分 UseCase

> 轨道 8 · Stage 27/29 | 前置: M10,M24 | 依赖共享: `doc/share/06-error-handling.md` | 🟢 独占

## 执行目标

实现批量添加资源 UseCase + 拆分资源 UseCase + 扫描进度流。

## 共享契约引用

- `doc/share/06-error-handling.md` — ScanResult、Progress\<T\>
- `doc/share/01-data-models.md` — Resource Entity/Domain
- `@prd/01-资源库首页.md` — 拆分资源交互

## 子任务

### M27.1 ScanResourcesUseCase

遍历文件系统目录 → 生成 ResourceEntity → 批量插入 Room → 通过 Progress Flow 发射进度更新 → 返回 ScanResult。

**产出物**：`domain/usecase/ScanResourcesUseCase.kt`

### M27.2 BatchAddResourcesUseCase

接收 ResourcePicker 勾选的路径列表 → 每条路径创建 Resource → 可选弹出标签选择弹窗。

**产出物**：`domain/usecase/BatchAddResourcesUseCase.kt`

### M27.3 SplitResourceUseCase

接收原 Resource + ResourcePicker 勾选的子项 → 为每个子项创建新 Resource（继承标签与否可配）→ 标记原 Resource 状态。

**产出物**：`domain/usecase/SplitResourceUseCase.kt`

## 验收标准

- [ ] 扫描过程通过 Progress Flow 实时发射进度
- [ ] 批量添加完成后首页显示新增资源
- [ ] 拆分后子资源独立存在，原资源状态正确
- [ ] 失败的条目记录到 ScanResult.failures
- [ ] `./gradlew build` 通过
