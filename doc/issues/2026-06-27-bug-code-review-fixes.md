# bug: 代码审查问题集中修复

> 日期: 2026-06-27 | 类型: bug | 状态: ✅ 已完成

## 现象

代码审查发现项目中存在多处与核心使用流程相关的问题：

- 本地文件源仍为占位实现，无法真实访问本地资源。
- 本地资源添加依赖手动输入路径，移动端使用体验不符合预期。
- 文件源列表页无法进入实际文件浏览与批量添加流程。
- 查看器页面仅显示占位页，未渲染真实图片/PDF 页面内容。
- SMB 根路径和共享名处理存在重复拼接风险。
- 资源详情保存时组织模式未持久化。
- 资源列表查询存在标签 N+1 查询和来源名称缺失问题。
- 数据库备份流程会关闭 Koin 管理的单例数据库。
- 文档中的 DI 技术栈仍描述为 Hilt，与 Kotlin 2.4 下实际采用 Koin 的项目现状不一致。

## 复现步骤

1. 添加本地或 SMB 文件源。
2. 从来源列表进入资源浏览、选择资源并添加。
3. 打开首页资源详情，修改组织模式后保存。
4. 打开图片/PDF 类资源查看器。
5. 执行数据库备份和资源列表筛选/搜索。

## 期望效果

- 本地文件源可通过系统文件夹选择器授权，并能持久读取授权目录下的资源。
- 来源列表可进入文件浏览页并批量添加资源。
- SMB 路径在 share 内部解析，避免重复 share 前缀。
- 查看器能加载真实页面位图，并把 IO 工作移出主线程。
- 资源详情保存能持久化组织模式。
- 首页资源列表能批量加载标签和来源名称，减少重复查询。
- 数据库备份不破坏应用持有的数据库单例。
- 共享契约和入口文档反映 Koin 作为当前 DI 方案。

## 影响分析

| 维度 | 内容 |
|------|------|
| 根因 | 多个 MVP 阶段留下了占位实现、跨模块契约未贯通，以及部分数据访问逻辑在功能接入后未做性能和生命周期校正 |
| 修改文件 | FileSource、Repository、DAO、Source/Viewer/Home ViewModel 与 Screen、Koin Module、备份管理、SMB 数据源、测试与 DI 文档 |
| 影响 stage | 影响本地/SMB 文件源、资源添加、首页资源列表、查看器、数据库备份和依赖注入契约相关阶段产物 |

## 执行计划

