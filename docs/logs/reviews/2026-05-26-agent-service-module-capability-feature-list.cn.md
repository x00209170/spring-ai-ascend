---
level: L1
view: [scenarios, logical, process, development, physical]
module: agent-service
affects_level: [L1, L2]
affects_view: [scenarios, logical, process, development, physical]
status: proposed
language: zh-CN
relates_to:
  - docs/logs/reviews/2026-05-26-agent-service-l1-4plus1-rewrite-wave-1.en.md
  - docs/logs/reviews/2026-05-25-xiaoming-agent-service-l1-review-wave-1.en.md
  - docs/logs/reviews/2026-05-26-agent-service-l1-oss-comparison-review.cn.md
  - docs/logs/reviews/2026-05-26-agent-service-l1-oss-comparison-source.cn.md
  - docs/L1/agent-service/README.md
  - docs/L1/agent-service/scenarios.md
  - docs/L1/agent-service/logical.md
  - docs/L1/agent-service/process.md
  - docs/L1/agent-service/physical.md
  - docs/L1/agent-service/development.md
  - docs/L1/agent-service/spi-appendix.md
  - docs/adr/0136-vocabulary-reconciliation-pr71-task-vs-run.yaml
  - docs/adr/0137-suspendsignal-canonical-interruptsignal-glossary.yaml
  - docs/adr/0138-agent-service-five-layer-l1-ratification.yaml
  - docs/adr/0139-fast-slow-path-narrowed-semantics.yaml
  - docs/adr/0140-agent-service-layer5-split.yaml
  - docs/adr/0141-internal-event-queue-design-only.yaml
  - docs/adr/0142-run-aggregate-single-owner.yaml
  - docs/adr/0143-review-log-demotion-l1-canonical-move.yaml
  - docs/adr/0144-agent-service-layer-package-matrix.yaml
  - docs/adr/0145-run-event-sealed-hierarchy.yaml
---

# Agent Service L1 — 模块功能详细描述与特性清单

> 日期：2026-05-26  
> 范围：`agent-service` 模块的服务职责、模块功能、特性清单与 L2 设计输入。  
> 定位：本文是 `docs/logs/reviews/` 下的架构评审意见，用于把历史 feature inventory、rc55 canonical 4+1 视图与 OSS 对比结论收敛成一份便于人和 AI 后续引用的能力清单。本文不替代 `docs/L1/agent-service/` 下的 canonical L1 视图；若有冲突，以 `docs/L1/agent-service/` 为准。

## 1. 结论先行

Agent Service 不是某个 Agent SDK、Workflow Engine、ChatClient 或 A2A starter 的薄封装，而是一个 **Agent-native control service**。它把外部协议入口、Run / Task / Session 三聚合、内部事件通道、Task-centric runtime governance、engine dispatch、prompt / tool / model translation，以及 tenant / idempotency / trace / RLS / cancel race 等企业级约束收敛到一个 Java 服务边界内。

最新 L1 架构下，Agent Service 的功能清单必须从“有什么组件”升级为“每项能力由哪一层拥有、依赖哪些 contract、哪些状态已经 shipped、哪些仍是 design_only、哪些必须进入 L2 contract”。尤其要保留以下四条红线：

1. **Run / Task / Session 不是同一个概念**：Task 管 control state，Run 管 execution state，Session 管 context state；Run aggregate 由 Layer 2 单独拥有。
2. **Layer 3 是 design_only 绑定层**：当前没有 `service.queue/` 代码 home；任何内部事件设计都必须绑定 `control` / `data` / `rhythm` 三轨物理通道。
3. **Layer 4 是 runtime governance 核心**：SuspendSignal、RuntimeMiddleware、Fast / Slow Path、S2C callback、cancel race、middleware short-circuit 都在 Layer 4 收口，但 Run 状态写入必须通过 Layer 2 CAS。
4. **Layer 5 拆成 5a / 5b**：5a 负责 EngineRegistry / ExecutorAdapter / execution dispatch；5b 负责 ContextProjector / PromptTemplate / StructuredOutputConverter / ChatAdvisor。RuntimeMiddleware 与 ChatAdvisor 可以组合，但不是别名。

