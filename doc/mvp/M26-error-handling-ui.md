# M26 — 错误处理全局组件

> 轨道 8 · Stage 26/29 | 前置: M10 | 依赖共享: `doc/share/06-error-handling.md` | 🟢 独占

## 执行目标

实现 ErrorView 可复用组件 + BaseViewModel 基类，统一全 App 的错误处理 UI。

## 共享契约引用

- `doc/share/06-error-handling.md` — DomainError、UiState、Progress\<T\>
- `doc/share/05-theme-tokens.md` — 功能色
- `@tech/05-错误处理策略.md` — 各层错误处理约定

## 子任务

### M26.1 ErrorView 组件

可复用的错误状态组件：
- 错误图标 + 用户可读消息
- 可重试时显示 "重试" 按钮
- 不可重试时仅显示消息
- 三种尺寸：页面级 / 卡片级 / 行内级

**产出物**：`ui/components/ErrorView.kt`

### M26.2 BaseViewModel 基类

- `_uiState: MutableStateFlow<UiState>` / `uiState: StateFlow<UiState>`
- `errorMessage` + `lastError` + `canRetry`
- `handleResult<T>(result: Result<T>)` 方法
- `mapError(error: DomainError): String` 用户可读消息映射

**产出物**：`shared/BaseViewModel.kt`（或放在 `ui/base/`）

### M26.3 扫描进度组件

进度条 + 当前/总数显示 + 失败项统计。

**产出物**：`ui/components/ScanProgressBar.kt`

### M26.4 全局异常兜底

在 `ResourceViewerApp.onCreate()` 中预热 Database，捕获 fatal 异常 → 显示全局错误页面。

## 验收标准

- [ ] ErrorView 在所有错误状态场景正确渲染
- [ ] BaseViewModel 减少 ViewModel 中的重复代码
- [ ] 数据库损坏时显示全局错误页
- [ ] `./gradlew build` 通过
