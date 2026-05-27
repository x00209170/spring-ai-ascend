---
level: L2
view: scenarios
status: draft
---

# Observability Harness Spec

## 目的

为 trace、event、audit 和 golden trace assertion 生成 harness 草案。

## 适用读者

Observability、Workflow、Tool Gateway、测试负责人。

## 维护规则

- Observability 只记录事实，不修正业务状态。
- 每个核心 scenario 必须有 golden trace assertions。

| Field | Value |
|---|---|
| Module Name | Observability capability |
| Module Boundary | TraceContext, trace entry filter, RuntimeMiddleware event, TaskEvent draft, audit writer |
| Responsibilities | record trace/span/event/audit；provide replay evidence；make failures visible。 |
| Non-Responsibilities | 不改变 Task State；不吞掉错误；不拥有业务查询 API。 |
| Owned State | Trace / Span / Event / Audit semantics。 |
| Provided Contracts | ICD-Workflow-Observability |
| Consumed Contracts | all scenario event emissions。 |
| Upstream Mocks | Workflow event emitter；Tool Gateway audit event；Gateway intake event。 |
| Downstream Stubs | telemetry sink accepted/rejected/timeout。 |
| Scenario Fixtures | BA-001..BA-003; technical S1..S6。 |
| Contract Tests | task_created_trace_present；suspend_resume_trace_sequence；tool_denied_audit_present。 |
| State Machine Tests | none direct; assert observability does not mutate Task State。 |
| Failure Injection Cases | sink timeout；schema invalid event；duplicate event；replay tenant mismatch。 |
| Golden Trace Assertions | scenario-specific required event sequence。 |
| Architecture Invariants | INV-007, INV-008 |
| Compatibility Tests | additive event fields tolerated；variant removal requires ADR。 |
| Local Runner Requirement | in-memory event collector。 |
| CI Gate Requirements | golden trace tests for every promoted scenario。 |
