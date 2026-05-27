---
level: L2
view: process
status: draft
---

# ICD-CS-Capability-Placement

## 目的

定义 C-Side 与 S-Side 之间的能力放置和 handoff 语义，支撑本地工具调用、本地上下文装配、平台公共中间件调用和混合部署形态。

## 适用读者

集成 `agent-client` 的应用开发者、agent-client、agent-service、agent-bus、agent-middleware、harness 生成器、架构评审者。

## 维护规则

- 本 ICD 是 draft，正式 wire contract 需要与 ADR-0049、ADR-0051、ADR-0074、ADR-0101 和 `s2c-callback.v1.yaml` 对齐。
- 不得把 C-Side business fact、business ontology 或本地凭据默认写入 S-Side 状态。
- 每个 handoff 必须保留 tenant、trace、task、task cursor 和 execution locus 证据；runId 只作为历史兼容关联字段。

| Field | Value |
|---|---|
| ICD ID | ICD-CS-Capability-Placement |
| Participating Modules | agent-client, agent-bus, agent-service, agent-execution-engine, agent-middleware |
| Interaction Purpose | 为一次 Agent 执行中的 context、tool、memory、middleware 能力选择执行位置，并在 C-Side / S-Side 之间传递受控指令和结果。 |
| Direction | S-Side -> C-Side capability request；C-Side -> S-Side capability result；S-Side -> platform middleware adapter when platform-owned capability is selected。 |
| Deployment Variants | platform_centric；weak_department_paas；protected_local_capability；business_centric；hybrid_enterprise_individual。 |
| Placement Values | `platform_hosted_service`；`local_client`；`business_service`；`platform_middleware`；`delegated_adapter`；`federation_remote`。 |
| Request Semantics | capability request 必须包含 capabilityRef、placement、taskId、taskCursorRef、businessRuleSubsetRef、skillPoolLimitRef、traceId、idempotencyKey、redactionPolicyRef；runId 可作为兼容字段。 |
| Response Semantics | capability result 必须返回 outcome、resultRef 或 resultSummary、evidenceRef、businessFactEvents、costReceipt、errorEnvelope；敏感 payload 可只返回 opaque reference。 |
| Context Semantics | C-Side 本地装配 context 时，S-Side 只能收到 placeholder-preserved result、source refs、token estimate、filter reason 或 delegated context package。 |
| Tool Semantics | 本地工具调用必须通过 S2C / Yield handoff；平台工具调用必须通过 agent-middleware Skill / adapter；两者都必须产生 tool decision evidence。 |
| Memory Semantics | business ontology 和 business facts 属于 C-Side；S-Side 只保存执行轨迹、指标、审计和已授权 delegated memory。 |
| Error Semantics | local_unavailable、capability_denied、placement_forbidden、delegation_required、placeholder_violation、platform_adapter_unavailable 必须可区分。 |
| Retry Responsibility | S-Side 可重试 compute compensation；C-Side local capability 的副作用重试由 C-Side idempotency contract 决定。 |
| Timeout Semantics | local capability timeout 触发 suspend / retry / degraded business decision，不得被静默转换为平台工具调用。 |
| Idempotency Semantics | 每个 handoff 使用 idempotencyKey；本地工具副作用必须由 C-Side 保证去重或返回 duplicate evidence。 |
| Security / Permission Semantics | placement 不能扩大权限；C-Side 凭据不得透传给 S-Side；platform_middleware adapter 使用平台授权和 tenant guard。 |
| Hosted Service Semantics | weak_department_paas 下，平台托管 runtime operations；tenant 只能通过受控配置、授权、发布和回滚入口影响 Agent 行为。 |
| Audit Semantics | 记录 placement decision、policy reason、capabilityRef、actor、tenant、trace、outcome、delegation grant ref。 |
| Observability Fields | tenantId, traceId, taskId, runId when present as compatibility field, capabilityRef, placement, execution_locus, policyDecision, outcome, latency, costReceipt。 |
| Versioning Strategy | 新 placement value additive；改变 ownership 或 side-effect 语义属于 breaking change，需要 ADR / CR。 |
| Contract Tests | sensitive_context_not_persisted_by_platform；local_tool_uses_s2c_handoff；platform_public_service_uses_middleware_adapter；execution_locus_trace_present；placeholder_preserved_roundtrip。 |
| Open Issues | machine-readable contract 尚为 draft；delegation grant schema、redaction policy schema 和 local capability registry 尚未正式定义。 |
