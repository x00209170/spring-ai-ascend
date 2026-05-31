---
phase: AI-2
trigger: AI-1 完成需求归一化，需求分类表已形成
human_review_density: standard
---

# AI-2：架构影响分析

## 应该做什么

1. 逐项分析需求对现有架构的影响
2. 判断每个影响涉及的能力、模块、状态、契约、场景
3. 初步判定变更级别（Level 0/1/2/3，参考 `change-governance.md`）
4. 发现需要升级给人类裁决的高风险项
5. 生成架构影响矩阵
6. 进入 AI-3 生成版本架构边界

## 输入

| 来源 | 路径 |
|---|---|
| 结构化版本意图 | `10-governance/version-intents/<version>.md` |
| 现有架构基线 | `00-overview/`、`01-capabilities/`、`04-modules/`、`06-state/`、`05-contracts/` |
| 变更分级规则 | `10-governance/change-governance.md` |
| 架构不变量 | `07-invariants/architecture-invariants.md` |
| 现有契约 | `05-contracts/` |

## 输出

| 产出 | 归档位置 | 状态 |
|---|---|---|
| 架构影响矩阵 | 版本意图文件的附录 | draft |
| 变更级别初判 | 架构影响矩阵中 | draft |
| 升级候选清单 | 架构影响矩阵中 | draft |

## 影响分析维度

对每个需求检查：

- 是否改变 Capability（能力图）
- 是否改变 Module Responsibility（模块责任卡）
- 是否改变 State Owner / Writer（状态归属矩阵）
- 是否改变 ICD / Contract（契约）
- 是否改变 Scenario（场景）
- 是否改变 Invariant（不变量）
- 是否改变 Harness / Verification（验证矩阵）

## 变更级别判定

| Level | 含义 | 参考 |
|---|---|---|
| Level 0 | 模块内部实现变更 | 不改变外部语义 |
| Level 1 | 向后兼容的接口变更 | additive only |
| Level 2 | 跨模块语义变更 | 需要 CR，可能需要 ADR |
| Level 3 | 架构原则、状态归属或控制权变更 | 必须 ADR |

## 0→1 特殊处理

如果是从零设计新架构，影响分析不是"增量影响"，而是"架构取向约束分析"：

1. 从版本意图中提取架构取向偏好（部署偏好、数据偏好、集成偏好、演进偏好）
2. 生成 2-3 个候选架构方向
3. 每个候选说明：适合什么、牺牲什么、未来怎么演进
4. 不直接生成单一目标架构

## 何时停下问人

- 影响涉及 Level 3 变更
- 多个需求互相约束，存在多种满足方式
- 需要改变 `must_keep` 中声明的不可变边界
- 0→1 场景下需要人类选择架构方向

## 反模式

- 把所有影响都标记为 Level 0，回避升级
- 不读现有架构就做影响判断
- 0→1 场景下直接生成单一架构，不给候选
