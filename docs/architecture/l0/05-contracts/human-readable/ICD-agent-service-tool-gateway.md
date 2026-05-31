---
level: L2
view: process
status: draft
---

# ICD-AgentService-ToolGateway

## 目的

定义 agent-service / Orchestrator 与 Tool Gateway capability 之间的工具调用治理语义。

## 适用读者

agent-service、agent-middleware、业务 Skill 实现者、评审者。

## 维护规则

- Tool Gateway 是 capability 聚合。
- 任何不可逆副作用必须有幂等和审计。

| Field | Value |
|---|---|
| ICD ID | ICD-AgentService-ToolGateway |
| Participating Modules | agent-service, agent-middleware, agent-execution-engine |
| Interaction Purpose | 在执行 agent step 时安全调用 Skill / Tool，并保证权限、capacity、幂等、错误和 audit 语义。 |
| Direction | Agent Service / Workflow -> Tool Gateway；Tool Gateway -> tool result / suspended / denied / failed。 |
| Sync / Async Model | tool call 可同步返回，也可返回 suspend / callback 需求。 |
| Request Semantics | tenant、task、step、attempt、skillKey、inputRef、traceContext、idempotencyRef；runId 只作为兼容关联字段。 |
| Response Semantics | completed、failed、suspended、denied、rateLimited。 |
| Event Semantics | tool.requested、tool.authorized、tool.started、tool.completed、tool.failed、tool.suspended。 |
| State Semantics | ToolCallRecord 由 Tool Gateway capability 拥有；Task state 由 agent-service Task owner 拥有。 |
| Error Semantics | permission_denied、skill_not_found、rate_limited、tool_timeout、tool_non_retryable_error。 |
| Retry Responsibility | Workflow 决定重试；Tool Gateway 提供 retryable classification。 |
| Timeout Semantics | 长等待通过 suspend/resume 或 rate limit envelope 表达。 |
| Idempotency Semantics | attemptId + skillKey + input hash / ref 防止重复不可逆副作用。 |
| Security / Permission Semantics | Skill 执行前必须校验 tenant policy 和 skill permission。 |
| Audit Semantics | 所有不可逆工具调用必须有 audit envelope。 |
| Observability Fields | tenantId, traceId, taskId, runId when present as compatibility field, stepId, attemptId, skillKey, policyDecision, outcome。 |
| Versioning Strategy | 新 skill kind additive；改变 error classification 为 breaking。 |
| Backward Compatibility Rules | business skill input schema 变更需要兼容策略或新版本。 |
| Contract Tests | permission_denied_has_no_side_effect；rate_limited_returns_suspend_reason；duplicate_attempt_does_not_repeat_side_effect；tool_timeout_visible。 |
| Open Issues | 完整 Skill lifecycle 和 sandbox runtime enforcement 待后续落地。 |
