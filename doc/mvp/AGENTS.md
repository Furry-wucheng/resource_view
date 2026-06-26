# MVP 阶段规划 — 多 Agent 并行

> Android 版 Resource Viewer · 共 29 个微阶段 · 9 条并行轨道
> 工作流: `doc/share/10-worktree-guide.md`

---

## 依赖关系图

```
轨道 0 (Foundation, 串行):
  M00 → M01 → M02 → M03 → M04 → M05

轨道 1 (Data Layer, 串行, 依赖 M02):
  M06 → M07 → M08 → M09 → M10

轨道 2 (Shared, 依赖 M09):
  M11

轨道 3 (Local Source, 串行, 依赖 M10+M11):
  M12 → M13 → M14

轨道 4 (Tag System, 并行于 轨道3, 依赖 M10):
  M15 → M16

轨道 5 (SMB, 串行, 依赖 M11+M12):
  M17 → M18

轨道 6 (Viewer Ext, 依赖 M11+M14):
  M19 ─┐
  M20 ─┤ 可并行
  M21 ─┤
  M22 ─┘

轨道 7 (Home+Components, 依赖 M10+M15+M16):
  M23 → M24 → M25

轨道 8 (Polish, 依赖多轨道):
  M26 → M27 → M28
```

## 进度追踪

| Stage | 名称 | 轨道 | 前置 | 状态 | Agent |
|-------|------|------|------|------|-------|
| M00 | 依赖配置与版本同步 | 0 | — | ✅ 已完成 | — |
| M01 | 主题系统与设计令牌 | 0 | M00 | ✅ 已完成 | opencode/stellar-orchid |
| M02 | DI 骨架 (Koin) | 0 | M00 | ✅ 已完成 | opencode/clever-wolf |
| M03 | 导航系统 | 0 | M02 | ✅ 已完成 | opencode/clever-wolf |
| M04 | AppShell 响应式布局 | 0 | M03 | ✅ 已完成 | claude |
| M05 | 三个 Tab 占位页面 | 0 | M03,M01 | ✅ 已完成 | opencode |
| M06 | Room Entity + Enum + Converters | 1 | M02 | ✅ 已完成 | opencode/stellar-orchid |
| M07 | Room DAO 接口 | 1 | M06 | ✅ 已完成 | claude |
| M08 | AppDatabase 配置+播种+迁移骨架 | 1 | M07 | ✅ 已完成 | claude |
| M09 | Domain Models | 1 | M06 | ✅ 已完成 | claude |
| M10 | Repository 层 + SecurePrefs | 1 | M08,M09 | ✅ 已完成 | claude |
| M11 | 全部共享接口契约 | 2 | M09 | ✅ 已完成 | claude |
| M12 | LocalFileSource 实现 | 3 | M10,M11 | ✅ 已完成 | claude |
| M13 | 核心功能单元测试 | 3 | M12 | ✅ 已完成 | claude |
| M14 | 基础查看器 (ImageFolderProvider + HorizontalPager) | 3 | M13 | ✅ 已完成 | claude |
| M15 | 标签 CRUD (TagManager + TagEditor + ViewModel) | 4 | M10 | ✅ 已完成 | — |
| M16 | 筛选栏 + 标签交集查询 | 4 | M15 | ✅ 已完成 | — |
| M17 | SmbFileSource + SMB 源配置 UI | 5 | M11,M12 | ✅ 已完成 | claude |
| M18 | SMB 视频 DataSource | 5 | M17 | ⬜ 待开始 | — |
| M19 | 视频播放器组件 (Media3 + VideoPlayer) | 6 | M11,M14 | ⬜ 待开始 | — |
| M20 | Gallery + FlatGrid 策略 | 6 | M11,M14 | ⬜ 待开始 | — |
| M21 | Chapter + ChapterGallery 策略 + ChapterListScreen | 6 | M11,M20 | ⬜ 待开始 | — |
| M22 | PDF 查看器 (PdfRenderer + PdfContentProvider) | 6 | M11,M14 | ⬜ 待开始 | — |
| M23 | 首页网格完整实现 (HomeScreen + ViewModel + 缩略图 LRU) | 7 | M10,M16 | ⬜ 待开始 | — |
| M24 | ResourcePicker + 资源详情弹窗 | 7 | M23 | ⬜ 待开始 | — |
| M25 | 设置页面 (SettingsScreen + AppConfig + 缓存管理) | 7 | M23 | ⬜ 待开始 | — |
| M26 | 错误处理全局组件 (ErrorView + BaseViewModel) | 8 | M10 | ✅ 已完成 | claude |
| M27 | 批量添加 + 拆分 UseCase | 8 | M10,M24 | ⬜ 待开始 | — |
| M28 | ProGuard + 深色模式完善 + 边界打磨 | 8 | M25 | ⬜ 待开始 | — |

> 状态图例: ⬜ 待开始 · 🔵 进行中 · ✅ 已完成 · ❌ 阻塞

---

## 并行窗口

### 瓶颈点：谁能同时开工？

```
         M00 (依赖配置)
        /            \
      M01 (主题)    M02 (DI骨架)        ← 窗口1: M01 ∥ M02
       |              |    \
       |             M03  M06 (Entity)   ← 窗口2: M03 ∥ M06
       |            /  \   |    \
      M05        M04 M05 M07  M09        ← 窗口3: M04∥M05∥M07∥M09 (4路)
       |                        |    \
       |                       M08   M11  ← 窗口4: M08 ∥ M11
       |                         \   /
       +---→ ... → ... → ... →  M10     ← 汇合点：M08+M09 都完成后 M10
                                     |
          ┌──────────┬───────────────┼───────────────┐
         M12        M15            M26             ...
       (LocalFS)  (标签CRUD)  (错误组件)            ...
          |          |                              ...
         M13        M16                            ...
       (浏览器)   (筛选栏)                          ...
          |
         M14
       (查看器)
          |
    ┌─────┼─────┬─────┐
   M17   M19   M20   M22      ← 窗口6: M17∥M19∥M20∥M22
  (SMB) (视频)(Gallery)(PDF)
```

### 逐窗口解释

| 窗口 | 触发条件 | 可并行的 stage | 最多 agent 数 |
|------|---------|---------------|-------------|
| **1** | M00 完成 | M01, M02 | 2 |
| **2** | M02 完成 | M03, M06 | 2 |
| **3** | M06 + M03 完成 | M04, M05, M07, M09 | 4 |
| **4** | M09 完成 | M08, M11 | 2 |
| **5** | M10 + M11 完成 | M12, M15, M26 | 3 |
| **6** | M14 完成 | M17, M19, M20, M22 | 4 |
| **7** | M16 完成 | M23 | 1 (但 M25 可搭 M23 末班) |
| **8** | M23 完成 | M24, M25 | 2 |
| **9** | M25 完成 | M28 | 1 |
| **10** | M24 完成 | M27 | 1 |

### 最大并行度

窗口 3 和窗口 6 期间可同时 4 个 worktree 并行开发。整体流水线：

```
Agent A: M00→M01→M03→M04→M05 → M12→M13→M14 → M19 → ...
Agent B:          M02→M06→M07→M08 → M10 → M15→M16 → M23→M24 → ...
Agent C:                      M09→M11 → M17→M18 → M20→M21 → M22 → ...
Agent D:                                     M26 → M25 → M27 → M28
```

> 以上为理想分配。实际视团队人数调整，单人也可串行完成全部 29 个 stage。
