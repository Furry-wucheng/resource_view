# M27 — 批量添加 + 拆分 UseCase

> 时间: 2026-06-25 | Agent: claude | 状态: ✅ 已完成 | 前置: M10,M24

## 设计决策

### D-001: UseCase 返回类型选择
- **背景**: ScanResourcesUseCase 需要发射进度更新，而 BatchAdd/Split 只需返回最终结果
- **选择**: ScanResourcesUseCase 返回 `Flow<Progress<ScanResult>>`，BatchAdd/Split 返回 `Result<ScanResult>`
- **备选**: 全部使用 Flow，但 BatchAdd/Split 不需要中间进度，使用 Flow 会增加调用方复杂度
- **影响文件**: `domain/usecase/ScanResourcesUseCase.kt`, `domain/usecase/BatchAddResourcesUseCase.kt`, `domain/usecase/SplitResourceUseCase.kt`

### D-002: 文件类型过滤策略
- **背景**: ResourceType 枚举只有 FOLDER/PDF/ARCHIVE/VIDEO，没有 IMAGE 类型
- **选择**: 图片文件（jpg/png 等）在 UseCase 中返回 null 跳过，不创建 Resource
- **备选**: 扩展 ResourceType 添加 IMAGE，但会破坏现有架构（图片应通过 FOLDER 包含）
- **影响文件**: `domain/usecase/ScanResourcesUseCase.kt:85-95`, `domain/usecase/BatchAddResourcesUseCase.kt:80-90`

### D-003: 批量插入失败处理
- **背景**: 批量插入可能部分失败（如 UNIQUE 冲突）
- **选择**: 使用 `insertAll` 批量插入，如果整体失败则将所有条目记录到 failures
- **备选**: 逐条插入并单独处理失败，但性能较差
- **影响文件**: `domain/usecase/ScanResourcesUseCase.kt:60-70`, `domain/usecase/BatchAddResourcesUseCase.kt:55-65`

### D-004: 拆分后父资源状态
- **背景**: 拆分子资源后，父资源应如何标记
- **选择**: 设置 `isAvailable = false` 隐藏父资源，而非删除（保留历史记录）
- **备选**: 直接删除父资源，但会丢失标签等元数据
- **影响文件**: `domain/usecase/SplitResourceUseCase.kt:95-110`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `domain/usecase/ScanResourcesUseCase.kt` | 🆕 新增 | 扫描目录 → 批量插入 → Progress Flow |
| `domain/usecase/BatchAddResourcesUseCase.kt` | 🆕 新增 | 批量添加资源 |
| `domain/usecase/SplitResourceUseCase.kt` | 🆕 新增 | 拆分资源为子项 |
| `domain/usecase/ScanResourcesUseCaseTest.kt` | 🆕 新增 | 5 个测试用例 |
| `domain/usecase/BatchAddResourcesUseCaseTest.kt` | 🆕 新增 | 5 个测试用例 |
| `domain/usecase/SplitResourceUseCaseTest.kt` | 🆕 新增 | 5 个测试用例 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 已知问题 / TODO

- [ ] 图片文件（jpg/png 等）目前被跳过，后续可能需要扩展 ResourceType 或调整架构
- [ ] 批量插入使用 REPLACE 策略，可能覆盖已有资源的标签关联
- [ ] 拆分操作未继承标签（inheritTags 参数已预留但标签关联逻辑待实现）
