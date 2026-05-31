---
level: L1
view: development
status: draft
---

# ADR-002: Contract-first Module Interaction

## 目的

要求核心跨模块协作先有 ICD 和 machine-readable contract，再进入 harness 和实现。

## 适用读者

所有模块负责人、AI agent、测试负责人。

## 维护规则

本文是交付视角 ADR 草案；正式生产契约仍以 `docs/contracts/` 和 `docs/contracts/contract-catalog.md` 为准。

| Field | Value |
|---|---|
| ADR ID | ADR-A2DH-002 |
| Title | Cross-module interaction is contract-first |
| Status | Draft |
| Context | task.md 要求模块并行开发、harness 生成和集成验证不依赖口头约定。 |
| Problem | 只按模块职责开发会遗漏状态、错误、幂等、重试、权限和观测语义。 |
| Decision | Gateway ↔ Workflow、Workflow ↔ Agent Service、Agent Service ↔ Tool Gateway、Agent Service ↔ Context Engine、Workflow ↔ Observability 必须有 human-readable ICD 和 machine-readable draft contract。 |
| Alternatives Considered | A. 只写 API 字段，无法生成 harness。B. 只写场景，缺少模块边界。C. 当前选择：ICD + YAML + scenario assertions 三件套。 |
| Consequences | 每个新 interaction 必须进入 Verification Matrix；机器可读 YAML 进入生产前迁移到 `docs/contracts/`。 |
| Impacted Modules | agent-service, agent-execution-engine, agent-middleware, agent-bus, agent-client |
| Related Principles | PR-004, PR-006 |
| Related Contracts | all ICD documents |
| Related Scenarios | BA-001..BA-003; technical S1..S6 |
| Verification Method | Contract Test, architecture review |
| Open Questions | 哪些 draft YAML 会正式进入 `docs/contracts/` 需要后续 ADR。 |
