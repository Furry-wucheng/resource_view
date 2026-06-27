# perf: SMB 图片查看器首屏与翻页过慢

> 日期: 2026-06-27 | 类型: perf | 状态: ✅ 已完成

## 哪里慢/卡/耗

Android 原生版打开 SMB 图片资源时，首张图片等待明显，翻回已经看过的页面仍可能重新下载。

## 当前表现

- `HorizontalPager(beyondViewportPageCount = 2)` 在首屏同时构建当前页及相邻页，最多并发读取、解码 5 张 SMB 原图。
- `ViewerViewModel.loadPageBitmap()` 每次直接调用 `ContentProvider.loadPage()`，没有页面缓存，也没有合并同页并发请求。
- 相邻页网络请求与当前可见页争抢同一 SMB 连接和带宽；页面离开组合后再次进入会重复读取。
- 单页失败只显示错误文字，没有独立重试入口。

## 期望效果

- 当前可见页始终优先，不被预取请求抢占。
- 当前页成功后再顺序预取前后各 2 页。
- 解码页面使用 200MB LRU；同一页面的并发请求只触发一次底层读取。
- 页面失败可独立重试，失败结果不进入缓存。

## 影响分析

| 维度 | 内容 |
|------|------|
| 瓶颈定位 | Pager 激进并发预构建 + 无页面缓存/请求合并；并非 Kotlin、Compose 或 smbj 天然慢 |
| 修改文件 | `ViewerViewModel.kt`、`ViewerScreen.kt`、新增 `PageLoader.kt` 与单元测试 |
| 影响 stage | 修正 M14 查看器，并实现 parity 提案 5.12、5.13 的核心部分 |

## 执行计划

1. RED：覆盖同页请求合并、LRU 淘汰、失败重试与清空。
2. GREEN：实现通用、协程安全的页面加载器。
3. REFACTOR：Viewer 使用 200MB Bitmap LRU，当前页完成后顺序预取；Pager 禁止并发越界预构建。
4. 增加单页重试 UI，运行 test/build/lint。

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/screens/viewer/PageLoader.kt` | 新增 | 请求合并 + 按字节计量的 LRU |
| `ui/screens/viewer/ViewerViewModel.kt` | 修改 | 当前页优先、200MB 缓存、可取消顺序预取 |
| `ui/screens/viewer/ViewerScreen.kt` | 修改 | 禁止并发越界预构建、单页错误重试 |
| `ui/screens/viewer/PageLoaderTest.kt` | 新增 | 缓存、合并、淘汰与失败恢复测试 |

## 验收结果

- [x] 同页并发请求只执行一次底层加载。
- [x] 失败不缓存，页面可独立重试。
- [x] 当前页完成后才顺序预取相邻页面，旧预取可取消。
- [x] 200MB LRU 按 Bitmap 实际分配字节数计量。
- [x] `./gradlew test lint build` 通过（423 tests，lint 无 error，debug/release build 成功）。

> 真机 SMB 吞吐受 NAS、Wi-Fi 和原图大小影响；本次修复消除了应用侧确定存在的并发争抢与重复下载。建议后续用同一组图片记录首图 TTFP 与缓存命中翻页耗时。
