---
level: L1
view: governance
status: draft
---

# A2D 工作模型（内核）

## 目的

A2D（Architecture to Delivery）是 AI 将版本意图批量转化为可审阅架构和可交付实现的推理、编辑、验证与追踪模型。

A2D 不是目录结构，也不是一批静态文档，也不是逐需求人工审批流。目录只负责归档活动产物。

本文是 A2D 的精简内核，包含跨阶段共享的信息。每个阶段的具体操作步骤、输入输出和退出条件定义在对应的阶段契约中。

## 版本级自动化模型

A2D 默认以版本为单位运行。人类集中注入版本意图和架构边界，AI 在边界内批量完成分析、文档编辑、任务拆解、实现、验证和归档。

```text
H0 人类注入版本意图
 -> AI-1 需求归一化与分流        [阶段契约: a2d-phases/phase-intake.md]
 -> AI-2 架构影响分析            [阶段契约: a2d-phases/phase-impact.md]
 -> AI-3 生成版本架构边界        [阶段契约: a2d-phases/phase-envelope.md]
H1 人类确认版本架构边界
 -> AI-4 批量生成 A2D 产物       [阶段契约: a2d-phases/phase-produce.md]
 -> AI-5 生成 4+1 评审视图       [阶段契约: a2d-phases/phase-review-view.md]
 -> AI-6 生成交付视图            [阶段契约: a2d-phases/phase-delivery-view.md]
H2 人类集中审核架构包
 -> AI-7 自动实现               [阶段契约: a2d-phases/phase-implement.md]
 -> AI-8 集成验证与漂移检查      [阶段契约: a2d-phases/phase-verify.md]
H3 人类审核例外和发布风险
 -> AI-9 版本归档               [阶段契约: a2d-phases/phase-archive.md]
```

人类视角的完整流程见 [a2d-human-checkpoints.md](a2d-human-checkpoints.md)。

## 产物状态

| 状态 | 含义 | 可用于开发吗 | 需要谁确认 |
|---|---|---|---|
| raw_input | 原始输入，可能不完整、冲突或未经分类。 | 否。 | 输入提供者确认背景。 |
| draft | 已整理但未裁决的草稿。 | 只能用于讨论。 | 相关负责人确认后才能升级。 |
| reviewed | 已评审，未必成为当前基线。 | 可用于探索性实现或 harness 草案。 | 评审者和相关负责人确认。 |
| accepted | 当前交付基线。 | 是。 | 架构负责人和模块负责人确认。 |
| superseded | 已被新版本替代。 | 否，除非明确用于迁移。 | 架构负责人确认替代关系。 |

## 归档位置约定

| 产物类型 | 默认归档位置 |
|---|---|
| 版本意图 | `10-governance/version-intents/<version>.md` |
| 版本架构边界 | `10-governance/architecture-envelopes/<version>.md` |
| 架构审核包 | `10-governance/review-packets/<version>.md` |
| 交付视图 | `10-governance/delivery-projections/<version>.md` |
| A2D Intake Record | `10-governance/a2d-intake/<id>.md` |
| Admission Decision | `10-governance/admission-decisions/<id>.md` |
| 基线说明 | `10-governance/baselines/<version>.md` |
| 原始需求记录 | [task.md](../task.md) |
| Conflict / Open Issue | [constraint-and-design-inventory.md](../constraint-and-design-inventory.md) |
| BA 场景 | `02-scenarios/BA-xxx-*.md` |
| Technical 场景 | `02-scenarios/technical/Sx-*.md` |
| 能力图 | [capability-map.md](../01-capabilities/capability-map.md) |
| 模块责任卡 | [module-responsibility-cards.md](../04-modules/module-responsibility-cards.md) |
| 模块设计包 | `04-modules/<module>/` |
| 状态归属矩阵 | [state-ownership-matrix.md](../06-state/state-ownership-matrix.md) |
| Human-readable ICD | `05-contracts/human-readable/*.md` |
| Machine-readable 草案 | `05-contracts/machine-readable/*.yaml` |
| Harness 设计 | `08-harness/` 或 `04-modules/<module>/harness-design.md` |
| 验证矩阵 | [verification-matrix.md](../09-verification/verification-matrix.md) |

## 相关文件

| 文件 | 内容 |
|---|---|
| [a2d-human-checkpoints.md](a2d-human-checkpoints.md) | 人类视角的检查点流程 |
| [a2d-roles.md](a2d-roles.md) | 角色与协作边界 |
| [a2d-views.md](a2d-views.md) | 双视图模型、验收口径和最小工作包 |
| [a2d-phases/](a2d-phases/) | AI 阶段契约（按阶段加载） |
| [change-governance.md](change-governance.md) | 变更分级规则 |
| [layer-update-protocol.md](layer-update-protocol.md) | 多层更新协议（L0/L1/L2 联动工作顺序） |
| [architecture-review-process.md](architecture-review-process.md) | 评审流程和 Finding 格式 |
| [document-artifact-catalog.md](document-artifact-catalog.md) | 目录职责索引 |

## 维护规则

- 阶段契约变更时，更新本文的阶段索引
- 新增产物类型时，更新归档位置约定
- 本文保持精简；详细内容放进阶段契约或相关文件
