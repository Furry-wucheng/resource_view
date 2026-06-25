# M08 — AppDatabase 配置

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M07

## 设计决策

### D-001: Schema 导出配置方式
- **背景**: 需要导出 Room 数据库 Schema 用于迁移验证和文档生成
- **选择**: 在 `build.gradle.kts` 的 `ksp` 块中配置 `room.schemaLocation` 参数
- **备选**: 使用注解处理器参数 `arg()`，但 KSP 插件需要在顶层配置
- **影响文件**: `app/build.gradle.kts:8-11`

### D-002: 备份管理器设计
- **背景**: 需要支持数据库备份和恢复功能，防止用户数据丢失
- **选择**: 创建独立的 `DatabaseBackupManager` 类，支持文件备份和 URI 备份两种方式
- **备选**: 将备份逻辑放在 Repository 层，但职责不够单一
- **影响文件**: `data/local/backup/DatabaseBackupManager.kt`
- **被依赖**: M25 设置页面将集成备份恢复 UI

### D-003: 备份文件格式
- **背景**: 需要确定备份文件的命名和存储格式
- **选择**: 使用 `backup_yyyyMMdd_HHmmss.db` 格式，同时备份 WAL 和 SHM 文件
- **备选**: 使用自定义格式或压缩包，但增加了复杂度
- **影响文件**: `data/local/backup/DatabaseBackupManager.kt:35-45`

### D-004: 迁移管理器设计
- **背景**: 需要管理数据库版本迁移和 Schema 校验
- **选择**: 创建 `DatabaseMigrator` 类，集中管理所有迁移定义和校验逻辑
- **备选**: 将迁移放在 AppDatabase companion object，但不够灵活
- **影响文件**: `data/local/migration/DatabaseMigrator.kt`
- **被依赖**: 后续版本升级将在此添加迁移定义

### D-005: Schema 校验实现
- **背景**: 需要验证导出的 Schema 文件是否包含所有必需的表
- **选择**: 使用 JSON 解析 Schema 文件，检查表名和列信息
- **备选**: 直接查询数据库元数据，但无法验证导出的 Schema
- **影响文件**: `data/local/migration/DatabaseMigrator.kt:50-80`

### D-006: 数据库统计功能
- **背景**: 需要提供数据库状态信息用于调试和展示
- **选择**: 在 DatabaseMigrator 中添加 `getDatabaseStats()` 方法
- **备选**: 创建独立的 StatsManager，但功能过于简单
- **影响文件**: `data/local/migration/DatabaseMigrator.kt:85-110`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `app/build.gradle.kts` | ✏️ 修改 | 添加 KSP Schema 导出配置 |
| `data/local/AppDatabase.kt` | ✏️ 修改 | 启用 exportSchema，添加 DATABASE_VERSION 常量 |
| `data/local/backup/DatabaseBackupManager.kt` | 🆕 新增 | 数据库备份恢复管理器 |
| `data/local/migration/DatabaseMigrator.kt` | 🆕 新增 | 数据库迁移管理和 Schema 校验 |
| `di/DatabaseModule.kt` | ✏️ 修改 | 注册 DatabaseMigrator 和 DatabaseBackupManager |
| `test/.../DatabaseBackupManagerTest.kt` | 🆕 新增 | 备份管理器单元测试 |
| `test/.../DatabaseMigratorTest.kt` | 🆕 新增 | 迁移管理器单元测试 |
| `androidTest/.../DatabaseBackupManagerInstrumentedTest.kt` | 🆕 新增 | 备份管理器仪器化测试 |
| `androidTest/.../DatabaseMigratorInstrumentedTest.kt` | 🆕 新增 | 迁移管理器仪器化测试 |
| `schemas/.../1.json` | 🆕 新增 | 导出的 Schema JSON 文件 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 实现细节

### Schema 导出配置
```kotlin
// build.gradle.kts
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}
```

### 备份管理器核心方法
- `createBackup(backupDir: File): Result<File>` - 创建备份
- `restoreFromBackup(backupFile: File): Result<Unit>` - 从文件恢复
- `restoreFromUri(uri: Uri): Result<Unit>` - 从 URI 恢复
- `getBackupFiles(backupDir: File): List<BackupInfo>` - 获取备份列表
- `deleteBackup(backupFile: File): Result<Unit>` - 删除备份

### 迁移管理器核心方法
- `getMigrations(): Array<Migration>` - 获取所有迁移
- `getCurrentVersion(): Int` - 获取当前版本
- `validateSchema(version: Int): SchemaValidationResult` - 校验 Schema
- `getDatabaseStats(database: AppDatabase): DatabaseStats` - 获取统计

## 已知问题 / TODO

- [ ] 备份文件未加密，敏感数据可能泄露
- [ ] 恢复备份后需要重启应用才能生效
- [ ] Schema 校验目前只检查表名，未验证列类型
- [ ] 后续可考虑添加自动备份调度功能
