# M07 — Room DAO 接口

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M06

## 设计决策

### D-001: 键集分页实现方式
- **背景**: 需要实现资源列表的分页加载，避免全量查询导致内存问题
- **选择**: 使用键集分页（Keyset Pagination），基于 `createdAt DESC, id ASC` 排序
- **备选**: 使用 Offset 分页，但随着数据量增长性能会下降
- **影响文件**: `data/local/dao/ResourceDao.kt:45-53`
- **被依赖**: M10 Repository 层将使用此方法实现分页加载

### D-002: 标签交集查询实现
- **背景**: 需要实现按多个标签筛选资源，返回同时拥有所有指定标签的资源
- **选择**: 使用 `GROUP BY HAVING COUNT(DISTINCT tagId) = N` 子查询实现交集
- **备选**: 使用 Room 不支持的 INTERSECT 语法
- **影响文件**: `data/local/dao/ResourceDao.kt:55-65`
- **被依赖**: M16 筛选栏功能将依赖此查询

### D-003: Flow 返回类型选择
- **背景**: DAO 查询需要支持实时数据更新
- **选择**: 对于列表查询使用 `Flow<List<Entity>>`，对于单条查询使用 `suspend fun`
- **备选**: 全部使用 PagingSource，但增加了不必要的复杂度
- **影响文件**: 所有 DAO 文件
- **被依赖**: M10 Repository 层将直接暴露这些 Flow

### D-004: 内置标签保护机制
- **背景**: 内置标签（如收藏）不应被用户删除
- **选择**: 在 `deleteById` 查询中添加 `AND isBuiltIn = 0` 条件
- **备选**: 在应用层检查，但数据库层更安全
- **影响文件**: `data/local/dao/TagDao.kt:35`
- **被依赖**: M15 标签 CRUD 功能将依赖此保护

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `data/local/dao/SourceDao.kt` | 🆕 新增 | 数据源 CRUD + 可用性更新 |
| `data/local/dao/ResourceDao.kt` | 🆕 新增 | 资源 CRUD + 键集分页 + 标签交集查询 + 搜索 |
| `data/local/dao/TagDao.kt` | 🆕 新增 | 标签 CRUD + 内置标签保护 + 资源计数 |
| `data/local/dao/ResourceTagDao.kt` | 🆕 新增 | 资源-标签关联 CRUD |
| `data/local/dao/AppConfigDao.kt` | 🆕 新增 | 应用配置单例读写 |
| `data/local/AppDatabase.kt` | ✏️ 修改 | 添加 DAO 抽象方法 |
| `di/DatabaseModule.kt` | ✏️ 修改 | 添加 DAO 提供方法 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

### D-005: Room 版本升级
- **背景**: Room 2.6.1 与 Kotlin 2.4.0 + KSP 2.3.9 存在兼容性问题，导致 "unexpected jvm signature V" 错误
- **选择**: 升级 Room 到 2.7.1
- **备选**: 降级 Kotlin 或 KSP 版本，但会影响其他依赖
- **影响文件**: `gradle/libs.versions.toml`
- **被依赖**: 所有使用 Room 的模块

## 已知问题 / TODO

- [ ] 后续可能需要添加 Paging 3 支持以优化大数据集分页
- [ ] 标签交集查询在标签数量较多时可能需要优化索引
