---
level: L0
view: logical
status: draft
---

# Architecture Overview

## 目的

用高层语言说明系统目标、能力地图、模块边界、控制权归属、状态归属、质量属性、关键 ADR、核心场景和开放风险。本文只建立 L0 心智模型，不写 L2/L3 实现细节。

## 适用读者

新加入的架构师、模块负责人、AI agent、架构评审者。

## 维护规则

- 不写具体数据库表、Redis key、MQ topic、TTL、retry 次数或 SDK 方法签名。
- 每个边界声明必须能追踪到 Capability、Module Card、State Matrix、ADR、ICD、Scenario、Verification。
- 未交付或未决能力必须标记为 `draft`、`design_only` 或 `Open Issue`。

## 1. 系统目标

`spring-ai-ascend` 的目标是提供一个自托管、可治理、可扩展的 agent runtime 平台，使外部 Spring 开发者可以通过 SPI、配置和契约组合 Agent、ModelGateway、Skill、Memory、Vector、Planner 等能力，同时让平台侧保留运行时控制权、租户隔离、观测、审计、容量治理和变更治理。

## 2. 问题域

当前平台面对三类长期问题：

1. Agent 执行链路跨越 HTTP 对外入口、Task lifecycle、engine dispatch、model/tool/memory、bus、observability 和 governance，若没有显式契约，模块并行开发会依赖口头约定。
2. 状态类型很多，Task、Session、Memory、Checkpoint、Tool Call、Audit、Trace、Policy 的 owner 不清楚会导致重复写入和恢复语义冲突；历史 `Run` 命名只能作为 client 侧一次调用或兼容别名，不再作为独立服务端状态模型。
3. 设计文档如果只可阅读、不可生成 harness，就无法稳定支持 AI 辅助开发和集成验证。

## 3. 核心架构原则

| Principle | L0 表达 | 下游约束 |
|---|---|---|
| 平台和业务解耦 | 平台不承载业务定制，业务通过 SPI 和配置扩展。 | `INV-001`, `MOD-001`, `ADR-002` |
| Contract-first | 跨模块交互先有 ICD / machine-readable contract，再生成 harness 和实现。 | `ADR-002`, `ICD-*` |
| State single owner | 每类核心状态必须有唯一 owner，writer 严格控制。 | `STATE-*`, `INV-002` |
| Cursor / suspend-first | 长任务不持有客户端连接，通过 cursor、suspend、resume 表达等待。 | `BA-002`, `S5`, `INV-006` |
| Runtime policy centralization | 工具、模型、memory、planner 等跨切面策略通过 middleware / hook / governance 进入，不绕过 runtime。 | `CAP-04`, `CAP-07` |
| Capability placement explicit | 工具、上下文、memory、retriever、approval UI、A2A 能力必须声明 execution locus，不把平台、本地 client、业务 service 混写。 | `CAP-12`, `ICD-CS-Capability-Placement`, `S3`, `S4`, `S5`, `S6` |
| Boundary-mediated A2A | 同一个 `agent-service` 进程内的多 Agent 协作由 service 闭环管理；跨 service / 跨部门 / 跨部署边界的 A2A 控制指令统一通过 `agent-bus` 传递。 | `BA-003`, `S6` |
| Control / data / stream separation | Gateway 只作为微服务入口能力代理 `agent-service`；`agent-bus` 主要传递控制指令；大数据载荷通过外部对象存储等 data path 引用传递；对外实时输出优先由 `agent-service` 通过 SSE 承载，不把 token stream 写成 Bus 事件流。 | `BA-001`, `S6`, `CAP-09`, `CAP-11` |
| Harness-first | 每个核心场景必须有 assertions，每个模块必须有 harness spec。 | `HAR-*`, `VER-*` |

## 4. L0 架构模块准入和运行链路

本文不把 root `pom.xml` 的 reactor module 清单直接等同于 L0 架构模块。进入本 Overview 的模块边界前，先做准入判定：

