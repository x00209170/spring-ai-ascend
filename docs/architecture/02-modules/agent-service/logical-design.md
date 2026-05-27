---
level: L1
view: logical
status: draft
---

# Agent Service Logical Design

## 目的

定义 `agent-service` 在当前架构口径下的内部职责层、状态边界、跨模块协作和开发切片入口，使模块开发可以在不等待所有 L2 细节的情况下启动。

## 核心口径

- `agent-service` 是 Task lifecycle、Session shell、HTTP 对外入口、SSE stream、Task tree、client invocation reference 和 service 侧观测查询面的 owner。
- `agent-service` 与 `agent-execution-engine` 同进程运行；service 拥有入口与状态，engine 拥有执行 SPI 和编排 SPI。
- Gateway 是微服务入口能力，只代理 `agent-service`；不是 `agent-service` 内部业务编排层。
- `agent-bus` 只承载 S2C / callback / 跨边界 A2A 控制指令和 data reference envelope；`agent-service` 不拥有 Bus physical channel。
- `Run` 是历史实现命名或 client invocation 兼容表达；服务端 canonical 状态是 Task。

## 内部职责层

| Layer | 名称 | 责任 | 不负责 | 主要场景 |
|---|---|---|---|---|
| AS-L1 | Entry & Streaming Surface | 接收 Gateway 代理来的 HTTP 请求；绑定 tenant、actor、auth reference、idempotency、trace；提供 Task query；提供 SSE stream endpoint。 | 不直接执行 engine；不拥有 Gateway；不把 SSE 写成 Bus token stream。 | S1, BA-001 |
| AS-L2 | Task / Session / Idempotency Manager | 创建 Task；维护 TaskStateStore controlled transition；维护 Session shell；维护 IdempotencyRecord；维护 client invocation reference；兼容历史 RunRepository / RunStateMachine 命名。 | 不让 engine、bus、middleware、client 直接写 Task State；不持久化客户业务事实。 | S1, S2, S5 |
| AS-L3 | Context & Placement Coordinator | 根据 placement profile 决定 context、memory、retriever、tool、approval UI 的执行位置；发起本地 client / business service handoff；记录 execution locus。 | 不拥有 middleware SPI 的全局语义；不读取本地 client 私有资源；不定义客户数据源细粒度权限。 | S3, S4, S5 |
| AS-L4 | In-process Engine Control Adapter | 同进程调用 `agent-execution-engine`；把 engine result 翻译成状态转换意图、tool intent、context request、suspend-required、child-task / delegation intent 或 terminal response。 | 不把 engine 做成远程服务；不让 engine 直接写 Task State；不拥有 engine SPI canonical definition。 | S2, S5, S6 |
| AS-L5 | Cross-boundary Control Coordinator | 对接 `agent-bus` 的 S2C / callback / A2A control command；维护 parent / child Task tree；处理 resume validation；在跨边界结果中只接收 data reference。 | 不拥有 Bus physical channel；不接管同一 service 内以外的远端执行；不承载大型 payload 或逐 token stream。 | BA-002, BA-003, S5, S6 |
| AS-L6 | Evidence & Operations Surface | 产出 Task timeline、step evidence、context / tool / model decision evidence、audit、metrics、LLM cost attribution key 和 replay-safe evidence export。 | 不成为业务 outcome state owner；不把敏感正文写入 metrics label；不绕过 Observability 约束。 | BA-001..BA-003 |

## 核心控制流

1. Gateway 将业务请求代理到 `agent-service` HTTP 对外入口。
2. AS-L1 绑定 tenant、actor、auth reference、idempotency key 和 trace。
3. AS-L2 执行 idempotency claim，创建或复用 Task，并返回 client invocation reference。
4. AS-L4 同进程调用 `agent-execution-engine`，获得 step result 或 suspend / delegation intent。
5. AS-L3 根据 placement profile 决定是否调用 platform middleware、local client、business service 或 delegated adapter。
6. AS-L5 对 S2C、callback、resume、跨边界 A2A 控制指令进行受控交互；同一 service 内多 Agent 协作由 AS-L2 / AS-L5 维护 Task tree 和 join。
7. AS-L2 通过受控入口推进 Task 状态。
8. AS-L1 通过 SSE stream endpoint 输出实时事件；AS-L6 输出 trace、audit、metrics 和 debug evidence。

## 状态边界

| State | Owner in agent-service | Writer | Reader | 说明 |
|---|---|---|---|---|
| Task Execution State | AS-L2 | `agent-service` controlled transition path | engine, bus, client query, observability | 服务端 canonical 执行状态。 |
| Session State | AS-L2 | `agent-service` Session shell | context coordinator, engine | 作为 context projection 输入，不等于 Memory owner。 |
| IdempotencyRecord | AS-L2 | HTTP 对外入口 / manager controlled path | HTTP 对外入口, audit | 防止重复 Task 创建。 |
| Client Invocation Reference | AS-L1 / AS-L2 | service creates, client stores local handle | client, trace, audit | 不成为第二套服务端 Run state。 |
| Task Tree Relationship | AS-L2 / AS-L5 | service when creating child Task or accepting federation result | observability, operations, bus correlation | 同一 service 内协作由 service 闭环；跨边界只接收受控结果。 |
| SSE Stream Event | AS-L1 / AS-L6 | service stream publisher | client | 实时输出事件，不是 Task State owner。 |
| LLM Cost Attribution Reference | AS-L2 / AS-L6 | service records attribution key; middleware/model emits usage | operations, cost governance | 平台统计 LLM 成本，不统计客户内部工具成本。 |

## 跨模块协作

| 对接模块 / 能力 | agent-service 发出 | agent-service 接收 | 边界规则 |
|---|---|---|---|
| `agent-execution-engine` | engine envelope、placement profile、allowed capabilities、checkpoint reference | step result、suspend-required、tool intent、context request、child-task intent、terminal response | 同进程调用；engine 不写 Task State。 |
| `agent-middleware` | model / tool / memory / retriever / policy / capacity intent | governed result、decision evidence、usage evidence | middleware 拥有 SPI 语义；service 只协调和记录。 |
| `agent-bus` | S2C request、callback expectation、A2A control command、resume correlation | callback / completion / failure / timeout、data reference envelope | Bus 只传控制和引用，不传大 payload / token stream。 |
| `agent-client` | invocation reference、cursor、SSE event、S2C / Yield instruction、Task timeline query response | 业务请求、本地 capability result、resume event | client 可以执行本地 context / memory / retriever / tool / approval UI。 |
| Observability capability | TaskEvent、audit event、metrics dimension、cost attribution key、debug evidence | trace sampling / export decision | Observability 不修改 Task 状态。 |

## 当前不进入 L1 的内容

- 具体 Java 包、类、方法签名、数据库表和索引。
- 具体 SSE event schema、A2A wire schema、data reference envelope 字段。
- 内部队列的物理实现。
- service-to-service A2A 流式回传方案。
- RunEvent sealed Java type 的最终实现形态。
