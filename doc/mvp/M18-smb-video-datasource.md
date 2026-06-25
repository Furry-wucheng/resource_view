# M18 — SMB 视频 DataSource

> 轨道 5 · Stage 18/29 | 前置: M17 | 依赖共享: `doc/share/02-interfaces.md` §1 | 🟢 独占

## 执行目标

实现 ExoPlayer 自定义 `DataSource`，将播放器读取请求桥接到 smbj 文件流。

## 共享契约引用

- `doc/share/02-interfaces.md` §1 — FileSource.readRange 定义
- `@tech/03-性能优化设计.md` §7 — SmbDataSource 参考实现（smbj 同步 API）

## 子任务

### M18.1 SmbDataSource

实现 `DataSource` 接口：
- `open(DataSpec)`: 打开 smbj 文件句柄，`seek` 到指定 position
- `read(buffer, offset, length)`: 直接从 smbj 同步 InputStream 读取，**不使用 `runBlocking`**
- `close()`: 关闭文件句柄和 InputStream
- `getUri()`: 返回 null（SMB 没有标准 URI）

**注意**：`read()` 是同步方法。直接使用 smbj 的同步阻塞 API（`DiskShare.openFile()` → `File.getInputStream()`），
不经过协程，避免 `runBlocking` 导致 ExoPlayer I/O 线程池饥饿。需为 read 增加超时机制。

**产出物**：`data/remote/smb/SmbDataSource.kt`

### M18.2 SmbDataSourceFactory

实现 `DataSource.Factory`，每次创建新 `SmbDataSource` 实例。

**产出物**：`data/remote/smb/SmbDataSourceFactory.kt`

### M18.3 FileSource.readRange 优化确认

确认 `LocalFileSource` 和 `SmbFileSource` 的 `readRange` 实现支持高效的随机读取（SeekableStream / RandomAccessFile）。

## 验收标准

- [ ] ExoPlayer 可通过 SmbDataSource 播放 SMB 共享上的视频文件
- [ ] 拖动进度条时能正确 seek 到目标位置
- [ ] readRange 有超时机制（防止网络断开永久阻塞）
- [ ] `./gradlew build` 通过