| 现有项 | 准入判定 | 是否进入 L0 模块边界 | 处理方式 |
|---|---|---|---|
| `agent-service` | Architecture Module | 是 | 核心运行模块，进入 §8。 |
| `agent-execution-engine` | Architecture Module | 是 | 核心执行模块，进入 §8。 |
| `agent-middleware` | Architecture Module | 是 | 核心中间件契约模块，进入 §8。 |
| `agent-bus` | Architecture Module | 是 | 跨平面通信模块，进入 §8。 |
| `agent-client` | Runtime Component | 是，但标记为 SDK / 本地能力边界 | 作为外部调用边界进入 §8，不等同于当前服务端主执行路径。 |
| `agent-evolve` | Runtime Component | 是，但标记为 evolution / deferred 边界 | 作为演进平面边界进入 §8，不等同于常规 request path。 |
| `spring-ai-ascend-dependencies` | Build Artifact | 否 | 下沉为 build / version governance，不作为 L0 架构模块。 |
| `spring-ai-ascend-graphmemory-starter` | Packaging Artifact / Adapter Scaffold | 否 | 下沉为 adapter / starter 实现约束，不作为 L0 架构模块。 |

因此，L0 架构模块边界只覆盖核心运行模块和必要运行边界；构建、发布、starter、fixture、demo、adapter scaffold 不进入 §8 的模块职责边界。

下面的图是运行时控制链路视图，只展示主执行路径。`agent-evolve` 是 evolution 边界，不在常规 request path 上；BoM 和 starter 不参与控制流。

```text
Runtime control path:
  External Client
  -> agent-client (SDK / local capability endpoint, W3+ binding)
  -> Gateway capability (microservice gateway, proxies only to agent-service)
  -> agent-service.platform (HTTP 对外入口, tenant, auth, idempotency, SSE stream endpoint)
  -> agent-service.runtime / task / session (Task, Session, client invocation reference adapters)
  -> in-process agent-execution-engine (orchestration SPI, engine SPI, planner SPI)
  -> agent-middleware (model, skill, memory, vector, prompt, advisor, runtime middleware SPI)
  -> agent-bus.s2c / cross-boundary A2A / federation / channels (cross-plane callback, control command and bus)
```

运行链路关键说明：

- `agent-service` 是 HTTP 对外入口、Task execution lifecycle、Session 运行时壳、Agent registry、client invocation reference adapter host 和 SSE streaming endpoint。
- `agent-service` 与 `agent-execution-engine` 的目标运行形态是同进程组合：service 拥有入口、状态和对外流式输出，engine 拥有执行 SPI 和编排能力；二者不是默认远程服务边界。
- `agent-execution-engine` 是 Orchestrator、Checkpointer、RunContext、SuspendSignal、ExecutorAdapter、Planner 等执行侧 SPI owner。
- `agent-middleware` 是 ModelGateway、Skill、Memory、Vector、Retriever、PromptTemplate、ChatAdvisor、RuntimeMiddleware 的能力 owner。
- `Gateway` 是微服务框架的总体入口能力；当前系统只有 `agent-service` 对外暴露服务，因此 Gateway 只能代理 `agent-service`，不得被写成独立业务编排模块。
- `agent-bus` 是 S2C、跨边界 A2A control command、federation 和三通道 bus contract 的 owner；它不接管同一 service 进程内的多 Agent 调度，也不承载高频 token streaming。
- `agent-client` 是 SDK / 本地能力侧；普通业务请求经 Gateway / `agent-service` 入口，S2C callback 和跨边界控制交互经 `agent-bus` 契约。
- `agent-evolve` 是 evolution plane，当前主要是边界和 SPI 草案，不在常规 request path 上，因此只作为边界模块描述。

### 4.1 部署形态和能力放置

部署形态不改变 L0 模块边界，但会改变工具调用、上下文装配和数据驻留的位置。Overview 必须把这些形态作为运行架构的一部分，而不是只描述平台集中式路径。

| Variant | 部署 / 执行位置 | 适用用户 | 架构含义 |
|---|---|---|---|
| Platform-Centric / Mode A | `agent-client` 在业务侧；service / engine / bus / middleware 在平台侧。 | 轻量业务应用、SaaS 集成方。 | 平台集中完成 context、tool、model、middleware 和 observability；数据源仍按客户授权边界访问。 |
| Weak Department / PaaS Tenant | service / engine / bus / middleware 全部由平台托管；业务侧只保留配置、数据源授权引用、发布验收和轻量入口。 | 没有独立 service 部署能力的业务部门。 | 平台提供 hosted service、默认中间件、租户隔离、配置治理、发布控制、LLM 成本统计和运维面；不替客户定义细粒度数据源权限。 |
| Protected Local Capability | 敏感工具、本地 context、local memory / retriever、approval UI 留在 C-Side；平台通过 S2C / Yield 发出指令。 | 强业务方、核心系统、数据不出域场景。 | S-Side 只拥有执行轨迹、指标、审计、LLM 成本和受控结果，不拥有业务事实。 |
| Business-Centric / Federated / Mode B | client + service + engine 可在业务侧；bus / middleware / federation hub 可在平台侧。 | 自托管、边缘部署、强业务部门。 | 本地低延迟执行闭环 + 平台 federation / middleware proxy；跨 service / 跨部门 A2A 控制指令仍通过平台 Bus。 |
| Hybrid Enterprise Individual | 个人本地工具走 client；企业公共服务走平台 middleware adapter。 | 企业内个人或部门应用开发者。 | 同一次 Task 允许 local tool、local context、platform public service、delegated memory 混合出现。 |

