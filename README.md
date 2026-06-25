# Resource Viewer — Android

一个 Android 原生的资源管理与查看应用，用于管理和浏览本地/SMB 网络上的图片、PDF、视频等资源。

## 功能特性

- 📁 **资源库首页**：自适应缩略图网格，支持标签筛选和搜索
- 🏷️ **标签系统**：自定义标签 + 收藏标记，交集筛选
- 📂 **数据源管理**：本地文件夹 + SMB 网络共享
- 🖼️ **统一查看器**：图片/PDF/视频混合浏览，左右翻页
- 📊 **组织模式**：章节 / 章节画廊 / 平铺网格 / 画廊四种模式
- ⚙️ **设置管理**：缓存管理、主题切换、查看器配置

## 技术栈

| 维度 | 方案 |
|------|------|
| 语言 | Kotlin 2.x |
| UI | Jetpack Compose + Material3 |
| 架构 | MVVM + Repository + Hilt DI |
| 数据库 | Room |
| 视频 | Media3 ExoPlayer |
| 图片 | Coil |
| SMB | smbj |
| PDF | pdfium-android |

## 文档

详见 `doc/` 目录：

- [产品需求文档](doc/prd/) — 完整的产品功能定义
- [技术设计文档](doc/tech/) — Android 平台技术方案
- [页面原型](doc/design/) — HTML 交互原型
- [MVP 规划](doc/mvp/) — 分阶段开发计划

## 快速开始

```bash
# 克隆项目
git clone <repo-url>

# 构建
./gradlew build

# 运行测试
./gradlew test

# 安装到设备
./gradlew installDebug
```

## 最低要求

- Android 8.0 (API 26)
- Kotlin 2.x
- AGP 9.x
