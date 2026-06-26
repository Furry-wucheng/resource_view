# M12 — 合规：权限 + 隐私政策 + 数据删除

> **状态**：✅ 已完成

## 背景

Android 13+ 将存储权限拆分为细粒度的媒体权限，应用需要在运行时请求 `READ_MEDIA_IMAGES`、`READ_MEDIA_VIDEO`、`READ_MEDIA_AUDIO`。同时，应用需要提供隐私政策说明和用户数据删除入口。

## 目标

1. **运行时权限请求**：首次启动时请求媒体读取权限
2. **隐私政策弹窗**：首次启动时显示隐私政策，用户同意后不再显示
3. **数据删除入口**：设置页提供清除所有数据的功能

## 实现方案

### 1. 权限请求

**位置**：`MainActivity.kt` 使用 `ActivityResultContracts.RequestMultiplePermissions`

**权限列表**：
- `android.permission.READ_MEDIA_IMAGES`
- `android.permission.READ_MEDIA_VIDEO`
- `android.permission.READ_MEDIA_AUDIO`
- `android.permission.READ_EXTERNAL_STORAGE` (Android 12 及以下)

**流程**：
1. 应用启动时检查是否已获取权限
2. 如果未获取，弹出权限请求
3. 权限被拒绝时显示说明对话框

### 2. 隐私政策弹窗

**位置**：`PrivacyConsentDialog.kt` 组件

**存储**：使用 `AppConfigEntity.hasAcceptedPrivacy` 字段记录用户是否已同意

**流程**：
1. 首次启动时检查 `hasAcceptedPrivacy` 状态
2. 如果未同意，显示隐私政策对话框
3. 用户点击"同意"后更新数据库状态
4. 后续启动不再显示

### 3. 数据删除入口

**位置**：`SettingsScreen.kt` 添加"清除所有数据"选项

**功能**：
- 清除数据库所有表数据
- 清除 SecurePrefs 中的密码
- 清除缓存文件
- 重启应用

## 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `AndroidManifest.xml` | 修改 | 添加媒体权限声明 |
| `AppConfigEntity.kt` | 修改 | 添加 `hasAcceptedPrivacy` 字段 |
| `AppConfigDao.kt` | 修改 | 添加隐私同意状态查询方法 |
| `MainActivity.kt` | 修改 | 添加权限请求逻辑 |
| `PrivacyConsentDialog.kt` | 新建 | 隐私政策对话框组件 |
| `SettingsScreen.kt` | 修改 | 添加数据删除入口 |
| `SettingsViewModel.kt` | 新建 | 设置页 ViewModel |
| `DataClearRepository.kt` | 新建 | 数据清除仓库 |

## 验证方式

1. **权限测试**：
   - 卸载应用后重新安装，验证权限请求弹窗出现
   - 拒绝权限后，验证说明对话框出现
   - 授予权限后，验证不再请求

2. **隐私政策测试**：
   - 清除应用数据后启动，验证隐私弹窗出现
   - 点击同意后重启，验证弹窗不再出现

3. **数据删除测试**：
   - 添加一些数据源和资源
   - 进入设置页点击"清除所有数据"
   - 验证所有数据被清除，应用重启

## 依赖关系

- 依赖 M10（Repository 层）已完成
- 依赖 M09（数据库）已完成