这些形态的契约入口见 `ICD-CS-Capability-Placement`，权威 ADR 包括 ADR-0033、ADR-0049、ADR-0051、ADR-0074、ADR-0101。

## 5. 核心控制流

1. Client 提交运行意图。
2. 入口层完成租户、认证、幂等和 trace 绑定；真实落点是 `agent-service`，SDK / ingress 侧由 `agent-client` 和 `agent-bus` 参与。
3. 执行编排层把请求转换成 Task / engine envelope；真实落点是 `agent-execution-engine` 的 Orchestrator / orchestration SPI，以及 `agent-service` 的 Task 控制入口。client 侧可以把这次调用称为 run / invocation，但服务端不再把 Run 作为独立 canonical 状态。
4. Task 状态写入只能通过 `agent-service` 的受控状态入口完成；当前代码或历史文档中的 `RunRepository` 命名视为 Task lifecycle 的实现兼容名，不能引入第二套 Run State owner。
5. Engine 执行过程中需要模型、工具、memory、retriever、approval UI 或外部能力时，先进行 capability placement 判定：platform hosted service、platform middleware、本地 client、business service、delegated adapter 或 federation remote。
6. 平台侧能力必须经过 middleware / hook / skill capacity / policy 边界；C-Side 本地能力通过 S2C / Yield handoff，不得伪装成平台本地调用。
7. 同一 `agent-service` 进程内的多 Agent 协作由 service 管理 parent / child Task 关系和 join；跨 service / 跨部门 / 跨部署 A2A 控制指令必须通过 `agent-bus` 传递，并能挂接到 Task tree。
8. 对外实时结果流由 `agent-service` 通过 SSE 等服务端流式能力输出；Bus 不作为逐 token 事件流通道。
9. 大型数据包、多模态内容或重载结果走 data path：先写入第三方对象存储或客户指定存储，再在控制指令中传递 URI / object reference / metadata，需求方按授权直接拉取内容。
10. 长等待或外部输入需要 suspend/resume，不持有物理线程或连接。
11. Observability 能力记录 trace、span、event、audit、execution locus、LLM cost attribution 以及必要的黄金轨迹断言。

## 6. 核心数据流

```text
Tenant + Actor + Request Intent
  -> agent-service: Task / Session identity and client invocation reference
  -> agent-execution-engine: EngineEnvelope / PlanningRequest
  -> capability placement: platform_hosted_service / local_client / business_service / platform_middleware / delegated_adapter / federation_remote
  -> agent-middleware / agent-client / business service: Model / Skill / Memory / Retriever / Tool / Local Context interaction
  -> data reference path: object URI / content reference / metadata when payload is large or multimodal
  -> agent-service SSE: streaming response to external client when real-time output is needed
  -> TaskEvent / Trace / Audit / LLM Cost Attribution
  -> Verification evidence
```

L0 数据流只描述语义，不绑定具体表、key、topic 或序列化格式。

## 7. 核心状态流

Task 执行状态的 L0 语义：

```text
Pending -> Running -> Suspended -> Running -> Terminal
Pending -> Cancelled
Running -> Cancelled
Running -> Failed
Running -> Succeeded
```

Task 是服务端 canonical 控制状态，Session 是上下文状态，Memory 是知识状态。`Run` 不再作为独立服务端状态进入 L0，只能作为 client 侧一次调用、SDK 函数名或历史实现命名的兼容表达。三者不得被混写成同一个状态概念。

## 8. 模块职责边界

本节只列 §4 准入通过的 Architecture Module / 必要 Runtime Component。能力聚合名只作为“提供 / 参与能力”出现，不能替代模块边界。