本文给出的特性清单不把 OSS 项目当作权威来源。A2A Java、Spring AI A2A、AgentScope Runtime、Temporal、Conductor、LangGraph、LangChain4j、Spring AI、AutoGen、CrewAI、OpenAI Agents、Semantic Kernel 只作为模式证据；本项目的权威边界仍由 ADR、Rule、contract catalog 与 canonical L1 视图决定。

## 2. AI / 人类共同可读的模块语义

### 2.1 模块一句话定义

Agent Service 是一个 Java / Spring 形态的 Agent 控制面服务，负责把外部请求安全地转为 tenant-bound Run / Task / Session 状态变化，并在不让 Runtime 直接绕过治理边界的前提下，调度异构 agent engine、治理工具与模型调用、处理暂停 / 恢复 / 取消 / 回调 / 多 agent 协作。

### 2.2 能力 ownership 速查

| 层 | 层名 | 拥有的能力 | 不拥有的能力 | L2 设计关键词 |
|---|---|---|---|---|
| Layer 1 | Access Layer | HTTP / A2A / MQ ingress、tenant 绑定、JWT cross-check、idempotency、trace origination、协议转译 | Run 状态机、TaskStore、Runtime lifecycle、Middleware 调用 | thin adapter、AgentCard、Task cursor、cancel/query/stream endpoint |
| Layer 2 | Session & Task Manager | Run aggregate、Task aggregate、Session aggregate、IdempotencyRecord、RLS-bound persistence、CAS transition primitive | RuntimeMiddleware 链、engine execution、队列 worker | Task lifecycle、RunRepository CAS、Session projection、Memory mutation discipline |
| Layer 3 | Internal Event Queue | RunEvent channel routing 的设计边界、Producer / Consumer / Lease / Ack / Retry / DeadLetter contract | 当前不拥有代码 home，不拥有单一 MQ 或单一 durability mode | control/data/rhythm、lease、ack、dead-letter、durability tier |
| Layer 4 | Task-Centric Control Layer | Orchestrator、SuspendSignal catch、RuntimeMiddleware chain、Fast / Slow Path routing、resume、cancel race classification、S2C coordination | Run aggregate ownership、ChatAdvisor、direct engine-specific state model | HookPoint ordering、short-circuit、exception propagation、resume semantics |
| Layer 5a | Engine Dispatch & Execution | EngineRegistry.resolve(envelope)、ExecutorAdapter strict matching、EngineHookSurface、executor dispatch | Middleware policy execution、prompt/tool/model translation ownership | EngineCapability、adapter lifecycle、streaming/tool/checkpoint semantics |
| Layer 5b | Translation & Tool-Intercept | ContextProjector、PromptTemplate、StructuredOutputConverter、ChatAdvisor / AdvisorChain、Spring AI invocation shaping | RuntimeMiddleware、Run CAS、queue routing | AdvisorChain order、context projection, structured output, tool shaping |

### 2.3 状态与事实源词汇

| 概念 | AI 可用定义 | 人类设计注意事项 |
|---|---|---|
| Run | 一次 transient compute snapshot，拥有 RunStatus DFA 与 execution-state transition。 | Run aggregate 只由 Layer 2 持久化拥有；Layer 4 只能调用 `RunRepository.updateIfNotTerminal(...)`。 |
| Task | 协议 / 控制层看到的 done-or-not / why-stopped 状态。 | Task 不是 Run rename；Task.A2aState 与 A2A protocol 对齐，但不能替代内部 RunStatus。 |
| Session | conversation / variables / context 的载体。 | Session projection 和 Memory mutation 必须平台化并发纪律，不能依赖调用者自律。 |
| SuspendSignal | checked exception 形态的一等 suspension mechanism。 | `InterruptSignal` 只能作为 glossary synonym；Java identifier 与 L1 canonical path 使用 SuspendSignal。 |
| RuntimeMiddleware | Layer 4 的 HookPoint-driven runtime policy chain。 | 不属于 Layer 5b；不要把它和 ChatAdvisor 合并。 |
| ChatAdvisor | Layer 5b 的 Spring AI model-call / tool-shaping advisor。 | 可与 RuntimeMiddleware 组合，但触发粒度和作用域不同。 |
| RunEvent | 设计中的 sealed hierarchy，用于状态、暂停、恢复、取消、S2C、child-run、terminal transition 的事件表达。 | 当前 contract design_only；Java sealed type 后续 impl-mode 落地。 |
| Internal Event Queue | 绑定 RunEvent / control signal 到 agent-bus 三轨通道的 L1 层。 | 当前没有 `service.queue/`；任何文档不得暗示已 shipped。 |

