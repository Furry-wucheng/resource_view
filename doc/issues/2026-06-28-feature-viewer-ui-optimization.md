# feature: 查看器 UI 全面优化

> 日期: 2026-06-28 | 类型: feature | 状态: ✅ 已完成

## 描述

对查看器（ViewerScreen）进行全面的 UI/UX 优化，包括：
1. 视频播放器重构：移除 ExoPlayer 自带蒙版、自定义控制栏、长按倍速上下滑动调整
2. 工具栏统一风格：半透明黑底+圆角、SegmentedButton 替代 DropdownMenu
3. 图片查看支持捏合缩放和边缘滑动翻页（引入 zoomable 库）
4. 单击切换顶栏/底栏显隐（图片和视频模式均支持）
5. 排序按钮改为 DropdownMenu

## 验收标准

- [x] 视频播放时单击显示/隐藏工具栏，无蒙版遮挡
- [x] 视频长按倍速时显示当前倍速提示，上下滑动调整（1x~3x，粒度 0.25）
- [x] 视频底部 25% 区域水平拖动控制进度，显示目标时间浮窗
- [x] 视频控制栏只保留播放/暂停按钮，无多余按钮
- [x] 顶栏/底栏统一风格：半透明黑底（alpha=0.65）+ 16dp 圆角
- [x] 翻页方向/双页模式使用 SegmentedButton，文字改为中文
- [x] 图片支持双指捏合缩放（1x~5x），缩放跟手
- [x] 图片缩放后边缘滑动可切换到上一张/下一张
- [x] 图片单击切换顶栏/底栏显隐
- [x] 视频播放时隐藏 SlideBar
- [x] 图片模式工具栏隐藏时 SlideBar 也隐藏
- [x] HomeScreen 排序按钮改为 DropdownMenu
- [x] build/test passed

## 影响分析

| 维度 | 内容 |
|------|------|
| 修改文件 | ViewerToolbar.kt, SlideBar.kt, VideoPlayer.kt, ViewerScreen.kt, HomeScreen.kt |
| 影响 stage | M14 (basic-viewer), M19 (video-player) |
| 聚合文件 | libs.versions.toml（新增 zoomable 依赖） |
| 新增依赖 | net.engawapg.lib:zoomable:2.1.0 |

## 执行计划

1. ViewerToolbar：移除收藏按钮、增加高度、改 DropdownMenu → SegmentedButton、改背景
2. SlideBar：增加高度 → 改用 Material3 Slider、统一风格
3. VideoPlayer：移除 VideoSeekGestureArea、禁用 ExoPlayer 自带控制栏、自定义播放/暂停+进度条、长按倍速+上下滑动调整
4. ViewerScreen：视频播放时隐藏 SlideBar、图片模式单击切换工具栏、传递 toolbarVisible
5. HomeScreen：排序按钮改 DropdownMenu
6. 引入 zoomable 库替换自定义手势代码

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| ViewerToolbar.kt | 重写 | 统一 UI 风格、SegmentedButton、设置齿轮图标 |
| SlideBar.kt | 重写 | Material3 Slider、统一 UI 风格 |
| VideoPlayer.kt | 重写 | 禁用 ExoPlayer 控制栏、自定义 UI、长按倍速+滑动调整 |
| ViewerScreen.kt | 修改 | 传递 toolbarVisible、图片 zoomable、SlideBar 显隐逻辑 |
| HomeScreen.kt | 修改 | 排序按钮改 DropdownMenu |
| custom_player_view.xml | 新增 | 自定义 PlayerView 布局 |
| libs.versions.toml | 追加 | zoomable 依赖 |
| build.gradle.kts | 追加 | zoomable 依赖 |
