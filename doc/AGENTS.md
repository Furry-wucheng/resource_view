# Resource Viewer — Android 项目文档

> 本文档目录为 Resource Viewer Android 原生版本的完整文档体系。
> 基于 Flutter 版 PRD 复用，技术文档针对 Android 平台（Kotlin + Jetpack Compose）重写。

## 文档结构

| 目录 | 内容 | 说明 |
|------|------|------|
| `prd/` | 产品需求文档 | 从 Flutter 版复用，产品逻辑与平台无关 |
| `tech/` | 技术设计文档 | 针对 Android 平台重写 |
| `design/` | 页面原型 | 从 Flutter 版复用 HTML 原型，可参考 |
| `share/` | 🔵 只读共享契约 | 跨 Agent 的接口/模型/规范，所有人引用但不可修改 |
| `mvp/` | 🟢 MVP 阶段指令 | 29 个微阶段，多 Agent 并行 worktree 开发 |
| `decisions/` | 🟡 设计决策日志 | 每阶段完成后的设计决策，用于溯源和快速定位 |
| `issues/` | 📥 协作提案 | 新增需求/Bug/性能优化，人-AI 协作模板入口 |

## 核心技术栈

| 维度 | 方案 |
|------|------|
| 语言 | Kotlin 2.4.0 |
| UI 框架 | Jetpack Compose + Material3 |
| 架构 | MVVM + Repository |
| DI | Hilt 2.51 |
| 导航 | Navigation Compose |
| 数据库 | Room 2.6.1 |
| 视频播放 | Media3 ExoPlayer 1.5.x |
| 图片加载 | Coil 3.5.0 |
| SMB | smbj 0.13.0 |
| PDF | Android PdfRenderer + pdfium-android 1.0.0 |
| 压缩包 | Apache Commons Compress 1.28.0 / zip4j 2.11.6 |

## 入口文档

所有 Agent 在开始任何工作前，必须阅读**根目录 `AGENTS.md`**。

## Agent 阅读规则

进入 `doc/` 下任何子目录前，必须先阅读该目录的 `AGENTS.md`，确认目录用途和文件索引后，再进入具体文档。
