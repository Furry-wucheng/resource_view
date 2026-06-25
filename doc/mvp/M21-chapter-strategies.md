# M21 — Chapter + ChapterGallery 策略 + ChapterListScreen

> 轨道 6 · Stage 21/29 | 前置: M11,M20 | 依赖共享: `doc/share/02-interfaces.md` §3 | 🟢 独占

## 执行目标

实现章节相关组织模式 + 章节列表页面。

## 共享契约引用

- `doc/share/02-interfaces.md` §3 — OrganizationStrategy + Chapter
- `@prd/07-资源组织结构.md` — 章节模式交互
- `@design/chapter-list.html` — 原型参考

## 子任务

### M21.1 ChapterStrategy

- `getChapters`: 列出子文件夹作为章节
- `getContents`: 返回空（由章节控制）
- `createProvider`: 为特定章节创建 ImageFolderProvider

**产出物**：`shared/organization/ChapterStrategy.kt`

### M21.2 ChapterGalleryStrategy

- `getChapters`: 根层子文件夹作为章节
- `getContents`: 章内递归扁平所有图片（跨子文件夹连续阅读）
- `createProvider`: 为章节 Gallery 创建内容提供者

**产出物**：`shared/organization/ChapterGalleryStrategy.kt`

### M21.3 ChapterListScreen

章节列表页面：
- 点击 CHAPTER/CHAPTER_GALLERY 模式资源 → 进入章节列表
- 每项显示章节名 + 封面缩略图 + 图片数量
- 点击章节 → 进入 ViewerScreen

**产出物**：`ui/screens/viewer/ChapterListScreen.kt`

### M21.4 更新 DetectOrganizationModeUseCase

补充 CHAPTER 和 CHAPTER_GALLERY 的自动判定逻辑。

## 验收标准

- [ ] 含子文件夹的资源自动判定为 CHAPTER 模式
- [ ] 章节列表显示每个章节的封面和图片数
- [ ] 选章后进入查看器，只浏览该章内容
- [ ] CHAPTER_GALLERY 模式下章内图片连续翻页
- [ ] `./gradlew build` 通过
