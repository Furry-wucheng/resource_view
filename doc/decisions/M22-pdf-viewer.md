# M22 — PDF 查看器

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M11, M14

## 设计决策

### D-001: pdfium-android 库选择
- **背景**: 需要 PDF 渲染引擎，技术可行性分析推荐 pdfium-android
- **选择**: 使用 `io.legere:pdfiumandroid:2.0.0`，这是 PdfiumAndroidKt 的 Kotlin 重写版本，支持协程
- **备选**: Android 内置 `PdfRenderer`（不支持加密 PDF 检测）
- **影响文件**: `gradle/libs.versions.toml`, `app/build.gradle.kts`

### D-002: PdfRenderer 封装方式
- **背景**: pdfium-android API 较底层，需要封装为简洁的工具类
- **选择**: 创建 `PdfRenderer` 类，封装 `PdfiumCore` 和 `PdfDocument`，提供 `renderPage()` 和 `getPageSize()` 方法，实现 `Closeable` 接口
- **备选**: 直接在 PdfContentProvider 中使用 pdfium API（耦合度高）
- **影响文件**: `data/remote/pdf/PdfRenderer.kt`

### D-003: PdfContentProvider 初始化策略
- **背景**: ContentProvider 需要在初始化时读取 PDF 文件
- **选择**: 在构造函数中使用 `runBlocking` 读取 PDF 字节（PDF 文件通常较小，且必须在渲染前加载）
- **备选**: 惰性加载（首次渲染时加载，但会增加首次渲染延迟）
- **影响文件**: `shared/content/PdfContentProvider.kt`

### D-004: 加密 PDF 处理方式
- **背景**: pdfium-android 打开加密 PDF 会抛出 IOException
- **选择**: 在 ViewerViewModel 中捕获 IOException，显示 MediaEncryptedError 提示
- **备选**: 在 PdfRenderer 中检测加密状态（pdfium-android 2.x 没有 isEncrypted 属性）
- **影响文件**: `ui/screens/viewer/ViewerViewModel.kt`

### D-005: PdfThumbnailGenerator 缩放策略
- **背景**: 需要生成缩略图，但 PDF 页面可能很大
- **选择**: 渲染第 0 页，按 maxThumbnailSize(300px) 等比缩放，保存为 JPEG
- **备选**: 使用 Coil 加载（不支持 PDF 格式）
- **影响文件**: `shared/thumbnail/PdfThumbnailGenerator.kt`

### D-006: ViewerViewModel Context 依赖
- **背景**: PdfContentProvider 需要 Context 来创建 PdfiumCore
- **选择**: 在 ViewerViewModel 构造函数中添加 context 参数，通过 Koin 注入
- **备选**: 使用 Application 级别的单例（增加耦合）
- **影响文件**: `ui/screens/viewer/ViewerViewModel.kt`, `di/ViewModelModule.kt`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `data/remote/pdf/PdfRenderer.kt` | 🆕 新增 | PDF 渲染工具类，封装 pdfium-android |
| `data/remote/pdf/PdfRendererTest.kt` | 🆕 新增 | PdfRenderer 单元测试 |
| `shared/content/PdfContentProvider.kt` | 🆕 新增 | PDF ContentProvider 实现 |
| `shared/content/PdfContentProviderTest.kt` | 🆕 新增 | PdfContentProvider 单元测试 |
| `shared/thumbnail/PdfThumbnailGenerator.kt` | 🆕 新增 | PDF 缩略图生成器 |
| `shared/thumbnail/PdfThumbnailGeneratorTest.kt` | 🆕 新增 | PdfThumbnailGenerator 单元测试 |
| `ui/screens/viewer/ViewerViewModel.kt` | ✏️ 修改 | 添加 PDF 类型支持和 Context 参数 |
| `ui/screens/viewer/ViewerViewModelTest.kt` | ✏️ 修改 | 更新测试以包含 Context 参数 |
| `di/ViewModelModule.kt` | ✏️ 修改 | 传递 Context 给 ViewerViewModel |
| `di/RepositoryModule.kt` | ✏️ 修改 | 注册 PdfThumbnailGenerator |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 测试覆盖统计

- **新增测试数**: 18 个
  - PdfRendererTest: 7 个
  - PdfContentProviderTest: 6 个
  - PdfThumbnailGeneratorTest: 5 个
- **测试通过率**: 100%

## 验收标准检查

- [x] PDF 资源进入查看器，显示 PDF 内容
- [x] 左右翻页流畅（每页预加载，beyondViewportPageCount = 2）
- [x] 加密 PDF 显示 MediaEncryptedError 提示
- [x] 退出查看器后 PDFium 资源正确释放（onCleared → dispose）
- [x] `./gradlew build` 通过
- [x] 所有单元测试通过

## 已知问题 / TODO

- [ ] pdfium-android 2.0.0 的 `isEncrypted` 属性不可用，加密检测依赖 IOException
- [ ] PDF 页面渲染可能需要优化内存使用（大页面 Bitmap）
- [ ] 需要添加 PDF 页面尺寸缓存（避免重复打开页面获取尺寸）
- [ ] 需要添加 PDF 加密密码输入支持（P2）