## 3. 模块功能详细描述

### 3.1 Layer 1 — Access Layer

Layer 1 负责所有外部入口的协议收敛，把外部 Client、A2A peer、MQ / Event Bus 消息转为内部可治理的请求。它的核心价值不是执行 Agent，而是保证每个入口都带着同一组上下文进入服务边界：`tenantId`、idempotency key、trace id、认证主体、协议类型、请求体 hash 与 cursor / stream 语义。

**主要功能**：

1. **统一 HTTP / gRPC / WebSocket / A2A / MQ 入口**：REST 路由由 `openapi-v1.yaml` 约束；A2A 与 MQ / Ingress 当前仍是 design_only contract。
2. **tenant / auth / trace / idempotency 横切绑定**：在进入 Run / Task 创建之前完成 JWT tenant claim cross-check、TenantContextFilter、IdempotencyHeaderFilter、TraceExtractFilter。
3. **Task Cursor 边界**：长周期请求必须尽快返回 cursor，不能把客户端连接持有到执行结束。
4. **协议转译而非状态拥有**：A2A controller、HTTP controller、MQ adapter 不拥有 RunRepository、TaskStateStore 或 ExecutorAdapter lifecycle。
5. **错误 envelope 统一**：cross-tenant、idempotency drift、schema invalid、illegal transition 等错误由统一错误模型返回。

**开源模式依据**：A2A Java SDK 与 Spring AI A2A 说明 AgentCard / Task protocol / endpoint auto-configuration 有价值；AgentScope Runtime 说明 protocol handler 应保持 thin handoff；Conductor 说明 task query / poll / update / heartbeat API 需要明确入口边界。

**L2 设计输入**：A2A Access L2 需要定义 AgentCard、Task cursor、cancel/query/streaming endpoint、controller thinness rule、request → Layer 2 / Layer 4 service handoff contract。

### 3.2 Layer 2 — Session & Task Manager

Layer 2 是 Agent Service 的事实源层，负责 Run / Task / Session 三聚合及其持久化约束。它不是普通 CRUD 层，而是 state ownership boundary：所有 Run 状态变化必须经过 Layer 2 的 CAS primitive，所有 tenant-bound records 必须符合 tenantId-first 与 RLS 约束。

**主要功能**：

1. **Run aggregate 管理**：创建 Run、维护 RunStatus、执行 RunStateMachine validation、提供 `updateIfNotTerminal(...)` 原子转移入口。
2. **Task aggregate 管理**：维护 A2A protocol 可见的 control state，包括 SUBMITTED / WORKING / INPUT_REQUIRED / COMPLETED / FAILED 等状态。
3. **Session aggregate 管理**：保存 conversation messages、variables、session projection，并为 ContextProjector 提供上下文输入。
4. **Idempotency persistence**：通过 IdempotencyRecord / dedup table 保证重复请求不会重复创建 Run / Task。
5. **tenantId-first persistence**：Run / Task / Session / lifecycle audit / idempotency 相关记录必须携带 tenantId，并在 durable backend 中启用 RLS。
6. **状态审计事实源**：状态变化要能生成 lifecycle audit 与 RunEvent 数据，支持观察性与 evolution export。

**开源模式依据**：A2A TaskStore 证明 protocol task state 需要 final / interrupted 谓词；Temporal history 与 Conductor task persistence 证明长周期执行需要持久事实源；LangChain4j 的 `@MemoryId` 并发风险说明 Session / Memory 并发不能留给调用者。

**L2 设计输入**：优先定义 Task lifecycle L2，包括 `Task.A2aState` 谓词、`TaskStateStore.transitionIf(...)` 或等价 CAS 原语、Session projection 并发规则、Run-to-Session N:M 投影、RLS migration sequence。

### 3.3 Layer 3 — Internal Event Queue

Layer 3 是内部事件与队列语义的设计边界，当前必须标注为 design_only。它的职责不是声明“未来用 Kafka”，而是把 Agent Service 内部的事件意图、通道选择、durability tier、retry、dead-letter、heartbeat、lease 与 ack 关系表达清楚。

**主要功能**：

