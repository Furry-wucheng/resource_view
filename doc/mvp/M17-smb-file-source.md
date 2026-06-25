# M17 — SmbFileSource + SMB 源配置 UI

> 轨道 5 · Stage 17/29 | 前置: M11,M12 | 依赖共享: `doc/share/02-interfaces.md` §1 | 🟢 独占 + 🟡 聚合(SourceListScreen 追加 SMB 添加弹窗)

## 执行目标

实现 SMB 协议的文件源 + SMB 源添加/编辑/测试连接 UI。

## 共享契约引用

- `doc/share/02-interfaces.md` §1 — FileSource 接口
- `doc/share/03-di-contracts.md` — SmbModule 契约
- `@prd/03-数据源管理.md` — SMB 源交互
- `@design/source-list.html` — 原型参考

## 子任务

### M17.1 SmbClientWrapper

封装 smbj 的 `SmbClient`：连接、listDirectory、stat、readFile、disconnect。处理 `SMBApiException` 状态码映射。

**产出物**：`data/remote/smb/SmbClientWrapper.kt`

### M17.2 SmbFileSource

实现 `FileSource` 接口，桥接 `SmbClientWrapper`。路径拼接 `smb://host/share + relativePath`。

**产出物**：`shared/filesource/SmbFileSource.kt`

### M17.3 SMB 添加弹窗

在 `SourceListScreen` 中追加 "添加 SMB 源" 弹窗：
- 地址 (host+port)
- 用户名/密码
- 域名（可选）
- 共享文件夹列表（连接后拉取）
- "测试连接" 按钮
- 确认添加

**产出物**：`ui/screens/sources/SourceListScreen.kt`（追加 SMB 弹窗，或独立文件 `components/AddSmbDialog.kt`）

### M17.4 SmbModule

创建 Hilt Module，提供 `SmbClientWrapper` 单例。

**产出物**：`di/SmbModule.kt`

### M17.5 更新 SourceListViewModel

从占位 ViewModel 升级为完整 CRUD + 测试连接。

**产出物**：`ui/screens/sources/SourceListViewModel.kt`

## 验收标准

- [ ] 添加 SMB 源 → 测试连接成功 → 保存到数据库
- [ ] 密码存入 EncryptedSharedPreferences（不入 Room）
- [ ] 连接失败时显示 SourceUnreachableError / SourceAuthError
- [ ] 文件浏览器可浏览 SMB 源目录
- [ ] `./gradlew build` 通过
