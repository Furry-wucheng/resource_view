# 决策日志 · 模板

> 每个 Stage 完成后，在 `doc/decisions/` 中创建 `Mxx-xxx.md`，使用以下模板。

---

```markdown
# Mxx — Stage 名称

> 时间: YYYY-MM-DD | Agent: xxx | 状态: ✅ 已完成 | 前置: Mxx

## 设计决策

### D-001: [决策标题]
- **背景**: 遇到的问题或需要选择的技术方向
- **选择**: 采用方案 A，核心实现思路 + 关键代码片段（如有）
- **备选**: 考虑过但放弃的方案 B，放弃原因
- **影响文件**: `path/to/File.kt:15-30`
- **被依赖**: Mxx, Myy 消费此接口 → 不可随意改动签名

### D-002: [决策标题]
...

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/theme/Color.kt` | 🆕 新增 | 12色标签预设 + 功能色 |
| `ui/navigation/Screen.kt` | ✏️ 修改 | 追加 FileBrowser 路由 |
| ...

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 已知问题 / TODO

- [ ] pdfium-android fork 活跃度待观察（当前使用降级方案）
- [ ] BouncyCastle 在部分 Android ROM 上可能需要额外的 proguard rules
- [ ] ...
```

---

## 记录内容指南

### 什么需要记录？

| 场景 | 示例 | 必须记录 |
|------|------|---------|
| 选择了一个技术方案而非另一个 | Coil vs Glide; Room vs SQLDelight | ✅ |
| 接口签名有 trade-off 考量 | `readRange` 为什么用 offset+length 而非 InputStream | ✅ |
| 绕过了一个已知的库 bug/限制 | BouncyCastle Android 冲突处理 | ✅ |
| 代码组织有非显而易见的理由 | 为什么 FileSourceFactory 放在 shared 而非 data | ✅ |
| 标准 CRUD / 模板代码 | 简单的 Entity 定义、DAO 方法 | ❌ 不需要 |

### 什么不需要记录？

- Entity 字段的简单映射（直接搬运 PRD）
- 标准 Compose 组件组装
- 明显的命名选择
- 装饰性的代码组织调整
