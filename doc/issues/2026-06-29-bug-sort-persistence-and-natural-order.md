# bug: 资源库排序未持久化 + 文件名排序未按自然顺序

> 日期: 2026-06-29 | 类型: bug | 状态: ✅ 已完成

## 现象

1. 资源库（首页）切换排序方式后，退出再进入恢复为默认排序（按添加时间↑），排序偏好没有被保存
2. 各处文件名排序（资源库名称排序、文件浏览器名称排序）均为字典序，出现 "1, 10, 12, 2, 20" 而非 "1, 2, 10, 12, 20"
3. 文件浏览器按文件夹的排序偏好已持久化（FileBrowserPrefsStore），但缺少天然排序能力

## 复现步骤

### 排序未持久化
1. 进入资源库首页
2. 点击排序 → 选择"按名称 A-Z"
3. 退出应用 / 切换到其他 Tab 再回来
4. 排序恢复为"按添加时间↑"

### 自然排序
1. 文件浏览器中某目录有文件 "1.jpg", "2.jpg", "10.jpg", "20.jpg"
2. 排序选择"按名称 A-Z"
3. 显示顺序为 "1.jpg", "10.jpg", "20.jpg", "2.jpg"（非预期）

## 期望效果

- 排序偏好跨会话保持
- 文件名排序按自然顺序（字母数字）：数字部分按数值比较

## 影响分析

| 维度 | 内容 |
|------|------|
| 根因 | HomeViewModel._sort 硬编码默认值 ADDED_ASC，setSort() 不持久化；sortEntries 使用 `it.name.lowercase()` 做纯字符串比较 |
| 修改文件 | shared/util/NaturalOrderComparator.kt(新增), data/local/datastore/HomePrefsStore.kt(新增), di/DataStoreModule.kt, di/ViewModelModule.kt, ui/screens/home/HomeViewModel.kt, ui/screens/sources/FileBrowserViewModel.kt |
| 影响 stage | M23（HomeViewModel）、M13（FileBrowserViewModel）、聚合文件 di/DataStoreModule.kt、di/ViewModelModule.kt |

## 执行计划

1. 创建 NaturalOrderComparator — 字母数字片段化比较，Comparator<String>
2. 创建 HomePrefsStore — DataStore Preferences 存储 resourceSort
3. 修改 DI 模块注册 HomePrefsStore
4. HomeViewModel 注入 HomePrefsStore，init 加载 + setSort 持久化
5. HomeViewModel + FileBrowserViewModel 名称排序使用 NaturalOrderComparator
6. 编写测试覆盖

## 产出

| 文件 | 操作 | 说明 |
|------|------|------|
| `shared/util/NaturalOrderComparator.kt` | 🆕 新增 | 字母数字自然排序 Comparator |
| `data/local/datastore/HomePrefsStore.kt` | 🆕 新增 | DataStore 持久化首页排序偏好 |
| `di/DataStoreModule.kt` | ✏️ 修改 | 注册 HomePrefsStore 单例 |
| `di/ViewModelModule.kt` | ✏️ 修改 | HomeViewModel 注入 HomePrefsStore |
| `ui/screens/home/HomeViewModel.kt` | ✏️ 修改 | 排序持久化 + 自然排序 |
| `ui/screens/sources/FileBrowserViewModel.kt` | ✏️ 修改 | 名称排序使用自然排序 |
| `shared/util/NaturalOrderComparatorTest.kt` | 🆕 新增 | 8 个自然排序单元测试 |
| `HomeViewModelTest.kt` | ✏️ 修改 | 新增 5 个测试（持久化+自然排序） |
| `FileBrowserViewModelTest.kt` | ✏️ 修改 | 新增 3 个自然排序测试 |
