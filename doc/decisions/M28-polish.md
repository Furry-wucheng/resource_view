# M28 — 打磨完善

> 时间: 2026-06-27 | Agent: claude | 状态: ✅ 已完成 | 前置: M25

## 设计决策

### D-001: ProGuard 规则策略
- **背景**: Release 构建需要启用 R8 混淆以减小 APK 体积，但多个依赖库需要 keep 规则
- **选择**: 集中式 `proguard-rules.pro`，按依赖库分段注释，覆盖 Room、Koin、smbj、BouncyCastle、pdfium、Coil、Media3、Compose 等
- **备选**: 分散式 consumer-rules，每个模块独立配置。放弃原因：当前单模块架构，集中管理更清晰
- **影响文件**: `app/proguard-rules.pro:1-230`

### D-002: mbassy javax.el 兼容处理
- **背景**: smbj 依赖 mbassy 事件总线，mbassy 可选依赖 javax.el（Expression Language），Android 环境不存在
- **选择**: 使用 `-dontwarn javax.el.**` 抑制警告，mbassy 的 EL 功能在 Android 上不会被调用
- **备选**: 添加 javax.el stub 库。放弃原因：增加无用依赖，dontwarn 更简洁
- **影响文件**: `app/proguard-rules.pro:75-76`

### D-003: 深色模式验证策略
- **背景**: 需要确保所有主题颜色在深色/浅色模式下都有足够对比度
- **选择**: 单元测试验证 WCAG 2.0 对比度标准（4.5:1），而非 UI 截图对比
- **备选**: Screenshot 测试。放弃原因：配置复杂，单元测试覆盖核心指标更高效
- **影响文件**: `app/src/test/.../DarkModeTest.kt`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `app/proguard-rules.pro` | 🆕 新增 | ProGuard/R8 混淆规则（230 行） |
| `app/build.gradle.kts` | ✏️ 修改 | 启用 R8 minify 和 shrinkResources |
| `app/src/test/.../ProGuardRulesTest.kt` | 🆕 新增 | ProGuard 规则存在性验证 |
| `app/src/test/.../DarkModeTest.kt` | 🆕 新增 | 深色模式对比度测试 |
| `app/src/test/.../EdgeCaseTest.kt` | 🆕 新增 | 边界情况处理测试 |
| `doc/mvp/AGENTS.md` | ✏️ 修改 | M28 状态更新为 ✅ |

## 已知问题 / TODO

- [ ] R8 混淆后需真机验证完整功能流程
- [ ] 深色模式当前仅验证颜色定义，未覆盖运行时 UI 截图
