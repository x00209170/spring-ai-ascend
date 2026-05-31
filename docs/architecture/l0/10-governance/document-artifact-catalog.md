---
level: L1
view: governance
status: draft
---

# Document Artifact Catalog

## 目的

登记 `docs/architecture/` 的目录级职责，说明每个目录主要承载什么内容、服务什么 A2D 活动、下级文档如何展开和如何检查质量。

本文先管理目录，不直接管理所有文件。原因是不同目录的展开方式不同，尤其 `04-modules/<module>/` 下每个模块可能拥有自己的文件组织方法。文件级清单应由对应目录的 README 或模块级 catalog 承担。

## 适用读者

架构负责人、模块负责人、文档作者、评审者、AI agent、harness 生成器。

## 维护规则

- 每个 `docs/architecture/` 下的一级目录必须在本文登记。
- 重要的二级目录如果有独立职责，也必须在本文登记，例如 `04-modules/<module>/`、`05-contracts/human-readable/`、`02-scenarios/technical/`。
- 本文只定义目录级主职责；目录内的文件级管理由该目录 README、模块 README 或模块级 catalog 继续展开。
- 新增、删除、改名目录，或改变目录职责时，必须同步更新本文。
- 如果某个文件找不到清晰归属，先回到本文判断它应该属于哪个目录；如果没有合适目录，再判断是否需要新增目录。
- 如果某个模块需要特殊文件组织方式，必须在该模块 README 中说明，不要求强行套用其他模块的文件结构。

## 字段说明

| 字段 | 含义 |
|---|---|
| 目录 / 文件组 | 被管理的目录，或根目录下必须单独说明的一组文件。 |
| 管理粒度 | 本文管理到目录、文件组、模板目录还是具体文件。 |
| 主要内容 | 该目录应该承载的信息范围。 |
| 主要作用 | 该目录在 A2D 或评审中的用途。 |
| A2D 活动 | 主要由哪些 A2D 活动产出或维护。 |
| 下级展开规则 | 目录内部如何继续管理文件。 |
| 质量检查点 | 评审时需要重点检查的约束。 |

## 目录总览