1. **RunEvent routing contract**：每类 RunEvent 必须声明应进入 control、data 或 rhythm 哪条物理通道。
2. **Producer / Consumer 分离**：Layer 2 / Layer 4 生产事件，未来 Layer 3 Producer 负责按 intent 选择通道；Consumer 负责 lease、ack、retry、dead-letter。
3. **三轨物理隔离**：cancel / resume / suspend / S2C request 进入 control；payload、state transition、S2C response、child-run completion 进入 data；heartbeat / tick 进入 rhythm。
4. **durability tier 正交化**：in-memory / semi-persistent / persistent 不是 queue 类型，而是每条物理通道的部署选项。
5. **idempotency 与 dedup**：at-least-once delivery 必须以 tenantId + idempotency key 或 event id 去重。
6. **long-horizon timer 基础**：rhythm channel 为 Tick Engine、deadline、timeout、resume sweep 提供边界。

**开源模式依据**：Temporal worker task queue 证明 lease / ack / retry / history 的必要性；Conductor worker poll/update 和 dead-letter 提供 queue-worker contract 参照；AutoGen message envelope / subscription / cancellation token 证明 actor/message runtime 的 envelope discipline 有价值。

**L2 设计输入**：Internal Event Queue L2 需要定义 `RunEventPublisher`、`RunEventConsumer`、`QueueLease`、ack / retry / dead-letter、heartbeat、channel routing tests；在代码 home 落地前，所有文档保持 design_only 标识。

### 3.4 Layer 4 — Task-Centric Control Layer

Layer 4 是 Agent Service 区别于普通 Agent SDK 的核心。它不直接拥有 Run aggregate，但负责决定执行如何被启动、暂停、恢复、取消、短路、升级 Slow-Path、触发 middleware、调用 S2C callback 与处理 child-run join。

**主要功能**：

1. **Orchestrator control loop**：根据 Run / Task / Session 与 EngineEnvelope 调度执行，并在 Runtime 返回、抛出 SuspendSignal 或发生错误时分类处理。
2. **SuspendSignal handling**：捕获 child-run 与 S2C callback 两类 checked suspension，将 Run 转为 SUSPENDED，并保存 resume 所需上下文。
3. **RuntimeMiddleware chain**：在 HookPoint.before_tool / after_tool / before_llm / before_resume 等边界执行 policy、quota、memory governance、sandbox routing、observability、failure handling。
4. **Fast / Slow Path routing**：DualTrackRouter / SlowTrackJudge 当前 design_only；Fast-Path 只能省略中间 checkpoint，不得省略 tenantId、RLS、reactive I/O 或 metadata persistence。
5. **cancel race classification**：处理 cancel winner / loser，保持 terminal idempotency 和 illegal transition 响应语义。
6. **S2C callback coordination**：通过 S2cCallbackEnvelope 与 S2cCallbackTransport 触发客户端能力调用，等待响应后恢复 Run。
7. **child-run orchestration**：在 A2A / multi-agent 场景中挂起 parent Run、派发 child Run、根据 child terminal status 恢复或失败。

**开源模式依据**：LangGraph interrupt/resume/checkpoint、Temporal signal/timer、Conductor human task、OpenAI Agents interruption / next-step loop 都说明 runtime governance 应是一等结构，而不是 SDK runner 的内部细节。

**L2 设计输入**：RuntimeMiddleware + SuspendSignal L2 需要定义 HookPoint ordering、short-circuit、exception propagation、resume semantics、S2C timeout、child-run join policy、cancel loser audit event。

### 3.5 Layer 5a — Engine Dispatch & Execution

Layer 5a 屏蔽异构 engine 的执行差异，把 graph engine、agent loop、未来 LangChain / LlamaIndex / external runtime shell 等能力统一到 EngineRegistry / ExecutorAdapter contract。它负责 dispatch 和 execution，不负责 runtime governance policy。

**主要功能**：

1. **EngineRegistry strict matching**：每次 dispatch 必须走 `EngineRegistry.resolve(envelope)`，按 `engine_type` 找到唯一 ExecutorAdapter。
2. **ExecutorAdapter lifecycle**：适配 Graph、AgentLoop、未来外部 engine，统一 execute / resume / stream / suspend boundary。
3. **EngineHookSurface emission**：engine 执行中需要触发 tool / model / resume 等边界时，只能向 Layer 4 发 HookPoint event，不能直接调用 RuntimeMiddleware。
4. **SuspendSignal propagation**：executor 可以抛出 SuspendSignal，但捕获和状态持久化由 Layer 4 + Layer 2 完成。
5. **capability-aware enhancement**：未来可以在 strict matching 之后引入 EngineCapability，但不能削弱 Rule R-M.a/.b 的 strict matching 底线。

