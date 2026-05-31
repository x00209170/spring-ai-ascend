---
phase: AI-4
trigger: H1 已确认版本架构边界
human_review_density: light
---

# AI-4：批量生成或更新 A2D 产物

## 应该做什么

在已确认的版本架构边界内，批量生成或更新架构产物。这是 AI 工作量最大的阶段，涵盖从场景建模到模块设计的全部内容。

按顺序执行以下活动（跳过不影响本版本的）：

### 活动 1：架构准入判定

在生成产物前，先对每个待处理项做准入分类。分类集合固定为 9 类：

```text
Architecture Module       # 可进入 L0 Overview 的模块边界
Runtime Component         # 必要的运行时组件
Capability                # 能力聚合，不作为真实模块主键
Contract                  # 契约
State                     # 状态对象
Build Artifact             # BoM、starter 等，不得作为 L0 架构模块
Packaging Artifact        # 打包工件，不得作为 L0 架构模块
Implementation Constraint # 实现约束
Open Issue                # 未决问题
Reject                    # 不进入本次设计
```

只有 `Architecture Module` 和必要的 `Runtime Component` 可以进入 L0 Overview 模块边界。`Build Artifact`、BoM、starter、dependency management、test fixture、demo、adapter scaffold 不得作为 L0 架构模块出现。

产出归档：`10-governance/admission-decisions/<id>.md`

### 活动 2：场景建模

- 从业务需求生成或更新 BA-* 业务活动场景
- 抽取技术子场景
- 确保场景能串起多个模块

产出归档：`02-scenarios/BA-xxx-*.md`、`02-scenarios/technical/Sx-*.md`

### 活动 3：能力拆解

- 从场景抽取能力候选
- 建立能力与模块的承接关系
- 更新能力图

产出归档：`01-capabilities/capability-map.md`

### 活动 4：模块责任承接

- 生成或更新模块责任卡
- 明确职责、非职责、状态责任、协作边界
- 裁决跨模块边界冲突

产出归档：`04-modules/module-responsibility-cards.md`、`04-modules/<module>/README.md`

### 活动 5：状态与契约设计

- 抽取状态对象，确认 owner / writer / reader
- 生成或更新 human-readable ICD
- 生成 machine-readable contract draft
- 更新架构不变量

产出归档：`06-state/`、`05-contracts/`、`07-invariants/`

### 活动 6：模块详细设计

- 对需要进入并行开发的模块，生成模块设计包
- 对旧设计做准入判定：保留、修正、下沉、废弃
- 生成 4+1 视图、状态模型、流程设计、开发视图、开发切片

产出归档：`04-modules/<module>/` 下各文件

## 输入

| 来源 | 路径 |
|---|---|
| 版本意图 | `10-governance/version-intents/<version>.md` |
| 版本架构边界 | `10-governance/architecture-envelopes/<version>.md` |
| 架构影响矩阵 | 版本意图文件的附录 |
| 现有架构基线 | `00-overview/` ~ `09-verification/` |
| 文档约束 | `10-governance/architecture-documentation-constraints.md` |

## 输出

| 产出 | 归档位置 |
|---|---|
| 业务活动场景 | `02-scenarios/` |
| 能力图更新 | `01-capabilities/` |
| 模块责任卡更新 | `04-modules/` |
| 状态归属更新 | `06-state/` |
| 契约草案 | `05-contracts/` |
| 不变量更新 | `07-invariants/` |
| 模块设计包 | `04-modules/<module>/` |
| 产出冲突和开放问题 | `constraint-and-design-inventory.md` 或模块 `open-issues.md` |

## 退出条件

- 每个受影响的模块都有更新后的责任卡
- 每个核心状态有 owner
- 每个跨模块交互有契约或 Open Issue
- 每个关键场景有 assertions
- 所有文档遵守 `architecture-documentation-constraints.md`

## 何时停下问人

- 触发版本架构边界的升级条件
- 发现版本架构边界未覆盖的影响
- 模块边界冲突无法自行裁决
- 文档约束发现新的模式问题需要回填

## 反模式

- 只更新部分产物，导致能力图和模块卡不一致
- 跳过场景建模直接写模块设计
- 把旧设计全部保留，不做准入判定
- 在 L0 文档中混入 L2/L3 实现细节