| 架构模块 / 运行边界 | 准入类型 | 负责什么 | 不负责什么 | 主要提供能力 |
|---|---|---|---|---|
| `agent-service` | Architecture Module | HTTP 对外入口；tenant / auth / idempotency / trace 入口；SSE streaming endpoint；Task execution lifecycle；Task tree；Session 壳；Agent / AgentRegistry SPI；client invocation reference adapters。 | 不拥有 bus 物理通道；不拥有 engine SPI canonical definition；不拥有 model / skill / memory / vector 等 middleware SPI 的全局语义；不允许外部模块绕过受控入口写 Task State。 | Gateway, Agent Service, Runtime Governance, Observability, Context Engine 的 agent-service 侧 |
| `agent-execution-engine` | Architecture Module | 与 `agent-service` 同进程运行；Orchestrator / Checkpointer / RunContext / SuspendSignal 等 orchestration SPI；ExecutorAdapter / GraphExecutor / AgentLoopExecutor 等 engine SPI；Planner SPI；engine envelope dispatch 语义。 | 不拥有 HTTP 对外入口；不直接写 Task State；不拥有业务 tool / model provider；不作为默认远程微服务边界。 | Execution Orchestration, Agent Execution |
| `agent-middleware` | Architecture Module | RuntimeMiddleware；ModelGateway；Skill / SkillRegistry；Memory / Vector / Retriever / Embedding；PromptTemplate；ChatAdvisor 等中间件 SPI。 | 不拥有 Task lifecycle；不写 Task State；不承载业务工具内部实现；不依赖 `agent-service` 或 `agent-execution-engine`。 | Tool Governance, Context Assembly, Enterprise Middleware Integration |
| `agent-bus` | Architecture Module | S2C callback transport；跨边界 A2A control command；federation SPI；control / data-reference / rhythm 三通道契约；控制指令中的 payload reference 语义。 | 不执行 engine；不拥有 Task State；不解释业务结果；不承载 provider adapter；不接管同一 service 进程内的多 Agent 调度；不作为 token stream 或大 payload 数据通道；不替代微服务 Gateway。 | S2C / A2A, Federation, Bus Governance |
| `agent-client` | Runtime Component | SDK 壳；client-side cursor / callback / SSE stream consumption 的目标位置。 | 不直接调用 compute_control HTTP route；不导入 `agent-service` / `agent-execution-engine` / `agent-middleware`。 | Developer SDK, Local Capability Endpoint |
| `agent-evolve` | Runtime Component | Evolution plane Java adapter 壳；online / offline evolution SPI 边界。 | 不写 Task State；不依赖 compute_control、bus 或 client 模块；不把 evolution export 写成默认持久化行为。 | Evolution, Offline / Online Learning Boundary |

下沉项：

| 项 | 分类 | 下沉位置 |
|---|---|---|
| `spring-ai-ascend-dependencies` | Build Artifact / dependency governance | build governance、version consistency、release checklist；不进入 L0 模块边界。 |
| `spring-ai-ascend-graphmemory-starter` | Packaging Artifact / adapter scaffold | adapter implementation constraint 或 starter appendix；不进入 L0 模块边界。 |

能力聚合和架构模块的关系在 [Capability Map](../01-capabilities/capability-map.md) 中展开；详细责任卡见 [Module Responsibility Cards](../04-modules/module-responsibility-cards.md)。如果某个 reactor module 只是构建、发布或 starter 工件，它可以在实现约束中出现，但不得被提升为 L0 架构模块。

## 9. 平台能力和业务能力归属

平台能力：

- Task / Session lifecycle 约束，以及 client invocation reference 兼容。
- SPI、contract、hook、middleware、capacity、tenant isolation。
- Trace、audit、idempotency、policy、verification gate。
- LLM 调用成本统计和按 tenant / app / agent / parent-child Task 的归集。
- 跨 service / 跨部门 / 跨部署 A2A / federation 控制面、发布控制能力和 Bus contract。
- `agent-service` 对外 SSE streaming，和 Bus 控制指令 / data reference 的职责隔离。
- 弱部门 PaaS 的 hosted runtime、配置治理和运维面。

业务能力：

- AgentDefinition 内容、业务 prompt、业务 skill 实现、业务 memory 内容。
- 外部系统领域状态和业务审批策略。
- 客户数据源的认证、授权和细粒度权限定义；平台只使用客户授予的 auth reference 调用数据源。
- 发布验收和实际审批动作；平台提供控制能力，不自动替运营人员或客户授权人员批准。
- 非 LLM 的客户内部工具成本和业务系统成本；平台只记录 usage reference / trace。
- 业务侧可以实现 SPI，但不得 patch 平台内部实现。