**开源模式依据**：LangGraph4j / LangGraph 证明 graph execution kernel 与 checkpoint 能力可被适配；AgentScope Runtime Runner 证明 runtime wrapper 形态有价值；OpenAI Agents Runner、CrewAI Flow、AutoGen runtime、Semantic Kernel process 都是未来可能被适配的 engine / runner 类型，而不是 Agent Service 本身。

**L2 设计输入**：Engine Adapter L2 需要定义 EngineCapability、adapter lifecycle、streaming、tool-call boundary、checkpoint compatibility、structured-output 支持、engine mismatch failure semantics。

### 3.6 Layer 5b — Translation & Tool-Intercept

Layer 5b 负责把 Session context、prompt template、structured output、model/tool invocation shaping 与 Spring AI / LangChain4j / Semantic Kernel 式的 invocation kernel 对齐。它的变化节奏通常快于 5a execution dispatch，因此 ADR-0140 将其拆出。

**主要功能**：

1. **ContextProjector**：从 Session / Memory projection 生成 InjectedContext。
2. **PromptTemplate**：根据 context 和 template 生成 model prompt。
3. **StructuredOutputConverter**：把模型输出转换为 typed domain object。
4. **ChatAdvisor / AdvisorChain**：在 ChatClient / model-call boundary 处理 tool shaping、request decoration、response interpretation。
5. **tool-call interpretation**：把模型或 SDK 产生的 tool request 表达为 Layer 4 可治理的 HookPoint / middleware 调用。
6. **model / tool / output translation**：吸收 Spring AI、LangChain4j、Semantic Kernel 等 library 的 invocation pattern，但不让它们拥有服务级状态。

**开源模式依据**：Spring AI ChatClient / Advisor、LangChain4j AiServices / ToolExecutor、Semantic Kernel plugin filter 都说明 model/tool/function invocation kernel 是独立变化面。它们能增强 5b，但不能替代 Layer 4 governance。

**L2 设计输入**：Layer 5b L2 需要定义 ContextProjector → PromptTemplate → AdvisorChain → StructuredOutputConverter 顺序、ChatAdvisor 与 RuntimeMiddleware 组合顺序、tool-call short-circuit、exception propagation、structured output schema drift handling。

## 4. 特性清单

### 4.1 清单状态说明

| 状态 | 含义 | L2 / impl 使用方式 |
|---|---|---|
| shipped | 已有代码 / contract / gate 形成可验证路径。 | 可作为实现依赖，但仍需按当前文件确认最新口径。 |
| implemented_unverified | 已有参考实现或 SPI，但持久化、门禁或 L2 contract 未完全闭环。 | 进入 L2 时必须补测试与 contract。 |
| design_only | 只在 ADR / contract / L1 文档中声明，不能宣称 runtime enforced。 | L2 先补 boundary contract，再进入 impl-mode。 |
| L2-backlog | 本文从 L1 + OSS 对比抽取的候选能力。 | 只能作为后续 L2 设计入口，不直接改代码。 |

### 4.2 P0 — L2 必须优先闭环的基础特性

