# bug: GIF 查看时无法播放，只作为静态图片

> 日期: 2026-06-28 | 类型: bug | 状态: ✅ 已完成

## 现象

打开 GIF 文件时，查看器只显示静态第一帧，不会播放动画。

## 复现步骤

1. 进入包含 GIF 文件的资源库或文件浏览器
2. 点击打开一个 GIF 文件
3. 查看器显示静态图片，无动画效果

## 期望效果

GIF 文件在查看器中应自动播放动画，类似视频循环播放。

## 影响分析

| 维度 | 内容 |
|------|------|
| 根因 | 1. 缺少 `coil-gif` 依赖<br>2. Coil ImageLoader 未注册 GIF 解码器<br>3. `PageBitmapLoader` 使用 `BitmapFactory` 只解码第一帧<br>4. `ViewerScreen.PageContent` 使用静态 `Image(bitmap)` 渲染 |
| 修改文件 | `gradle/libs.versions.toml`、`app/build.gradle.kts`、`CoilModule.kt`、`ViewerItem.kt`、`ImageFolderProvider.kt`、`MixedFolderProvider.kt`、`ViewerScreen.kt` |
| 影响 stage | 无 stage 产物需修改，属于独立 bug 修复 |
| 聚合文件 | `libs.versions.toml`（追加依赖） |

## 执行计划

1. **添加依赖**：`coil-gif` 库
2. **配置 Coil**：注册 `AnimatedImageDecoder.Factory()`
3. **扩展数据模型**：`ViewerItem.ImagePage` 添加 `extension` 字段
4. **更新 Provider**：`ImageFolderProvider`、`MixedFolderProvider` 填充 extension
5. **修改渲染逻辑**：GIF 文件使用 Coil `AsyncImage` 渲染动画
6. **TDD 测试**：编写单元测试验证 GIF 检测逻辑

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `doc/issues/2026-06-28-bug-gif-not-playing.md` | 新增 | 本 bug 报告 |
| `gradle/libs.versions.toml` | 修改 | 追加 coil-gif 依赖 |
| `app/build.gradle.kts` | 修改 | 添加 implementation |
| `CoilModule.kt` | 修改 | 注册 AnimatedImageDecoder.Factory() |
| `ViewerItem.kt` | 修改 | ImagePage 添加 extension 字段和 isAnimated 属性 |
| `ImageFolderProvider.kt` | 修改 | 添加 getPageExtension() 和 getPageUri() 方法 |
| `MixedFolderProvider.kt` | 修改 | buildViewerItems() 填充 extension |
| `ViewerViewModel.kt` | 修改 | 添加 getPageUri() 方法，创建 ImagePage 时填充 extension |
| `ViewerScreen.kt` | 修改 | GIF 使用 Coil AsyncImage 渲染动画 |
| `ViewerItemTest.kt` | 新增 | 测试 isAnimated 属性 |
| `ImageFolderProviderTest.kt` | 修改 | 添加 getPageExtension 测试 |

验收: build/test/lint 全部通过
