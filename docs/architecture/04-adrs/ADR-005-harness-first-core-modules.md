---
level: L1
view: scenarios
status: draft
---

# ADR-005: Harness-first Core Modules

## 目的

规定核心模块开发必须从 harness spec、contract tests、scenario fixtures 和 failure injection 开始。

## 适用读者

模块负责人、测试负责人、AI agent。

## 维护规则

本文是本目录的交付 ADR 草案；实现前必须把具体 test class 和 CI gate 写入 Verification Matrix。

| Field | Value |
|---|---|
| ADR ID | ADR-A2DH-005 |
| Title | Core module development is harness-first |
| Status | Draft |
| Context | task.md 要求架构文档能支撑 harness 生成、测试验证和模块并行开发。 |
| Problem | 直接写实现会把跨模块语义藏在代码里，导致 AI agent 生成不一致的 mocks、stubs 和错误处理。 |
| Decision | Workflow / Orchestrator、Agent Service、Tool Gateway、Context Engine、Observability 都必须有 harness spec。实现任务只能消费已明确的 mocks、stubs、fixtures、contract tests、state-machine tests 和 golden trace assertions。 |
| Alternatives Considered | A. 先实现再补测试，反馈太晚。B. 只做集成测试，定位困难。C. 当前选择：模块 harness + scenario fixtures + verification matrix。 |
| Consequences | Verification Matrix 成为后续 CI gate 和 harness 生成入口；缺少 verification 的设计项标记为 Unverified。 |
| Impacted Modules | all core modules |
| Related Principles | PR-005, PR-006 |
| Related Contracts | all ICD documents |
| Related Scenarios | BA-001..BA-003; technical S1..S6 |
| Verification Method | Contract Test, Scenario Test, Failure Injection, Golden Trace Test |
| Open Questions | 是否将本目录 draft harness 自动生成到测试源码，需要后续实现设计。 |
