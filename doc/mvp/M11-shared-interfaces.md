# M11 — 全部共享接口契约

> 轨道 2 · Stage 11/29 | 前置: M09 | 依赖共享: `doc/share/02-interfaces.md` | 🟢 独占

## 执行目标

一次性创建所有跨模块接口，后续 stage 各自实现。这是并行开发的契约基础。

## 共享契约引用

- `doc/share/02-interfaces.md` — 所有接口签名（直接搬运实现）
- `doc/share/01-data-models.md` §4 — ViewerItem 联合类型
- `doc/share/06-error-handling.md` — Result<T> 类型

## 子任务

### M11.1 FileSource 接口 + FileEntry

创建 `FileSource` 接口（含 `listDirectory`、`stat`、`readFile`、`readRange`、`openInputStream`、`testConnection`、`disconnect`）。同一文件中定义 `FileEntry` data class。

**产出物**：`shared/filesource/FileSource.kt`

### M11.2 FileSourceFactory

创建工厂对象，根据 `SourceType` 返回对应实现（当前 LOCAL→LocalFileSource；SMB→SmbFileSource；FTP/WEBDAV→UnsupportedOperationException）。

**产出物**：`shared/filesource/FileSourceFactory.kt`

### M11.3 ContentProvider 接口

创建 `ContentProvider` 接口（`pageCount`、`loadPage(index)`、`dispose()`）。

**产出物**：`shared/content/ContentProvider.kt`

### M11.4 OrganizationStrategy 接口 + Chapter

创建 `OrganizationStrategy` 接口（`mode`、`getChapters`、`getContents`、`createProvider`）。同一文件中定义 `Chapter` data class。

**产出物**：`shared/organization/OrganizationStrategy.kt`

### M11.5 ThumbnailGenerator 接口

创建 `ThumbnailGenerator` 接口（`canHandle(type)`、`generate(resource, fileSource, cacheDir)`）。

**产出物**：`shared/thumbnail/ThumbnailGenerator.kt`

### M11.6 DomainError + Result + Progress

实现错误处理基础设施（如果 M09 未做的话）。

**产出物**：`domain/error/DomainError.kt`、`domain/error/Result.kt`、`domain/error/Progress.kt`

## 验收标准

- [ ] 所有接口签名与 `doc/share/02-interfaces.md` 完全一致
- [ ] FileSourceFactory 支持 LOCAL/SMB 返回正确类型
- [ ] 编译通过（接口没有实现体依赖）
- [ ] `./gradlew build` 通过
