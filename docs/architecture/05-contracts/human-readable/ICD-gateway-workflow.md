---
level: L2
view: process
status: draft
---

# ICD-Gateway-Workflow

## 目的

定义外部入口到 Workflow / Orchestrator 能力的交互语义，支撑 Task 创建、取消、恢复、client invocation reference 和入口级幂等 harness。

## 适用读者

Gateway、Workflow、agent-service、agent-client、agent-bus 负责人。

## 维护规则

- 本 ICD 是 draft，生产契约仍以 `docs/contracts/openapi-v1.yaml` 和 ingress contract 为准。
- 任何状态写入必须引用 agent-service Task state owner，不得在 Gateway 侧直接完成；历史 RunRepository 只能作为实现兼容名。

| Field | Value |
|---|---|
| ICD ID | ICD-Gateway-Workflow |
| Participating Modules | agent-service, agent-client, agent-bus, agent-execution-engine |
| Interaction Purpose | 将外部请求转化为受租户、幂等和 trace 约束的 Task 执行意图，并返回 client invocation reference。 |
| Direction | Gateway -> Workflow intent；Workflow -> Gateway cursor/status/error response。 |
| Sync / Async Model | 入口可以同步返回 accepted/cursor；长任务通过 status / resume / callback 后续推进。 |
| Request Semantics | 请求必须携带 tenant、actor、idempotency、trace 或可由 Gateway 生成的等价 carrier。 |
| Response Semantics | 返回 cursor、current status、error envelope 或 terminal summary；不得暴露其他租户资源。 |
| Event Semantics | Gateway 只发出 intake / cancel / resume intent，不直接发 terminal event。 |
| State Semantics | Task State 写入由 agent-service controlled lifecycle owner 完成。 |
| Error Semantics | tenant mismatch、duplicate intent、invalid request、illegal transition 必须可区分。 |
| Retry Responsibility | Client 可以重试入口请求；Gateway 负责幂等 claim；Workflow 负责执行级重试策略。 |
| Timeout Semantics | 入口 timeout 不代表 Task 失败；通过 cursor/status 查询。 |
| Idempotency Semantics | 相同业务 intent 重复提交不得创建重复 Task。 |
| Security / Permission Semantics | Gateway 执行 tenant/auth 前置校验；后续步骤仍必须使用 Task context / RunContext compatibility tenant。 |
| Audit Semantics | intake、cancel、resume intent 必须进入 audit / trace。 |
| Observability Fields | tenantId, traceId, taskId, runId when present as compatibility field, actor, intentType, idempotencyKey hash/ref。 |
| Versioning Strategy | additive fields allowed；breaking semantic change requires ADR / CR。 |
| Backward Compatibility Rules | 已有入口语义不得在无 ADR 情况下改变状态码类别或幂等含义。 |
| Contract Tests | duplicate_create_is_idempotent；cross_tenant_is_not_visible；cancel_terminal_state_is_rejected；entry_timeout_does_not_mark_failed。 |
| Open Issues | agent-client runtime binding 与 ingress runtime enforcement 仍需后续落地。 |