| ID | 特性 | Owner | 状态 | 功能说明 | L2 设计输出 | 约束 / 不可误解点 |
|---|---|---|---|---|---|---|
| AS-F01 | Run aggregate single-owner transition | Layer 2 | shipped / needs durable backend | Run 创建、状态转移、terminal classification、cancel race 都通过 Layer 2 统一管理。 | `RunRepository.updateIfNotTerminal` durable implementation、transition result taxonomy、audit event mapping。 | Layer 4 不直接写 Run；Layer 5 不拥有状态。 |
| AS-F02 | Task lifecycle transition primitive | Layer 2 | L2-backlog | Task.A2aState 作为 protocol / control state，需要 final / interrupted / active 谓词和 CAS-style transition。 | `Task.A2aState` predicates、`TaskStateStore.transitionIf(...)` 或等价 contract、Task ↔ Run projection rules。 | 不改变 RunStatus DFA；A2A TaskStore 不替代 RunRepository。 |
| AS-F03 | Session projection and memory mutation discipline | Layer 2 + 5b | L2-backlog | Session 保存对话与变量，Memory 作为外部能力投影进 context；并发 mutation 必须平台化。 | Session N:M Run projection、ContextProjector consistency、Memory mutation lock / version / conflict policy。 | 不把 LangChain4j `@MemoryId` 风格并发风险留给调用者。 |
| AS-F04 | Internal Event Queue minimum contract | Layer 3 | design_only / L2-backlog | 定义 Producer / Consumer / Lease / Ack / Retry / DeadLetter / Heartbeat 与三轨 channel routing。 | `RunEventPublisher`、`RunEventConsumer`、`QueueLease`、dead-letter schema、routing tests。 | `service.queue/` 当前不存在；不得宣称已落地。 |
| AS-F05 | RunEvent sealed hierarchy and routing | Layer 2 / 3 / 4 | design_only | 用 sealed RunEvent 表达 created、state transition、suspend、resume、cancel、S2C、child-run、terminal。 | Java sealed hierarchy、contract tests、channel routing table、EvolutionExport enforcement。 | 当前 `run-event.v1.yaml` design_only；Java 类型后续 impl-mode 落地。 |
| AS-F06 | RuntimeMiddleware ordering contract | Layer 4 | L2-backlog | 定义 HookPoint middleware 顺序、short-circuit、exception propagation、audit emission。 | HookPoint ordering matrix、HookOutcome semantics、failure propagation table。 | RuntimeMiddleware 只属于 Layer 4，不进入 Layer 5b。 |
| AS-F07 | SuspendSignal resume semantics | Layer 4 + 5a | implemented_unverified / L2-backlog | checked exception 统一 child-run、S2C callback、tool-await 等 suspension。 | SuspendReason taxonomy、checkpoint contract、resume payload schema、timeout semantics。 | 不引入 InterruptSignal Java rename；Interrupt 只是 glossary synonym。 |
| AS-F08 | Access Layer thin adapter contract | Layer 1 | L2-backlog | HTTP / A2A / MQ controller 只做协议转译、tenant/idempotency/trace 绑定与 service handoff。 | A2A endpoint / AgentCard / cancel / query / streaming / cursor contract。 | controller 不拥有 TaskStore、RunRepository 或 executor lifecycle。 |

### 4.3 P1 — 增强可组合性与可观测性的特性

| ID | 特性 | Owner | 状态 | 功能说明 | OSS 证据 | L2 设计输出 |
|---|---|---|---|---|---|---|
| AS-F09 | Engine capability discovery | Layer 5a | L2-backlog | 在 strict engine_type matching 后增加 capability-aware selection / validation。 | LangChain4j capability、A2A AgentCard、AgentScope metadata。 | EngineCapability schema、resolution order、mismatch failure tests。 |
| AS-F10 | Checkpointer minimal SPI | Layer 4 / 5a | implemented_unverified / L2-backlog | 只承载 compute snapshot，不吞并 Session、Memory 或 workflow history。 | LangGraph checkpoint saver、Temporal history 作为反例边界。 | Snapshot schema、storage backend、resume compatibility。 |
| AS-F11 | S2C callback alignment | Layer 4 + agent-bus | shipped / L2 expansion | 服务端暂停并向客户端请求能力，客户端响应后恢复 Run。 | A2A input_required、Conductor human task、Temporal signal。 | callback timeout、schema-invalid、capacity exhausted、resume re-auth。 |
| AS-F12 | Child-run / A2A peer collaboration | Layer 1 / 4 / 5a | design_only / L2-backlog | parent Run 挂起，peer child Run 执行，child terminal status 回流后恢复 parent。 | A2A Java、AgentScope Runtime、OpenAI Agents handoff。 | parentRunId correlation、join policy、peer failure recovery。 |
| AS-F13 | Cancel loser audit semantics | Layer 2 / 4 | shipped concept / L2 expansion | cancel-vs-complete race 的 loser 需要 re-read post-CAS terminal 并返回 200 或 409。 | Temporal / Conductor race modeling。 | `TransitionResult` taxonomy、CancelRequestedEvent rejection audit。 |
| AS-F14 | Per-channel observability | Layer 3 / Physical | L2-backlog | control / data / rhythm 各自输出 queue op metrics、lag、retry、dead-letter 指标。 | Conductor queue visibility、Temporal task queue metrics。 | `springai_ascend_queue_<channel>_<op>_total` 指标 contract。 |
| AS-F15 | Admin / debug process query API | Layer 1 / 2 / 4 | L2-backlog | 提供 Run / Task / Session / event / middleware decision 的查询视图。 | Conductor WorkflowResource、Spring AI Alibaba workflow debug。 | query endpoint、tenant-scoped filter、no bypass of Layer 2/4。 |
| AS-F16 | Protocol-level Task cursor | Layer 1 / 2 | L2-backlog | 统一 long-running request 的 cursor、poll、SSE、webhook 返回路径。 | A2A task event stream、Conductor task API、LangGraph run/thread API。 | cursor schema、poll result shape、stream event mapping。 |

