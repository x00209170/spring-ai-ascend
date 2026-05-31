---
level: L2
view: process
status: draft
---

# ICD-Workflow-AgentService

## 目的

定义 Workflow / Orchestrator 能力与 agent-service Task、Session、client invocation reference adapters 之间的交互语义。

## 适用读者

agent-execution-engine、agent-service、harness 生成器。

## 维护规则

- 区分 state intent 和 state write。
- Workflow 不得直接拥有 Task State writer；历史 Run 命名不得恢复成第二套服务端状态 owner。

| Field | Value |
|---|---|
| ICD ID | ICD-Workflow-AgentService |
| Participating Modules | agent-execution-engine, agent-service |
| Interaction Purpose | Orchestrator 发起执行、状态转换意图、checkpoint、resume 和 terminal handling。 |
| Direction | Workflow -> agent-service repository / reference adapters；agent-service -> Workflow current state / resume payload。 |
| Sync / Async Model | reference path 可同步；durable target 异步化。 |
| Request Semantics | 必须包含 task identity、tenant identity、execution definition 或 equivalent envelope、trace context；run identity 只作为兼容字段。 |
| Response Semantics | 返回 state transition result、suspend request、terminal result、failure reason 或 retryable signal。 |
| Event Semantics | TaskCreated、TaskStateTransition、SuspendRequested、ResumeRequested、TerminalTransition 等语义事件。 |
| State Semantics | Task State 只通过 agent-service controlled lifecycle owner 写入；Workflow 只发起 transition intent。 |
| Error Semantics | invalid transition、engine mismatch、checkpoint failure、resume payload invalid 必须可区分。 |
| Retry Responsibility | Workflow 负责执行级 retry；repository 负责 idempotent transition guard。 |
| Timeout Semantics | 执行 timeout 可以触发 suspend、fail 或 retry，具体由 scenario 和 policy 决定。 |
| Idempotency Semantics | transition command must not duplicate terminal side effects。 |
| Security / Permission Semantics | tenantId 来自 Task context / RunContext compatibility，不从 platform request ThreadLocal 读取。 |
| Audit Semantics | 每次状态转换和 suspend/resume 必须有 audit / trace。 |
| Observability Fields | tenantId, taskId, runId when present as compatibility field, traceId, fromStatus, toStatus, suspendReason, terminalReason。 |
| Versioning Strategy | state machine semantic change requires ADR and matrix update。 |
| Backward Compatibility Rules | 新 terminal reason additive；改变 transition legality 为 breaking。 |
| Contract Tests | valid_transition_commits_once；invalid_transition_rejected；suspend_persists_checkpoint_first；resume_revalidates_tenant。 |
| Open Issues | durable async orchestrator 的具体 primitive 不在本 ICD 决定。 |