1. 为本地文件源、SMB share 路径、资源详情保存、文件浏览和查看器行为补充单元测试。
2. 实现真实本地文件源和 SAF DocumentTree 文件源，用系统文件夹选择器替代手动路径输入。
3. 接通来源文件浏览页、批量添加资源用例和导航。
4. 修正 SMB share 内部路径解析和根目录文件 stat 行为。
5. 将查看器页面位图加载迁移到 IO dispatcher 并显示真实图像。
6. 优化资源列表 DAO 查询，批量加载来源名称和标签信息。
7. 调整数据库备份为 WAL checkpoint + 文件复制，不关闭单例数据库。
8. 同步 Koin DI 文档和入口指南。

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `app/src/main/java/dev/wucheng/resource_viewer/shared/filesource/LocalFileSource.kt` | 新增 | 真实本地文件系统 FileSource 实现，包含根目录逃逸保护 |
| `app/src/main/java/dev/wucheng/resource_viewer/shared/filesource/DocumentTreeFileSource.kt` | 新增 | 基于 SAF DocumentTree 的本地授权目录 FileSource |
| `app/src/main/java/dev/wucheng/resource_viewer/ui/screens/sources/AddLocalDialog.kt` | 新增 | 本地来源添加弹窗，使用文件夹选择器授权 |
| `app/src/main/java/dev/wucheng/resource_viewer/ui/screens/sources/FileBrowserScreen.kt` | 新增 | 来源文件浏览和资源选择界面 |
| `app/src/main/java/dev/wucheng/resource_viewer/ui/screens/sources/FileBrowserViewModel.kt` | 新增 | 文件浏览、目录导航、资源选择与批量添加状态管理 |
| `app/src/test/java/dev/wucheng/resource_viewer/ui/screens/sources/FileBrowserViewModelTest.kt` | 新增 | 文件浏览 ViewModel 行为测试 |
| `app/src/main/java/dev/wucheng/resource_viewer/shared/filesource/FileSourceFactory.kt` | 修改 | 按本地路径类型创建 LocalFileSource 或 DocumentTreeFileSource |
| `app/src/main/java/dev/wucheng/resource_viewer/data/remote/smb/SmbClientWrapper.kt` | 修改 | 修正根目录文件 stat 父路径处理 |
| `app/src/main/java/dev/wucheng/resource_viewer/shared/filesource/SmbFileSource.kt` | 修改 | share 与 share 内路径拆分 |
| `app/src/main/java/dev/wucheng/resource_viewer/data/remote/smb/SmbDataSource.kt` | 修改 | share 内部路径解析对齐 FileSource |
| `app/src/main/java/dev/wucheng/resource_viewer/data/repository/FilesystemRepository.kt` | 修改 | 注入 Context 并创建支持 SAF 的 FileSource |
| `app/src/main/java/dev/wucheng/resource_viewer/data/repository/ResourceRepository.kt` | 修改 | 批量加载标签和来源名称，支持组织模式持久化 |
| `app/src/main/java/dev/wucheng/resource_viewer/data/local/dao/ResourceDao.kt` | 修改 | 新增带来源名称的资源查询和组织模式更新 SQL |
| `app/src/main/java/dev/wucheng/resource_viewer/data/local/dao/TagDao.kt` | 修改 | 新增资源标签批量查询和标签计数快照 |
| `app/src/main/java/dev/wucheng/resource_viewer/data/local/backup/DatabaseBackupManager.kt` | 修改 | 备份前 checkpoint WAL，不关闭数据库单例 |
| `app/src/main/java/dev/wucheng/resource_viewer/ui/navigation/Screen.kt` | 修改 | 新增文件浏览路由 |
| `app/src/main/java/dev/wucheng/resource_viewer/ui/navigation/AppNavGraph.kt` | 修改 | 来源列表与文件浏览页导航接入 |
| `app/src/main/java/dev/wucheng/resource_viewer/ui/screens/sources/SourceListScreen.kt` | 修改 | 本地来源使用 OpenDocumentTree 文件夹选择器 |
| `app/src/main/java/dev/wucheng/resource_viewer/ui/screens/sources/SourceListViewModel.kt` | 修改 | 本地来源表单和添加逻辑 |
| `app/src/main/java/dev/wucheng/resource_viewer/ui/screens/home/HomeViewModel.kt` | 修改 | 保存资源详情时同步持久化组织模式 |
| `app/src/main/java/dev/wucheng/resource_viewer/ui/screens/viewer/ViewerScreen.kt` | 修改 | 加载并展示真实页面位图 |
| `app/src/main/java/dev/wucheng/resource_viewer/ui/screens/viewer/ViewerViewModel.kt` | 修改 | Provider 构造和页面位图加载迁移到 IO dispatcher，支持 SAF 视频 URI |
| `app/src/main/java/dev/wucheng/resource_viewer/di/RepositoryModule.kt` | 修改 | 注册新增 UseCase 并注入 Context |
| `app/src/main/java/dev/wucheng/resource_viewer/di/ViewModelModule.kt` | 修改 | 注册 FileBrowserViewModel |
| `AGENTS.md` | 修改 | DI 技术栈更新为 Koin |
| `doc/share/03-di-contracts.md` | 修改 | DI 契约更新为 Koin Module |
| `app/src/test/java` 和 `app/src/androidTest/java` 相关测试 | 修改 | 覆盖本地/SMB 文件源、来源添加、文件浏览、查看器、首页保存、备份和仓库行为 |

## 验收

- [x] `.\gradlew.bat test --no-configuration-cache`
- [x] `.\gradlew.bat build --no-configuration-cache`
- [x] `.\gradlew.bat lint --no-configuration-cache`

## 是否需合并回原文档

[x] 是 → 已更新 `AGENTS.md` 和 `doc/share/03-di-contracts.md`，说明当前项目使用 Koin。  
[ ] 否 → 独立提案，不回溯
