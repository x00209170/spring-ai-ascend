---
level: L0
view: scenarios
status: draft
---

# System Principles

## 目的

把权威治理原则翻译为本目录可执行的文档约束，使后续 AI agent 和模块负责人能从原则直接落到 capability、contract、scenario、harness 和 verification。

## 适用读者

架构师、模块负责人、harness 生成器、评审者。

## 维护规则

- 本文只解释交付含义，不复制 `CLAUDE.md` 的全部规则正文。
- 每条原则必须有下游 artifact 和 verification。
- 如果原则暂时无法自动验证，必须提供人工评审方式。

## 原则到交付约束

| ID | 原则 | 交付约束 | 关联 artifact | 验证 |
|---|---|---|---|---|
| PR-001 | Business / Platform Decoupling | 业务实现只通过 SPI、配置和业务模块接入，不修改平台内部实现。 | Module Cards, `INV-001` | static architecture check, PR review |
| PR-002 | Capability Before Module | 先定义能力，再映射模块，避免因为模块名相似重复建设。 | Capability Map | architecture review |
| PR-003 | Single State Owner | 每类状态只有一个 owner，writer 比 reader 更严格。 | State Matrix, `INV-002` | state-machine test, static check |
| PR-004 | Contract-first Interaction | 所有核心跨模块调用必须有 ICD 和 contract test idea。 | ICD, machine-readable YAML | contract test |
| PR-005 | Scenario-first Verification | 每个 BA-* 核心场景必须能串起业务活动、模块协作和 assertions；technical sub-scenario 只验证机制。 | Scenario Specs | scenario test |
| PR-006 | Harness-first Development | 模块开发从 mocks、stubs、fixtures、contract tests、failure injection 开始。 | Harness Specs | local runner, CI gate |
| PR-007 | Design-only Honesty | 未 runtime enforced 的 contract 不得写成已交付能力。 | Inventory, ICD, Scenario | review checklist |
| PR-008 | Runtime-owned Governance | 模型、工具、memory、planner、callback 的跨切面策略走 runtime hook、middleware、capacity、audit。 | Tool Gateway ICD, Invariants | ArchUnit, scenario test |
| PR-009 | Cursor / Suspend Instead of Hold | 长任务返回 cursor 或进入 suspend/resume，不占用客户端连接或执行线程。 | BA-002, technical S5, Workflow Harness | state machine test |
| PR-010 | Trace and Audit Everywhere | 核心交互传播 tenant、trace、Task identity 和必要的 client invocation reference，失败路径也有可见信号。 | Observability ICD, Verification Matrix | golden trace test |
| PR-011 | Explicit Capability Placement | 工具、上下文、memory、retriever、approval UI、A2A 都必须声明执行位置和数据边界。 | `ICD-CS-Capability-Placement`, BA-001, technical S3-S6 | scenario test, security review |
| PR-012 | Platform LLM Cost Governance | LLM 调用成本由平台统一统计和归集；客户内部工具成本不由平台定价。 | BA-001, BA-003, technical S1, S2, S4, S6 | metrics assertion, governance review |
| PR-013 | Boundary-mediated A2A | 同一 `agent-service` 进程内的多 Agent 协作由 service 管理；跨 service / 跨部门 / 跨部署边界的 A2A 控制指令必须通过 `agent-bus`，不得由 engine 或业务 service 私连绕过平台控制面。 | BA-003, technical S6 | golden trace test, architecture review |
| PR-014 | Control / Data / Stream Separation | Gateway 只代理对外暴露的 `agent-service`；`agent-service` 通过 SSE 承载对外实时输出；`agent-bus` 只承载控制指令、callback 和 data reference envelope，不承载大型 payload 或逐 token stream。 | BA-001, technical S6, Module Cards | scenario assertion, architecture review |

## A2D-H 使用规则

任何新设计项必须回答：

```text
Principle:
Capability:
Owner:
Contract:
Scenario:
Executable Spec:
Harness:
Verification:
Governance:
```

无法填写的字段标记为 `Traceability Missing`，不得进入已完成状态。

## 禁止模式

- 用“协助处理”“参与管理”替代明确职责。
- 把实现选项写成架构原则。
- 把某个 module 的 reference implementation 写成全局契约。
- 在 L0 中写具体表、key、topic、method signature、timeout 数值。
- 把 design_only 能力写成 shipped。
- 把客户数据源权限配置写成平台自有权限模型。
- 把跨 service / 跨部门 / 跨部署 A2A 写成 service / engine 之间的私有直连，或把同一 service 内协作错误下放给 Bus。
- 把 Bus 写成大型多模态 payload 通道、逐 token stream 通道或微服务 Gateway。
- 把 service 对外 SSE 流式输出、A2A 控制指令和对象存储 data path 混成同一个通信机制。
- 让 AI agent 根据单一大文档生成代码，而不经过 ICD、Scenario 和 Harness。
