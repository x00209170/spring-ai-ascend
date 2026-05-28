---
level: L1
view: governance
status: draft
---

# A2D Working Model

## 目的

定义从 Architecture 到 Delivery 的协作方法，明确每个管理活动的触发条件、输入、流程、产出物、责任人、归档位置和退出标准。

A2D 不是目录结构，也不是一批静态文档。A2D 是一组管理活动：

```text
需求进入 -> 架构准入 -> 场景建模 -> 能力拆解 -> 模块承接
-> 状态与契约 -> 模块设计 -> harness 设计 -> 实现任务
-> 集成验证 -> 版本归档
```

目录只负责归档活动产物。每个活动必须先定义“要产出什么”，再定义“这个产物归档到哪里”。

## 适用读者

业务负责人、架构负责人、模块负责人、开发者、评审者、AI agent、harness 生成器。

## 维护规则

- 每个 A2D 活动必须使用统一结构：触发条件、输入、流程、产出物与归档、退出标准。
- “产出物与归档”必须逐项对齐，不得出现产出物没有归档位置，或归档位置没有对应产出物。
- AI 可以生成草稿、整理冲突、维护追踪矩阵和提出 harness 建议，但不能替代人类做架构裁决。
- 架构裁决必须由架构负责人或对应模块负责人确认后才能进入 baseline。
- 新增活动、产物或归档位置时，必须同步更新本文档和 [README](../README.md) 的文档地图。
- 如果活动产物与权威来源冲突，以 `CLAUDE.md`、`docs/adr/`、`docs/governance/architecture-status.yaml` 和正式 contract catalog 为准。

## 角色与责任

| 角色 | 核心责任 | 不负责 |
|---|---|---|
| 业务负责人 | 提供业务目标、用户类型、关键场景、优先级和业务验收口径。 | 不裁决模块边界和状态 owner。 |
| 架构负责人 | 裁决模块边界、状态归属、跨模块契约、架构约束和变更级别。 | 不替代模块负责人完成模块内部设计。 |
| 模块负责人 | 细化模块职责、内部流程、状态机、接口语义、开发切片和 harness。 | 不单方面改变 L0/L1 架构边界。 |
| 开发者 | 根据已确认的设计包实现代码、测试和必要的验证证据。 | 不在没有设计输入时直接创造跨模块语义。 |
| 评审者 | 检查架构一致性、可实现性、可验证性和风险覆盖。 | 不把评审评论当成唯一归档位置。 |
| AI agent | 整理输入、生成草稿、发现冲突、维护追踪、生成测试建议和任务草案。 | 不做最终业务承诺、架构裁决或发布审批。 |

## 产物状态

| 状态 | 含义 | 可用于开发吗 | 需要谁确认 |
|---|---|---|---|
| raw_input | 原始输入，可能不完整、冲突或未经分类。 | 否。 | 输入提供者确认背景。 |
| draft | 已整理但未裁决的草稿。 | 只能用于讨论。 | 相关负责人确认后才能升级。 |
| reviewed | 已评审，未必成为当前 baseline。 | 可用于探索性实现或 harness 草案。 | 评审者和相关负责人确认。 |
| accepted | 当前交付基线。 | 是。 | 架构负责人和模块负责人确认。 |
| superseded | 已被新版本替代。 | 否，除非明确用于迁移。 | 架构负责人确认替代关系。 |

## 归档位置约定

