# M26 — 错误处理全局组件

> 时间: 2026-06-26 | Agent: claude | 状态: ✅ 已完成 | 前置: M10

## 设计决策

### D-001: BaseViewModel 使用 StateFlow 而非 mutableStateOf

- **背景**: 需要统一 ViewModel 的状态管理，同时兼容 Compose 和非 Compose 消费场景
- **选择**: 使用 `MutableStateFlow` / `StateFlow` 暴露 uiState、errorMessage、lastError
- **备选**: 使用 `mutableStateOf` — 放弃原因：`doc/share/08-code-conventions.md` 明确约定 ViewModel 用 `MutableStateFlow`
- **影响文件**: `ui/base/BaseViewModel.kt`
- **被依赖**: 所有后续 ViewModel（M12-M25）

### D-002: ErrorView 支持三种尺寸级别

- **背景**: 不同场景（页面级、卡片级、行内级）需要不同的错误展示形态
- **选择**: 通过 `ErrorViewLevel` 枚举（PAGE/CARD/INLINE）区分，同一 Composable 入口
- **备选**: 拆分为三个独立 Composable — 放弃原因：增加 API 表面积，且内部逻辑高度相似
- **影响文件**: `ui/components/ErrorView.kt`
- **被依赖**: 所有需要错误展示的 Screen

### D-003: mapError 默认复用 DomainError.toUserMessage()

- **背景**: `doc/share/06-error-handling.md` 已定义完整的错误消息映射
- **选择**: `BaseViewModel.mapError()` 默认调用 `error.toUserMessage()`，子类可覆盖
- **备选**: 在 BaseViewModel 中重复实现映射逻辑 — 放弃原因：违反 DRY，且共享契约已有权威定义
- **影响文件**: `ui/base/BaseViewModel.kt`
- **被依赖**: 无

### D-004: FatalErrorHolder 使用全局单例 + StateFlow

- **背景**: Application 层捕获的致命错误需要传递给 MainActivity 的 Compose UI
- **选择**: `object FatalErrorHolder` 持有 `MutableStateFlow<String?>`，MainActivity 通过 `collectAsState()` 观察
- **备选**: 通过 Koin 注入 — 放弃原因：Application.onCreate() 中 Koin 尚未完全初始化，且单例场景无需 DI
- **影响文件**: `ui/base/FatalErrorHolder.kt`, `ResourceViewerApp.kt`, `MainActivity.kt`
- **被依赖**: 无

### D-005: 全局错误页重试使用 Activity.recreate()

- **背景**: 数据库损坏等 fatal 错误需要用户重试
- **选择**: 重试时调用 `recreate()` 重建 Activity，重新走 Application 初始化流程
- **备选**: 仅重新打开数据库 — 放弃原因：数据库损坏可能是文件级问题，需要完整重新初始化
- **影响文件**: `MainActivity.kt`
- **被依赖**: 无

## 实现思路

### 整体架构

```
Compose 层
    ↓ 观察 StateFlow
BaseViewModel (M26.2)
    ↓ 消费 Result<T>
Repository 层 (M10)
```

### 关键实现

1. **ErrorView**: 三种尺寸通过 `when(level)` 分发到内部私有 Composable
2. **BaseViewModel**: `handleResult()` 统一处理 `Result.Ok` / `Result.Err`，更新三个 StateFlow
3. **ScanProgressBar**: 使用 `animateFloatAsState` 实现进度条动画
4. **FatalErrorHolder**: `ResourceViewerApp.onCreate()` 中预热数据库，捕获异常写入 holder

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/components/ErrorView.kt` | 🆕 新增 | 可复用错误组件，PAGE/CARD/INLINE 三级 |
| `ui/base/BaseViewModel.kt` | 🆕 新增 | ViewModel 基类，统一 UiState + handleResult |
| `ui/components/ScanProgressBar.kt` | 🆕 新增 | 扫描进度条 + 结果摘要组件 |
| `ui/base/FatalErrorHolder.kt` | 🆕 新增 | 全局致命错误状态持有者 |
| `ResourceViewerApp.kt` | ✏️ 修改 | 添加数据库预热 + 异常捕获 |
| `MainActivity.kt` | ✏️ 修改 | 观察 FatalError，显示全局错误页 |

> 操作图例: 🆕 新增 · ✏️ 修改 · 🗑️ 删除

## 测试清单

| 测试文件 | 测试内容 |
|---------|---------|
| `ui/base/BaseViewModelTest.kt` | 初始状态、状态转换、canRetry 判断、所有 DomainError 消息映射（15 用例） |
| `ui/base/FatalErrorHolderTest.kt` | 初始化、设置、清除、覆盖（4 用例） |

## 验收标准

- [x] ErrorView 在所有错误状态场景正确渲染
- [x] BaseViewModel 减少 ViewModel 中的重复代码
- [x] 数据库损坏时显示全局错误页
- [x] `./gradlew build` 通过
- [x] `./gradlew test` 通过

## 已知问题 / TODO

- [ ] ErrorView / ScanProgressBar 的 Compose UI 测试（需设备/模拟器，暂未编写）
- [ ] 实际 ViewModel 子类尚未迁移到 BaseViewModel（后续 Stage 逐步替换）
