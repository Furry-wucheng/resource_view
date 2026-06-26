# M13 — 核心功能单元测试

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M1 完成

## 设计决策

### D-001: 测试覆盖范围选择
- **背景**: 需要为核心功能编写单元测试，确保代码质量
- **选择**: 优先测试 Entity 转换函数、DomainError 层次结构、Result 类、Progress 类
- **备选**: 可以先测试 Repository 层，但 Entity 转换是更基础的功能
- **影响文件**: `data/local/entity/*EntityTest.kt`, `domain/error/*Test.kt`

### D-002: 测试命名规范
- **背景**: 需要统一测试命名风格
- **选择**: 采用 `should...when...` 格式，清晰描述期望行为
- **备选**: 可以使用 backtick 格式，但可读性稍差
- **影响文件**: 所有新增测试文件

### D-003: 测试结构模式
- **背景**: 需要保持测试代码一致性
- **选择**: 严格遵循 AAA 模式（Arrange-Act-Assert），每个测试只验证一个行为
- **备选**: 可以使用 Given-When-Then，但 AAA 更简洁
- **影响文件**: 所有新增测试文件

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `app/src/test/java/.../data/local/entity/SourceEntityTest.kt` | 🆕 新增 | 测试 SourceEntity 到 Source 的转换 |
| `app/src/test/java/.../data/local/entity/ResourceEntityTest.kt` | 🆕 新增 | 测试 ResourceEntity 到 Resource 的转换 |
| `app/src/test/java/.../data/local/entity/TagEntityTest.kt` | 🆕 新增 | 测试 TagEntity 到 Tag 的转换 |
| `app/src/test/java/.../data/local/entity/AppConfigEntityTest.kt` | 🆕 新增 | 测试 AppConfigEntity 到 AppConfig 的转换 |
| `app/src/test/java/.../domain/error/ResultTest.kt` | 🆕 新增 | 测试 Result 类和扩展函数 |
| `app/src/test/java/.../domain/error/DomainErrorTest.kt` | 🆕 新增 | 测试 DomainError 层次结构 |
| `app/src/test/java/.../domain/error/ProgressTest.kt` | 🆕 新增 | 测试 Progress 类和 ScanResult |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 测试覆盖统计

- **总测试数**: 125 个（原有 98 个 + 新增 27 个）
- **测试通过率**: 100%
- **覆盖的核心功能**:
  - Entity 到 Domain Model 的转换（4 个 Entity）
  - DomainError 的用户消息映射（16 种错误类型）
  - DomainError 的重试逻辑（canRetry 扩展）
  - Result 类的 Ok/Err 创建和 runCatching
  - Progress 类的三种状态（Update/Done/Error）

## 已知问题 / TODO

- [ ] Repository 层单元测试待补充（需要 MockK 或真实数据库）
- [ ] ViewModel 层单元测试待补充（androidTest 已有部分覆盖）
- [ ] 考虑添加参数化测试减少重复代码