| 产物类型 | 默认归档位置 | 说明 |
|---|---|---|
| 原始需求记录 | [task.md](../task.md) | 早期讨论、未结构化输入、临时记录。 |
| A2D Intake Record | `docs/architecture/10-governance/a2d-intake/<id>.md` | 结构化需求入口。 |
| Admission Decision | `docs/architecture/10-governance/admission-decisions/<id>.md` | 准入分类和处理结论。 |
| Conflict / Open Issue | [constraint-and-design-inventory.md](../constraint-and-design-inventory.md) 或模块 `open-issues.md` | 全局冲突进 inventory，模块内部问题进模块目录。 |
| Business Activity Scenario | `docs/architecture/06-scenarios/BA-xxx-*.md` | 系统级业务活动场景。 |
| Technical Scenario | `docs/architecture/06-scenarios/technical/Sx-*.md` | 技术机制子场景。 |
| Module Scenario Projection | `docs/architecture/02-modules/<module>/scenarios/<scenario-id>.md` | 模块视角承接系统场景的一部分。 |
| Capability Map / Capability Detail | [capability-map.md](../01-capabilities/capability-map.md) 或 `docs/architecture/01-capabilities/<capability-id>.md` | 能力总览和能力细化。 |
| Module Responsibility Card | [module-responsibility-cards.md](../02-modules/module-responsibility-cards.md) | 模块职责总览。 |
| Module Development Pack | `docs/architecture/02-modules/<module>/` | 模块详细设计包。 |
| State Ownership Matrix | [state-ownership-matrix.md](../03-state/state-ownership-matrix.md) | 状态 owner、writer、reader、forbidden writer。 |
| Human-readable ICD | `docs/architecture/05-contracts/human-readable/*.md` | 人类可读交互契约。 |
| Machine-readable Contract Draft | `docs/architecture/05-contracts/machine-readable/*.yaml` | harness-first 机器可读草案。 |
| Harness Design | `docs/architecture/08-harness/*.md` 或 `docs/architecture/02-modules/<module>/harness-design.md` | 系统级或模块级 harness 设计。 |
| Verification Matrix Row | [verification-matrix.md](../09-verification/verification-matrix.md) | 设计项到验证证据的追踪。 |
| Baseline Note | `docs/architecture/10-governance/baselines/<version>.md` | 阶段性版本归档。 |

## A0 需求进入

### 触发条件

出现新的业务目标、用户类型、部署形态、模块需求、关键技术约束或评审发现。

### 输入

| 输入 | 提供者 | 说明 |
|---|---|---|
| 业务目标 | 业务负责人 | 用户希望完成什么业务活动。 |
| 用户类型 | 业务负责人 | 强部门、弱部门、企业个人、平台运营方等。 |
| 使用形态 | 业务负责人 / 架构负责人 | 本地 client、平台托管 service、混合部署、跨部门协作等。 |
| 已知约束 | 架构负责人 / 模块负责人 | 数据不出域、成本统计、发布审批、鉴权体系、A2A 控制方式等。 |
| 不确定问题 | 任意参与者 | 需要后续澄清的问题。 |

### 流程

1. 输入提供者提出原始需求。
2. AI 整理成结构化 intake，明确“谁、在什么场景、为了什么目标、受什么约束”。
3. 架构负责人判断是否进入 A2D。
4. 输入不足时，AI 生成澄清问题；输入足够时，进入 A1。

### 产出物与归档

| 环节 | 产出物 | 责任人 | 状态 | 归档位置 |
|---|---|---|---|---|
| 原始需求记录 | raw_input | 输入提供者 | raw_input | [task.md](../task.md) |
| 结构化需求入口 | A2D Intake Record | AI agent 起草，业务负责人确认 | draft / accepted | `docs/architecture/10-governance/a2d-intake/<id>.md` |
| 进入判断 | A2D Entry Decision | 架构负责人 | accepted | 同一份 A2D Intake Record 的“进入判断”章节 |
| 澄清问题 | Clarification Questions | AI agent 起草，输入提供者回答 | draft / accepted | 同一份 A2D Intake Record 的“澄清问题”章节 |

### 退出标准

- 需求能用一句话说明业务活动。
- 用户类型和部署形态明确，或被列为澄清问题。
- 已知约束和未决问题被显式记录。
- 架构负责人确认该需求是否进入 A2D。

## A1 架构准入判定

### 触发条件

有新项准备进入 Overview、Capability Map、Module Cards、Scenario、Contract、State Matrix 或模块设计包。

### 输入

| 输入 | 提供者 | 说明 |
|---|---|---|
| A2D Intake Record | AI agent | A0 的结构化输出。 |
| 现有架构文档 | AI agent | 当前 `docs/architecture/` 内容。 |
| 权威来源 | AI agent | `CLAUDE.md`、`docs/adr/`、`docs/governance/*`、正式 contract。 |
| 代码结构 | AI agent / 开发者 | root `pom.xml`、模块 metadata、已有实现。 |
| 分类判断 | 架构负责人 | 对分类和边界的最终裁决。 |

