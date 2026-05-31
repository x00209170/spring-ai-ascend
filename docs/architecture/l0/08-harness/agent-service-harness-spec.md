---
level: L2
view: scenarios
status: draft
---

# Agent Service Harness Spec

## 目的

为 agent-service 提供入口、Task lifecycle、Session、idempotency、client invocation reference adapter 和 observability 的 harness 草案。

## 适用读者

agent-service、测试负责人、AI agent。

## 维护规则

- Harness 必须验证 service.platform 与 service.runtime 边界。
- Task lifecycle single owner 是硬性断言；历史 Run 命名只能作为兼容字段。

| Field | Value |
|---|---|
| Module Name | agent-service |
| Module Boundary | HTTP 对外入口 + SSE stream + Task lifecycle + Session shell + client invocation reference adapters |
| Responsibilities | tenant/auth/idempotency 入口；TaskStateStore；历史 RunRepository / RunStateMachine 兼容入口；ContextProjector；Agent SPI；reference adapters；SSE 实时输出。 |
| Non-Responsibilities | 不拥有 bus physical channel；不拥有 middleware SPI provider semantics；不让外部 writer 写 Task State；不创建第二套 Run State。 |
| Owned State | Task Execution State, Session State, IdempotencyRecord, HTTP 对外入口 trace carrier, client invocation reference。 |
| Provided Contracts | Gateway-Workflow, Workflow-AgentService, Workflow-Observability |
| Consumed Contracts | engine envelope, middleware SPI, bus S2C / callback / A2A control |
| Upstream Mocks | client request；Gateway intent；Workflow transition intent。 |
| Downstream Stubs | Engine adapter；Tool Gateway；Context Engine；Observability sink；Bus control stub；SSE sink。 |
| Scenario Fixtures | BA-001, BA-002, BA-003; technical S1, S2, S3, S5。 |
| Contract Tests | create task idempotency；tenant mismatch hidden；TaskStateStore transition guard；context package tenant scope。 |
| State Machine Tests | legal transitions accepted；illegal transitions rejected；terminal transitions idempotent where required。 |
| Failure Injection Cases | duplicate request；repository unavailable；context unavailable；resume after terminal；telemetry sink failure；SSE client disconnect；duplicate child completion。 |
| Golden Trace Assertions | tenantId and traceId present at HTTP 对外入口；task.created；task.state_transition；context.built。 |
| Architecture Invariants | INV-001, INV-002, INV-004, INV-007 |
| Compatibility Tests | error envelope shape pinned；additive response fields tolerated。 |
| Local Runner Requirement | dev-posture in-memory harness; no external LLM or database required for draft tests。 |
| CI Gate Requirements | architecture checks, state tests, scenario contract tests。 |