### 4.4 P2 — 平台化能力与治理增强特性

| ID | 特性 | Owner | 状态 | 功能说明 | 设计约束 |
|---|---|---|---|---|---|
| AS-F17 | AgentCard / capability publication | Layer 1 + 5a | L2-backlog | 对外声明 agent 能力、engine capability、protocol support、streaming support。 | 能力声明不能改变 EngineRegistry strict matching；只作为入口发现与候选过滤。 |
| AS-F18 | Sandbox / tool governance enforceability alignment | Layer 4 + agent-middleware | L2-backlog | tool execution 通过 RuntimeMiddleware 与 Sandbox SPI，拒绝超出 physical sandbox 能执行的 logical grant。 | 必须符合 Rule R-L；不能让 Runtime 直接调用 Sandbox。 |
| AS-F19 | Model / tool / prompt translation profile | Layer 5b | L2-backlog | 把 Spring AI / LangChain4j / Semantic Kernel 风格 prompt、tool、structured output 能力转为统一 profile。 | 不把 ChatAdvisor 变成 RuntimeMiddleware；profile 只是 translation surface。 |
| AS-F20 | Evolution export governance | Layer 2 / 3 / 4 / agent-evolve | design_only / L2-backlog | 每个 RunEvent 标注 IN_SCOPE / OUT_OF_SCOPE / OPT_IN，决定 evolution plane 可见性。 | OUT_OF_SCOPE 不得被 evolution plane 持久化；OPT_IN 要有用户或 policy 授权。 |
| AS-F21 | DualTrackRouter policy refinement | Layer 4 | design_only / L2-backlog | 根据 estimated wall-clock、external input、S2C、A2A、deployment locus 选择 Fast / Slow Path。 | Fast-Path 不能跳过 tenantId、RLS、reactive、metadata persistence。 |
| AS-F22 | Durable RLS-backed Run / Task / Session storage | Layer 2 + Physical | L2-backlog | 将 in-memory reference path 推进到 Postgres / R2DBC / RLS 持久实现。 | 每个 tenant_id table 创建时启用 RLS；cross-tenant SELECT 作为 defense-in-depth 返回空。 |
| AS-F23 | Worker lease / heartbeat / retry policy | Layer 3 / 4 | L2-backlog | 长周期任务的 worker lease、heartbeat、retry、dead-letter 与 timeout 策略。 | lease 与 business state 分离；heartbeat 走 rhythm channel。 |
| AS-F24 | Multi-runtime adapter lifecycle | Layer 5a | L2-backlog | 为 graph、agent-loop、actor runtime、crew orchestration、kernel process 等不同 runtime 定义 adapter lifecycle。 | engine adapter 只能被适配，不能反向决定 Layer 2/4 状态模型。 |

## 5. 按视图组织的 L2 设计指导

### 5.1 Scenarios 视图应继续覆盖的能力

后续 L2 文档应至少保留 S1-S5 的路径覆盖：标准同步 intake、长周期 ReAct / tool loop、A2A peer collaboration、S2C callback、cancel during execution。每个新特性都要说明它落在哪个 scenario 上，并标注是否引入新的 RunEvent、HookPoint、queue channel 或 persistence table。

### 5.2 Logical 视图应继续覆盖的能力

Logical L2 必须以 5 层 + 5a/5b split 为主线，避免重新混合 Layer 4 和 Layer 5b。所有状态相关特性先问 owner：是 Task control state、Run execution state、Session context state，还是 RunEvent audit / evolution event。不能用一个泛化 StateStore 模糊三聚合边界。

### 5.3 Process 视图应继续覆盖的能力

Process L2 必须画出 winner / loser、resume success / timeout / schema invalid、middleware proceed / short-circuit / fail、queue ack / retry / dead-letter 等分支。只画 happy path 不足以指导实现。

