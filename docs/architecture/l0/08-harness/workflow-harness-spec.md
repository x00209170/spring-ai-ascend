---
level: L2
view: scenarios
status: draft
---

# Workflow Harness Spec

## 目的

为 Workflow / Orchestrator capability 生成本地 harness、contract tests、state-machine tests 和 failure injection cases。

## 适用读者

agent-execution-engine、agent-service、AI agent、测试负责人。

## 维护规则

- Harness 绑定行为，不绑定具体存储或 broker 实现。
- 所有 Task State 写入断言必须经过 agent-service controlled lifecycle entry。

| Field | Value |
|---|---|
| Module Name | Workflow / Orchestrator |
| Module Boundary | agent-execution-engine orchestration SPI + agent-service control/reference adapters |
| Responsibilities | dispatch engine；handle suspend/resume；classify retry；emit transition intent；consume Checkpointer。 |
| Non-Responsibilities | 不直接写 Task State；不直接调用 provider-specific tools。 |
| Owned State | orchestration-local execution context；checkpoint intent。 |
| Provided Contracts | ICD-Workflow-AgentService, ICD-Workflow-Observability |
| Consumed Contracts | ICD-Gateway-Workflow, ICD-AgentService-ToolGateway, ICD-AgentService-ContextEngine |
| Upstream Mocks | Gateway create/cancel/resume intent；TaskStateStore state reader；historical RunRepository compatibility reader。 |
| Downstream Stubs | Engine adapter completed/failed/suspended；Tool Gateway completed/denied/rate-limited；Context Engine built/unavailable；Observability accepted/rejected。 |
| Scenario Fixtures | BA-001, BA-002, BA-003; technical S1, S2, S5 plus S4 tool failure fixture。 |
| Contract Tests | valid_transition_commits_once；invalid_transition_rejected；suspend_persists_checkpoint_first。 |
| State Machine Tests | Pending -> Running；Running -> Suspended；Suspended -> Running；Running -> Succeeded；Running -> Failed；Running -> Cancelled。 |
| Failure Injection Cases | checkpoint failure；duplicate resume；out-of-order terminal result；engine mismatch；tool timeout。 |
| Golden Trace Assertions | task.created；engine.dispatched；suspend.requested；resume.requested；task.terminal。 |
| Architecture Invariants | INV-002, INV-006, INV-007 |
| Compatibility Tests | additive event fields tolerated；state semantic change rejected without ADR。 |
| Local Runner Requirement | pure-JUnit harness can run without external provider。 |
| CI Gate Requirements | state machine tests and contract tests must run in verify phase when implementation lands。 |