| 目录 / 文件组 | 管理粒度 | 主要内容 | 主要作用 | A2D 活动 | 下级展开规则 | 质量检查点 |
|---|---|---|---|---|---|---|
| 根目录文件组：`README.md`、`task.md`、`constraint-and-design-inventory.md` | 文件组 | 文档集入口、原始任务输入、全局约束和冲突清单。 | 提供文档体系入口、raw input 暂存和跨目录问题索引。 | H0 原始输入 / AI-1 需求归一化 / AI-9 版本归档 | 根目录只保留全局入口和全局索引；正式设计应迁移到对应目录。 | `task.md` 不得被当成 accepted 设计；全局 Conflict / Open Issue 必须有后续位置或 owner。 |
| `00-overview/` | 目录 | L0 系统概览、术语、系统原则和运行架构心智模型。 | 帮助读者先理解系统运行架构，而不是实现细节。 | AI-4 准入判定 / AI-4 模块责任承接 / AI-9 版本归档 | 目录内可以按 overview、glossary、principles 展开；新增文件必须仍服务 L0 心智模型。 | 不得把 BoM、starter、demo、fixture 或依赖管理写成 L0 架构模块。 |
| `01-capabilities/` | 目录 | 能力地图、能力 owner、能力与场景/模块/验证方式的映射。 | 检查核心场景是否有能力承接，并支撑模块并行开发。 | AI-4 能力拆解 | 先维护总能力地图；能力复杂到需要独立生命周期时，再增加 `<capability-id>.md`。 | 能力不得伪装成模块；每个关键能力必须有 owner 或 Open Issue。 |
| `04-modules/` | 目录 | 模块责任总览和各模块设计包入口。 | 管理模块边界、模块设计包和并行开发入口。 | AI-4 模块责任承接 / AI-4 模块详细设计 | 根下维护 `module-responsibility-cards.md`；每个模块使用 `04-modules/<module>/` 自己展开。 | 主键必须是真实模块；支撑框架、依赖、starter 不得作为 L0 模块。 |
| `04-modules/<module>/` | 模板目录 | 单个模块的 README、设计、状态、流程、开发视图、harness、open issues 或其他模块自定义文档。 | 让模块负责人把系统级职责转成可开发、可验证的模块设计包。 | AI-4 模块详细设计 / AI-6 交付视图 | 每个模块必须至少有 README 说明本模块文件管理方法；是否使用 4+1、state model、process design 等文件由模块复杂度决定。 | 模块文件结构可以不同，但必须能追溯到场景、能力、状态、契约、harness 和验证矩阵。 |
| `04-modules/agent-service/` | 当前模块目录 | agent-service 当前模块设计包兼容入口。 | 指向 `l1/agent-service/` 的权威 L1 4+1 架构，避免旧链接失效。 | AI-4 模块详细设计 / AI-6 交付视图 | 文件级索引由该目录 README 维护；后续新内容直接进入 L1 对应子目录。 | 必须遵守 Task 是服务端状态、Run 仅为历史或 client 视角兼容的口径；不得越界承担 Bus 或 Gateway 职责。 |
| `06-state/` | 目录 | 状态 owner、writer、reader、forbidden writer 和状态边界。 | 作为状态一致性和跨模块写入边界的主索引。 | AI-4 状态与契约设计 | 当前以状态矩阵为主；状态模型细节可下沉到模块目录。 | 每个核心状态只能有一个 owner；禁止隐式多写。 |
| `03-adrs/` | 目录 | 交付视角 ADR 草案和待提升决策。 | 记录当前 A2D 过程中的设计决策草案，不替代正式 `docs/adr/`。 | AI-4 状态与契约设计 / AI-9 版本归档 | 每个草案独立成文；提升为正式决策时迁移或同步到权威 ADR 目录。 | 必须诚实标记 draft；不得把草案写成已 runtime enforced。 |
| `05-contracts/` | 目录 | 人类可读 ICD 和机器可读 contract draft。 | 管理跨模块交互契约，并支撑 mock、stub、contract test 和 harness。 | AI-4 状态与契约设计 / AI-6 交付视图 | 下分 human-readable 与 machine-readable；同一契约应保持语义配对。 | 机器可读 YAML 必须是 harness-first draft，进入生产前必须同步正式 contract catalog。 |
| `05-contracts/human-readable/` | 二级目录 | 人类可读 ICD、交互语义、错误语义、调用方向和边界说明。 | 让人类先确认契约语义，再生成机器可读草案。 | AI-4 状态与契约设计 | 每个 ICD 可以对应一个或多个 machine-readable draft；复杂契约可拆分多个 ICD。 | 必须说明参与模块、控制路径、数据路径、状态影响和 open issues。 |
| `05-contracts/machine-readable/` | 二级目录 | YAML contract draft、mock/stub/test 生成输入。 | 为 harness-first 验证提供机器可读素材。 | AI-4 状态与契约设计 / AI-6 交付视图 | 每个 YAML 必须能追溯到 human-readable ICD；字段语义以 ICD 为准。 | 必须保持 `status: draft`；不得声明 production enforcement。 |
| `02-scenarios/` | 目录 | BA-* 业务活动场景、场景索引和场景管理规则。 | 用真实业务活动检验模块划分、能力覆盖和契约完整性。 | AI-4 场景建模 | BA-* 是主线；technical 子场景只能作为机制验证。 | 核心场景必须是业务活动，不能只列技术流程。 |
| `02-scenarios/technical/` | 二级目录 | 技术机制子场景，例如创建 Task、执行 step、上下文装配、工具调用、暂停恢复、A2A federation。 | 支撑 BA 场景下的机制级验证和 harness 生成。 | AI-4 场景建模 / AI-6 交付视图 | 每个 technical scenario 必须挂到至少一个 BA-*，或明确标记为 future / open issue。 | 不得替代 BA-* 成为核心场景；历史 Run 命名必须说明当前 Task 口径。 |
| `07-invariants/` | 目录 | 架构不变量、禁止路径和可检查约束。 | 支撑静态检查、评审和 harness 断言。 | AI-4 状态与契约设计 / AI-8 集成验证与漂移检查 | 当前以总不变量清单为主；模块特有不变量可下沉到模块目录。 | 不变量必须可验证，并能追踪到场景、状态、契约或原则。 |
| `08-harness/` | 目录 | 系统级或跨模块 harness 规格。 | 将场景、状态、契约和不变量转成可执行验证思路。 | AI-6 交付视图 | 按能力或跨模块机制组织；模块专属 harness 优先放到模块目录。 | 每个 harness spec 必须能追溯到场景、契约、状态或 verification row。 |
| `09-verification/` | 目录 | 验证矩阵、测试策略和验证证据索引。 | 作为设计项到测试、评审、CI 或人工验证的追踪入口。 | AI-6 交付视图 / AI-8 集成验证与漂移检查 | verification matrix 是主索引；test strategy 说明测试组织方式。 | 每个关键设计项必须有验证方式或未覆盖说明。 |
| `10-governance/` | 目录 | A2D 工作模型内核、目录 catalog、文档约束、评审流程、变更治理、角色边界、双视图模型和阶段契约。 | 管理文档体系本身，约束 AI 与人类协作方式，并提供从版本意图到自动实现的集中检查点。 | H1 版本架构边界确认 / H2 架构审核包确认 / H3 发布例外审核 | 治理文件可以继续按阶段契约、版本意图、版本架构边界、审核包、交付视图、基线等子目录展开。 | 过程发现的新文档问题必须沉淀为约束或 catalog 更新；AI 不得把每个需求都升级成人工审批点。 |
| `10-governance/a2d-phases/` | 目录 | AI 阶段契约文件，每个文件定义一个 AI 阶段的操作步骤、输入输出和退出条件。 | 让 AI 按需加载当前阶段的契约，而不是一次性读取完整流程手册。 | AI-1 ~ AI-9 各阶段 | 每个阶段契约使用统一模板：应该做什么、输入、输出、退出条件、何时停下问人、反模式。 | 阶段契约不得与 `CLAUDE.md` 工程规则冲突；升级条件必须与版本架构边界一致。 |
| `10-governance/a2d-human-checkpoints.md` | 文件 | 人类视角的 A2D 检查点流程，只说明"人类输入什么、获得什么、确认什么"。 | 给人类看的简化流程，隐藏 AI 内部阶段细节。 | H0 ~ H3 所有检查点 | 无下级展开。 | 不得在人类流程中暴露 AI 内部阶段拆分。 |
| `10-governance/a2d-roles.md` | 文件 | 角色与协作边界，定义人类和 AI 在 A2D 中的责任划分。 | 明确谁做什么、谁不做什么、何时由人类裁决。 | 所有阶段 | 无下级展开。 | 角色边界不得与 `CLAUDE.md` 治理规则矛盾。 |
| `10-governance/a2d-views.md` | 文件 | 双视图模型、视图验收口径和最小 A2D 工作包。 | 确保 A2D 产物同时服务交付和评审两个视角，且不维护两套事实。 | AI-5 / AI-6 / H2 | 无下级展开。 | 两个视图必须引用同一批 A2D 已接受产物。 |
| `10-governance/layer-update-protocol.md` | 文件 | 多层更新协议，定义 L0/L1/L2 联动工作顺序、层间一致性和审批边界。 | 当变更同时影响多层时，提供工作编排规则，防止跳层更新或层间不一致。 | H0 ~ H3 所有检查点 | 无下级展开。 | 不得跳层更新；下层发现冲突必须上报。 |
| `10-governance/version-intents/` | 预留目录 | 版本目标、功能需求、非功能需求、非目标、约束、风险预算和发布门槛。 | 保存人类对版本的集中输入，作为 AI 批量分析和建模的入口。 | H0 人类注入版本意图 / AI-1 需求归一化与分流 | 按 `<version>.md` 建档；同一版本的后续版本架构边界、审核包、交付视图必须回链到该文件。 | 不得要求人类先写完整设计；必须明确范围、非目标、约束和发布门槛。 |
| `10-governance/architecture-envelopes/` | 预留目录 | AI 自动推进边界、允许变更、禁止变更、兼容性策略、验证要求和升级条件。 | 定义检查点通过后 AI 可以自动推进的版本级边界。 | AI-3 生成版本架构边界 / H1 版本架构边界确认 | 按 `<version>.md` 建档；必须引用版本意图和架构影响矩阵。 | 边界外变化必须升级；不得在触发升级条件后继续自动实现。 |
| `10-governance/review-packets/` | 预留目录 | 4+1 评审视图、决策摘要、影响面、变更级别、开放问题和验证增量。 | 为 H2 提供人类可审阅的架构包，避免人工阅读全部 A2D 原始文档。 | AI-5 生成 4+1 评审视图 / H2 架构审核包确认 | 按 `<version>.md` 建档；必须回链到产生它的 A2D 已接受产物。 | 不得维护另一套事实；必须能追溯到场景、能力、模块卡、状态矩阵、ICD、不变量、harness 或验证矩阵。 |
| `10-governance/delivery-projections/` | 预留目录 | 开发切片、实现任务草案、DoD、harness 计划、测试计划和验证证据索引。 | 为自动实现和开发协作提供可执行交付计划。 | AI-6 生成交付视图 / AI-7 自动实现、文档回写、测试补齐 | 按 `<version>.md` 建档；任务必须能追溯到版本架构边界和验证矩阵。 | 不得生成脱离版本架构边界的实现任务；每个关键任务必须有 DoD 和验证方式。 |
| `10-governance/a2d-intake/` | 预留目录 | 结构化需求入口记录。 | 保存从 raw input 进入 A2D 的正式入口。 | H0 原始输入 / AI-1 需求归一化 | 当前可先用 `task.md` 过渡；稳定后按 `<id>.md` 建档。 | 每份 intake 必须说明用户、目标、约束、进入判断和澄清问题。 |
| `10-governance/admission-decisions/` | 预留目录 | 准入分类和处理结论。 | 保存 Architecture Module / Capability / Contract / State 等分类判断。 | AI-4 准入判定 | 当前可先在 inventory 中记录；稳定后按 `<id>.md` 建档。 | 每个被纳入 Overview 或模块边界的对象必须有准入依据。 |
| `10-governance/baselines/` | 预留目录 | 阶段性架构基线说明。 | 管理版本归档、替代关系和遗留问题结转。 | AI-9 版本归档 | 当前可先在 README 或 PR 中记录；稳定后按 `<version>.md` 建档。 | 基线必须说明当前有效、草稿、已替代和结转开放问题。 |
| `l1/<service>/` | 服务架构目录 | 单个服务的 L1 4+1 架构、服务级约束、服务级开放问题。 | 承接 L0 服务边界，并把服务内部逻辑、流程、部署、开发视图和场景结构化。 | H2 架构审核包确认 / AI-4 模块详细设计 / AI-8 集成验证与漂移检查 | 每个服务必须至少包含 README 和 4+1 视图文件；复杂服务可以增加 `contracts.md`、`verification.md`、`open-issues.md`。 | L1 可以细化 L0，但不得改写 L0；必须声明继承的 L0 边界、状态归属和跨服务契约。 |
| `l1/<service>/l2/<topic>/` | 服务内专题目录 | 单服务 L2 技术专题设计。 | 细化某个 L1 服务视图、边界合同或关键机制。 | AI-4 模块详细设计 / AI-6 交付视图 / AI-8 集成验证与漂移检查 | 每个专题按需要维护 README、design、contracts、verification、open-issues。 | 必须声明父级 L1 服务和被细化的 L1 视图；不得引入违反 L1 的依赖、状态 owner 或调用路径。 |