### 5.4 Physical 视图应继续覆盖的能力

Physical L2 必须同时描述五平面部署、RLS、三轨 bus、sandbox boundary、tenant propagation。尤其是 Layer 3：channel choice 与 durability tier 是正交轴，不能把 “in-memory / persistent” 写成 queue 类型。

### 5.5 Development 视图应继续覆盖的能力

Development L2 必须把每项能力映射到 package home、SPI、contract YAML、gate test。`service.queue/`、`service.platform.a2a/`、RunEvent sealed types 等当前未落地 package 必须保持 future / design_only 标识，直到真实代码 home 出现。

## 6. 建议的 L2 拆分顺序

| 顺序 | L2 主题 | 覆盖特性 | 为什么优先 |
|---:|---|---|---|
| 1 | Task lifecycle + Run transition contract | AS-F01、AS-F02、AS-F13 | 状态事实源是所有 runtime governance 的底座；先闭环 CAS、Task predicates、transition result。 |
| 2 | Internal Event Queue contract | AS-F04、AS-F05、AS-F14、AS-F23 | Layer 3 当前 design_only，但 OSS 对比显示 queue / worker / lease / dead-letter 是长周期任务必要条件。 |
| 3 | RuntimeMiddleware + SuspendSignal semantics | AS-F06、AS-F07、AS-F11、AS-F12 | Layer 4 是差异化核心；必须先定义 ordering、short-circuit、resume、timeout。 |
| 4 | Engine Adapter capability and lifecycle | AS-F09、AS-F10、AS-F24 | 多 engine 扩展必须先守住 strict matching，再谈 capability-aware。 |
| 5 | Access Layer A2A / cursor / admin query | AS-F08、AS-F15、AS-F16、AS-F17 | 入口层最后细化，避免 controller 反向污染核心状态机。 |
| 6 | Physical persistence and governance hardening | AS-F18、AS-F20、AS-F21、AS-F22 | RLS、sandbox、evolution export、DualTrackRouter 属于跨层硬化，适合在核心 contract 后闭环。 |

## 7. 过时或应删除的旧特性表达

| 旧表达 | 最新判断 | 替代表达 |
|---|---|---|
| “Task as scheduling core = rename Run” | 拒绝 | Task 是 control state；Run 是 execution state；两者共存。 |
| “InterruptSignal” 作为 Java 机制名 | 拒绝 | 使用 `SuspendSignal`；Interrupt 仅为 glossary synonym。 |
| “Internal Event Queue = one queue + three storage modes” | 拒绝 | 三轨 physical channel + per-channel durability tier。 |
| “Fast-Path 不强制持久化” | 拒绝 | Fast-Path 不强制 checkpoint；Run / Task metadata 与 RLS persistence 仍必须存在。 |
| “RuntimeMiddleware 与 ChatAdvisor 都是 Shadow Tool Interceptor” | 拒绝 | RuntimeMiddleware 属于 Layer 4；ChatAdvisor 属于 Layer 5b；二者组合但不等价。 |
| “Engine adapter 决定服务状态模型” | 拒绝 | Engine 只能被适配；Layer 2/4 状态模型由 Agent Service 主权拥有。 |
| “A2A TaskStore 可替代内部 RunRepository” | 拒绝 | A2A TaskStore 只覆盖 protocol control state，不能承载 Run execution state 与 tenant/RLS/cancel race。 |
| “Workflow history 可替代 Run / Task / Session” | 拒绝 | Temporal / Conductor 可借鉴 durable execution，但不能替代 Agent-native 聚合边界。 |

## 8. 最终判断

Agent Service 的模块功能不应再以旧版 F-01..F-22 的粗粒度 feature inventory 为主，而应以 **L1 canonical 5 层 ownership + Run/Task/Session 三聚合 + Rule-bound governance + OSS pattern evidence** 为主线重新组织。本文给出的 AS-F01..AS-F24 是面向后续 L2 的候选能力目录：它们不是一次性实现清单，而是每个 L2 设计必须回答的能力边界问题。

后续设计优先级应放在状态转移、事件队列、Hook / Suspend 语义、engine adapter contract 与 Access thinness 的可验证化；不建议先扩大 engine adapter 数量，也不建议把任一 OSS 框架作为 Agent Service 的替代边界。
