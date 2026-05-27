---
level: L1
view: process
status: draft
---

# ADR-004: Tool and Skill Governance

## 目的

明确 Tool Gateway 是治理能力聚合，所有工具调用必须经过 Skill、RuntimeMiddleware、capacity、audit 和 observability 语义。

## 适用读者

agent-middleware、agent-service、业务 skill 实现者、harness 生成器。

## 维护规则

本文是交付视角 ADR 草案；正式权威参考 ADR-0122、ADR-0127、ADR-0134。

| Field | Value |
|---|---|
| ADR ID | ADR-A2DH-004 |
| Title | Tool calls are governed through Skill and RuntimeMiddleware boundaries |
| Status | Draft |
| Context | task.md 要求 Tool Gateway owns authorization and audit；当前真实边界由 agent-middleware 的 Skill / RuntimeMiddleware / ModelGateway 等 SPI 和 agent-service integration adapters 组成。 |
| Problem | 业务 Agent 直接调用外部工具会绕过 tenant、capacity、audit、idempotency、trace 和 suspend/resume 语义。 |
| Decision | 工具调用必须表达为 Skill 或等价 middleware-mediated operation。Tool Gateway capability 负责 authz、capacity、audit、idempotency 和 error semantics，但不拥有业务工具内部状态。 |
| Alternatives Considered | A. 每个 Agent 自己调用工具，最灵活但不可治理。B. 单独新建 Tool Gateway module，当前仓库无此拓扑。C. 当前选择：能力聚合，落在 agent-middleware + agent-service。 |
| Consequences | BA-001 / BA-002 和 technical S4 / S5 的 Tool Gateway harness 必须覆盖 permission denied、capacity full、timeout、duplicate callback 和 audit trace。 |
| Impacted Modules | agent-middleware, agent-service, agent-execution-engine |
| Related Principles | PR-008, PR-010 |
| Related Contracts | ICD-AgentService-ToolGateway |
| Related Scenarios | BA-001, BA-002; technical S4, S5 |
| Verification Method | Contract Test, Failure Injection, Security Review |
| Open Questions | 完整 skill lifecycle runtime enforcement 依赖后续实现波次。 |