## 下级展开规则

### 系统级目录

`00-overview/`、`01-capabilities/`、`06-state/`、`03-adrs/`、`05-contracts/`、`02-scenarios/`、`07-invariants/`、`08-harness/`、`09-verification/`、`10-governance/`、`l1/` 是系统级目录。

系统级目录的展开规则：

- 目录职责由本文定义。
- 文件级索引优先放在该目录 README；没有 README 时，由目录中主文档承担入口职责。
- 新增文件必须说明它补充哪个 A2D 产物，不能只是因为“有内容可写”而新增。
- 当目录内文件数量或类型变多时，可以新增目录级 README 或局部 catalog。

### 模块级目录

`04-modules/<module>/` 是模块级目录。模块级目录可以拥有自己的文件管理方法。

模块级目录的最低要求：

- 必须有 README，说明本模块设计包的文件地图和阅读顺序。
- 必须说明哪些文件是当前 accepted，哪些是 draft，哪些只是迁移参考。
- 必须能追溯到系统级场景、能力、状态、契约、harness 和 verification matrix。
- 如果模块内部文件结构与 `agent-service` 不同，只要 README 说明清楚即可，不要求强制一致。

### 文件级 catalog 的触发条件

只有在以下情况下，才需要继续打开文件级 catalog：

- 某个目录下文件数量较多，README 已不足以说明职责边界。
- 某个模块存在多条并行开发线，需要明确每个文件的 owner、状态和验证关系。
- 评审中多次出现文件职责漂移、重复承载或归档位置不清。
- AI 需要基于目录内文件自动生成 harness、任务或评审报告，而 README 信息不足。

文件级 catalog 应放在对应目录内，例如：

```text
docs/architecture/l0/04-modules/<module>/document-artifact-catalog.md
docs/architecture/l0/02-scenarios/document-artifact-catalog.md
```

## 反模式

- 顶层 catalog 直接展开所有文件，导致模块无法使用自己的文件管理方法。
- 模块目录没有 README，却依赖全局 catalog 解释模块内部文件。
- 新增目录没有说明主职责，只是把相似文件堆在一起。
- 技术子场景、契约草案、harness spec 找不到所属 BA 场景或 A2D 活动。
- 目录职责改变后，只改文件内容，不更新本文。
