# 00 — 术语定义

> 所有 Agent 必须使用统一术语，避免跨 stage 沟通歧义。

---

## 核心概念

| 术语 | 定义 |
|------|------|
| **数据源 (Source)** | 用户添加的文件来源，如本地文件夹或 SMB 网络共享 |
| **资源 (Resource)** | 一个可浏览/阅读的内容单元，通常为一个文件夹或单个 PDF/压缩包/视频文件 |
| **标签 (Tag)** | 内置标签（"收藏"）或自定义标签，可绑定到任意 Resource |
| **组织模式 (OrganizationMode)** | Resource 内部文件的浏览方式：CHAPTER / CHAPTER_GALLERY / FLATGRID / GALLERY |
| **章节 (Chapter)** | 子文件夹，CONTAINER 类型资源的一级子目录 |
| **ResourcePicker** | 可复用树形扫描选择弹窗组件 |

## 枚举常量

### SourceType
```
LOCAL   — 本地文件系统 (java.io.File)
SMB     — SMB 网络共享 (smbj)
FTP     — FTP 协议（预留）
WEBDAV  — WebDAV 协议（预留）
```

### ResourceType
```
FOLDER  — 文件夹容器（可含子文件夹或图片）
PDF     — 单个 PDF 文件
ARCHIVE — 压缩包（ZIP/RAR）
VIDEO   — 视频文件
```

### OrganizationMode
```
CHAPTER         — 章节模式：子文件夹 → 章节列表 → 选章进入阅读
CHAPTER_GALLERY — 章节画廊：根层章节 + 章内递归扁平阅读（无"下一章"提示）
FLATGRID        — 平铺网格：一层图片直接网格浏览
GALLERY         — 画廊模式：与平铺同入口，但用画廊风格呈现
null            — 未判定，由 DetectOrganizationModeUseCase 异步填充
```

## 技术术语

| 术语 | 含义 |
|------|------|
| **worktree** | Git worktree，一个独立的工作目录，不同 agent 在不同 worktree 并行开发 |
| **🟢 独占文件** | 只有一个 owning stage 拥有写入权 |
| **🟡 聚合文件** | 骨架由某个 stage 创建，后续 stage 在末尾追加内容 |
| **🔵 只读共享** | `doc/share/` 中的文档，所有 agent 引用但绝不在开发 stage 中修改 |
| **轨道 (Track)** | 一组串行依赖的 stage 序列，不同轨道可并行 |
