---
level: L1
view: scenarios
status: draft
---

# Architecture Invariants

## 目的

把原则和 ADR 转换为可检查不变量，支撑代码审查、harness 和 CI gate。

## 适用读者

架构师、模块负责人、AI agent、测试负责人。

## 维护规则

- 每条 invariant 必须有 verification。
- 暂时不能自动验证时，必须写明人工审查方法。

```yaml
invariants:
  - id: INV-001
    name: PlatformBusinessDecoupling
    rule:
      platform_code_must_not_contain_business_customization: true
      extension_path:
        - SPI
        - configuration
    forbidden:
      - business patches platform internals
      - business code depends on service platform internals
    verification:
      - static_dependency_scan
      - architecture_review
    related_adr:
      - ADR-A2DH-002
    related_scenarios:
      - BA-001
      - BA-002
      - technical/S1
      - technical/S4

  - id: INV-002
    name: TaskStateSingleWriter
    rule:
      state: TaskExecutionState
      only_writer: agent-service controlled lifecycle entry
    forbidden:
      - Gateway writes Task terminal state
      - ToolGateway mutates Task lifecycle
      - Engine adapter bypasses agent-service Task state owner
      - historical Run naming becomes a second server-side state owner
    verification:
      - state_machine_test
      - static_architecture_check
      - cancel_race_scenario
    related_adr:
      - ADR-A2DH-001
    related_scenarios:
      - BA-001
      - BA-002
      - BA-003
      - technical/S1
      - technical/S2
      - technical/S5

  - id: INV-003
    name: ToolCallsThroughGovernance
    rule:
      all_tool_calls_go_through: ToolGatewayCapability
    forbidden:
      - Agent directly invokes irreversible external tool
      - Skill bypasses policy and audit
    verification:
      - contract_test
      - failure_injection_test
      - security_review
    related_adr:
      - ADR-A2DH-004
    related_scenarios:
      - BA-001
      - BA-002
      - technical/S4

  - id: INV-004
    name: ContextProducedByContextEngine
    rule:
      context_package_owner: ContextEngineCapability
    forbidden:
      - Engine builds hidden context from raw memory
      - Gateway creates prompt context
    verification:
      - contract_test
      - projection_assertion
    related_adr:
      - ADR-A2DH-003
    related_scenarios:
      - BA-001
      - technical/S3

  - id: INV-005
    name: BusinessStateNotOwnedByRuntime
    rule:
      business_state_owner: business_system
    forbidden:
      - Agent runtime owns external business record
      - ToolGateway silently mutates business state without skill contract
    verification:
      - architecture_review
      - tool_contract_test
    related_adr:
      - ADR-A2DH-004
    related_scenarios:
      - BA-001
      - BA-002
      - technical/S4

  - id: INV-006
    name: SuspendInsteadOfHold
    rule:
      long_waits_use:
        - cursor
        - suspend
        - resume
    forbidden:
      - hold client connection for long horizon work
      - block orchestrator thread waiting for external client
    verification:
      - state_machine_test
      - failure_injection_test
      - manual_architecture_review
    related_adr:
      - ADR-A2DH-001
    related_scenarios:
      - BA-002
      - BA-003
      - technical/S5

  - id: INV-007
    name: TraceContextPropagation
    rule:
      cross_module_calls_propagate:
        - tenantId
        - traceId
        - runId when known
    forbidden:
      - provider adapter emits telemetry directly without platform carrier
      - runtime reads platform ThreadLocal tenant
    verification:
      - golden_trace_test
      - static_architecture_check
    related_adr:
      - ADR-A2DH-005
    related_scenarios:
      - BA-001
      - BA-002
      - BA-003
      - technical/S1
      - technical/S2
      - technical/S3
      - technical/S4
      - technical/S5

  - id: INV-008
    name: IrreversibleSideEffectsNeedAuditAndIdempotency
    rule:
      irreversible_side_effects_require:
        - idempotency
        - audit
        - trace
    forbidden:
      - duplicate attempt repeats irreversible tool call
      - side effect has no audit envelope
    verification:
      - contract_test
      - failure_injection_test
    related_adr:
      - ADR-A2DH-004
      - ADR-A2DH-005
    related_scenarios:
      - BA-001
      - BA-002
      - technical/S4
      - technical/S5

  - id: INV-009
    name: SubAgentVisibleUnderRunTree
    rule:
      child_work_must_be_correlated_under_parent_run: true
    forbidden:
      - implicit cross workflow handoff
      - child agent writes parent terminal state directly
    verification:
      - future_scenario_test
      - manual_architecture_review
    related_adr:
      - ADR-A2DH-001
    related_scenarios:
      - BA-003
      - future-child-run-technical-scenario
```
