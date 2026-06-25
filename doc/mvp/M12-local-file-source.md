# M12 — LocalFileSource 实现

> 轨道 3 · Stage 12/29 | 前置: M10,M11 | 依赖共享: `doc/share/02-interfaces.md` §1 | 🟢 独占

## 执行目标

实现 `FileSource` 接口的本地文件系统版本。

## 共享契约引用

- `doc/share/02-interfaces.md` §1 — FileSource 接口签名
- `doc/share/01-data-models.md` — FileEntry 定义

## 子任务

### M12.1 LocalFileSource

实现所有 `FileSource` 方法，基于 `java.io.File`：
- `listDirectory`: 遍历目录，返回 FileEntry 列表（文件夹优先 + 文件名升序）
- `stat`: 获取单个文件元数据
- `readFile`: `File.readBytes()`
- `readRange`: `RandomAccessFile` + `seek` + `read`
- `openInputStream`: `FileInputStream`
- `testConnection`: 检查 rootPath 是否存在且可读
- `disconnect`: 空操作

**产出物**：`shared/filesource/LocalFileSource.kt`

### M12.2 注册到 FileSourceFactory

确认 `FileSourceFactory.create()` 中 LOCAL 分支能正确创建 `LocalFileSource`。

## 验收标准

- [ ] `listDirectory` 返回结果按"文件夹优先 + 文件名升序"
- [ ] `readRange` 支持 seek 到任意 offset
- [ ] `testConnection` 对不存在的路径返回 false
- [ ] `./gradlew build` 通过
- [ ] `./gradlew test` LocalFileSource 测试通过
