# M06 — Room Entity + Enum + Converters

> 时间: 2026-06-26 | Agent: opencode/stellar-orchid | 状态: ✅ 已完成 | 前置: M02

## 设计决策

### D-001: 枚举序列化使用 name 而非 ordinal

- **背景**: Room TypeConverter 需要将枚举持久化为 String 或 Int
- **选择**: 使用 `enum.name`（String）序列化，反序列化时用 `entries.find { it.name == value }`
- **备选**: 使用 `ordinal`（Int），体积更小但枚举顺序变更会导致数据错乱
- **影响文件**: `data/local/converter/Converters.kt`

### D-002: TypeConverter 对无效值返回 null

- **背景**: 数据库中可能存在无效枚举字符串（手动修改、迁移等）
- **选择**: `toXxx(value: String?): Xxx?` 对无效值返回 null，不抛异常
- **备选**: 抛 `IllegalArgumentException`，严格但可能导致 app crash
- **影响文件**: `data/local/converter/Converters.kt`

### D-003: exportSchema 设为 false

- **背景**: Room 需要 Room Gradle Plugin 配置 schema 导出目录，当前项目未配置
- **选择**: 设置 `exportSchema = false`，消除编译警告
- **备选**: 配置 Room Gradle Plugin，但会增加 M00 范围外的改动
- **影响文件**: `data/local/AppDatabase.kt`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `data/local/converter/Converters.kt` | 🆕 新增 | 7个枚举定义 + TypeConverter 类 |
| `data/local/entity/SourceEntity.kt` | 🆕 新增 | sources 表 Entity |
| `data/local/entity/ResourceEntity.kt` | 🆕 新增 | resources 表 Entity（FK、4索引） |
| `data/local/entity/TagEntity.kt` | 🆕 新增 | tags 表 Entity（name唯一索引） |
| `data/local/entity/ResourceTagEntity.kt` | 🆕 新增 | resource_tags 关联表 Entity |
| `data/local/entity/AppConfigEntity.kt` | 🆕 新增 | app_config 表 Entity（单例） |
| `data/local/AppDatabase.kt` | ✏️ 修改 | 添加 @Database、@TypeConverters、entities |
| `test/.../converter/ConvertersTest.kt` | 🆕 新增 | 枚举转换往返测试 |

## 已知问题 / TODO

- [ ] Room schema 导出待 M08 阶段配置 Room Gradle Plugin 后启用