### 流程

1. AI 对每个待处理项做准入分类。
2. 分类集合固定为：

```text
Architecture Module
Runtime Component
Capability
Contract
State
Build Artifact
Packaging Artifact
Implementation Constraint
Open Issue
Reject
```

3. 架构负责人确认分类。
4. 只有 `Architecture Module` 和必要的 `Runtime Component` 可以进入 L0 Overview 的模块边界。
5. `Build Artifact`、BoM、starter、dependency management、test fixture、demo、adapter scaffold 不得作为 L0 架构模块出现。
6. AI 按分类把内容归档到对应位置。

### 产出物与归档

| 环节 | 产出物 | 责任人 | 状态 | 归档位置 |
|---|---|---|---|---|
| 准入分类 | Admission Decision | AI agent 起草，架构负责人确认 | draft / accepted | `docs/architecture/10-governance/admission-decisions/<id>.md` |
| 分类结果汇总 | Admission Classification Table | AI agent | draft / accepted | 同一份 Admission Decision |
| 冲突记录 | Conflict Record | AI agent 起草，架构负责人确认 | draft / accepted | [constraint-and-design-inventory.md](../constraint-and-design-inventory.md) |
| 文档准入规则更新 | Documentation Constraint Update | AI agent 起草，架构负责人确认 | draft / accepted | [architecture-documentation-constraints.md](architecture-documentation-constraints.md) |
| 文档迁移结果 | Updated Target Documents | AI agent | draft / accepted | 被迁移项对应的目标文档，例如 Overview、Capability、Module、State、Contract 或 Scenario |

### 退出标准

- 每个待处理项都有分类。
- 被写入 Overview 的内容只用于理解系统运行架构。
- 构建、依赖、发布、fixture 类内容被下沉到实现约束、build governance 或附录。
- 分类冲突进入 Conflict Record 或 Open Issue。

## A2 核心场景建模

### 触发条件

需要验证模块划分是否合理、能力是否齐备、关键用户活动是否可被系统支撑。

### 输入

| 输入 | 提供者 | 说明 |
|---|---|---|
| A2D Intake Record | AI agent | 需求背景和约束。 |
| 用户画像 | 业务负责人 | 强部门、弱部门、企业个人、运营方等。 |
| 部署形态 | 架构负责人 | 本地能力、平台托管、混合部署、跨部门 A2A。 |
| 关键业务活动 | 业务负责人 | 用户真实完成的业务活动。 |
| 已知技术机制 | 模块负责人 | SSE、Bus、A2A、数据引用、鉴权、成本统计等。 |

### 流程

1. 业务负责人描述真实业务活动。
2. AI 生成 BA 场景草稿，优先描述用户目标、开发态体验、运行态体验和异常路径。
3. 架构负责人确认场景是否足以串起多个架构模块。
4. 模块负责人补充本模块在场景中的输入、输出、状态变化和观测要求。
5. AI 抽取 technical sub-scenario，用于描述可测试机制。

### 产出物与归档

| 环节 | 产出物 | 责任人 | 状态 | 归档位置 |
|---|---|---|---|---|
| 业务活动建模 | Business Activity Scenario | AI agent 起草，业务负责人和架构负责人确认 | draft / accepted | `docs/architecture/06-scenarios/BA-xxx-*.md` |
| 技术机制拆解 | Technical Scenario | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/06-scenarios/technical/Sx-*.md` |
| 模块场景投影 | Module Scenario Projection | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/scenarios/<scenario-id>.md` |
| 场景未决问题 | Scenario Open Issue | AI agent 起草，负责人确认 | draft / accepted | 场景文件的“Open Issues”章节；跨场景问题同步到 [constraint-and-design-inventory.md](../constraint-and-design-inventory.md) |

### 退出标准

- 场景是业务活动，不只是功能清单。
- 场景能串起多个模块、状态、契约和观测要求。
- 开发态和运行态关注点被显式说明。
- 未决技术问题进入 Scenario Open Issue，而不是隐藏在正文里。

