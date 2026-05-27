---
level: L1
view: scenarios
status: draft
---

# Verification Matrix

## 目的

把 ADR、ICD、Scenario、Invariant、Harness 要求纳入统一验证矩阵，指导后续 CI gate、contract test 和 harness 生成。

## 适用读者

测试负责人、模块负责人、AI agent、评审者。

## 维护规则

- 没有验证方式的设计项标记为 `Unverified`。
- 新增设计项必须追加矩阵行，不得只改正文。
- Status 只能表达本文档验证计划状态，不代表 production shipped。

| Design Item ID | Design Item Type | Description | Related Module | Related Scenario | Verification Type | Test / Review Name | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| ADR-A2DH-001 | ADR | Task lifecycle state writer single owner; historical Run naming is compatibility only | agent-service, agent-execution-engine | BA-001, BA-002, BA-003; technical S1, S2, S5, S6 | State Machine Test, Static Architecture Check | task_state_single_writer_harness | AgentService | Draft |
| ADR-A2DH-002 | ADR | Cross-module interaction is contract-first | all core modules | BA-001..BA-003; technical S1..S6 | Manual Architecture Review | contract_first_review_checklist | Architecture | Draft |
| ADR-A2DH-003 | ADR | Context ownership split across Session and Memory contracts | agent-service, agent-middleware | BA-001; technical S3 | Contract Test | context_package_contract_harness | AgentService | Draft |
| ADR-A2DH-004 | ADR | Tool calls go through governance | agent-middleware, agent-service | BA-001, BA-002; technical S4, S5 | Contract Test, Security Review | tool_gateway_governance_harness | Middleware | Draft |
| ADR-A2DH-005 | ADR | Core modules are harness-first | all core modules | BA-001..BA-003; technical S1..S6 | Manual Architecture Review | harness_first_readiness_review | Architecture | Draft |
| ICD-Gateway-Workflow | ICD | Entry intent to Workflow | agent-service, agent-bus | BA-001, BA-002; technical S1, S5 | Contract Test | duplicate_create_is_idempotent | AgentService | Draft |
| ICD-Workflow-AgentService | ICD | Workflow state intent to Task owner | agent-service, agent-execution-engine | BA-001, BA-002, BA-003; technical S2, S5 | State Machine Test | invalid_transition_rejected | AgentService | Draft |
| ICD-AgentService-ContextEngine | ICD | Context projection and package build | agent-service, agent-middleware | BA-001; technical S3 | Contract Test | context_package_has_tenant_scope | AgentService | Draft |
| ICD-AgentService-ToolGateway | ICD | Governed skill/tool call | agent-middleware, agent-service | BA-001, BA-002; technical S4 | Contract Test, Failure Injection | permission_denied_has_no_side_effect | Middleware | Draft |
| ICD-Workflow-Observability | ICD | Runtime evidence emission | agent-service, agent-middleware | BA-001..BA-003; technical S1..S6 | Golden Trace Test | suspend_resume_trace_sequence | Observability | Draft |
| ICD-CS-Capability-Placement | ICD | C-Side / S-Side capability placement and local handoff | agent-client, agent-bus, agent-service, agent-middleware | BA-001, BA-002, BA-003; technical S3, S4, S5, S6 | Contract Test, Security Review | capability_placement_handoff_harness | Architecture | Draft |
| BA-001 | Business Activity Scenario | Agent handles business request | agent-client, agent-bus, agent-service, agent-execution-engine, agent-middleware | BA-001 | Scenario Test, Golden Trace Test | ba_001_agent_handles_business_request_harness | Architecture | Draft |
| BA-002 | Business Activity Scenario | Human approval tool call | agent-service, agent-execution-engine, agent-middleware, agent-bus, agent-client | BA-002 | Scenario Test, State Machine Test, Failure Injection | ba_002_human_approval_tool_call_harness | Architecture | Draft |
| BA-003 | Business Activity Scenario | Multi-agent delegation | agent-service, agent-execution-engine, agent-bus, agent-middleware | BA-003; technical S6 | Scenario Test, Manual Architecture Review | ba_003_multi_agent_delegation_harness | Architecture | Draft |
| CAP-11 | Capability | Developer Experience / Operational Insight | agent-client, agent-service, agent-bus, agent-execution-engine, agent-middleware | BA-001, BA-002, BA-003; technical S1..S6 | Golden Trace Test, Metrics Assertion, Harness Review | developer_operations_insight_harness | Observability | Draft |
| CAP-12 | Capability | Deployment Locus / Capability Placement | agent-client, agent-bus, agent-service, agent-execution-engine, agent-middleware | BA-001, BA-002, BA-003; technical S3, S4, S5, S6 | Contract Test, Security Review, Scenario Test | capability_placement_policy_harness | Architecture | Draft |
| PAAS-WeakDepartment | Deployment Variant | Weak department uses platform-hosted service without local runtime deployment | agent-service, agent-execution-engine, agent-middleware, agent-bus | BA-001; technical S1, S3, S4 | Scenario Test, Configuration Governance Review | weak_department_hosted_service_onboarding | Architecture | Draft |
| S1 | Technical Sub-scenario | Create Task / Invocation mechanism | agent-service | BA-001, BA-002; technical S1 | Scenario Test | create_task_invocation_scenario_harness | AgentService | Draft |
| S2 | Technical Sub-scenario | Execute Agent Step mechanism | agent-service, agent-execution-engine | BA-001, BA-002, BA-003; technical S2 | Scenario Test | execute_agent_step_harness | Engine | Draft |
| S3 | Technical Sub-scenario | Build Context Package mechanism | agent-service, agent-middleware | BA-001; technical S3 | Contract Test | build_context_package_harness | AgentService | Draft |
| S4 | Technical Sub-scenario | Tool Call With Governance mechanism | agent-middleware | BA-001, BA-002; technical S4 | Failure Injection Test | tool_call_with_governance_harness | Middleware | Draft |
| S5 | Technical Sub-scenario | Suspend / Resume mechanism | agent-service, agent-execution-engine, agent-bus | BA-002, BA-003; technical S5 | State Machine Test, Failure Injection | suspend_resume_harness | Engine | Draft |
| S6 | Technical Sub-scenario | Child Task / Federation mechanism | agent-service, agent-bus, agent-execution-engine, agent-middleware | BA-003; technical S6 | Scenario Test, Failure Injection, Golden Trace Test | child_task_federation_harness | Architecture | Draft |
| INV-001 | Invariant | Platform/business decoupling | all | BA-001, BA-002; technical S1, S4 | Static Architecture Check | platform_business_decoupling_scan | Architecture | Draft |
| INV-002 | Invariant | Task State single writer | agent-service | BA-001, BA-002, BA-003; technical S1, S2, S5, S6 | Static Architecture Check, State Machine Test | task_state_single_writer_scan | AgentService | Draft |
| INV-003 | Invariant | Tool calls through governance | agent-middleware | BA-001, BA-002; technical S4 | Contract Test | tool_calls_through_governance | Middleware | Draft |
| INV-004 | Invariant | Context packages produced by Context Engine capability | agent-service, agent-middleware | BA-001; technical S3 | Contract Test | context_projection_contract | AgentService | Draft |
| INV-005 | Invariant | Business state not owned by runtime | external business modules | BA-001, BA-002; technical S4 | Manual Architecture Review | business_state_boundary_review | Architecture | Draft |
| INV-006 | Invariant | Suspend instead of hold | agent-execution-engine, agent-service | BA-002, BA-003; technical S5 | Failure Injection Test | suspend_releases_execution_resource | Engine | Draft |
| INV-007 | Invariant | Trace context propagation | all core modules | BA-001..BA-003; technical S1..S6 | Golden Trace Test | required_trace_fields_present | Observability | Draft |
| INV-008 | Invariant | Irreversible side effects need audit and idempotency | agent-middleware | BA-001, BA-002; technical S4, S5 | Contract Test, Failure Injection | duplicate_tool_attempt_guard | Middleware | Draft |
| INV-009 | Invariant | Sub-agent visible under Task tree | agent-service, agent-bus | BA-003; technical S6 | Scenario Test, Golden Trace Test | swarm_task_tree_review | Architecture | Draft |
| HAR-Workflow | Harness | Workflow harness spec | agent-execution-engine, agent-service | BA-001, BA-002, BA-003; technical S1, S2, S5, S6 | Harness Review | workflow_harness_readiness | Engine | Draft |
| HAR-AgentService | Harness | Agent Service harness spec | agent-service | BA-001, BA-002, BA-003; technical S1, S2, S3, S5, S6 | Harness Review | agent_service_harness_readiness | AgentService | Draft |
| HAR-ToolGateway | Harness | Tool Gateway harness spec | agent-middleware | BA-001, BA-002; technical S4 | Harness Review | tool_gateway_harness_readiness | Middleware | Draft |
| HAR-ContextEngine | Harness | Context Engine harness spec | agent-service, agent-middleware | BA-001; technical S3 | Harness Review | context_engine_harness_readiness | AgentService | Draft |
| HAR-Observability | Harness | Observability harness spec | agent-service, agent-middleware | BA-001..BA-003; technical S1..S6 | Harness Review | observability_harness_readiness | Observability | Draft |

## Verification Type 说明

| Type | 用途 |
|---|---|
| Contract Test | 验证 ICD / YAML 语义。 |
| Scenario Test | 验证端到端流程和 assertions。 |
| State Machine Test | 验证合法和非法状态转换。 |
| Failure Injection Test | 验证 timeout、duplicate、out-of-order、provider failure。 |
| Compatibility Test | 验证 additive / breaking change 策略。 |
| Golden Trace Test | 验证 trace/event/audit 序列。 |
| Static Architecture Check | 验证依赖方向、forbidden writer、layer purity。 |
| Manual Architecture Review | 处理尚不能自动化的设计边界。 |
| Security Review | 验证权限、租户和 policy 语义。 |
