# M22 — PDF 查看器

> 轨道 6 · Stage 22/29 | 前置: M11,M14 | 依赖共享: `doc/share/02-interfaces.md` §2 | 🟢 独占

## 执行目标

实现 PDF 查看器：pdfium-android 渲染 + PdfContentProvider + 在 ViewerScreen 中集成。

## 共享契约引用

- `doc/share/02-interfaces.md` §2 — ContentProvider 接口
- `@prd/04-资源查看器.md` — PDF 查看器交互
- `@tech/01-技术可行性分析.md` §3.2 — pdfium-android 用法

## 子任务

### M22.1 PdfRenderer 工具类

封装 pdfium-android 的 `PdfiumCore`，提供：
- `openDocument(fd/bytes)`: 打开 PDF
- `getPageCount()`: 页数
- `renderPage(index, width, height)`: 渲染指定页为 Bitmap

**产出物**：`data/remote/pdf/PdfRenderer.kt`

### M22.2 PdfContentProvider

实现 `ContentProvider` 接口：
- `pageCount`: PDF 总页数
- `loadPage(index)`: 渲染为 Bitmap → 压缩为 JPEG ByteArray
- `dispose()`: 关闭 PDFium 文档

**产出物**：`shared/content/PdfContentProvider.kt`

### M22.3 PdfThumbnailGenerator

实现 `ThumbnailGenerator`：渲染第 0 页 → resize → 保存为缩略图。

**产出物**：`shared/thumbnail/PdfThumbnailGenerator.kt`

### M22.4 集成到 ViewerScreen

在 `ViewerViewModel` 中，PDF 类型 Resource 使用 `PdfContentProvider`。`ViewerScreen` 中处理 PDF 页面（渲染为 ImagePage，统一渲染路径）。

## 验收标准

- [x] PDF 资源进入查看器，显示 PDF 内容
- [x] 左右翻页流畅（每页预加载）
- [x] 加密 PDF 显示 MediaEncryptedError 提示
- [x] 退出查看器后 PDFium 资源正确释放
- [x] `./gradlew build` 通过