## A3 能力拆解

### 触发条件

系统级场景已经成立，需要判断平台必须提供哪些能力，以及能力由哪些模块承接。

### 输入

| 输入 | 提供者 | 说明 |
|---|---|---|
| Business Activity Scenario | AI agent | A2 产物。 |
| Technical Scenario | AI agent / 模块负责人 | 机制级验证场景。 |
| Overview 模块边界 | 架构负责人 | 当前 L0 模块边界。 |
| 现有 Capability Map | AI agent | 已有能力定义。 |

### 流程

1. AI 从 BA 场景抽取能力候选。
2. 架构负责人判断能力是否属于平台核心能力。
3. 模块负责人确认本模块是否提供、消费或协同该能力。
4. AI 建立场景、能力、模块、状态、契约之间的追踪关系。

### 产出物与归档

| 环节 | 产出物 | 责任人 | 状态 | 归档位置 |
|---|---|---|---|---|
| 能力候选抽取 | Capability Candidate List | AI agent | draft | [capability-map.md](../01-capabilities/capability-map.md) 的候选或待确认章节 |
| 能力准入裁决 | Capability Admission | 架构负责人 | accepted | [capability-map.md](../01-capabilities/capability-map.md) |
| 能力细化 | Capability Detail | AI agent 起草，架构负责人确认 | draft / accepted | `docs/architecture/01-capabilities/<capability-id>.md` |
| 模块承接关系 | Capability Ownership Mapping | AI agent 起草，模块负责人确认 | draft / accepted | [capability-map.md](../01-capabilities/capability-map.md) |
| 场景追踪关系 | Capability-to-Scenario Mapping | AI agent | draft / accepted | [capability-map.md](../01-capabilities/capability-map.md) |

### 退出标准

- 每个关键场景至少能追踪到一个能力。
- 每个核心能力都有 owner 或明确 Open Issue。
- 能力没有被误写成真实架构模块。
- 能力和模块之间的 provided / consumed / collaborated 关系明确。

## A4 模块责任承接

### 触发条件

能力已经确认，需要落到具体模块职责、非职责和协作边界。

### 输入

| 输入 | 提供者 | 说明 |
|---|---|---|
| Capability Map | AI agent | 能力和 owner 候选。 |
| Architecture Overview | 架构负责人 | L0 模块边界。 |
| BA / Technical Scenario | AI agent | 场景驱动的模块参与关系。 |
| ADR / 既有设计 | AI agent | 已确认的架构决策。 |

### 流程

1. AI 生成或更新模块责任卡。
2. 模块负责人确认职责、非职责、输入、输出和下游依赖。
3. 架构负责人裁决跨模块边界冲突。
4. AI 更新模块责任卡、模块 README 和冲突清单。

### 产出物与归档

