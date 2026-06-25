# M02 — DI 骨架

> 时间: 2026-06-25 | Agent: opencode | 状态: ✅ 已完成 | 前置: M00

## 设计决策

### D-001: DI 框架选择 — Koin 替换 Hilt

- **背景**: 原计划使用 Hilt 作为 DI 框架，但 Hilt 2.59.2 与 Kotlin 2.4.0 存在 metadata 版本不兼容（Hilt 最高支持 metadata 2.3.0，Kotlin 2.4.0 生成 metadata 2.4.0）
- **选择**: 改用 Koin 4.0.2 作为 DI 框架
  - Koin 原生支持 Kotlin 2.4.0
  - 无需 Gradle 插件，仅需依赖库
  - Compose 集成良好（koin-androidx-compose）
- **备选**: 
  - 降级 Kotlin 到 2.3.x — 但用户明确要求使用 Kotlin 2.4.0
  - 等待 Hilt 更新 — 时间不确定，阻塞开发
- **影响文件**: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `di/DatabaseModule.kt`, `di/RepositoryModule.kt`
- **被依赖**: M03, M06, M08, M10 等所有需要 DI 的后续 Stage

### D-002: Koin 模块组织方式

- **背景**: 需要定义 Koin 模块的组织方式
- **选择**: 使用顶层 `val xxxModule = module { }` 定义，在 Application 中统一加载
  - `databaseModule` — 提供 AppDatabase 实例
  - `repositoryModule` — 占位，M10 填充
- **备选**: 使用 `@Module` 注解类 — Koin 不需要注解，更简洁
- **影响文件**: `ResourceViewerApp.kt`, `di/DatabaseModule.kt`, `di/RepositoryModule.kt`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ResourceViewerApp.kt` | 🆕 新增 | Koin 初始化入口 |
| `di/DatabaseModule.kt` | 🆕 新增 | Koin 模块骨架（provideDatabase） |
| `di/RepositoryModule.kt` | 🆕 新增 | Koin 模块占位（M10 填充） |
| `data/local/AppDatabase.kt` | 🆕 新增 | Room 占位类（M08 替换） |
| `MainActivity.kt` | ✏️ 修改 | 移除 @AndroidEntryPoint |
| `AndroidManifest.xml` | ✏️ 修改 | 注册 ResourceViewerApp |
| `gradle/libs.versions.toml` | ✏️ 修改 | Koin 4.0.2 替换 Hilt |
| `app/build.gradle.kts` | ✏️ 修改 | Koin 依赖替换 Hilt |
| `build.gradle.kts` | ✏️ 修改 | 移除 Hilt 插件声明 |
| `settings.gradle.kts` | ✏️ 修改 | 移除 kotlin-metadata-jvm workaround |

## 已知问题 / TODO

- [ ] 需要更新 `doc/share/03-di-contracts.md` 中的 Hilt 契约为 Koin 契约（需 PR review）
- [ ] AppDatabase 占位类需要 M08 替换为完整实现
- [ ] RepositoryModule 需要 M10 填充具体 providers
