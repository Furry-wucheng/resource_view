# M06 — Room Entity + Enum + Converters

> 轨道 1 · Stage 6/29 | 前置: M02 | 依赖共享: `doc/share/01-data-models.md` | 🟢 独占

## 执行目标

创建所有 Room Entity 定义、枚举类型和 TypeConverter。

## 共享契约引用

- `doc/share/01-data-models.md` §1 — Entity 完整定义（直接搬运实现）
- `doc/share/01-data-models.md` §2 — Enum 定义
- `doc/share/07-directory-layout.md` — 文件位置

## 子任务

### M06.1 枚举定义

创建 `SourceType`、`ResourceType`、`OrganizationMode`、`ThemeMode`、`PageDirection`、`DoublePageMode`、`AutoSyncInterval` 枚举。

**产出物**：`data/local/converter/Converters.kt`（枚举放在 converters 包或独立 enum 文件）

### M06.2 SourceEntity

创建 `sources` 表 Entity，含所有字段和注解。

**产出物**：`data/local/entity/SourceEntity.kt`

### M06.3 ResourceEntity

创建 `resources` 表 Entity，含 外键(FK→sources, CASCADE)、4 个索引、唯一约束(sourceId, relativePath)。

**产出物**：`data/local/entity/ResourceEntity.kt`

### M06.4 TagEntity

创建 `tags` 表 Entity，含 name 唯一索引。

**产出物**：`data/local/entity/TagEntity.kt`

### M06.5 ResourceTagEntity

创建 `resource_tags` 关联表 Entity，含复合主键、双 FK (CASCADE)、双向索引。

**产出物**：`data/local/entity/ResourceTagEntity.kt`

### M06.6 AppConfigEntity

创建 `app_config` 表 Entity（单例，id=1）。

**产出物**：`data/local/entity/AppConfigEntity.kt`

### M06.7 TypeConverters

创建 Room TypeConverter 类，注册所有枚举 ↔ String 转换。

**产出物**：`data/local/converter/Converters.kt`

## 验收标准

- [ ] 所有 Entity 字段类型、注解与 `doc/share/01-data-models.md` 一致
- [ ] FK 约束、索引、级联删除正确声明
- [ ] TypeConverter 覆盖所有枚举
- [ ] `./gradlew build` 通过（Room KSP 生成的代码正确）
