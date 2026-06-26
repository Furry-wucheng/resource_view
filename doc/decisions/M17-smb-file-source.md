# M17 — SmbFileSource + SMB 源配置 UI

> 日期: 2026-06-26 | Agent: claude | 状态: ✅ 已完成

---

## 决策概览

本阶段实现了 SMB 协议的文件源和 SMB 源添加/编辑/测试连接 UI。

---

## 决策记录

### D-17-01: SmbClientWrapper 异常处理策略

**问题**: `DomainError` 是 sealed class 而非 Exception 的子类，无法直接 throw。

**决策**: 创建自定义异常类 `SmbConnectionException`、`SmbAuthException`、`SmbFileException`，在 SmbClientWrapper 中抛出这些异常，由 SmbFileSource 捕获并重新抛出。

**理由**:
- 保持 DomainError 的纯数据特性
- 在 FileSource 层统一处理异常转换
- 便于测试和错误传播

**权衡**: 需要在 SmbFileSource 中重复 catch-throw 逻辑。

---

### D-17-02: SMB 连接管理模式

**问题**: SMB 连接应该在每次操作时创建还是保持长连接？

**决策**: 使用懒连接模式，首次操作时自动连接，后续操作复用连接。

**理由**:
- 简化 API 使用，调用者无需关心连接状态
- 避免频繁创建/销毁连接的开销
- 通过 `isConnected()` 检查连接状态

**权衡**: 需要处理连接断开后的重连逻辑。

---

### D-17-03: SMB 共享名称解析

**问题**: 如何从 rootPath 解析出共享名称？

**决策**: 从 rootPath 的第一段路径提取共享名称。例如 `/myshare/folder` → 共享名称 `myshare`。

**理由**:
- 符合 SMB 协议的路径规范
- 与设计文档中的路径格式一致
- 便于路径拼接

**示例**:
```
rootPath: /myshare/folder
shareName: myshare
basePath: /myshare/folder
```

---

### D-17-04: Koin 模块组织

**问题**: SMB 相关依赖应该如何组织？

**决策**: 创建独立的 `smbModule`，提供 `SMBClient` 和 `SmbClientWrapper` 单例。

**理由**:
- 遵循项目约定的模块化结构
- 便于测试时替换 mock
- 清晰的依赖边界

**模块结构**:
```kotlin
val smbModule = module {
    single<SMBClient> { SMBClient(SmbConfig.builder().build()) }
    single { SmbClientWrapper(get()) }
}
```

---

### D-17-05: SourceListViewModel 设计

**问题**: ViewModel 应该管理哪些状态？

**决策**: 使用单一 `SourceListUiState` 数据类管理所有 UI 状态，包括：
- 数据源列表
- 加载状态
- 错误信息
- SMB 表单数据
- 测试连接状态

**理由**:
- 单一状态源，便于状态管理
- 简化 Compose 的状态订阅
- 笔筒架构模式

---

## 产出物清单

| 文件 | 类型 | 说明 |
|------|------|------|
| `data/remote/smb/SmbClientWrapper.kt` | 新增 | SMB 客户端封装 |
| `data/remote/smb/SmbExceptions.kt` | 新增 | 自定义异常类 |
| `shared/filesource/SmbFileSource.kt` | 新增 | FileSource 实现 |
| `shared/filesource/FileSourceFactory.kt` | 修改 | 更新工厂方法 |
| `di/SmbModule.kt` | 新增 | Koin 模块 |
| `ui/screens/sources/SourceListViewModel.kt` | 新增 | ViewModel |
| `ui/screens/sources/SourceListScreen.kt` | 修改 | 添加弹窗和列表 |
| `ui/screens/sources/AddSmbDialog.kt` | 新增 | SMB 添加弹窗 |
| `ResourceViewerApp.kt` | 修改 | 注册 smbModule |
| `di/ViewModelModule.kt` | 修改 | 注册 ViewModel |

## 测试覆盖

| 测试类 | 测试数 | 状态 |
|--------|--------|------|
| `SmbClientWrapperTest` | 13 | ✅ 通过 |
| `SmbFileSourceTest` | 14 | ✅ 通过 |
| `SourceListViewModelTest` | 11 | ✅ 通过 |

## 验收标准完成情况

- [x] 添加 SMB 源 → 测试连接成功 → 保存到数据库
- [x] 密码存入 EncryptedSharedPreferences（不入 Room）
- [x] 连接失败时显示 SourceUnreachableError / SourceAuthError
- [x] 文件浏览器可浏览 SMB 源目录
- [x] `./gradlew build` 通过
