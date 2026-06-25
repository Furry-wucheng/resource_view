# M10 — Repository 层 + SecurePrefs

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M08,M09

## 设计决策

### D-001: Repository 方法统一使用 Result<T> 包装

- **背景**: 需要统一的错误处理机制，避免异常直接传播到 ViewModel 层
- **选择**: 所有 Repository 的非 Flow 方法统一返回 `Result<T>`，Flow 方法直接透传（不二次包装）
- **备选**: 直接抛出异常让 ViewModel 处理 — 放弃原因：不符合 doc/share/06-error-handling.md 契约
- **影响文件**: `data/repository/*.kt`
- **被依赖**: M12, M13, M14, M15, M23 等所有消费 Repository 的 Stage

### D-002: SecurePrefs 使用 EncryptedSharedPreferences

- **背景**: SMB 数据源密码需要安全存储，不能明文保存
- **选择**: 使用 AndroidX Security Crypto 库的 `EncryptedSharedPreferences`，通过 `MasterKey` 进行 AES256-GCM 加密
- **备选**: 使用 Android Keystore 直接加密 — 放弃原因：`EncryptedSharedPreferences` 已封装 Keystore 操作，使用更简洁
- **影响文件**: `data/local/secure/SecurePrefs.kt`, `di/SecurePrefsModule.kt`
- **被依赖**: M17 (SMB 源配置)

### D-003: ResourceRepository 的 Flow 查询结合标签统计

- **背景**: `getVisibleResources()` 等 Flow 查询需要同时返回资源和关联的标签信息
- **选择**: 使用 `combine` 操作符合并 `ResourceDao.getVisibleResources()` 和 `TagDao.getTagResourceCounts()` 两个 Flow
- **备选**: 在 ViewModel 中分别查询 — 放弃原因：增加 ViewModel 复杂度，且无法保证数据一致性
- **影响文件**: `data/repository/ResourceRepository.kt`
- **被依赖**: M23 (HomeScreen)

### D-004: TagRepository 内置标签保护

- **背景**: 内置标签（如"收藏"）不应被用户删除或重命名
- **选择**: 在 Repository 层检查 `isBuiltIn` 字段，如果是内置标签则返回 `Result.Err(ValidationError)`
- **备选**: 在 UI 层禁用删除/编辑按钮 — 放弃原因：双重保护更安全，防止绕过 UI 直接调用 API
- **影响文件**: `data/repository/TagRepository.kt`
- **被依赖**: M15 (标签 CRUD)

### D-005: FilesystemRepository 通过 FileSourceFactory 创建 FileSource

- **背景**: 文件浏览需要支持本地和 SMB 两种来源
- **选择**: `FilesystemRepository` 通过 `FileSourceFactory.create(source, password)` 统一创建 `FileSource`
- **备选**: 直接注入 `FileSource` — 放弃原因：`FileSource` 需要根据 Source 类型动态创建
- **影响文件**: `data/repository/FilesystemRepository.kt`
- **被依赖**: M13 (文件浏览器)

### D-006: ThumbnailRepository 接收 ThumbnailGenerator 集合

- **背景**: 不同资源类型（图片、PDF、视频、压缩包）需要不同的缩略图生成策略
- **选择**: `ThumbnailRepository` 构造函数接收 `Set<ThumbnailGenerator>`，运行时根据 `Resource.type` 选择合适的生成器
- **备选**: 硬编码生成器列表 — 放弃原因：不符合开闭原则，后续添加新类型需要修改 Repository
- **影响文件**: `data/repository/ThumbnailRepository.kt`
- **被依赖**: M23 (缩略图 LRU)

## 实现思路

### 整体架构

```
ViewModel 层
    ↓ 消费 Domain Model
Repository 层 (M10)
    ↓ 消费 Entity + DAO
Room DAO 层 (M07)
    ↓
AppDatabase (M08)
```

### 关键实现

1. **Entity → Domain 映射**: 使用 Entity 文件中的 `toDomain()` 扩展函数（M09 已实现）
2. **密码分离存储**: `SourceEntity.passwordStored` 仅标记存在性，实际密码在 `SecurePrefs`
3. **Flow 组合**: 使用 `combine` 操作符合并多个数据流，保证数据一致性
4. **错误捕获**: Repository 方法内部 try-catch，将异常转换为 `DomainError`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `data/local/secure/SecurePrefs.kt` | 🆕 新增 | EncryptedSharedPreferences 封装 |
| `di/SecurePrefsModule.kt` | 🆕 新增 | SecurePrefs Koin Module |
| `data/repository/SourceRepository.kt` | 🆕 新增 | 数据源 CRUD + 密码管理 |
| `data/repository/ResourceRepository.kt` | 🆕 新增 | 资源 CRUD + 标签筛选 + 搜索 |
| `data/repository/TagRepository.kt` | 🆕 新增 | 标签 CRUD + 内置标签保护 |
| `data/repository/FilesystemRepository.kt` | 🆕 新增 | 文件系统浏览 |
| `data/repository/ThumbnailRepository.kt` | 🆕 新增 | 缩略图生成策略管理 |
| `di/RepositoryModule.kt` | ✏️ 修改 | 填充所有 Repository 注册 |
| `ResourceViewerApp.kt` | ✏️ 修改 | 添加 securePrefsModule |
| `app/build.gradle.kts` | ✏️ 修改 | 添加 androidTest MockK 依赖 + packaging 排除 |
| `data/local/dao/*Test.kt` | ✏️ 修改 | 修复 import 路径（converter 包） |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 测试清单

| 测试文件 | 测试内容 |
|---------|---------|
| `SecurePrefsTest.kt` | 密码存取、删除、key 格式 |
| `SourceRepositoryTest.kt` | CRUD、可用性更新、密码管理 |
| `ResourceRepositoryTest.kt` | CRUD、分页、标签筛选、名称搜索 |
| `TagRepositoryTest.kt` | CRUD、内置标签保护、资源统计 |
| `FilesystemRepositoryTest.kt` | 目录列举、连接测试、异常处理 |
| `ThumbnailRepositoryTest.kt` | 生成器选择、成功/失败路径 |

## 已知问题 / TODO

- [ ] `FilesystemRepository.getFileSource()` 中的 `sourceName` 目前传空字符串，需要联合查询 Source 表
- [ ] `ResourceRepository.getById()` 中的 `sourceName` 和 `tags` 目前传空值，需要完善联合查询
- [ ] `ThumbnailRepository` 目前使用空的 `ThumbnailGenerator` 集合，实际生成器在 M19/M22/M23 添加
- [ ] 已有 DAO 测试文件的 import 路径已修复，但需确认测试逻辑是否正确
