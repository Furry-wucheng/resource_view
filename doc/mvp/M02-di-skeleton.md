# M02 — Hilt DI 骨架

> 轨道 0 · Stage 2/29 | 前置: M00 | 依赖共享: `doc/share/03-di-contracts.md` | 🟢 独占 + 🟡 聚合(DatabaseModule 骨架)

## 执行目标

配置 Hilt 应用入口、Activity 和基础 DI Module 占位。

## 共享契约引用

- `doc/share/03-di-contracts.md` — DatabaseModule 提供的契约签名
- `doc/share/07-directory-layout.md` — 文件位置

## 子任务

### M02.1 Application 类

创建 `@HiltAndroidApp` 入口类。

**产出物**：`ResourceViewerApp.kt`

### M02.2 MainActivity

创建 `@AndroidEntryPoint` Activity，使用 `setContent { ResourceViewerTheme { ... } }`（当前 Theme 占位）。

**产出物**：`MainActivity.kt`

### M02.3 DatabaseModule（骨架）

创建 DI Module，只声明 `provideDatabase`（当前数据库类占位，M08 完成）。后续 M08 追加 DAO 的 `@Provides` 方法。

**产出物**：`di/DatabaseModule.kt`

### M02.4 RepositoryModule（占位）

创建空的 RepositoryModule 文件（类体留空）。后续 M10 填充。

**产出物**：`di/RepositoryModule.kt`

## 验收标准

- [ ] Hilt 注册结构符合 `doc/share/03-di-contracts.md`
- [ ] 应用可冷启动不崩溃（`MainActivity` 正常渲染）
- [ ] `./gradlew build` 通过
- [ ] KSP 代码生成正常工作（Hilt 编译通过）
