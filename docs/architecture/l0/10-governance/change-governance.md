---
level: L1
view: governance
status: draft
---

# Change Governance

## 目的

定义架构、契约、状态机和 harness 的变更分级，避免后续设计再次混杂。

## 适用读者

架构师、模块负责人、评审者、AI agent。

## 维护规则

- Level 2 以上变更必须更新 Verification Matrix。
- Level 3 变更必须有 ADR / CR。
- 任何改变 state owner、writer 或 cross-module semantics 的变更不得只改实现代码。

## 变更分级

| Level | 含义 | Examples | Required Reviewers | Required Document Updates | Required Tests | Required ADR / CR | Compatibility Requirements |
|---|---|---|---|---|---|---|---|
| Level 0 | 模块内部实现变更 | 重构内部 helper；优化本地 runner。 | module owner | Harness optional | existing module tests | no | 不改变外部语义 |
| Level 1 | 向后兼容的接口变更 | 添加 optional field；新增 event additive field。 | module owner + contract owner | ICD / YAML / Verification Matrix | contract regression | CR optional | additive only |
| Level 2 | 跨模块语义变更 | error classification 改变；retry owner 改变；context package semantics 改变。 | architecture + affected module owners | ICD, Scenario, Harness, Verification Matrix | contract + scenario tests | CR required, ADR if durable | backward compatibility plan |
| Level 3 | 架构原则、状态归属或控制权变更 | 新增 Task State writer；把历史 Run 命名恢复成第二套服务端状态；改变 owner；绕过 Tool Gateway；变更 Gateway to compute routing。 | architecture owner + all affected module owners | Overview, Principles, Module Cards, State Matrix, ADR, ICD, Scenario, Invariants, Harness, Verification | full scenario + static architecture check | ADR required | migration and deprecation plan |

## Task 状态机变更规则

如果 Task 状态机发生变化，至少需要：

- 更新 [State Ownership Matrix](../06-state/state-ownership-matrix.md)。
- 更新 [ADR-001](../03-adrs/ADR-001-run-lifecycle-ownership.md) 或新增正式 ADR。
- 更新 Gateway ↔ Workflow 和 Workflow ↔ Agent Service ICD。
- 更新 Workflow Harness Spec。
- 更新相关 Scenario Spec。
- 更新 Verification Matrix。
- 通过状态机测试、场景测试和 golden trace tests。

## Contract 变更规则

| Change | Required Action |
|---|---|
| 添加 optional 字段 | 更新 ICD / YAML / compatibility test。 |
| 改 required 字段 | Level 2 或 Level 3，必须有 ADR / migration。 |
| 改错误语义 | 更新 ICD、Scenario failure flow、contract tests。 |
| 改 retry owner | Level 2，必须更新 harness 和 failure injection。 |
| 改状态 owner | Level 3，必须 ADR。 |

## AI Agent 执行约束

AI agent 生成实现任务前必须先定位：

```text
Capability
Module Card
State Owner
ICD
Scenario
Invariant
Harness Spec
Verification Matrix row
```

缺失任一项时，任务状态为 `Needs Architecture Input`，不得直接生成 production code。
