# Resource Viewer — Android

> Android 原生版 Resource Viewer，基于 Flutter 版 PRD 复用，使用 Kotlin + Jetpack Compose 重写。
> **本文件是所有 Agent 的入口指南。开始任何开发前必须阅读。**

---

## 技术栈

| 维度 | 方案 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.4.0 |
| KSP | KSP | 2.3.9 |
| UI | Jetpack Compose + Material3 | BOM 2026.06.00 |
| 架构 | MVVM + Repository | — |
| DI | Hilt | 2.51 |
| 导航 | Navigation Compose | 2.8.0 |
| 数据库 | Room | 2.6.1 |
| 视频 | Media3 ExoPlayer | 1.5.0 |
| 图片 | Coil | 3.5.0 |
| SMB | smbj | 0.13.0 |
| PDF | pdfium-android (io.legere) | 2.0.0 |
| 压缩包 | zip4j + Commons Compress | 2.11.6 / 1.28.0 |
| 安全 | Security Crypto | 1.1.0-alpha06 |
| 协程 | kotlinx-coroutines | 1.8.0 |
| UUID | benasher-uuid | 0.8.2 |
| 测试 | JUnit 4 + MockK | 4.13.2 / 1.13.16 |
| 最低 SDK | API 33 (Android 13) | — |
| 目标 SDK | API 36 (Android 15) | — |

## 构建

```bash
./gradlew build                    # 编译
./gradlew test                     # 单元测试
./gradlew connectedAndroidTest     # 设备测试
./gradlew lint                     # 代码检查
./gradlew createDebugCoverageReport # 覆盖率
```

---

## 文档结构

```
resource_viewer/
├── AGENTS.md                       # ← 本文件：入口指南 + 协作约定
├── doc/
│   ├── share/                      # 🔵 只读共享契约（10 文件）
│   │   ├── 00-glossary.md          #   术语定义
│   │   ├── 01-data-models.md       #   Entity/Domain Model/Enum/ViewerItem
│   │   ├── 02-interfaces.md        #   FileSource/ContentProvider/OrganizationStrategy/ThumbnailGenerator
│   │   ├── 03-di-contracts.md      #   Hilt Module 契约
│   │   ├── 04-navigation-routes.md #   路由定义 + NavHost + BottomNavBar
│   │   ├── 05-theme-tokens.md      #   Color/Type/Shape
│   │   ├── 06-error-handling.md    #   DomainError/Result/Progress/UiState
│   │   ├── 07-directory-layout.md  #   包目录结构 + 文件命名
│   │   ├── 08-code-conventions.md  #   Kotlin/Compose 编码规范
│   │   ├── 09-testing-conventions.md # 测试金字塔/mock 策略/模板
│   │   └── 10-worktree-guide.md    #   Git worktree 协作流程
│   ├── mvp/                        # 🟢 任务指令（事前，29 stage）
│   │   ├── AGENTS.md               #   进度总览 + 依赖图 + 并行窗口
│   │   ├── M00-dependencies.md
│   │   ├── M01-theme-system.md
│   │   ├── ...
│   │   └── M28-polish.md
│   ├── decisions/                  # 🟡 决策日志（事后）
│   │   ├── AGENTS.md                #   索引
│   │   ├── TEMPLATE.md              #   决策记录模板
│   │   ├── cross-reference.md      #   反向索引（按组件/主题倒查）
│   │   └── Mxx-xxx.md              #   各 stage 决策记录（完成后创建）
│   ├── issues/                      # 📥 协作提案（新增需求/Bug/性能优化）
│   │   └── AGENTS.md                #   模板 + 流程
│   ├── prd/                        # 产品需求文档（Flutter 版复用）
│   ├── tech/                       # 技术设计文档
│   └── design/                     # 页面原型 HTML
```

---

## 协作约定

### 文件所有权分类

| 标记 | 含义 | 规则 |
|------|------|------|
| 🟢 **独占文件** | 只有一个 owning stage | 其他人不创建同名文件，不修改已有内容 |
| 🟡 **聚合文件** | 骨架 stage 创建，后续 stage 追加 | 只能**在文件末尾追加**，绝不修改已有代码；追加前必须 rebase |
| 🔵 **只读共享** | `doc/share/` 中所有文件 | 所有 Agent 引用，**绝不在 stage 开发中修改**。变更需 PR review |

### 提案流程

新增需求、Bug 修复、性能优化统一通过 `doc/issues/` 提交：

1. 在 `doc/issues/` 下新建 `{YYYY-MM-DD}-{feature|bug|perf}-{简短描述}.md`
2. 按 `doc/issues/AGENTS.md` 中的模板填写描述和验收标准
3. AI 在 plan 阶段补充影响分析和执行计划，标记 🟡 进行中
4. AI 按现有 worktree + TDD 流程执行开发
5. 完成后填写产出清单，标记 ✅ 已完成
6. 若提案标注"需合并回原文档"，AI 负责更新对应 share/prd/tech 文档

### 聚合文件清单

