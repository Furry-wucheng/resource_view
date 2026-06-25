# M08 — AppDatabase 配置

> 轨道 1 · Stage 8/29 | 前置: M07 | 依赖共享: `doc/share/01-data-models.md` | 🟡 聚合(AppDatabase.kt)

## 执行目标

创建 `AppDatabase` 类，配置 WAL 模式、外键约束、内置标签播种、迁移骨架。

## 共享契约引用

- `doc/share/01-data-models.md` §5 — 内置标签常量
- `doc/share/03-di-contracts.md` — DatabaseModule 需更新提供 DAO 方法
- `@tech/04-数据库设计.md` §7 — 播种 SQL

## 子任务

### M08.1 AppDatabase 类

创建 `@Database` 抽象类，声明所有 Entity 和 DAO 抽象方法。

**产出物**：`data/local/AppDatabase.kt`

### M08.2 数据库回调

实现 `Callback`：
- `onCreate`：执行内置标签 `INSERT OR IGNORE` SQL
- `onOpen`：执行 `PRAGMA foreign_keys = ON` + 幂等补建内置标签

**产出物**：`data/local/AppDatabase.kt`（companion object callback）

### M08.3 更新 DatabaseModule

在 `di/DatabaseModule.kt` 的 `provideDatabase` 方法中：
- 配置 `addCallback(AppDatabase.createCallback())`
- 配置 `setJournalMode(WRITE_AHEAD_LOGGING)`
- 追加各 DAO 的 `@Provides` 方法

**产出物**：`di/DatabaseModule.kt`（追加）

### M08.4 迁移骨架

创建 `MIGRATION_1_2` 占位示例（空实现，待真实迁移时补充内容）。

**产出物**：`data/local/AppDatabase.kt`（companion object val）

## 验收标准

- [ ] 内置标签 "收藏" 在首次创建数据库时自动播种
- [ ] 外键约束在每次打开数据库时启用
- [ ] WAL 模式已配置
- [ ] DatabaseModule 提供所有 DAO 的注入
- [ ] `./gradlew build` 通过
