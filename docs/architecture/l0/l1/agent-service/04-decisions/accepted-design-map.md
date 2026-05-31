---
level: L1
view: modules
status: draft
---

# Agent Service Accepted Design Map

## 目的

对 `docs/architecture/l0/l1/agent-service/*` 中已有设计项做准入判定，避免把旧口径、实现细节或已冲突内容原样迁移到新的 `docs/architecture` 文档体系。

## 准入分类

| Classification | 含义 | 迁移方式 |
|---|---|---|
| Adopt | 与当前架构一致，可进入本 development pack。 |
| Adopt with Rewrite | 思路有价值，但术语、owner 或边界需要按当前架构改写。 |
| Implementation Constraint | 不进入 L0 / L1 模块边界，进入后续 L2 或实现约束。 |
| Open Issue | 方向未定或存在冲突，先记录问题，不作为已决设计。 |
| Reject | 与当前架构原则冲突，不迁移。 |

## 迁移判定表

| Source Item | 原始含义 | Classification | 当前判定 | 迁移目标 |
|---|---|---|---|---|
| Access Layer | HTTP / A2A / MQ / Ingress 收敛，绑定 tenant、auth、idempotency、trace。 | Adopt with Rewrite | 采纳“入口治理”职责，但 Gateway 不作为 `agent-service` 内部模块；微服务 Gateway 只代理 `agent-service`，A2A 控制指令不走普通 HTTP 入口。 | `logical-design.md` L1；S1 |
| IngressGateway | 旧文档中的入口组件名。 | Adopt with Rewrite | 改写为“微服务 Gateway 能力 + agent-service HTTP 对外入口”；不得写成 L0 架构模块，也不得归入 Bus。 | `logical-design.md` L1；governance DOC-C-002.b |
| A2A Service / MQ Adapter | 旧文档中作为 Access Layer 的入口变体。 | Adopt with Rewrite | A2A 跨边界控制指令归 `agent-bus`；`agent-service` 只接收受控回调、resume 或平台托管请求，不拥有 Bus physical channel。MQ adapter 若出现，属于 transport binding，不是 L1 核心模块边界。 | S5 / S6；后续 A2A ICD |
| Run aggregate single owner | Run / RunStatus / RunRepository 是旧服务端执行状态 owner。 | Adopt with Rewrite | 改写为 Task lifecycle single owner；`RunRepository` / `RunStateMachine` 只能作为历史实现兼容入口。 | `logical-design.md` L2；State Matrix |
| Task aggregate | Task / TaskStateStore。 | Adopt | `agent-service` 是 Task Execution State 和 Task tree relationship 的 owner。 | `logical-design.md` L2；S1 / S2 / S5 / S6 |
| Session aggregate | Session / ContextProjector。 | Adopt | `agent-service` 维护 Session shell；Context projection 是 `agent-service` + `agent-middleware` 的能力协作。 | `logical-design.md` L2；S3 |
| IdempotencyRecord | 入口幂等记录。 | Adopt | 作为 `agent-service` owned state；需要 S1 harness 覆盖重复请求。 | `development-slices.md` AS-SLICE-002 |
| Internal Event Queue | 旧文档中的 service.queue / Producer / Consumer。 | Open Issue | 可以保留“内部异步意图分发”的概念，但当前不把它写成已交付层；不得把 Bus data channel 写成 payload 通道。 | `open-issues.md` OI-AS-003 |
| Three-Track Bus: control / data / rhythm | 旧文档把 bus 三通道作为 Layer 3 binding。 | Adopt with Rewrite | control / rhythm 可作为 Bus 治理方向；data 改写为 data-reference，不承载大型 payload。`agent-service` 只消费 Bus contract，不拥有 Bus physical channel。 | `logical-design.md` L5；S5 / S6 |
| External Runtime: agent-execution-engine | 旧图把 engine 画成 External Runtime。 | Adopt with Rewrite | 当前已决策 service 与 engine 同进程运行；engine 是独立架构模块，但不是默认远程服务。 | `logical-design.md` L4；S2 |
| RuntimeMiddleware exclusive home | RuntimeMiddleware 归控制层。 | Adopt with Rewrite | 当前模块边界中 RuntimeMiddleware SPI owner 是 `agent-middleware`；`agent-service` 只触发 hook / governance intent，不拥有 middleware 全局语义。 | `logical-design.md` L4 / L5 |
| Engine Dispatch & Execution Layer | EngineRegistry、ExecutorAdapter、GraphExecutor、AgentLoopExecutor。 | Adopt with Rewrite | 归 `agent-execution-engine`；`agent-service` 同进程调用 engine，并只接收状态转换意图、tool intent、context request、child-task intent。 | `logical-design.md` L4；S2 |
| Translation & Tool-Intercept Layer | ContextProjector、PromptTemplate、StructuredOutputConverter、ChatAdvisor。 | Adopt with Rewrite | ContextProjector 的 service 侧可保留；Prompt / Advisor / ModelGateway / Tool Gateway 语义归 `agent-middleware`。 | `logical-design.md` L3 / L5 |
| Tenant isolation / RLS | tenantId-first、隔离与审计。 | Adopt | 保留为实现约束和 harness 断言；具体表结构下沉 L2。 | `development-slices.md` AS-SLICE-001 / 002 |
| CAS 状态转换 | updateIfNotTerminal / terminal idempotency。 | Adopt with Rewrite | 保留为 TaskStateStore controlled transition；方法名属于实现兼容，不进入 L0。 | `logical-design.md` L2；development slice |
| RunStatus State Machine | 旧 Run 状态机。 | Adopt with Rewrite | 改写为 Task 状态机；历史 RunStatus 只作为兼容映射。 | State Matrix；S1 / S2 / S5 |
| sealed RunEvent hierarchy | 旧执行事件层级。 | Adopt with Rewrite | 改写为 TaskEvent / execution event hierarchy；RunEvent 只作为历史命名兼容。 | Observability / future ICD |
| Package matrix / class names | 包结构、类名、方法名。 | Implementation Constraint | 不进入本 L1 pack 主体；进入后续 development view 或 L2。 | 后续 L2 |
| SSE streaming endpoint | 旧 L1 未系统表达的新决策。 | Adopt | 新增为 `agent-service` 对外实时输出能力，client 消费 SSE，Bus 不承载 token stream。 | `logical-design.md`, `process-design.md`, `harness-design.md` |

## 迁移原则

- 旧文档中能帮助模块开发的内容，不因为术语旧就丢弃；但必须改写后进入当前口径。
- 与“Task canonical、service-engine 同进程、Gateway / Bus 拆分、Bus 不承载 payload / token stream”冲突的内容，不兼容保留。
- 类、方法、包、表、topic、filter 名称只能作为实现约束或历史线索，不提升为架构边界。
