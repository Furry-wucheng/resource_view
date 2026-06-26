# M12 — 合规：权限 + 隐私政策 + 数据删除

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M10

## 设计决策

### D-001: 权限请求时机与方式

- **背景**: Android 13+ 将存储权限拆分为细粒度的媒体权限，需要在运行时请求
- **选择**: 在 MainActivity 中使用 ActivityResultContracts.RequestMultiplePermissions，首次启动时请求
- **备选**: 在需要时动态请求，放弃原因：用户体验差，频繁弹窗
- **影响文件**: `MainActivity.kt:40-80`

### D-002: 隐私政策存储方式

- **背景**: 需要记录用户是否已同意隐私政策
- **选择**: 在 AppConfigEntity 中添加 hasAcceptedPrivacy 字段，使用 Room 存储
- **备选**: 使用 SharedPreferences，放弃原因：与应用配置统一管理，便于数据清除
- **影响文件**: `data/local/entity/AppConfigEntity.kt`, `data/local/dao/AppConfigDao.kt`

### D-003: 数据清除策略

- **背景**: 用户需要能够清除所有应用数据
- **选择**: 在 SettingsViewModel 中直接操作数据库和 SharedPreferences，保留内置标签
- **备选**: 删除整个数据库文件，放弃原因：需要重新创建数据库，影响用户体验
- **影响文件**: `ui/screens/settings/SettingsViewModel.kt:50-80`

### D-004: 数据库迁移策略

- **背景**: 添加 hasAcceptedPrivacy 字段需要数据库迁移
- **选择**: 使用 Room Migration 实现 v1 -> v2 迁移
- **备选**: 破坏性迁移（fallbackToDestructiveMigration），放弃原因：会丢失用户数据
- **影响文件**: `data/local/migration/DatabaseMigrator.kt:130-140`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `AndroidManifest.xml` | ✏️ 修改 | 添加媒体权限声明 |
| `data/local/entity/AppConfigEntity.kt` | ✏️ 修改 | 添加 hasAcceptedPrivacy 字段 |
| `data/local/dao/AppConfigDao.kt` | ✏️ 修改 | 添加隐私政策查询方法 |
| `data/local/AppDatabase.kt` | ✏️ 修改 | 版本升级到 v2 |
| `data/local/migration/DatabaseMigrator.kt` | ✏️ 修改 | 实现 MIGRATION_1_2 |
| `data/local/secure/SecurePrefs.kt` | ✏️ 修改 | prefs 改为公开属性 |
| `MainActivity.kt` | ✏️ 修改 | 添加权限请求和隐私弹窗 |
| `ui/components/PrivacyConsentDialog.kt` | 🆕 新增 | 隐私政策对话框组件 |
| `ui/screens/settings/SettingsScreen.kt` | ✏️ 修改 | 添加数据删除入口 |
| `ui/screens/settings/SettingsViewModel.kt` | 🆕 新增 | 设置页 ViewModel |
| `di/ViewModelModule.kt` | 🆕 新增 | ViewModel 依赖注入模块 |
| `ResourceViewerApp.kt` | ✏️ 修改 | 注册 viewModelModule |
| `data/local/dao/AppConfigDaoTest.kt` | ✏️ 修改 | 添加隐私政策测试 |
| `ui/screens/settings/SettingsViewModelTest.kt` | 🆕 新增 | ViewModel 测试 |
| `ui/component/PrivacyConsentDialogTest.kt` | 🆕 新增 | 隐私弹窗 UI 测试 |
| `ui/screens/settings/SettingsScreenTest.kt` | 🆕 新增 | 设置页面 UI 测试 |
| `data/local/migration/DatabaseMigration1to2Test.kt` | 🆕 新增 | 数据库迁移测试 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 测试验证

- ✅ 单元测试全部通过 (`./gradlew testDebugUnitTest`)
- ✅ Lint 检查通过 (`./gradlew lint`)
- ✅ 编译成功 (`./gradlew build`)

## 已知问题 / TODO

- [ ] 隐私政策文本内容需要根据实际需求调整
- [ ] 权限被拒绝后的用户体验需要优化（目前只是状态更新）
- [ ] 数据清除后的应用重启需要更好的实现（目前只是状态重置）
