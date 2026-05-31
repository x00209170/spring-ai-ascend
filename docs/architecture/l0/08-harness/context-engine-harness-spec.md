---
level: L2
view: scenarios
status: draft
---

# Context Engine Harness Spec

## 目的

为 Context Engine capability 生成 context projection 和 memory/retrieval integration 的 harness 草案。

## 适用读者

agent-service、agent-middleware、AI agent、测试负责人。

## 维护规则

- Context Engine 是 capability 聚合，harness 不创建不存在的 module。
- ContextPackage schema 仍为 draft。

| Field | Value |
|---|---|
| Module Name | Context Engine capability |
| Module Boundary | ContextProjector + Session + Memory / Retriever / Vector / PromptTemplate SPI |
| Responsibilities | build context package；validate tenant scope；record source refs；classify unavailable source。 |
| Non-Responsibilities | 不写 Task State；不执行业务工具；不拥有 Memory provider internals。 |
| Owned State | ContextPackage draft；projection metadata。 |
| Provided Contracts | ICD-AgentService-ContextEngine |
| Consumed Contracts | memory-store, vector-store, prompt-template |
| Upstream Mocks | Agent Service context request；Session snapshot。 |
| Downstream Stubs | Memory store success/empty/unavailable；Retriever timeout；PromptTemplate failure。 |
| Scenario Fixtures | BA-001; technical S3。 |
| Contract Tests | context_package_has_tenant_scope；same_input_same_projection_metadata；forbidden_memory_category_rejected。 |
| State Machine Tests | none direct; assert no direct Task State mutation。 |
| Failure Injection Cases | retrieval timeout；memory unavailable；schema invalid；source permission denied。 |
| Golden Trace Assertions | context.requested；context.built_or_failed；source refs present。 |
| Architecture Invariants | INV-004, INV-007 |
| Compatibility Tests | additive context source tolerated；schema breaking requires ADR。 |
| Local Runner Requirement | fake memory/retriever stubs。 |
| CI Gate Requirements | contract tests when context package formalizes。 |