| 文件 | 骨架创建于 | 追加方式 |
|------|-----------|---------|
| `gradle/libs.versions.toml` | M00 | `[versions]`/`[libraries]` 末尾追加 |
| `ui/navigation/Screen.kt` | M03 | sealed class 末尾追加 data object/class |
| `ui/navigation/AppNavGraph.kt` | M03 | NavHost composable 块末尾追加 |
| `data/local/AppDatabase.kt` | M08 | entities 参数追加 Entity；abstract fun 追加 DAO |
| `di/DatabaseModule.kt` | M02 | provides 方法末尾追加 |
| `di/RepositoryModule.kt` | M02 | M10 在 provides 方法末尾追加 |

### 分支命名

```
mvp-Mxx-功能描述            # 单 stage: mvp-M12-local-file-source
mvp-Mxx-Mxx-功能描述        # 多 stage 合并: mvp-M06-M08-room-data-layer
```

### 提交格式

```
feat(Mxx): 阶段简要描述

产出物:
- 新增 xx 个文件
- 修改 xx 个聚合文件

验收: build/test passed
```

---

## Stage 生命周期

每个 Agent 在开始一个 Stage 时，按以下流程操作：

### 1. 取 Stage

```
1. 阅读本文件（AGENTS.md）确认项目约定
2. 查看 doc/mvp/AGENTS.md 进度表，确认前置 stage 已全部 ✅
3. 阅读 stage 文档（doc/mvp/Mxx-xxx.md）
4. 阅读 stage 文档中列出的依赖共享文档（doc/share/0X-xxx.md）
5. 在 doc/mvp/AGENTS.md 将 stage 标记为 🔵 进行中
```

### 2. 建 Worktree

```bash
git fetch origin main
git worktree add ../resource_viewer-Mxx -b mvp-Mxx-功能描述
cd ../resource_viewer-Mxx
```

### 3. 开发

**所有代码必须采用测试驱动开发 (TDD)**：

```
1. RED   — 先写失败的测试（描述期望行为）
2. GREEN — 写最小实现让测试通过
3. REFACTOR — 重构实现，保持测试绿色
```

详细 TDD 流程见 `doc/share/09-testing-conventions.md` §1。

```
- 🟢 创建独占文件 → 按 doc/share/07-directory-layout.md 放到正确位置
- 🟡 修改聚合文件 → 先在末尾追加，不修改已有代码
- 🔵 只读共享文档 → 不修改 doc/share/ 中的任何文件
- 遵循 doc/share/08-code-conventions.md 编码规范
- 遵循 doc/share/09-testing-conventions.md 编写测试
```

### 4. 验证

```bash
./gradlew build     # 必须零错误
./gradlew test      # 必须全部通过
./gradlew lint      # 必须无 error
```

### 5. 写决策日志

在仓库主目录（非 worktree）中：

```bash
cd ../resource_viewer
git pull origin main  # 获取最新 decisions/

# 创建 doc/decisions/Mxx-xxx.md（使用 TEMPLATE.md 模板）
# 更新 doc/decisions/cross-reference.md（追加自己涉及的组件）
```

### 6. 提交与合并

```bash
git add .
git commit -m "feat(Mxx): 阶段描述"
git push origin mvp-Mxx-功能描述
# 创建 MR / 直接 merge
```

### 7. 更新进度

```
- 在 doc/mvp/AGENTS.md 将 stage 标记为 ✅ 已完成
- 在 doc/decisions/AGENTS.md 更新对应行
```

### 8. 清理

```bash
git worktree remove ../resource_viewer-Mxx
cd ../resource_viewer
git pull origin main
```

---

## 并行规则

- 同一 Track 内的 Stage **串行**执行（按序依赖）
- 不同 Track **可并行**，前提是前置依赖已满足
- 并行窗口参考 `doc/mvp/AGENTS.md` 中的依赖图
- 聚合文件冲突时，后 merge 的 Agent 负责 rebase 解决

---

## 紧急情况

| 场景 | 处理 |
|------|------|
| 发现 `doc/share/` 中的契约有问题 | 不要直接修改。在 stage 决策日志中记录，MR 讨论 |
| rebase 时聚合文件冲突 | 联系上一个 stage 作者协调；不可强行覆盖 |
| 前置 stage 未完成但想开始 | 用 mock 接口先行开发，但**必须在决策日志中写明 mock 假设**，前置完成后验证 |
| 文件同时被两个 stage 修改 | 检查文件所有权标记——只有 🟡 聚合文件允许多 stage 修改，且必须串行合并 |

---

## 架构速览

```
UI Layer (Compose Screen ↔ ViewModel via StateFlow)
    ↓
Domain Layer (Domain Model + UseCase)
    ↓
Data Layer (Repository → DAO / FileSource / Media)
```

核心抽象：
- **FileSource** 接口 — 统一本地/SMB/未来协议的文件访问
- **ContentProvider** 接口 — 查看器内容抽象（ImageFolder/Pdf/Archive）
- **OrganizationStrategy** 接口 — 四种组织模式策略
- **ThumbnailGenerator** 接口 — 按类型生成缩略图
