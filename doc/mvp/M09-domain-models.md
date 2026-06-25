# M09 — Domain Models

> 轨道 1 · Stage 9/29 | 前置: M06 | 依赖共享: `doc/share/01-data-models.md` §3-4 | 🟢 独占

## 执行目标

创建所有 Domain Model（Repository 对外暴露层），区别于 Entity。

## 共享契约引用

- `doc/share/01-data-models.md` §3 — Domain Model 定义
- `doc/share/01-data-models.md` §4 — ViewerItem 联合类型
- `doc/share/02-interfaces.md` §1 — FileEntry、Chapter 定义

## 子任务

### M09.1 Source

创建 `Source` data class（对应 SourceEntity，不含 password 字段）。

**产出物**：`domain/model/Source.kt`

### M09.2 Resource

创建 `Resource` data class（包含 sourceName 展平字段 + tags 内联）。

**产出物**：`domain/model/Resource.kt`

### M09.3 Tag

创建 `Tag` data class（包含 resourceCount 统计）。

**产出物**：`domain/model/Tag.kt`

### M09.4 FileEntry

创建 `FileEntry` data class（用于文件浏览器列表）。

**产出物**：`domain/model/FileEntry.kt`

### M09.5 Chapter

创建 `Chapter` data class（组织模式中的章节概念）。

**产出物**：`domain/model/Chapter.kt`

### M09.6 ViewerItem

创建 `ViewerItem` sealed class（ImagePage + Video）和 `VideoMediaSource` sealed class（LocalFile + SmbFile）。

**产出物**：`domain/model/ViewerItem.kt`

### M09.7 AppConfig

创建 `AppConfig` data class。

**产出物**：`domain/model/AppConfig.kt`

### M09.8 Entity → Domain 映射

在各自 Entity 文件中添加 `toDomain()` 扩展函数（或统一放在 `mapper/` 包）。

## 验收标准

- [ ] 所有 Domain Model 字段与 `doc/share/01-data-models.md` §3 一致
- [ ] ViewerItem sealed class 层级正确
- [ ] Entity.toDomain() 映射函数可用
- [ ] `./gradlew build` 通过