## 10. 关键质量属性

| 属性 | 可验证表达 |
|---|---|
| 可追踪 | 每个核心设计项能追踪到 Principle、Capability、Module、ADR、ICD、Scenario、Verification。 |
| 可恢复 | suspend/resume 场景必须证明 checkpoint 或 equivalent resume payload 在状态进入 Suspended 前可用。 |
| 幂等 | 不可逆副作用必须有 idempotency key 或等价去重语义，并有 audit record。 |
| 租户隔离 | 所有跨模块调用传播 tenantId；tenant mismatch 不泄露其他租户资源。 |
| 可观测 | 每个核心 scenario 有 trace/event/assertion，失败路径也有可见信号。 |
| 可演进 | 破坏性 contract 变更必须走 ADR / Change Governance。 |

## 11. 关键 ADR 索引

| 主题 | 权威 ADR | 本目录 ADR 草案 |
|---|---|---|
| agent-service consolidation / real module shape | ADR-0078 | `ADR-001`, `ADR-002` |
| engine envelope and strict matching | ADR-0072 | `ADR-002` |
| engine hooks and middleware | ADR-0073 | `ADR-004` |
| S2C callback | ADR-0074 | `ADR-001`, `ADR-004` |
| gateway / ingress boundary | ADR-0089 | `ADR-002` |
| agentic contract surface | ADR-0120..ADR-0128 | `ADR-003`, `ADR-004` |
| Task lifecycle single owner（历史 Run aggregate 命名兼容） | ADR-0142 | `ADR-001` |
| TaskEvent / execution event hierarchy（历史 RunEvent 命名兼容） | ADR-0145 | `ADR-001`, `ADR-005` |

## 12. 核心场景索引

核心场景必须是业务活动级场景，用来检验多个真实模块是否能围绕同一个业务目标协作。S1-S6 是 `technical/` 下的机制验证子场景，不再作为 Overview 的核心场景主入口。

| Business Activity Scenario | 业务活动 | 串联模块 | 主要验证 |
|---|---|---|---|
| BA-001 Agent Handles Business Request | 应用开发者集成 `agent-client`，开发态调试 Agent 编排，运行态承载业务请求和观测。 | agent-client, agent-bus, agent-service, agent-execution-engine, agent-middleware | Task lifecycle, context assembly, tool governance, developer debug timeline, metrics, trace/audit |
| BA-002 Human Approval Tool Call | 应用开发者接入高风险工具，开发态验证审批路径，运行态通过 suspend/resume 完成人工确认和审计。 | agent-service, agent-execution-engine, agent-middleware, agent-bus, agent-client | suspend/resume, S2C callback, tool approval, approval debug evidence, approval metrics |
| BA-003 Multi-Agent Delegation | 应用开发者编排多 Agent 协作，开发态理解父子 Task，运行态观察跨 Agent 成本、延迟和失败。 | agent-service, agent-execution-engine, agent-bus, agent-middleware | parent/child Task, same-service collaboration boundary, federation boundary, non-holding wait, Task tree trace, operations insight |

技术子场景索引：

| Technical Sub-scenario | 用途 |
|---|---|
| S1 Create Task / Invocation | 入口、幂等、Task 初始状态和 client invocation reference。 |
| S2 Execute Agent Step | agent step 执行、engine dispatch、terminal transition。 |
| S3 Build Context Package | Session / Memory / Retrieval 上下文装配。 |
| S4 Tool Call With Governance | tool/skill capacity、authz、audit、hook。 |
| S5 Suspend / Resume | 长等待释放资源、恢复状态。 |
| S6 Child Task / Federation | child Task、跨 service / 跨部门 A2A、Bus 控制指令、join 和 Task tree 聚合。 |

## 13. 风险和待决问题

| ID | 风险 | 处理 |
|---|---|---|
| RISK-001 | 通用模块名和真实 reactor module 名不一致。 | Module Cards 中提供能力到真实模块映射。 |
| RISK-002 | Tool Gateway、Context Engine 不是独立模块。 | 作为 capability 聚合处理，不生成不存在的模块路径。 |
| RISK-003 | 多个 design_only contract 还没有 runtime binding。 | 在 ICD、Scenario、Harness 中显式标记 draft / design_only。 |
| RISK-004 | harness 过早绑定实现细节。 | Harness 只绑定行为、mocks、stubs、assertions，不绑定存储或 broker 细节。 |
