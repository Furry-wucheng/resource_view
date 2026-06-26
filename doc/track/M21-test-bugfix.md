# M21 — 测试 + Bug 修复

## 概述

补充集成测试，修复遗留问题。

## 完成内容

### Bug 修复

#### 1. ImageFolderProvider.decodeBitmap 流重置 Bug

**问题描述：**
`ImageFolderProvider.decodeBitmap()` 方法在第一次调用 `BitmapFactory.decodeStream()`（`inJustDecodeBounds=true`）后，调用 `inputStream.reset()` 尝试重置流位置。但存在两个问题：
1. 流从未调用过 `mark()`，`reset()` 会失败
2. SMB 文件源（smbj）返回的 InputStream 不支持 `mark()/reset()`

**修复方案：**
将实现改为先将整个 InputStream 读入 `ByteArray`，然后使用 `BitmapFactory.decodeByteArray()` 进行两次解码（bounds + actual）。`ByteArray` 天然支持随机访问，无需 `mark()/reset()`。

**修改文件：**
- `app/src/main/java/dev/wucheng/resource_viewer/shared/content/ImageFolderProvider.kt`

### 新增测试

#### 2. TagViewModelTest（20 个测试）

新增 `TagViewModel` 单元测试，覆盖：
- 编辑器状态管理：`showCreateDialog`、`showEditDialog`、`dismissEditor`、`updateEditorName`、`updateEditorColor`
- 表单校验：空名称、纯空格名称、内置名称"收藏"、超过 20 字符、重复名称、未选颜色
- 创建模式：成功创建、插入失败
- 编辑模式：成功更新、同名保存、更新失败
- 删除操作：内置标签保护、成功删除、删除失败
- 错误清除：`clearError`

**新增文件：**
- `app/src/test/java/dev/wucheng/resource_viewer/ui/screens/tags/TagViewModelTest.kt`

#### 3. ThumbnailRepositoryTest（11 个测试）

新增 `ThumbnailRepository` 单元测试，覆盖：
- `generateThumbnail`：匹配 VIDEO 生成器、匹配 PDF 生成器、无匹配生成器返回 null、生成器返回 null、生成器抛异常返回 MediaLoadError
- `hasGenerator`：VIDEO/PDF/FOLDER/ARCHIVE 类型检查、空生成器集合

**新增文件：**
- `app/src/test/java/dev/wucheng/resource_viewer/data/repository/ThumbnailRepositoryTest.kt`

#### 4. ViewerViewModelTest 补充（4 个测试）

为 `ViewerViewModel` 补充边界条件测试：
- 视频资源 source 为 null 时返回错误
- 视频资源 source 加载失败时返回错误
- `goToPage` 传入负数时保持当前页
- `goToPage` 传入超出范围值时保持当前页
- `getFileSource` 失败时返回错误

**修改文件：**
- `app/src/test/java/dev/wucheng/resource_viewer/ui/screens/viewer/ViewerViewModelTest.kt`

#### 5. DetectOrganizationModeUseCaseTest 补充（2 个测试）

补充边界条件测试：
- 子文件夹存在但不含图片 → FLATGRID
- 仅含非图片媒体文件（PDF、视频） → FLATGRID

**修改文件：**
- `app/src/test/java/dev/wucheng/resource_viewer/domain/usecase/DetectOrganizationModeUseCaseTest.kt`

## 测试结果

所有单元测试通过（BUILD SUCCESSFUL）。
