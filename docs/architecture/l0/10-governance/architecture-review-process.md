---
level: L1
view: governance
status: draft
---

# Architecture Review Process

## 目的

定义如何评审本目录中的架构工件，以及如何把评审意见反馈到权威文档或后续实现任务。

## 适用读者

架构评审者、模块负责人、AI agent。

## 维护规则

- 评审必须先判定变更级别。
- 评审发现不得只写在评论里，必须进入对应文档或 Open Issue。
- 文档结构类 finding 必须判断是否需要回填到 [Architecture Documentation Constraints](architecture-documentation-constraints.md)。

## 评审入口

| Review Type | 触发条件 | 必读文档 |
|---|---|---|
| P0 document readiness | 新增或重写 Overview / Capability / Module / State。 | README, Documentation Constraints, Overview, Capability Map, Module Cards, State Matrix |
| Contract readiness | 新增或修改 ICD / YAML。 | ICD, machine-readable contract, related scenarios |
| Scenario readiness | 新增或修改 BA-* 或 technical scenario。 | Scenario README, BA-* Scenario, related technical scenario, State Matrix, Invariants, Verification Matrix |
| Harness readiness | 新增或修改 Harness Spec。 | Harness, ICD, Scenario, Verification Matrix |
| Governance readiness | 改变 owner、writer、review process。 | Change Governance, ADR draft, State Matrix |

## 评审问题清单

1. 是否使用真实模块名或明确说明是 capability 聚合？
2. 是否区分 shipped、design_only、draft、deferred？
3. 是否有唯一 state owner？
4. 是否写清 Non-Responsibilities？
5. 是否有 human-readable ICD 和 machine-readable draft？
6. 核心 Scenario 是否是业务活动级 BA-*，而不是只列技术机制？
7. Scenario 是否有 assertions？
8. Invariant 是否可验证？
9. Verification Matrix 是否覆盖该设计项？
10. 是否避免 L0 混入 L2/L3 细节？
11. 是否记录冲突和 Open Issue？
12. 是否违反 Documentation Constraints 中已有的命名、口径、表格主键或状态标记规则？

## 评审输出格式

```text
Finding ID:
Severity: P0 / P1 / P2 / P3
Affected Artifact:
Issue:
Required Change:
Related Principle:
Related Verification:
Status:
```

## 合并条件

- P0 / P1 finding 全部关闭或降级并记录理由。
- 每个新增设计项都有 Verification Matrix 行。
- 每个核心场景都是 BA-*，并能追踪到 technical sub-scenarios 或明确的 Open Issue。
- 每个 Open Issue 有 owner 或后续文档位置。
- Level 2 / Level 3 变更已进入 ADR / CR 流程。

## 与权威文档同步

本目录中的 draft 内容要提升为权威内容时：

1. 判断目标权威位置：`docs/adr/`、`docs/contracts/`、module `ARCHITECTURE.md`、module metadata 或 governance YAML。
2. 补齐正式 ADR / catalog / DFX / gate / test 绑定。
3. 更新本目录中的 draft 状态和反向链接。
4. 运行对应 verify 命令后再声明完成。
