# 08 — 代码约定

> 🔵 所有 Agent 遵循的 Kotlin / Compose 编码规范。

---

## Kotlin 基础

1. **不可变优先**：字段默认 `val`，集合默认 `List` 而非 `MutableList`
2. **函数单表达式**：简单逻辑用 `=` 替代 `{ return }`
3. **显式参数类型**：public API 不省略类型，内部 lambda 可省略
4. **`data class` 不做逻辑**：逻辑放 UseCase / Repository 中
5. **禁止 `!!` 强制解包**：用 `?.let`、`?:`、`requireNotNull`
6. **禁止 `lateinit`**：除非是 DI 注入的特殊场景

```kotlin
// ✅ Good
fun calculateTax(price: Double): Double = price * 0.13

data class SourceEntity(val id: String, val name: String)

// ❌ Bad
fun calculateTax(price: Double) = price * 0.13  // public API 缺返回类型
var items = mutableListOf<String>()  // 可变 + val = 尴尬
data class SourceEntity(val id: String) {
    fun validate(): Boolean { ... }  // data class 放逻辑
}
```

## Compose 约定

1. **Composable 函数用 `PascalCase`**，返回 `Unit`
2. **State 提升**：状态在最公共的祖先管理，子组件通过参数接收
3. **`Modifier` 作为最后一个可选参数**，默认 `Modifier`
4. **`remember` 存储的 lambda 用 `{}` 包装**，避免每次重组重新创建
5. **`LaunchedEffect(key)` 处理副作用**，`DisposableEffect` 处理资源清理
6. **避免 `mutableStateOf` 在 ViewModel 外**，ViewModel 用 `MutableStateFlow`

```kotlin
// ✅ Good
@Composable
fun ResourceGridItem(
    resource: Resource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(modifier = modifier.clickable { onClick() }) { ... }
}

// ✅ ViewModel state
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val resourceRepo: ResourceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.IDLE)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}

// ❌ Bad
@Composable
fun resourceGridItem(resource: Resource) { ... }  // camelCase
```

## ViewModel 约定

1. 使用 `@HiltViewModel` + `@Inject constructor`
2. UI state 用 `StateFlow<UiState>` 暴露，actions 用普通方法
3. 不持有 `Context` / `Activity` / `View` 引用
4. 只在 `viewModelScope` 中启动协程
5. `onCleared()` 中释放 ExoPlayer、ContentProvider 等资源

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.IDLE)
    val uiState = _uiState.asStateFlow()

    fun loadResources() {
        viewModelScope.launch {
            _uiState.value = UiState.LOADING
            resourceRepository.getVisibleResources().collect { result ->
                _uiState.value = when (result) {
                    is Result.Ok -> UiState.SUCCESS
                    is Result.Err -> UiState.ERROR
                }
            }
        }
    }
}
```

## Repository 约定

1. `suspend fun` 返回 `Result<T>`，内部 catch 异常
2. Flow 查询自动在 Room DAO 上触发（不包装）
3. 跨多个 DAO 的操作用 `@Transaction`

```kotlin
class SourceRepository(
    private val sourceDao: SourceDao,
    private val securePrefs: SecurePrefs,
) {
    fun getAllSources(): Flow<List<SourceEntity>> = sourceDao.getAllSources()

    suspend fun addSource(source: SourceEntity, password: String?): Result<Unit> {
        return try {
            sourceDao.insert(source)
            password?.let { securePrefs.putPassword(source.id, it) }
            Result.Ok(Unit)
        } catch (e: Exception) {
            Result.Err(DomainError.DatabaseError("添加数据源失败", e))
        }
    }
}
```

## 文件头注释

```kotlin
// 文件头部不需要版权声明
// 按功能分组 import（系统 → 第三方 → 项目内）
import android.content.Context
import androidx.compose.material3.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.wucheng.resource_viewer.domain.model.Source
```

## 禁止事项

- ❌ 不写冗余注释（函数签名已说明的不要注释）
- ❌ 不 emoji 在代码/注释中
- ❌ 不在 Compose 中直接 `viewModel.xxx.collect()` —— 用 `collectAsStateWithLifecycle()`
- ❌ 不创建无意义的 abstract class / interface（除非是共享 DSL 或策略模式）
- ❌ 不硬编码字符串（简单例外：tag name 校验、路由路径）
