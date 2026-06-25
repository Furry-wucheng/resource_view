# M09 — Domain Models

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M06

## 设计决策

### D-001: 提前创建 FileSource 接口
- **背景**: ViewerItem.VideoMediaSource.SmbFile 引用了 FileSource 接口，但该接口计划在 M11 创建
- **选择**: 提前创建最小化 FileSource 接口定义，仅包含接口签名，不含实现
- **备选**: 使用 Any 类型占位，待 M11 再替换 → 会破坏类型安全
- **影响文件**: `shared/filesource/FileSource.kt`
- **被依赖**: M11 将在此基础上添加 FileSourceFactory 和具体实现

### D-002: Resource.toDomain() 需要额外参数
- **背景**: Resource Domain Model 包含 sourceName 和 tags 字段，但 ResourceEntity 中没有这些字段
- **选择**: toDomain() 扩展函数接受 sourceName 和 tags 作为参数，由 Repository 层负责组装
- **备选**: 创建中间数据类 → 增加不必要的复杂度
- **影响文件**: `data/local/entity/ResourceEntity.kt:44-62`
- **被依赖**: M10 Repository 层将使用此映射函数

### D-003: Tag.toDomain() 需要 resourceCount 参数
- **背景**: Tag Domain Model 包含 resourceCount 统计字段，但 TagEntity 中没有
- **选择**: toDomain() 扩展函数接受 resourceCount 作为参数
- **备选**: 在 TagEntity 中添加冗余字段 → 违反数据库规范化
- **影响文件**: `data/local/entity/TagEntity.kt:21-30`
- **被依赖**: M10 Repository 层将使用此映射函数

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `domain/model/Source.kt` | 🆕 新增 | Source Domain Model |
| `domain/model/Resource.kt` | 🆕 新增 | Resource Domain Model（含 tags 内联） |
| `domain/model/Tag.kt` | 🆕 新增 | Tag Domain Model（含 resourceCount） |
| `domain/model/FileEntry.kt` | 🆕 新增 | FileEntry Domain Model |
| `domain/model/Chapter.kt` | 🆕 新增 | Chapter Domain Model |
| `domain/model/ViewerItem.kt` | 🆕 新增 | ViewerItem sealed class + VideoMediaSource |
| `domain/model/AppConfig.kt` | 🆕 新增 | AppConfig Domain Model |
| `shared/filesource/FileSource.kt` | 🆕 新增 | FileSource 接口定义（提前创建） |
| `data/local/entity/SourceEntity.kt` | ✏️ 修改 | 添加 toDomain() 扩展函数 |
| `data/local/entity/ResourceEntity.kt` | ✏️ 修改 | 添加 toDomain() 扩展函数 |
| `data/local/entity/TagEntity.kt` | ✏️ 修改 | 添加 toDomain() 扩展函数 |
| `data/local/entity/AppConfigEntity.kt` | ✏️ 修改 | 添加 toDomain() 扩展函数 |

## 已知问题 / TODO

- [ ] FileSource 接口目前仅定义，具体实现（LocalFileSource、SmbFileSource）将在 M12、M17 添加
- [ ] FileSourceFactory 将在 M11 创建
