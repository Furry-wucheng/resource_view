# M30 — 修复底栏导航 + SMB 线程与权限

> 日期: 2026-06-27 | Agent: claude

---

## D-001: 底栏 Tab 精简

**问题**: 底栏有 5 个 Tab（首页/知识/工具箱/我的/设置），其中知识/工具箱/我的是占位页面，缺少数据源入口。

**决策**: 精简为 3 个 Tab（首页/数据源/设置），删除 Knowledge/Toolbox/Profile 路由和页面文件。

**理由**: 底栏应只展示实际功能入口，减少用户困惑。

---

## D-002: 添加数据源统一类型选择

**问题**: SMB 数据源的添加按钮放在 TopAppBar 右上角，FAB 只能添加本地源，交互不一致。

**决策**: FAB 点击后弹出类型选择弹窗（本地文件夹 / SMB 网络共享），选择后再打开对应表单。

**理由**: 统一入口，用户自然理解"添加数据源"需要先选类型。

---

## D-003: SMB 线程调度

**问题**: SMB 连接使用 smbj 同步 API，`viewModelScope.launch` 默认在主线程执行，Android 9+ 抛 `NetworkOnMainThreadException`。

**决策**: 在 `SmbFileSource` 的所有 suspend 方法内部用 `withContext(Dispatchers.IO)` 包裹，ViewModel 层 `testSmbConnection` 也加 `withContext(Dispatchers.IO)`。

**理由**: 在最贴近 I/O 的层级切线程，上层调用方无需关心线程。

---

## D-004: 网络权限配置

**问题**: 缺少 `INTERNET` 权限，SMB 连接被拒绝。

**决策**: 添加 `INTERNET` 权限和 `android:usesCleartextTraffic="true"`。

**理由**: SMB 使用原始 TCP（端口 445），非 HTTPS，需要显式允许明文流量。

---

## D-005: 错误信息分层

**问题**: 异常信息直接显示在 UI 上，过于技术化。

**决策**: UI 显示用户友好文案（"连接失败，请检查地址、凭据和共享名称"），详细异常通过 `Log.e()` 打印到 Logcat（tag: `SourceListViewModel` / `SmbClientWrapper`）。

**理由**: 用户不需要看堆栈，开发者通过 Logcat 调试。
