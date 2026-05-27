---
level: L2
view: scenarios
status: draft
---

# Tool Gateway Harness Spec

## 目的

为 Tool Gateway capability 生成 skill/tool governance 的 mocks、stubs、contract tests 和 failure injection cases。

## 适用读者

agent-middleware、agent-service、业务 Skill 实现者。

## 维护规则

- 不可逆副作用必须有幂等和 audit 测试。
- Tool Gateway 不得直接写 Task State。

| Field | Value |
|---|---|
| Module Name | Tool Gateway capability |
| Module Boundary | agent-middleware Skill / RuntimeMiddleware / ModelGateway SPI + agent-service adapters |
| Responsibilities | skill lookup；authorization；capacity；tool-call audit；retry classification；suspend reason envelope。 |
| Non-Responsibilities | 不实现业务工具内部逻辑；不拥有 Task State；不写业务状态除非业务 skill 明确声明。 |
| Owned State | ToolCallRecord draft；Skill capacity claim draft；audit envelope。 |
| Provided Contracts | ICD-AgentService-ToolGateway |
| Consumed Contracts | engine-hooks, skill-definition, workflow-observability |
| Upstream Mocks | Orchestrator tool request；policy context；Task context / RunContext compatibility。 |
| Downstream Stubs | business skill success/failure/timeout；capacity registry full；audit sink failure。 |
| Scenario Fixtures | BA-001, BA-002; technical S4, S5。 |
| Contract Tests | permission_denied_has_no_side_effect；duplicate_attempt_does_not_repeat_side_effect；rate_limited_returns_suspend_reason。 |
| State Machine Tests | none direct; assert no direct Task State mutation。 |
| Failure Injection Cases | skill timeout；policy denied；capacity full；audit sink failure；duplicate callback。 |
| Golden Trace Assertions | tool.requested；tool.authorized_or_denied；tool.completed_or_failed；audit event。 |
| Architecture Invariants | INV-003, INV-005, INV-008 |
| Compatibility Tests | error classification additive；breaking reclassification requires ADR。 |
| Local Runner Requirement | pure fake Skill implementations, no external provider。 |
| CI Gate Requirements | contract tests and security review checklist when runtime binding lands。 |
