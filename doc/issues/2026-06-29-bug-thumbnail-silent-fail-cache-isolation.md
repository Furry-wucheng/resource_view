# bug: 缩略图生成静默失败 + 缓存隔离 + 清缓存不复生

> 日期: 2026-06-29 | 类型: bug | 状态: ✅ 已完成

## 现象

1. 添加资源后网络请求量很大，但首页没有一张封面缩略图
2. 清除缓存后，封面缩略图不再重新生成
3. 资源库点进去的四种模式缩略图缓存与文件浏览器缩略图缓存完全不互通
4. 缩略图生成到一半切页面，回来后不再重试

## 复现步骤

1. 在 SMB 源下添加一批文件夹（含图片/视频/PDF）
2. 回到首页 → 封面显示 fallback 色块，无缩略图
3. 打开资源点进去浏览 → ContentGrid 重新从网络加载缩略图（封面已生成的不复用）
4. 切到 ChapterList 模式 → 又走一遍网络重新加载
5. 生成缩略图中切换到其他页面 → 切回来缩略图位置永远空白

## 期望效果

1. 封面缩略图正常生成，失败原因可见（Logcat）
2. 清除缓存后自动重新生成封面
3. ContentGrid / ChapterList / FileBrowser 共享同一套缩略图缓存，一处加载处处命中
4. 切页面中断的加载，回来后可以重试

## 影响分析

| 维度 | 内容 |
|------|------|
| 根因 | ① 6 层异常静默吞没（FileEntryThumbnailLoader → Generator → BatchAdd → HomeViewModel），失败完全不可见；② ImageThumbnailGenerator/PdfThumbnailGenerator 的 saveBitmap() 在 diskCache.put() 前 recycle bitmap 导致 "Can't compress a recycled bitmap"；③ 三个 ViewModel 各自创建 ThumbnailLoadManager 实例，各自有独立内存 LRU；④ 磁盘缓存 key 使用 resourceId（而非 sourceId），FileBrowser vs ContentGrid 永远不命中；⑤ ChapterList 使用 size=0 的假 FileEntry，与 ContentGrid 永远不命中；⑥ CancellationException 被吞入 misses 集合，切页面后不再重试；⑦ clearing cache 只检查 thumbnailPath DB 字段是否为空，不检查文件是否存在 |
| 修改文件 | ThumbnailLoadManager, ThumbnailTaskPool, FileBrowserThumbnailDiskCache, 三个 Generator, FileEntryThumbnailLoader, BatchAddResourcesUseCase, HomeViewModel, ThumbnailRepository, CoilModule, ViewModelModule, 三个 ViewModel + 测试 |
| 影响 stage | M19, M20, M22, M23, M25, M27 |

## 执行计划

**P0 — 日志补全：**
1. FileEntryThumbnailLoader.kt + Log.e
2. 三个 Generator 的 catch + Log.e
3. BatchAddResourcesUseCase / HomeViewModel null 分支 + Log.w
4. ThumbnailRepository 无生成器 + Log.w

**P1 — 清缓存重生 + bitmap recycle 修复：**
5. HomeViewModel filter 加文件存在检查
6. ImageThumbnailGenerator/PdfThumbnailGenerator saveBitmap 调序（put 在 save 前）
7. FileBrowserThumbnailDiskCache key 去掉 modifiedAt

**P2 — 缓存统一：**
8. ThumbnailLoadManager 重构为 Koin single，remove sourceId 构造参数，加 load(sourceId, entry, policy)
9. 内存 LRU key 改为 "sourceId:relativePath"
10. CoilModule 注册 ThumbnailLoadManager 单例
11. 三个 ViewModel 注入共享实例，ContentGrid/ChapterList 传 resource.sourceId
12. ChapterList 区分 CHAPTER (DIRECT_CHILD) vs CHAPTER_GALLERY (RESOURCE_COVER) policy
13. ChapterList loadChapterCover 改用 fileSource.stat() 获取真 FileEntry
14. CancellationException 不加入 misses 集合

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `shared/thumbnail/FileEntryThumbnailLoader.kt` | ✏️ | +Log.e, +TAG |
| `shared/thumbnail/ImageThumbnailGenerator.kt` | ✏️ | +Log.e, saveBitmap 调序, BFS 复用, +TAG |
| `shared/thumbnail/PdfThumbnailGenerator.kt` | ✏️ | +Log.e, saveBitmap 调序, +TAG |
| `shared/thumbnail/VideoThumbnailGenerator.kt` | ✏️ | e.printStackTrace→Log.e, +TAG |
| `shared/thumbnail/FileBrowserThumbnailDiskCache.kt` | ✏️ | key 移除 modifiedAt |
| `shared/thumbnail/ThumbnailLoadManager.kt` | ✏️ | Koin single, load(sourceId,entry,policy), CancellationException 不吞 |
| `domain/usecase/BatchAddResourcesUseCase.kt` | ✏️ | null 分支 +Log.w |
| `data/repository/ThumbnailRepository.kt` | ✏️ | 无生成器 +Log.w, +TAG |
| `ui/screens/home/HomeViewModel.kt` | ✏️ | filter 加文件存在检查, null 分支 +Log.w |
| `di/CoilModule.kt` | ✏️ | +ThumbnailLoadManager 单例 |
| `di/ViewModelModule.kt` | ✏️ | 三个 VM 注入共享实例 |
| `ui/screens/viewer/ContentGridViewModel.kt` | ✏️ | 共享 LoadManager, resource.sourceId |
| `ui/screens/viewer/ChapterListViewModel.kt` | ✏️ | 共享 LoadManager, CHAPTER/CHAPTER_GALLERY policy, 真 FileEntry |
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ | 共享 LoadManager |
| `ui/screens/sources/FileBrowserViewModelTest.kt` | ✏️ | +mock ThumbnailLoadManager |
