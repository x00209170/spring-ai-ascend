---
level: L1
view: logical
status: draft
---

# ADR-001: Task Lifecycle Ownership

## 目的

把“Workflow owns Run lifecycle”的历史通用需求改写为当前文档集的 Task canonical 控制权边界。

## 适用读者

agent-service、agent-execution-engine、harness 生成器、评审者。

## 维护规则

本文是交付视角 ADR 草案，不替代 `docs/adr/0142-run-aggregate-single-owner.yaml`。

| Field | Value |
|---|---|
| ADR ID | ADR-A2DH-001 |
| Title | Task lifecycle state is written only through the agent-service controlled owner |
| Status | Draft, aligned with accepted ADR-0142 |
| Context | task.md 和历史 L1 文档使用 Run lifecycle / RunRepository 命名；当前架构口径要求服务端 canonical 状态收敛为 Task，Run 只作为 client invocation 或实现兼容名。 |
| Problem | 如果把 Workflow ownership 理解成 Orchestrator 可直接写 Task State，或把历史 Run 命名恢复成第二套服务端状态 owner，会破坏状态机、cancel race 和审计一致性。 |
| Decision | Workflow / Orchestrator 拥有“编排意图”和 suspend/resume 处理；agent-service controlled lifecycle entry / TaskStateStore 拥有 Task State 的唯一写入口。当前代码中的 RunRepository / Layer 2 命名只作为 Task lifecycle 的实现兼容。 |
| Alternatives Considered | A. Orchestrator 直接写 Task state，简单但破坏 single owner。B. Gateway 直接 cancel terminal state，入口快但绕过 state machine。C. 保留 Run 和 Task 两套服务端状态，概念清晰度差且会产生冲突。D. 当前选择：服务端状态收敛到 Task，Run 作为兼容别名。 |
| Consequences | Harness 必须 mock TaskStateStore / controlled transition primitive；ICD 要区分 state intent 和 state write；任何新 writer 或第二套 Run State owner 都是架构变更。 |
| Impacted Modules | agent-service, agent-execution-engine, agent-bus |
| Related Principles | PR-003, PR-009 |
| Related Contracts | ICD-Gateway-Workflow, ICD-Workflow-AgentService |
| Related Scenarios | BA-001, BA-002, BA-003; technical S1, S2, S5 |
| Verification Method | State Machine Test, static architecture check, cancel race scenario |
| Open Questions | durable repository 的具体 atomic primitive 属于 L2/L3，不在本文决定。 |
