# M20 — Gallery + FlatGrid 策略

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M11, M14

## 设计决策

### D-001: FlatGrid 和 Gallery 策略共享相同的文件获取逻辑
- **背景**: M20.1 和 M20.2 分别实现 FlatGridStrategy 和 GalleryStrategy，两者都需要获取文件夹内的图片文件
- **选择**: 两个策略使用相同的 `getContents` 实现逻辑（过滤图片文件），区别仅在于 `mode` 属性标识（FLATGRID vs GALLERY）。实际的 UI 布局差异由 View 层根据 mode 决定
- **备选**: 可以在策略内部实现不同的文件过滤逻辑（如 Gallery 递归获取），但当前阶段需求明确为相同的文件获取
- **影响文件**: `shared/organization/FlatGridStrategy.kt:24-31`, `shared/organization/GalleryStrategy.kt:24-31`

### D-002: DetectOrganizationModeUseCase 仅检查一级子目录
- **背景**: 自动检测文件夹组织模式时，需要判断子文件夹是否包含图片
- **选择**: 仅检查一级子目录（直接子文件夹）是否包含图片文件，不递归检查更深层级。理由：性能考虑，避免深层递归导致的延迟
- **备选**: 递归检查所有子目录，但可能导致大量 SMB 请求，影响用户体验
- **影响文件**: `domain/usecase/DetectOrganizationModeUseCase.kt:40-60`

### D-003: 混合文件类型默认使用 FLATGRID
- **背景**: 文件夹包含图片 + PDF + 视频等混合类型时的组织模式选择
- **选择**: 混合类型统一使用 FLATGRID 模式，将所有内容平铺展示
- **备选**: 可以按文件类型分组，但增加复杂度且 PRD 未明确要求
- **影响文件**: `domain/usecase/DetectOrganizationModeUseCase.kt:35-38`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `shared/organization/FlatGridStrategy.kt` | 🆕 新增 | FlatGrid 组织策略实现 |
| `shared/organization/GalleryStrategy.kt` | 🆕 新增 | Gallery 组织策略实现 |
| `domain/usecase/DetectOrganizationModeUseCase.kt` | 🆕 新增 | 自动检测组织模式用例 |
| `shared/organization/FlatGridStrategyTest.kt` | 🆕 新增 | FlatGridStrategy 单元测试 |
| `shared/organization/GalleryStrategyTest.kt` | 🆕 新增 | GalleryStrategy 单元测试 |
| `domain/usecase/DetectOrganizationModeUseCaseTest.kt` | 🆕 新增 | DetectOrganizationModeUseCase 单元测试 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 已知问题 / TODO

- [ ] Gallery 模式的 UI 布局实现需要在 View 层完成（当前仅实现数据层策略）
- [ ] DetectOrganizationModeUseCase 可能需要在后续阶段支持更智能的检测逻辑（如基于文件数量、目录深度等）
