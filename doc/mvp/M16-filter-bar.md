# M16 — 筛选栏 + 标签交集查询

> 轨道 4 · Stage 16/29 | 前置: M15 | 依赖共享: `doc/share/01-data-models.md` `doc/share/05-theme-tokens.md` | 🟢 独占 + 🟡 聚合(AppNavGraph 挂载)

## 执行目标

实现首页顶部标签筛选栏（全部/收藏/自定义标签交集筛选）+ FilterResourcesByTagsUseCase。

## 共享契约引用

- `doc/share/01-data-models.md` — Tag Domain Model、标签交集查询逻辑
- `doc/share/05-theme-tokens.md` — 标签颜色
- `doc/share/04-navigation-routes.md` — 筛选栏在 HomeScreen 内，不走路由
- `@prd/02-标签系统.md` — 交集筛选交互

## 子任务

### M16.1 FilterResourcesByTagsUseCase

实现标签交集查询逻辑：
- 空列表 → 返回全部 Resource
- 单个标签 → 返回含该标签的 Resource
- 多个标签 → 返回同时拥有所有选中标签的 Resource（交集）

**产出物**：`domain/usecase/FilterResourcesByTagsUseCase.kt`

### M16.2 FilterBar 组件

可横向滚动的标签条（LazyRow）：
- "全部" 始终第一位（默认选中）
- "收藏" 第二位（内置标签）
- 自定义标签按创建顺序排列
- 标签以彩色 Chip 呈现
- 支持多选（选中高亮 → 交集筛选）
- 末尾 "管理" 按钮 → 跳转 TagManagerScreen

**产出物**：`ui/components/FilterBar.kt`

### M16.3 集成到 HomeScreen

在 `HomeScreen.kt` 顶部挂载 `FilterBar`。

**产出物**：`ui/screens/home/HomeScreen.kt`（修改，在骨架基础上追加）

### M16.4 TagCount

创建 `TagCount` data class（用于 `getTagResourceCounts` 查询结果）。

**产出物**：`data/local/dao/TagDao.kt`（追加 data class，如已有则跳过）

## 验收标准

- [ ] 单选标签 → 网格只显示含该标签的资源
- [ ] 多选标签（交集）→ 网格只显示同时拥有的资源
- [ ] 点击 "全部" → 清除所有筛选
- [ ] "管理" 按钮跳转标签管理页面
- [ ] `./gradlew build` 通过
