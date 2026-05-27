---
level: L1
view: logical
status: draft
---

# ADR-003: Context Ownership

## 目的

明确 Context Engine 是能力聚合，不是当前独立 reactor module，并区分 Session、Memory、Context Package 的 owner。

## 适用读者

agent-service、agent-middleware、AI agent、harness 生成器。

## 维护规则

本文是交付视角 ADR 草案；正式语义仍以 ADR-0123、ADR-0124、ADR-0133 和 module metadata 为准。

| Field | Value |
|---|---|
| ADR ID | ADR-A2DH-003 |
| Title | Context is assembled from Session and Memory contracts, not owned by one hidden module |
| Status | Draft |
| Context | task.md 要求 Context Engine owns context packaging and versioning；当前仓库中 ContextProjector、MemoryStore、Retriever、VectorStore、PromptTemplate 分布在 agent-service 与 agent-middleware。 |
| Problem | 如果假设存在独立 Context Engine module，AI agent 会生成错误路径和错误依赖方向。 |
| Decision | Context Engine 在本目录中表示 capability 聚合。Session owner 在 agent-service；Memory / Vector / Retriever / Prompt 语义 owner 在 agent-middleware；Context Package 是跨 SPI 的输出契约。 |
| Alternatives Considered | A. 新增独立 module，超出当前任务。B. 把所有 context 归 agent-service，忽略 middleware SPI。C. 当前选择：能力聚合 + 显式 ICD。 |
| Consequences | Harness 要同时 mock Session / ContextProjector 和 Memory / Retriever；State Matrix 必须区分 Session、Memory、Context Package。 |
| Impacted Modules | agent-service, agent-middleware |
| Related Principles | PR-002, PR-003 |
| Related Contracts | ICD-AgentService-ContextEngine |
| Related Scenarios | BA-001; technical S3 |
| Verification Method | Contract Test, Projection Assertion |
| Open Questions | Context Package 是否迁移为正式 `docs/contracts/*.yaml` 需要后续 ADR。 |
