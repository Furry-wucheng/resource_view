# M10 — Repository 层 + SecurePrefs

> 轨道 1 · Stage 10/29 | 前置: M08,M09 | 依赖共享: `doc/share/01-data-models.md` `doc/share/03-di-contracts.md` `doc/share/06-error-handling.md` | 🟡 聚合(RepositoryModule.kt + DatabaseModule.kt)

## 执行目标

实现所有 Repository + SecurePrefs 密码存储封装。这是数据层的对外门面。

## 共享契约引用

- `doc/share/03-di-contracts.md` — RepositoryModule 契约、SecurePrefsModule 契约
- `doc/share/06-error-handling.md` — Result<T>、DomainError 层级
- `doc/share/01-data-models.md` — Entity/Domain Model 映射

## 子任务

### M10.1 SecurePrefs

封装 `EncryptedSharedPreferences`，提供 `putPassword(sourceId, pwd)` / `getPassword(sourceId)` / `removePassword(sourceId)`。

**产出物**：`data/local/secure/SecurePrefs.kt`

### M10.2 SecurePrefsModule

创建 Hilt Module，提供 `SecurePrefs` 单例。

**产出物**：`di/SecurePrefsModule.kt`

### M10.3 SourceRepository

CRUD + 密码存取 + Entity→Domain 映射。对外只暴露 Domain Model。

**产出物**：`data/repository/SourceRepository.kt`

### M10.4 ResourceRepository

CRUD + getVisibleResources(Flow) + filterByTags + searchByName + keysetPagination。对外只暴露 Domain Model。

**产出物**：`data/repository/ResourceRepository.kt`

### M10.5 TagRepository

CRUD + 内置标签不可删除/重命名检查 + getTagResourceCounts。

**产出物**：`data/repository/TagRepository.kt`

### M10.6 FilesystemRepository

基于 `SourceDao` 获取 Source → 通过 `FileSourceFactory` 创建 FileSource → listDirectory/stat。

**产出物**：`data/repository/FilesystemRepository.kt`

### M10.7 ThumbnailRepository

综合 ThumbnailGenerator 策略集合，根据 Resource.type 选择生成器。

**产出物**：`data/repository/ThumbnailRepository.kt`

### M10.8 RepositoryModule 填充

在 `di/RepositoryModule.kt` 中将所有 `@Singleton @Provides` 方法填入。

**产出物**：`di/RepositoryModule.kt`（填充）

## 验收标准

- [ ] 所有 Repository 方法使用 `Result<T>` 包装（catch Exception → Err）
- [ ] Flow 查询直接透传（不二次包装）
- [ ] SecurePrefs 正确使用 EncryptedSharedPreferences
- [ ] `./gradlew build` 通过
- [ ] `./gradlew connectedAndroidTest` 中 Repository 测试通过（见 M07 时的 DAO 测试）
