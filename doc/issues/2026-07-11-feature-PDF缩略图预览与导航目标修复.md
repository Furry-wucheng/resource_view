# feature: PDF 缩略图预览与导航目标修复

> 日期: 2026-07-11 | 类型: feature | 状态: ✅ 已完成

## 描述

文件浏览器缩略图生成目前仅支持图片和视频格式，PDF 文件在目录中无法显示预览封面。同时，导航系统对 PDF/ARCHIVE/VIDEO 类型的资源路由解析有误——它们不应按 organizationMode 路由到 CHAPTER_LIST/FLAT_GRID/GALLERY，而应直接进入 VIEWER（单一文件查看器）。

已在工作区完成实现但未提交，需补全文档和提交。

## 验收标准

- [x] 文件浏览器中 PDF 文件缩略图生成正常（调用 PdfRenderer 渲染第一页）
- [x] 文件夹预览时 PDF 优先级排在图片和视频之后
- [x] PDF/ARCHIVE/VIDEO 类型资源导航目标统一为 VIEWER
- [x] 测试覆盖 PDF 预览逻辑和导航目标解析
- [x] 构建、测试、lint 全部通过

## 影响分析

| 维度 | 内容 |
|------|------|
| 修改文件 | 9 个文件（6 源文件 + 2 测试 + 1 文档） |
| 影响 stage | 无 — 独立功能增强，不阻塞已有 stage |
| 聚合文件 | 无 |

## 是否需合并回原文档

- [ ] 是 → 需更新哪些 share/prd/tech 文档：
- [x] 否 → 独立提案，不回溯

## 执行计划

1. **RED**: 已有测试用例（`FileEntryThumbnailLoaderTest.kt` 新增 PDF 用例、`ResourceDestinationResolverTest.kt` 新增 PDF 资源用例）
2. **GREEN**: 实现已就位（`FileEntryThumbnailLoader` 增加 `decodePdf`、`isPdf`；`ResourceDestinationResolver` 增加 `resolveResourceDestination(resource)` 重载）
3. **REFACTOR**: 调整 `ThumbnailLoadManager` 和 `CoilModule` 注入 `Context` 以支持 PdfRenderer
4. **验证**: 运行 full build + test + lint
5. **提交**: 按规范提交

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `app/src/main/.../di/CoilModule.kt` | 修改 | ThumbnailLoadManager 注入 Context |
| `app/src/main/.../thumbnail/FileBrowserThumbnailDiskCache.kt` | 修改 | 缓存版本号 v1→v2（PDF 缩略图格式兼容） |
| `app/src/main/.../thumbnail/FileEntryThumbnailLoader.kt` | 修改 | 新增 PDF 缩略图解码 + Context 注入 |
| `app/src/main/.../thumbnail/ThumbnailLoadManager.kt` | 修改 | 透传 Context 到 Loader |
| `app/src/main/.../navigation/AppNavGraph.kt` | 修改 | 传递完整 resource 对象给解析器 |
| `app/src/main/.../navigation/ResourceDestinationResolver.kt` | 修改 | 新增重载处理 PDF/ARCHIVE/VIDEO→VIEWER |
| `app/src/test/.../thumbnail/FileEntryThumbnailLoaderTest.kt` | 修改 | 新增 PDF 预览和优先级测试 |
| `app/src/test/.../navigation/ResourceDestinationResolverTest.kt` | 修改 | 新增 PDF 资源导航测试 |
| `doc/share/09-testing-conventions.md` | 修改 | 修复 typo |
