# 决策日志

> 每个 Stage 完成后的设计决策记录。保证开发可溯源、快速定位意图。
> 模板: `TEMPLATE.md`

---

## 索引

| Stage | 名称 | 决策数 | 状态 | Agent | 日期 |
|-------|------|--------|------|-------|------|
| M00 | 依赖配置与版本同步 | — | ⬜ | — | — |
| M01 | 主题系统与设计令牌 | 3 | ✅ | opencode/stellar-orchid | 2026-06-25 |
| M02 | DI 骨架 (Koin) | 2 | ✅ | opencode/clever-wolf | 2026-06-25 |
| M03 | 导航系统 | 3 | ✅ | opencode/clever-wolf | 2026-06-26 |
| M04 | AppShell 响应式布局 | 3 | ✅ | claude | 2026-06-26 |
| M05 | 三个 Tab 占位页面 | — | ⬜ | — | — |
| M06 | Room Entity + Enum + Converters | 3 | ✅ | opencode/stellar-orchid | 2026-06-26 |
| M07 | Room DAO 接口 | 5 | ✅ | claude | 2026-06-26 |
| M08 | AppDatabase 配置 | — | ⬜ | — | — |
| M09 | Domain Models | 3 | ✅ | claude | 2026-06-26 |
| M10 | Repository 层 + SecurePrefs | — | ⬜ | — | — |
| M11 | 全部共享接口契约 | — | ⬜ | — | — |
| M12 | 合规：权限 + 隐私政策 + 数据删除 | 4 | ✅ | claude | 2026-06-26 |
| M13 | 核心功能单元测试 | 3 | ✅ | claude | 2026-06-26 |
| M14 | 基础查看器 | 5 | ✅ | claude | 2026-06-26 |
| M15 | 标签 CRUD | — | ⬜ | — | — |
| M16 | 筛选栏 + 标签交集查询 | — | ⬜ | — | — |
| M17 | SmbFileSource + SMB 源配置 UI | 5 | ✅ | claude | 2026-06-26 |
| M18 | SMB 视频 DataSource | — | ✅ | claude | 2026-06-26 |
| M19 | 视频播放器组件 | 6 | ✅ | claude | 2026-06-26 |
| M20 | Gallery + FlatGrid 策略 | 3 | ✅ | claude | 2026-06-26 |
| M21 | Chapter + ChapterGallery 策略 | — | ⬜ | — | — |
| M22 | PDF 查看器 | 6 | ✅ | claude | 2026-06-26 |
| M23 | 首页网格完整实现 | 4 | ✅ | claude | 2026-06-26 |
| M24 | ResourcePicker + 资源详情弹窗 | 5 | ✅ | claude | 2026-06-25 |
| M25 | 设置页面 | 5 | ✅ | claude | 2026-06-27 |
| M26 | 错误处理全局组件 | — | ⬜ | — | — |
| M27 | 批量添加 + 拆分 UseCase | 4 | ✅ | claude | 2026-06-25 |
| M28 | 打磨完善 | 3 | ✅ | claude | 2026-06-27 |
| M30 | 修复底栏导航 + SMB 线程与权限 | 5 | ✅ | claude | 2026-06-27 |
| batchadd-viewer-thumbnail-video-fix | 批量添加性能优化 + 查看器缩略图补全 + 视频支持 + 模式即时切换 | 8 | ✅ | opencode | 2026-06-29 |
| fix | 文件浏览、混合查看器、缩略图、标签与 SMB 回归修复 | 4 | ✅ | claude | 2026-06-27 |
| cache-refactor | 缓存管理重构 — 独立容量控制 + 预加载优化 + 缩略图复用 | 7 | ✅ | opencode | 2026-06-27 |
