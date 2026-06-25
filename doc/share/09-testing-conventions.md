# 09 — 测试约定

> 🔵 所有 Agent 编写测试时遵循的规范。
> 依据：`@tech/06-测试策略.md`

---

## 1. TDD 工作流

**所有生产代码必须通过以下 Red-Green-Refactor 循环编写：**

### 1.1 循环步骤

```
RED ──→ GREEN ──→ REFACTOR ──→ (循环)
```

| 步骤 | 动作 | 验证 |
|------|------|------|
| **RED** | 先写测试，描述期望行为。测试此时必然失败 | `./gradlew test` → 红色 |
| **GREEN** | 写最小生产代码让测试通过。不做任何额外设计 | `./gradlew test` → 绿色 |
| **REFACTOR** | 消除重复、改善结构、提取抽象。每步改动后立即跑测试 | `./gradlew test` → 保持绿色 |

### 1.2 按测试层级执行顺序

```
1. Domain Model 测试 → 2. Repository 测试 → 3. UseCase 测试 → 4. ViewModel 测试 → 5. Compose UI 测试
```

从底层到上层，每层通过后才写上层代码。**不跳层**。

### 1.3 TDD 约束

- **不在没有失败测试时写代码**：每条生产代码都有对应的先行测试
- **最小步长**：GREEN 阶段只写让测试通过的最少代码（哪怕硬编码一个返回值）
- **测试先行的产出物命名**：新文件 `Foo.kt` 必须先有 `FooTest.kt`（在同一级测试目录）
- **错误路径优先**：每个方法先写异常/边界测试，再写快乐路径

### 1.4 示例：实现 SourceRepository.addSource()

```
// RED: 先写测试
@Test
fun `should return Ok when source added successfully`() = runTest {
    val result = repo.addSource(testSource, password = null)
    assertTrue(result is Result.Ok)
}
// → ./gradlew test 红色 (repo.addSource 不存在)

// GREEN: 最小实现
class SourceRepository(...) {
    suspend fun addSource(source: SourceEntity, password: String?): Result<Unit> {
        sourceDao.insert(source)
        return Result.Ok(Unit)
    }
}
// → ./gradlew test 绿色

// RED: 再写失败测试
@Test
fun `should store password in secure prefs when provided`() = runTest {
    repo.addSource(testSource, password = "secret123")
    verify { securePrefs.putPassword(testSource.id, "secret123") }
}
// → 红色

// GREEN: 最小实现追加密码逻辑
// ... 加密码存储代码
// → 绿色

// REFACTOR: 提取 try-catch → Result.Err 等
```

---

## 2. 测试金字塔

```
         ┌──────────┐
         │   E2E   │  极少：关键用户流程
         ├──────────┤
        ┌┤ Compose  ├┐  少数：核心 UI 组件
        │└──────────┘│
       ┌┤ ViewModel ├┐  中等：状态转换 + 错误路径
       │└───────────┘│
      ┌┤  Repo/UC  ├┐  较多：CRUD + UseCase 编排
      │└────────────┘│
     ┌┤   Domain    ├┐  最广泛：data class + 枚举
     └┴─────────────┘┘
```

## 3. Mock 策略

| 被替换组件 | 方案 | 工具 |
|-----------|------|------|
| Room 数据库 | 内存库 | `Room.inMemoryDatabaseBuilder()` |
| `FileSource` 接口 | mock | MockK |
| `EncryptedSharedPreferences` | mock | MockK |
| Repository (在 ViewModel 测试中) | mock | MockK |
| ViewModel (在 Compose 测试中) | mock | MockK |

## 4. 测试目录规则

```
repo/domain/model  →  app/src/test/       # 无 Android 依赖
repo/data/*        →  app/src/androidTest/ # 需要 Room 内存库
ui/viewmodel       →  app/src/androidTest/ # 需要 ViewModel + runTest
ui/component       →  app/src/androidTest/ # 需要 Compose Rule
```

## 5. 命名约定

```kotlin
// 测试类
class ${Target}Test

// 测试方法
@Test
fun `should ${expected behavior} when ${condition}`()
// 示例
fun `should emit success state when resources loaded`()
fun `should emit error state when SMB unreachable`()
fun `should display all custom tags in filter bar`()
```

## 6. ViewModel 测试模板

```kotlin
class HomeViewModelTest {
    private lateinit var mockResourceRepo: ResourceRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        mockResourceRepo = mockk()
        viewModel = HomeViewModel(mockResourceRepo)
    }

    @Test
    fun `should emit loading then success state`() = runTest {
        coEvery { mockResourceRepo.getVisibleResources() } returns flowOf(Result.Ok(emptyList()))

        val states = mutableListOf<UiState>()
        val job = launch { viewModel.uiState.toList(states) }

        viewModel.loadResources()
        advanceUntilIdle()

        assertTrue(states.contains(UiState.LOADING))
        assertTrue(states.contains(UiState.SUCCESS))
        job.cancel()
    }
}
```

## 7. Compose UI 测试模板

```kotlin
@RunWith(AndroidJUnit4::class)
class FilterBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should display all custom tags`() {
        composeTestRule.setContent {
            FilterBar(
                tags = listOf(Tag("1", "热血", "#FF0000")),
                selectedTagIds = emptySet(),
                onTagClick = {},
            )
        }
        composeTestRule.onNodeWithText("全部").assertExists()
        composeTestRule.onNodeWithText("热血").assertExists()
    }
}
```

## 8. Repository 测试模板

```kotlin
@RunWith(AndroidJUnit4::class)
class TagRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: TagRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = TagRepository(db.tagDao(), db.resourceTagDao())
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun `filterByTags with two tags should return intersection`() = runTest {
        // ... seed data, assert intersection
    }
}
```

## 9. CI 命令

```bash
./gradlew test                       # 全部单元测试
./gradlew testDebugUnitTest          # Debug 单元测试
./gradlew connectedAndroidTest       # 设备/模拟器集成测试
./gradlew createDebugCoverageReport  # 覆盖率报告
```

## 10. 收工检查

- [ ] 正常路径有测试
- [ ] 每个 ViewModel 的 load → error → retry 回路有测试
- [ ] 新增的 Room DAO 查询有 Repository 测试
- [ ] 新增的 Composable 组件有 UI 测试
