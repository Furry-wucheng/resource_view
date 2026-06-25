# M00 — 依赖配置与版本同步

> 轨道 0 · Stage 0/29 | 前置: 无 | 依赖共享: 无 | 🟢 无聚合文件冲突

## 执行目标

补齐 `libs.versions.toml` 和 `app/build.gradle.kts` 中所有依赖声明，确保编译通过。

## 共享契约引用

- `doc/share/01-data-models.md` — 了解需持久化的实体，确认 Room 版本
- `doc/share/03-di-contracts.md` — 了解 Hilt 版本要求
- `doc/share/05-theme-tokens.md` — 了解 Coil/Compose 版本

## 子任务

### M00.1 补齐版本目录

在 `gradle/libs.versions.toml` 追加所有项目依赖的版本声明。

**产出物**：`gradle/libs.versions.toml`（追加，不删除已有内容）

需要追加的版本：

| 依赖 | 版本 |
|------|------|
| navigation-compose | 2.8.x |
| hilt-android | 2.51 |
| hilt-compiler | 2.51 |
| hilt-navigation-compose | 1.2.0 |
| room-runtime | 2.6.1 |
| room-ktx | 2.6.1 |
| room-compiler | 2.6.1 |
| room-testing | 2.6.1 |
| media3-exoplayer | 1.5.x |
| media3-ui | 1.5.x |
| coil-compose | 3.5.0 |
| coil-video | 3.5.0 |
| pdfium-android | 1.0.0 |
| zip4j | 2.11.6 |
| commons-compress | 1.28.0 |
| smbj | 0.13.0 |
| security-crypto | 1.1.0-alpha06 |
| kotlinx-coroutines-android | 1.8.x |
| benasher-uuid | 0.8.2 |
| mockk | 1.13.16 |
| kotlinx-coroutines-test | 1.8.x |

### M00.2 补齐模块依赖

在 `app/build.gradle.kts` 追加所有 `implementation`/`ksp`/`testImplementation` 声明。

**产出物**：`app/build.gradle.kts`（追加 dependencies）

### M00.3 添加 KSP 插件

在 `build.gradle.kts`（根）和 `app/build.gradle.kts` 添加 KSP 插件声明。

## 验收标准

- [x] `./gradlew build` 成功，零错误
- [x] 所有依赖声明可解析
- [x] KSP 插件正确配置（Hilt/Room 代码生成就绪）

> **注意**: pdfium-android 版本从 1.0.0 调整为 2.0.0（io.legere:pdfiumandroid），因为原版本在 Maven Central 上不可用且与 AndroidX 不兼容。