| 环节 | 产出物 | 责任人 | 状态 | 归档位置 |
|---|---|---|---|---|
| 模块责任定义 | Module Responsibility Card | AI agent 起草，模块负责人确认 | draft / accepted | [module-responsibility-cards.md](../02-modules/module-responsibility-cards.md) |
| 模块入口更新 | Module Pack README | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/README.md` |
| 边界裁决 | Module Boundary Decision | 架构负责人 | accepted | 对应 Module Responsibility Card 的“边界裁决”章节 |
| 边界冲突 | Module Boundary Conflict | AI agent 起草，架构负责人确认 | draft / accepted | [constraint-and-design-inventory.md](../constraint-and-design-inventory.md) |

### 退出标准

- 每个模块都有职责、非职责、状态责任和协作边界。
- 支撑框架、依赖、starter、BoM 不被写成模块。
- 同类 service 是否互相调度、是否进程内闭环等关键边界明确。
- 下游设计可以基于模块责任卡继续展开。

## A5 状态与契约设计

### 触发条件

模块之间出现状态流转、跨模块调用、消息传递、回调、SSE 输出、Bus 控制指令或数据引用。

### 输入

| 输入 | 提供者 | 说明 |
|---|---|---|
| Module Responsibility Card | AI agent | 模块职责和非职责。 |
| Scenario | AI agent | 状态和契约发生的位置。 |
| 状态候选 | AI agent / 模块负责人 | Task、Session、Agent、Tool Call、Approval、Trace 等。 |
| 交互语义 | 模块负责人 | 调用方向、同步/异步、错误语义、重试 owner。 |

### 流程

1. AI 从场景和模块责任中抽取状态对象。
2. 架构负责人确认状态 owner、writer、reader 和 forbidden writer。
3. 模块负责人定义状态流转和交互语义。
4. AI 生成 human-readable ICD、machine-readable contract draft 和状态矩阵更新。
5. 评审者检查是否存在多 owner、隐式写入、契约缺口或状态漂移。

### 产出物与归档

| 环节 | 产出物 | 责任人 | 状态 | 归档位置 |
|---|---|---|---|---|
| 状态候选抽取 | State Candidate List | AI agent | draft | [state-ownership-matrix.md](../03-state/state-ownership-matrix.md) 的候选或待确认章节 |
| 状态归属裁决 | State Ownership Decision | 架构负责人 | accepted | [state-ownership-matrix.md](../03-state/state-ownership-matrix.md) |
| 人类可读契约 | Human-readable ICD | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/05-contracts/human-readable/*.md` |
| 机器可读契约草案 | Machine-readable Contract Draft | AI agent 起草，模块负责人确认 | draft | `docs/architecture/05-contracts/machine-readable/*.yaml` |
| 不变量更新 | Architecture Invariant Update | AI agent 起草，架构负责人确认 | draft / accepted | [architecture-invariants.md](../07-invariants/architecture-invariants.md) |
| 契约或状态问题 | State / Contract Open Issue | AI agent 起草，负责人确认 | draft / accepted | 相关 ICD 或 [constraint-and-design-inventory.md](../constraint-and-design-inventory.md) |

### 退出标准

- 每个核心状态只有一个 owner。
- writer、reader、forbidden writer 明确。
- 每个跨模块交互都有契约或 Open Issue。
- 契约语义足以生成 mock、stub、contract test 或 harness 断言。

## A6 模块详细设计

### 触发条件

某个模块需要进入并行开发，或已有 L1/L2 设计需要迁移到当前 A2D 文档体系。

### 输入

| 输入 | 提供者 | 说明 |
|---|---|---|
| Module Responsibility Card | AI agent | A4 产物。 |
| State Ownership / ICD | AI agent | A5 产物。 |
| Scenario | AI agent | 系统级和模块投影场景。 |
| 既有 L1/L2 设计 | 模块负责人 / AI agent | 需要迁移或对照的设计文档。 |
| 代码结构 | 开发者 / AI agent | 当前实现约束。 |

### 流程

1. AI 读取既有设计和当前架构基线。
2. AI 对旧设计逐项做准入判定：保留、修正、迁移、下沉、废弃或列为 Open Issue。
3. 模块负责人确认模块内部逻辑、状态机、流程、开发视图和开发切片。
4. 架构负责人检查是否越过 L0/L1 边界。
5. AI 生成模块设计包和 4+1 视图。

### 产出物与归档

