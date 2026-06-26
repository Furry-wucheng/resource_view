# M20 — Gallery + FlatGrid 策略

> 轨道 6 · Stage 20/29 | 前置: M11,M14 | 依赖共享: `doc/share/02-interfaces.md` §3 | 🟢 独占

## 执行目标

实现两种简单组织模式：Gallery 和 FlatGrid。

## 共享契约引用

- `doc/share/02-interfaces.md` §3 — OrganizationStrategy 接口
- `doc/share/01-data-models.md` — FileEntry/Chapter 定义
- `@prd/07-资源组织结构.md` — 平铺网格/画廊交互

## 子任务

### M20.1 FlatGridStrategy

- `getChapters`: 返回空（无章节）
- `getContents`: 列出文件夹内所有图片文件
- `createProvider`: 创建 ImageFolderProvider

**产出物**：`shared/organization/FlatGridStrategy.kt`

### M20.2 GalleryStrategy

- 与 FlatGrid 相同的文件获取逻辑
- `createProvider` 使用不同的呈现方式（画廊风格布局，后续 UI 层区分）

**产出物**：`shared/organization/GalleryStrategy.kt`

### M20.3 DetectOrganizationModeUseCase

实现自动判定逻辑：
- 文件夹下仅图片 → FLATGRID
- 文件夹下有子文件夹（含图片） → CHAPTER
- 文件夹下有图片 + PDF + 视频混合 → FLATGRID

**产出物**：`domain/usecase/DetectOrganizationModeUseCase.kt`

## 验收标准

- [x] FlatGrid 模式资源进入查看器显示网格布局
- [x] 仅图片的文件夹自动判定为 FLATGRID
- [x] `./gradlew build` 通过
