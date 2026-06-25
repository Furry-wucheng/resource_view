# M07 — Room DAO 接口

> 轨道 1 · Stage 7/29 | 前置: M06 | 依赖共享: `doc/share/01-data-models.md` | 🟢 独占

## 执行目标

创建所有 Room DAO 接口，包含 CRUD、Flow 查询、键集分页、标签交集查询。

## 共享契约引用

- `doc/share/01-data-models.md` — Entity 类型
- `@tech/04-数据库设计.md` — DAO 详细 SQL 参考

## 子任务

### M07.1 SourceDao

CRUD + Flow getAllSources + updateAvailability。

**产出物**：`data/local/dao/SourceDao.kt`

### M07.2 ResourceDao

Flow getVisibleResources + getAvailableResources + getById + insert/IGNORE + insertAll/REPLACE + update + deleteById + deleteBySourceId + pageAfter (键集分页) + filterByTags (GROUP BY HAVING 交集) + searchByName。

**产出物**：`data/local/dao/ResourceDao.kt`

### M07.3 TagDao

Flow getAllTags + getBuiltInTags + insert/IGNORE + update + deleteById (isBuiltIn=0) + getTagResourceCounts (LEFT JOIN 关联统计)。

**产出物**：`data/local/dao/TagDao.kt`

### M07.4 ResourceTagDao

CRUD + deleteByXxx + getByResourceId + countByTagId。

**产出物**：`data/local/dao/ResourceTagDao.kt`

### M07.5 AppConfigDao

单例读取 + upsert。

**产出物**：`data/local/dao/AppConfigDao.kt`

## 验收标准

- [ ] 所有 DAO 方法签名正确，Query SQL 与 `@tech/04-数据库设计.md` 一致
- [ ] Flow 返回类型正确（自动被 Room 监听）
- [ ] 键集分页逻辑正确（`createdAt DESC, id ASC`）
- [ ] 标签交集查询使用 `GROUP BY HAVING COUNT(DISTINCT tagId) = N`
- [ ] `./gradlew build` 通过