| 环节 | 产出物 | 责任人 | 状态 | 归档位置 |
|---|---|---|---|---|
| 旧设计准入 | Accepted Design Map | AI agent 起草，架构负责人和模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/accepted-design-map.md` |
| 模块逻辑设计 | Logical Design | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/logical-design.md` |
| 模块状态模型 | State Model | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/state-model.md` |
| 模块流程设计 | Process Design | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/process-design.md` |
| 模块开发视图 | Development View | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/development-view.md` |
| 模块 4+1 视图 | 4+1 View | AI agent 起草，架构负责人和模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/4plus1-view.md` |
| 开发切片 | Development Slices | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/development-slices.md` |
| 模块开放问题 | Module Open Issues | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/open-issues.md` |
| 模块入口 | Module Pack README | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/README.md` |

### 退出标准

- 模块负责人能根据设计包拆任务。
- 开发者能理解输入、输出、状态机、异常和观测要求。
- harness 生成器能从设计包生成测试建议。
- 未决问题明确是否阻塞当前开发切片。

## A7 Harness 设计与生成

### 触发条件

模块设计包已经形成，需要把架构约束转为可执行验证。

### 输入

| 输入 | 提供者 | 说明 |
|---|---|---|
| Module Development Pack | AI agent | A6 产物。 |
| ICD / Contract Draft | AI agent | A5 产物。 |
| State Model | 模块负责人 | 状态流转和禁止路径。 |
| Scenario Assertions | AI agent | BA 和 technical scenario 中的 assertions。 |
| Failure Paths | 模块负责人 / 评审者 | 需要注入的异常和边界条件。 |

### 流程

1. AI 从场景、状态、契约生成 harness 候选。
2. 模块负责人确认哪些属于 unit、contract、integration、scenario、regression。
3. 评审者确认关键风险是否覆盖。
4. 开发者实现测试、fixture、mock、stub 或 golden trace。
5. AI 对照 Verification Matrix 检查覆盖缺口。

### 产出物与归档

| 环节 | 产出物 | 责任人 | 状态 | 归档位置 |
|---|---|---|---|---|
| harness 候选 | Harness Candidate List | AI agent | draft | 模块 `harness-design.md` 的候选章节 |
| 模块 harness 设计 | Module Harness Design | AI agent 起草，模块负责人确认 | draft / accepted | `docs/architecture/02-modules/<module>/harness-design.md` |
| 系统级 harness 策略 | System Harness Spec | AI agent 起草，架构负责人确认 | draft / accepted | `docs/architecture/08-harness/*.md` |
| 验证矩阵更新 | Verification Matrix Row | AI agent 起草，评审者确认 | draft / accepted | [verification-matrix.md](../09-verification/verification-matrix.md) |
| 测试实现 | Test / Fixture / Golden Trace | 开发者 | reviewed / accepted | 对应模块的 `src/test`、集成测试目录或 PR |
| 覆盖缺口 | Harness Coverage Finding | 评审者 / AI agent | draft / accepted | [verification-matrix.md](../09-verification/verification-matrix.md) 或模块 `open-issues.md` |

### 退出标准

- 核心状态流转有测试或明确未覆盖原因。
- 跨模块契约有 contract test 或 mock/stub 计划。
- 关键失败路径有 failure injection。
- 每个 harness 项能追溯到场景、状态、契约或不变量。

## A8 实现任务拆解

### 触发条件

模块设计包和 harness 设计已经足以支撑开发者开工。

### 输入

| 输入 | 提供者 | 说明 |
|---|---|---|
| Development Slices | 模块负责人 / AI agent | 可并行开发的切片。 |
| Harness Design | AI agent | 每个切片对应的验证方式。 |
| Open Issues | 模块负责人 / 架构负责人 | 阻塞或非阻塞问题。 |
| 当前代码结构 | 开发者 / AI agent | 实现落点和约束。 |

### 流程

1. AI 根据 Development Slices 生成任务候选。
2. 模块负责人确认任务边界、输入输出和 DoD。
3. 开发负责人安排优先级和负责人。
4. 开发者实现代码和测试。
5. AI 或评审者检查实现是否偏离设计包。

### 产出物与归档

| 环节 | 产出物 | 责任人 | 状态 | 归档位置 |
|---|---|---|---|---|
| 任务候选 | Implementation Task Draft | AI agent | draft | issue 系统、PR 描述草稿，或模块 `development-slices.md` 的任务章节 |
| 任务确认 | Accepted Implementation Task | 模块负责人 | accepted | issue 系统或 PR 描述 |
| 排期分配 | Task Owner / Priority | 开发负责人 | accepted | issue 系统 |
| 实现变更 | Code Changes | 开发者 | reviewed / accepted | 代码仓库和 PR |
| 测试变更 | Test Changes | 开发者 | reviewed / accepted | 对应模块测试目录和 PR |
| 验收证据 | Implementation Evidence | 开发者 / AI agent | reviewed / accepted | PR 描述、CI 结果、[verification-matrix.md](../09-verification/verification-matrix.md) |

### 退出标准

- 每个任务都有输入、输出、验收标准和 owner。
- 每个任务能追溯到场景、能力、状态、契约或 harness。
- 不产生脱离架构设计的孤儿实现。

## A9 集成验证与架构评审

### 触发条件

PR 准备合并、多个模块发生协作变更、契约或状态发生变化、harness 结果需要纳入评审。

### 输入

| 输入 | 提供者 | 说明 |
|---|---|---|
| PR diff | 开发者 / AI agent | 实际变更。 |
| 测试结果 | 开发者 / CI | verification evidence。 |
| 设计包 | AI agent | 被实现引用的设计输入。 |
| 契约和状态矩阵 | AI agent | 架构约束。 |
| Open Issues | 模块负责人 | 是否仍有阻塞。 |

### 流程

1. AI 汇总 PR、测试结果、契约变更和设计引用。
2. 评审者检查状态 owner、契约、调用方向、观测、成本、权限和错误语义。
3. 架构负责人裁决跨模块冲突。
4. AI 生成 verification finding 和后续修正文档。
5. 如果实现改变了设计事实，必须回写 A2D 文档或进入正式 ADR / CR。

### 产出物与归档

| 环节 | 产出物 | 责任人 | 状态 | 归档位置 |
|---|---|---|---|---|
| 验证证据汇总 | Verification Summary | AI agent | draft / reviewed | PR 描述或 [verification-matrix.md](../09-verification/verification-matrix.md) |
| 架构评审发现 | Architecture Review Finding | 评审者 | reviewed / accepted | PR review comment；需要沉淀时同步到 [architecture-review-process.md](architecture-review-process.md) 的 finding 格式或模块 `open-issues.md` |
| 冲突裁决 | Integration Decision | 架构负责人 | accepted | PR 描述、相关设计文档或 [constraint-and-design-inventory.md](../constraint-and-design-inventory.md) |
| 验证矩阵更新 | Verification Matrix Row Update | AI agent 起草，评审者确认 | draft / accepted | [verification-matrix.md](../09-verification/verification-matrix.md) |
| 文档回写 | Updated A2D Documents | AI agent / 模块负责人 | draft / accepted | 被实现改变的对应 A2D 文档 |
| 后续问题 | Follow-up Issue | 模块负责人 / 架构负责人 | accepted | issue 系统或模块 `open-issues.md` |

### 退出标准

- 设计和实现没有未解释的漂移。
- P0 / P1 finding 已关闭或降级并说明理由。
- 契约、状态、场景、harness 的追踪关系仍然成立。
- 需要回写的文档已经更新或明确创建后续任务。

## A10 版本归档

### 触发条件

一个设计包、模块切片、跨模块契约或阶段性架构版本准备作为后续工作的基线。

### 输入

| 输入 | 提供者 | 说明 |
|---|---|---|
| 已接受文档 | AI agent | 本轮 accepted 产物。 |
| PR / commit | 开发者 / AI agent | 变更记录。 |
| Open Issues | 模块负责人 / 架构负责人 | 未决问题和影响范围。 |
| ADR / CR | 架构负责人 | 正式决策或变更请求。 |

### 流程

1. AI 汇总本轮变更、接受项、废弃项和未决项。
2. 架构负责人确认哪些内容进入当前 baseline。
3. AI 标记 superseded 或 accepted，补充反向链接。
4. PR 合并后形成版本记录。

### 产出物与归档

| 环节 | 产出物 | 责任人 | 状态 | 归档位置 |
|---|---|---|---|---|
| 版本汇总 | Architecture Baseline Note | AI agent 起草，架构负责人确认 | draft / accepted | `docs/architecture/10-governance/baselines/<version>.md` |
| baseline 裁决 | Baseline Decision | 架构负责人 | accepted | 同一份 Architecture Baseline Note |
| 当前入口更新 | README Baseline Link | AI agent 起草，架构负责人确认 | draft / accepted | [README](../README.md) |
| 废弃关系记录 | Superseded Document Record | AI agent 起草，架构负责人确认 | accepted | 被替代文档的 frontmatter 或正文维护规则；必要时同步到 Architecture Baseline Note |
| 未决问题结转 | Carried Open Issues | AI agent 起草，负责人确认 | accepted | Architecture Baseline Note；模块问题同步到模块 `open-issues.md` |

### 退出标准

- 当前 L0/L1/L2 哪些内容有效清楚。
- 哪些内容只是 draft、哪些被 superseded 清楚。
- 后续 AI 和开发者不会把旧文档误读成当前基线。

## 最小 A2D 工作包

一个模块或能力进入开发前，至少需要形成以下工作包：

| 工件 | 是否必需 | 归档位置 |
|---|---|---|
| A2D Intake Record | 必需 | `docs/architecture/10-governance/a2d-intake/<id>.md` |
| Admission Decision | 必需 | `docs/architecture/10-governance/admission-decisions/<id>.md` |
| Business Activity Scenario 或 Module Scenario Projection | 必需 | `docs/architecture/06-scenarios/` 或 `docs/architecture/02-modules/<module>/scenarios/` |
| Capability Mapping | 必需 | [capability-map.md](../01-capabilities/capability-map.md) |
| Module Responsibility Card | 必需 | [module-responsibility-cards.md](../02-modules/module-responsibility-cards.md) |
| State Ownership Decision | 如涉及状态则必需 | [state-ownership-matrix.md](../03-state/state-ownership-matrix.md) |
| Human-readable ICD / Machine-readable Contract Draft | 如涉及跨模块交互则必需 | `docs/architecture/05-contracts/` |
| Module Development Pack | 模块并行开发必需 | `docs/architecture/02-modules/<module>/` |
| Harness Design | 进入实现前必需 | `docs/architecture/08-harness/` 或 `docs/architecture/02-modules/<module>/harness-design.md` |
| Verification Matrix Row | 必需 | [verification-matrix.md](../09-verification/verification-matrix.md) |

## A2D Ready / Done 判定

| 阶段 | Ready | Done |
|---|---|---|
| 进入场景建模 | A2D Intake Record 已说明用户、目标、约束。 | Business Activity Scenario 被业务和架构负责人确认。 |
| 进入能力拆解 | Business Activity Scenario 能串起模块。 | Capability Mapping 有 owner 或 Open Issue。 |
| 进入模块设计 | Module Responsibility Card 已确认。 | Module Development Pack 可支撑任务拆解。 |
| 进入 harness | State Ownership、ICD、Scenario Assertions 明确。 | Harness Design 能覆盖核心状态、契约和失败路径。 |
| 进入实现 | Module Development Pack、Harness Design、DoD 齐备。 | Code Changes、Test Changes、Implementation Evidence 完成。 |
| 进入归档 | 集成验证完成且无阻塞 finding。 | Architecture Baseline Note、Superseded Document Record、Carried Open Issues 清楚。 |

## 人类与 AI 的协作边界

| 事项 | AI 可以做 | 必须由人类确认 |
|---|---|---|
| 业务目标 | 整理、归纳、发现缺口。 | 目标是否真实、优先级是否正确。 |
| 模块边界 | 提出候选边界和冲突。 | 哪些是架构模块、哪些不是。 |
| 状态 owner | 根据文档和代码提出建议。 | 唯一 owner、writer、forbidden writer。 |
| 契约语义 | 起草 ICD、YAML、错误语义和测试建议。 | 契约是否代表真实承诺。 |
| harness | 生成候选测试和覆盖矩阵。 | 风险覆盖是否足够。 |
| 实现任务 | 拆分任务草案和 DoD。 | 任务优先级、负责人和是否可开工。 |
| 版本归档 | 汇总变更和标记关系。 | 哪些内容进入 baseline。 |

## 反模式

- 产出物表和归档位置表主语不同，需要读者自己推断映射关系。
- 先建目录，再倒推应该写什么内容。
- 把原始讨论直接当成 accepted 设计。
- 只写模块内部细节，不说明它来自哪个场景、能力和契约。
- 把构建制品、starter、BoM、demo、fixture 写成 L0 架构模块。
- AI 直接根据模糊需求生成 production code，而没有经过 A2D Ready 判定。
- 评审发现只留在聊天或 PR 评论里，不回写到文档、Open Issue 或 Verification Matrix。
