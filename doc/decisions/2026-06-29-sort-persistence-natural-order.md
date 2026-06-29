# 排序持久化 + 自然排序修复

> 时间: 2026-06-29 | Agent: opencode/deepseek-v4 | 状态: ✅ 已完成 | 前置: 无

## 设计决策

### D-001: 首页排序持久化方案选择 — DataStore Preferences
- **背景**: 首页排序偏好（ResourceSort）在 HomeViewModel 中使用 MutableStateFlow 管理，但每次 ViewModel 创建都重置为 ADDED_ASC，需要持久化。
- **选择**: 使用 DataStore Preferences 创建 HomePrefsStore，与 FileBrowserPrefsStore 保持一致的持久化模式。按 enum name 序列化存储，加载时用 `valueOf` 反序列化，非法值回退默认。
- **备选**: Room AppConfigEntity 添加字段。放弃原因：需要数据库迁移（AppConfigEntity 已有 version 5），会增加不必要的复杂度。DataStore 更轻量，且与现有的 FileBrowserPrefsStore 技术栈统一。
- **影响文件**: `data/local/datastore/HomePrefsStore.kt`, `di/DataStoreModule.kt`, `di/ViewModelModule.kt`, `ui/screens/home/HomeViewModel.kt`
- **被依赖**: 无，独立功能

### D-002: 自然排序算法选择 — 片段化 CompareBy
- **背景**: 当前所有名称排序使用 `it.name.lowercase()` 做纯字符串比较，导致 "10" < "2" 的字典序问题。
- **选择**: 实现 `NaturalOrderComparator : Comparator<String>`，将字符串按连续数字/非数字分割为片段，逐片段比较：数字片段按 Long 值比较，非数字片段按小写字符串比较。每个字符串一次扫描完成比较。
- **备选**: 零填充方案（将数字统一补齐到相同宽度再字符串比较）。放弃原因：需要预扫描最大数字宽度，2 遍遍历；且补零后字符串变长，与原语义偏离。片段化比较更直观，性能可接受（目录文件数通常 < 500）。
- **影响文件**: `shared/util/NaturalOrderComparator.kt`, `ui/screens/home/HomeViewModel.kt`, `ui/screens/sources/FileBrowserViewModel.kt`
- **被依赖**: 无，独立工具类

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `shared/util/NaturalOrderComparator.kt` | 🆕 新增 | 字母数字自然排序 Comparator |
| `data/local/datastore/HomePrefsStore.kt` | 🆕 新增 | DataStore 持久化首页排序 |
| `di/DataStoreModule.kt` | ✏️ 修改 | 注册 HomePrefsStore 单例 |
| `di/ViewModelModule.kt` | ✏️ 修改 | HomeViewModel 工厂新增 HomePrefsStore 参数 |
| `ui/screens/home/HomeViewModel.kt` | ✏️ 修改 | init 加载 + setSort 持久化 + 自然排序 |
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 修改 | 名称排序改用 NaturalOrderComparator |
| `shared/util/NaturalOrderComparatorTest.kt` | 🆕 新增 | 8 个自然排序单元测试 |
| `HomeViewModelTest.kt` | ✏️ 修改 | 新增 5 个测试 |
| `FileBrowserViewModelTest.kt` | ✏️ 修改 | 新增 3 个自然排序测试 |

## 已知问题 / TODO

- 无
