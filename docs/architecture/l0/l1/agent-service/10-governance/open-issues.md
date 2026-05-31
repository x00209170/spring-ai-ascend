---
level: L1
view: modules
status: draft
---

# Agent Service Open Issues

## 目的

记录 `agent-service` development pack 中尚未定稿、但不阻塞当前模块 L1 启动的问题。每个问题都必须有影响范围和建议归宿，避免在实现中隐式拍脑袋。

| ID | 问题 | 为什么重要 | 影响范围 | 建议归宿 |
|---|---|---|---|---|
| OI-AS-001 | Task timeline / evidence query 的正式 contract 尚未定义。 | BA-001 强依赖开发者能看到 step timeline、context evidence、tool decision evidence；没有 contract，harness 只能测内部对象。 | AS-SLICE-010, Observability, agent-client | Developer Experience ICD / Observability ICD |
| OI-AS-002 | Service SSE stream event schema 尚未定义。 | 已明确 `agent-service` 通过 SSE 输出实时结果，但事件类型、错误语义、完成语义和断线恢复策略还不能在 L1 写死。 | AS-SLICE-009, agent-client | Service SSE Stream ICD / L2 |
| OI-AS-003 | 内部异步队列是否存在、如何与 Bus 分界尚未定稿。 | 旧 L1 有 Internal Event Queue 和三通道概念；当前只能保留内部意图分发方向，不能把 Bus data channel 写成 payload 通道。 | AS-SLICE-006, AS-SLICE-008 | L2 process design / Bus contract |
| OI-AS-004 | A2A 流式回传技术方案未定。 | 调用方要求被调方流式返回时，逐 token 走 Bus 可能造成控制通道压力。 | AS-SLICE-008, BA-003, S6 | A2A stream ADR / dedicated stream contract |
| OI-AS-005 | TaskEvent / RunEvent 命名迁移需要实施策略。 | 旧文档和现有代码可能存在 RunEvent / RunStatus；新架构要求 Task canonical，但不能破坏兼容。 | AS-SLICE-003, AS-SLICE-010, Observability | TaskEvent compatibility plan / L2 |
| OI-AS-006 | AgentRegistry / AgentDefinition runtime binding 仍需细化。 | service 承载 Agent registration surface，但 AgentDefinition 的版本、发布、回滚和租户配置治理仍需和 PaaS 模式对齐。 | AS-SLICE-002, BA-001 weak department PaaS | PaaS tenant configuration governance |
| OI-AS-007 | Replay-safe fixture export 的脱敏策略未定。 | 该能力能显著提升开发者调试体验，但存在 PII、跨租户和客户核心业务数据风险。 | AS-SLICE-012 | Replay-safe Fixture ICD / governance |
| OI-AS-008 | Capability placement runtime binding 仍为 draft。 | service 必须判断 local client、business service、platform middleware、delegated adapter 的执行位置；如果 binding 不稳定，强部门和个人客户场景会分叉实现。 | AS-SLICE-005, AS-SLICE-006 | ICD-CS-Capability-Placement L2 |

## 不阻塞当前启动的原因

- 当前 L1 已经明确 `agent-service` 的状态 owner、非职责、同进程 engine 边界、Bus 边界和核心开发切片。
- 未定问题主要影响 wire schema、event schema、L2 物理实现和治理细节。
- 这些问题需要在对应切片进入实现前解决，但不妨碍模块负责人先围绕 AS-SLICE-001..011 拆任务和写 harness。
