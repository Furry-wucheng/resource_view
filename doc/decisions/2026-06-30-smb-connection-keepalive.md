# SMB 连接掉线监测与前台探活修复

> 时间: 2026-06-30 | Agent: opencode | 状态: ✅ 已完成 | 对应 Issue: 用户对话 — SMB 息屏掉线

## 设计决策

### D-001: 方案选型 — 应用层前台探活 vs TCP Keep-Alive vs 连接池 TTL

- **背景**: SMB 连接在息屏、Doze、网络切换、NAS 超时后经常掉线。用户回到前台操作时才报错，体验差。
- **选项梳理**:
  - **A. TCP Socket Keep-Alive**: 在 `SmbConfig` 注入自定义 `SocketFactory` 设置 `SO_KEEPALIVE` 和 `TCP_KEEPIDLE`。优点：操作系统层保活；缺点：smbj 0.13.0 不直接暴露 Socket 工厂，需要反射或自定义工厂，且 Android Doze 仍可能冻结网络。
  - **B. 连接池 TTL + 定期心跳**: 在 `FileSourceFactory` 层给每个连接加 TTL，定时发 SMB echo。优点：完全可控；缺点：需要常驻后台协程，增加电量和代码复杂度。
  - **C. 应用层前台探活（选中）**: 监听 `ProcessLifecycleOwner.ON_START`，回到前台时对所有已缓存 SMB 源执行一次轻量 `testConnection()`。探活失败即 `disconnect() + evict()`，让下次业务操作自动重建连接。优点：零后台耗电、代码侵入小、TDD 可测试；缺点：息屏期间无法保活，但用户无感知。
- **选择**: 采用方案 C，因为问题核心是"息屏再出现后的用户体验"，而非后台持续传输。`SmbFileSource.executeWithReconnect()` 已处理了单次操作的重连，探活只是提前清理死连接缓存。

### D-002: SmbConnectionMonitor 依赖注入与生命周期绑定

- **背景**: `ProcessLifecycleOwner` 是应用级 `LifecycleOwner`，其 `lifecycleScope` 随进程存活，不会因 Activity 配置变更而取消。
- **选择**: `SmbConnectionMonitor` 构造函数接收 `CoroutineScope` + `getSmbSources/evictSource` lambda，默认实现桥接 `FileSourceFactory`；`MainActivity.onCreate` 中初始化并注册到 `ProcessLifecycleOwner`，`onDestroy` 中注销。
- **备选**: 在 Application.onCreate 中初始化 → 放弃，项目目前无自定义 Application 类，为单一 monitor 引入 Application 不划算。

### D-003: FileSourceFactory 追加只读查询接口

- **背景**: `SmbConnectionMonitor` 默认需要获取缓存中的所有 SMB 源。
- **选择**: 在 `FileSourceFactory` 末尾追加 `getCachedSmbSources(): List<SmbFileSource>`（聚合文件规则：只追加，不修改已有代码）。
- **影响**: 后续若改用连接池或 TTL 方案，该接口可继续复用。

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `data/remote/smb/SmbConnectionMonitor.kt` | 🆕 新增 | 应用级 SMB 探活监视器，监听 ProcessLifecycleOwner |
| `data/remote/smb/SmbConnectionMonitorTest.kt` | 🆕 新增 | 5 个单元测试：无源/成功/失败/异常/多源独立处理 |
| `shared/filesource/FileSourceFactory.kt` | ✏️ 修改 | 末尾追加 `getCachedSmbSources()` |
| `MainActivity.kt` | ✏️ 修改 | `onCreate` 初始化 monitor，`onDestroy` 注销 |
| `gradle/libs.versions.toml` | ✏️ 修改 | 新增 `lifecycleProcess = "2.6.1"` + library 定义 |
| `app/build.gradle.kts` | ✏️ 修改 | 引入 `androidx.lifecycle:lifecycle-process` |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 已知问题 / TODO

- [ ] 真机验证：不同 NAS（群晖/QNAP/Windows）闲置 kill 超时不同，探活是否足够轻量
- [ ] 若后续增加后台 SMB 同步/扫描，需要升级为方案 B（连接池 TTL）或方案 A（TCP Keep-Alive）
- [ ] `SmbDataSource`（视频播放）每次新建 `SMBClient` 实例，未走 `FileSourceFactory` 缓存，掉线后由 ExoPlayer 重试逻辑处理。后续可考虑复用统一连接池
