# 10 — Worktree 协作流程

> 🔵 所有 Agent 必须在开始开发前阅读本文档。

---

## 整体工作流

```
取 stage → 建 worktree → 开发 → 验证 → 写决策日志 → 提 MR → merge → 删 worktree
```

## 1. 取 Stage 任务

1. 查看 `doc/mvp/AGENTS.md` 进度表，确认要做的 stage 前置依赖已全部完成
2. 阅读 stage 文档（`doc/mvp/Mxx-xxx.md`）了解产出物和验收标准
3. 阅读 stage 依赖的 `doc/share/` 共享契约（在 stage 文档中标注）
4. 在 `doc/mvp/AGENTS.md` 中将对应 stage 标记为 `🔵 进行中`

## 2. 建立 Worktree

```bash
# Stage ID 命名: mvp-Mxx-描述 (如 mvp-M12-local-source)
git worktree add ../resource_viewer-M12 -b mvp-M12-local-source

# 切换到新 worktree 工作
cd ../resource_viewer-M12
```

**分支命名规则**：
```
mvp-Mxx-功能描述    # 例: mvp-M12-local-file-source
mvp-Mxx-Mxx-...     # 合并多个 stage: mvp-M06-M07-room-entities
```

## 3. 开发规范

### 文件所有权

| 类型 | 规则 | 标记方式 |
|------|------|---------|
| 🟢 独占文件 | 只有自己的 stage 写入 | 不在其他 stage 的产出清单中 |
| 🟡 聚合文件 | 在已有文件末尾追加，**不修改已有代码** | 见下方聚合文件规则 |
| 🔵 共享文档 | 绝不修改 `doc/share/` 中的文件 | — |

### 聚合文件操作规则

以下文件是多 stage 共享的聚合文件，操作时需遵守：

| 聚合文件 | 骨架创建 | 追加方式 |
|----------|---------|---------|
| `gradle/libs.versions.toml` | M00 | 在 `[versions]` 和 `[libraries]` 末尾追加 |
| `ui/navigation/Screen.kt` | M03 | 在 sealed class 末尾追加 data object/class |
| `ui/navigation/AppNavGraph.kt` | M03 | 在 NavHost composable 块末尾追加 |
| `data/local/AppDatabase.kt` | M08 | 在 entities 参数追加 Entity，在 abstract fun 追加 DAO |
| `di/DatabaseModule.kt` | M02 | M08 在 provides 方法末尾追加 |
| `di/RepositoryModule.kt` | M02 | M10 在 provides 方法末尾追加 |

**操作前必须先 rebase：**
```bash
git fetch origin main
git rebase origin/main
```

## 4. 验证清单

每个 stage 完成前必须通过：

```bash
# 编译通过
./gradlew build

# 单元测试通过
./gradlew test

# Lint 无 error
./gradlew lint
```

## 5. 写决策日志

在 `doc/decisions/` 中创建对应 stage 的决策记录：

```bash
# 在仓库主目录（非 worktree）中操作
cd ../resource_viewer
git pull origin main  # 确保拿到最新 decisions/
# 创建 doc/decisions/Mxx-xxx.md
# 更新 doc/decisions/cross-reference.md
```

详见 `doc/decisions/AGENTS.md`。

## 6. 提交与合并

```bash
# 提交格式
git add .
git commit -m "feat(Mxx): 阶段描述

产出物:
- 新增 xxx 个文件
- 修改 xxx 个聚合文件

验收: build/tests passed"

# 推送到远程
git push origin mvp-Mxx-描述

# 创建 MR 或直接 merge（取决于团队约定）
```

## 7. 清理

```bash
# 合并后删除 worktree
git worktree remove ../resource_viewer-M12

# 更新主仓库
cd ../resource_viewer
git pull origin main
```

## 8. 紧急规则

- **共享契约冲突**：如果发现 `doc/share/` 中的契约有问题，不要直接修改。在本 stage 的决策日志中记录，提交 MR 讨论。
- **聚合文件冲突**：如果 rebase 时发现聚合文件有冲突，联系上一个 stage 的作者协调。
- **依赖阻塞**：如果前置 stage 未合并，不可开始开发。可以在本工作中 "mock" 前置接口，但必须写清 mock 假设并在合并前置后验证。
