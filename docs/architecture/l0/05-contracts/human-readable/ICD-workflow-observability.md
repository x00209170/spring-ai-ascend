---
level: L2
view: process
status: draft
---

# ICD-Workflow-Observability

## 目的

定义 Workflow / Orchestrator 与 Observability capability 的 trace、event、audit 和 golden trace 语义。

## 适用读者

agent-service、agent-middleware、Observability、测试负责人。

## 维护规则

- Observability 记录事实，不修改状态机结果。
- 每个 BA-* 核心 scenario 必须有 golden trace assertions；technical sub-scenario 负责机制级 trace 细节。

| Field | Value |
|---|---|
| ICD ID | ICD-Workflow-Observability |
| Participating Modules | agent-service, agent-middleware, agent-bus |
| Interaction Purpose | 记录执行、状态转换、suspend/resume、tool call、audit 和 failure evidence。 |
| Direction | Workflow / runtime -> Observability sink；Observability -> query/replay evidence。 |
| Sync / Async Model | emission 可以异步，但核心路径必须保留失败可见性。 |
| Request Semantics | emit event/span/audit with tenant, trace, task identity and semantic type；run identity 只作兼容关联字段。 |
| Response Semantics | emit accepted / rejected；不影响业务返回，除非 posture policy 要求 fail-closed。 |
| Event Semantics | TaskEvent、HookPoint event、tool event、audit event；RunEvent 只作历史兼容命名。 |
| State Semantics | Observability owns Trace / Span / Event records；不拥有 Task State。 |
| Error Semantics | telemetry failure must be visible；不得 silent degrade。 |
| Retry Responsibility | Observability sink 自己处理可重试写入；Workflow 不重复执行业务 side effect。 |
| Timeout Semantics | telemetry timeout 不得持有 Task execution indefinitely。 |
| Idempotency Semantics | eventId 或等价 correlation 防止重复 event 误判。 |
| Security / Permission Semantics | replay / query 必须按 tenant guard。 |
| Audit Semantics | 审计字段包含 actor、occurredAt、from/to status 或 operation outcome。 |
| Observability Fields | tenantId, traceId, spanId, taskId, runId when present as compatibility field, eventType, outcome, reason。 |
| Versioning Strategy | 新 event variant additive；删除或重命名 variant breaking。 |
| Backward Compatibility Rules | golden trace tests must accept additive fields but pin required fields。 |
| Contract Tests | task_created_trace_present；suspend_resume_trace_sequence；tool_denied_audit_present；telemetry_failure_visible。 |
| Open Issues | TaskEvent sealed Java hierarchy runtime binding 后续补齐；历史 RunEvent 查询可作为兼容别名。 |
