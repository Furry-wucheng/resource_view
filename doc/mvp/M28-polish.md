# M28 — 打磨完善

> 轨道 8 · Stage 28/29 | 前置: M25 | 依赖共享: 全部 share | 🟡 聚合(涉及多个文件修改)

## 执行目标

最终的打磨工作：ProGuard/R8 规则、深色模式完善、边界处理、性能优化收尾。

## 子任务

### M28.1 ProGuard / R8 规则

添加 keep 规则：
- Room Entity（防止反射混淆）
- Hilt Module（防止 DI 断裂）
- smbj / BouncyCastle（Android 兼容性）
- pdfium-android native .so
- Coil / Media3（防止类移除）
- Compose runtime（防止反射调用失败）

**产出物**：`app/proguard-rules.pro`（或 `consumer-rules.pro`）

### M28.2 深色模式完善

逐一检查所有 Screen 在深色模式下的显示：
- 查看器背景始终黑色（已保证）
- 文件浏览器卡片/列表项颜色
- 标签 Chip 颜色对比度
- 设置页开关/选择器

### M28.3 边界处理与打磨

- 新建源后资源数量实时更新
- 源不可达 → 资源置灰
- 大目录导航性能（>1000 文件）
- 空文件夹/损坏文件处理
- 返回手势适配
- 键盘快捷键（桌面模式）

### M28.4 最终检查

- [ ] `./gradlew build` 通过
- [ ] `./gradlew lint` 无 error
- [ ] `./gradlew test` 全部通过
- [ ] `./gradlew connectedAndroidTest` 核心流程通过
- [ ] 应用在 Android 13/14/15 真机可运行
