---
level: L2
view: process
status: draft
---

# ICD-AgentService-ContextEngine

## 目的

定义 agent-service 执行层到 Context Engine capability 的上下文装配交互语义。

## 适用读者

agent-service、agent-middleware、ContextProjector / Memory / Retriever 负责人。

## 维护规则

- Context Engine 是 capability 聚合，不是独立模块。
- 不得把业务 memory 内容写成平台 runtime state。

| Field | Value |
|---|---|
| ICD ID | ICD-AgentService-ContextEngine |
| Participating Modules | agent-service, agent-middleware |
| Interaction Purpose | 为 agent step 构建可执行上下文包，聚合 Session、Memory、Retriever、Vector 和 prompt variables。 |
| Direction | Agent Service -> Context Engine；Context Engine -> context package / projection result。 |
| Sync / Async Model | 初始 draft 可以同步；长上下文构建可异步化。 |
| Request Semantics | 包含 tenant、task/session identity、context need、memory category、trace context；runId 只作为兼容关联字段。 |
| Response Semantics | 返回 context package ref / inline projection / unavailable reason。 |
| Event Semantics | context.requested、context.built、context.failed。 |
| State Semantics | Session owner 在 agent-service；Memory owner 在 agent-middleware SPI；ContextPackage 是输出契约。 |
| Error Semantics | missing session、memory unavailable、retrieval timeout、policy denied。 |
| Retry Responsibility | Agent Service 可重试 projection；Memory write retry 由 Memory owner 决定。 |
| Timeout Semantics | context build timeout 不能直接标记 Task failed，除非 scenario policy 这样要求。 |
| Idempotency Semantics | 相同 session version + request shape 应产生等价 context package。 |
| Security / Permission Semantics | 所有 memory / vector read 必须使用 tenant scope。 |
| Audit Semantics | context source、version、redaction decision 进入 trace / audit。 |
| Observability Fields | tenantId, traceId, taskId, runId when present as compatibility field, sessionId, memoryCategory, contextVersion, sourceRefs。 |
| Versioning Strategy | ContextPackage schema breaking change requires ADR / CR。 |
| Backward Compatibility Rules | 新 context source additive；删除 source 或改变 projection semantics 为 breaking。 |
| Contract Tests | context_package_has_tenant_scope；same_input_same_projection_metadata；retrieval_timeout_visible；forbidden_memory_category_rejected。 |
| Open Issues | ContextPackage 是否正式迁移到 `docs/contracts/` 待决。 |
