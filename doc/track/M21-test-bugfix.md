# M21 — Chapter + ChapterGallery 策略 + ChapterListScreen

## 概述

实现章节相关组织模式 + 章节列表页面。

## 完成内容

### M21.1 ChapterStrategy

实现章节组织策略：
- `getChapters`: 列出子文件夹作为章节，统计图片数量和封面路径
- `getContents`: 返回空（由章节控制）
- `createProvider`: 为特定章节创建 ImageFolderProvider

**产出物：**
- `shared/organization/ChapterStrategy.kt`
- `shared/organization/ChapterStrategyTest.kt`（8 个测试）

### M21.2 ChapterGalleryStrategy

实现章节画廊组织策略：
- `getChapters`: 根层子文件夹作为章节，递归统计图片数量
- `getContents`: 章内递归扁平所有图片（跨子文件夹连续阅读）
- `createProvider`: 为章节 Gallery 创建 ImageFolderProvider

**产出物：**
- `shared/organization/ChapterGalleryStrategy.kt`
- `shared/organization/ChapterGalleryStrategyTest.kt`（8 个测试）

### M21.3 ChapterListScreen

实现章节列表页面：
- 点击 CHAPTER/CHAPTER_GALLERY 模式资源 → 进入章节列表
- 每项显示章节名 + 封面缩略图占位符 + 图片数量
- 点击章节 → 进入 ViewerScreen

**产出物：**
- `ui/screens/viewer/ChapterListScreen.kt`
- `ui/screens/viewer/ChapterListViewModel.kt`

**修改文件：**
- `ui/navigation/Screen.kt` — 新增 ChapterList 路由
- `ui/navigation/AppNavGraph.kt` — 新增 ChapterListScreen composable
- `di/ViewModelModule.kt` — 注册 ChapterListViewModel

### M21.4 更新 DetectOrganizationModeUseCase

补充 CHAPTER 和 CHAPTER_GALLERY 的自动判定逻辑：
- 含子文件夹的资源自动判定为 CHAPTER 模式
- 子文件夹含子子文件夹时判定为 CHAPTER_GALLERY 模式

**修改文件：**
- `domain/usecase/DetectOrganizationModeUseCase.kt`
- `domain/usecase/DetectOrganizationModeUseCaseTest.kt`（新增 2 个测试）

### Bug 修复（附带）

#### ImageFolderProvider.decodeBitmap 流重置 Bug

**问题描述：**
`ImageFolderProvider.decodeBitmap()` 方法在第一次调用 `BitmapFactory.decodeStream()`（`inJustDecodeBounds=true`）后，调用 `inputStream.reset()` 尝试重置流位置。但存在两个问题：
1. 流从未调用过 `mark()`，`reset()` 会失败
2. SMB 文件源（smbj）返回的 InputStream 不支持 `mark()/reset()`

**修复方案：**
将实现改为先将整个 InputStream 读入 `ByteArray`，然后使用 `BitmapFactory.decodeByteArray()` 进行两次解码。

**修改文件：**
- `shared/content/ImageFolderProvider.kt`

### 新增测试（附带）

- `TagViewModelTest`（20 个测试）
- `ThumbnailRepositoryTest`（11 个测试）
- `ViewerViewModelTest` 补充（4 个测试）

## 测试结果

- `./gradlew build` — BUILD SUCCESSFUL
- `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL
- 所有单元测试通过
