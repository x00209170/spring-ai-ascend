---
level: L1
view: development
status: draft
---

# Module Responsibility Cards

## 目的

按架构准入判定后的 Architecture Module / 必要 Runtime Component 定义职责、非职责、状态归属、上下游契约和开放问题。Gateway、Workflow、Context Engine、Tool Gateway、A2A、Capability Placement 等是能力或机制名，不是本文件的主键。BoM、starter、dependency management、adapter scaffold 不作为核心架构模块责任卡出现，只能下沉到附录或实现约束。

## 适用读者

模块负责人、AI agent、架构评审者、harness 生成器。

## 维护规则

- 核心 `Module Name` 必须是通过 Overview 准入判定的 Architecture Module / Runtime Component。
- `Owned State` 必须与 [State Ownership Matrix](../06-state/state-ownership-matrix.md) 一致。
- 能力聚合只出现在 `Provided Capabilities` / `Consumed Capabilities`，不得冒充模块。
- `Participating Scenarios` 必须优先列 BA-* 业务活动场景，再列 technical sub-scenario。
- 涉及 local client、business service、platform hosted service、platform middleware、federation remote 时，必须说明本模块在 capability placement 中承担什么，不承担什么。
- 平台统一统计 LLM 调用成本；客户内部工具成本、客户数据源权限和业务状态不得写成平台模块状态。

## 模块准入总表

| ID | Module / Artifact | Classification | 是否进入核心模块卡 | 说明 |
|---|---|---|---|---|
| MOD-001 | `agent-service` | Architecture Module | 是 | Task / Session / Agent service boundary，兼容历史 Run 命名。 |
| MOD-002 | `agent-execution-engine` | Architecture Module | 是 | Orchestration / engine / planner SPI boundary；目标运行形态与 `agent-service` 同进程。 |
| MOD-003 | `agent-middleware` | Architecture Module | 是 | Model / Skill / Memory / Retriever / Prompt / RuntimeMiddleware SPI boundary。 |
| MOD-004 | `agent-bus` | Architecture Module | 是 | S2C / 跨边界 A2A / federation / control bus boundary；Gateway 入口能力从 Bus 中拆出。 |
| MOD-005 | `agent-client` | Runtime Component | 是 | SDK / local capability execution boundary。 |
| MOD-006 | `agent-evolve` | Runtime Component | 是，但非主执行链路 | Evolution plane boundary。 |
| APP-A | `spring-ai-ascend-dependencies` | Build Artifact | 否 | 只进入 dependency / version governance 附录。 |
| APP-B | `spring-ai-ascend-graphmemory-starter` | Packaging Artifact / Adapter Scaffold | 否 | 只进入 adapter implementation constraint / starter 附录。 |

## MOD-001 `agent-service`

| Field | Value |
|---|---|
| Module Name | `agent-service` |
| Classification | Architecture Module |
| Runtime Position | 平台 hosted service 的核心 runtime；强部门模式下也作为平台侧 Task / control reference owner；与 `agent-execution-engine` 同进程运行。 |
| Purpose | 统一承载 HTTP 对外入口、tenant / auth / idempotency / trace 入口、SSE streaming endpoint、Task execution lifecycle、Session shell、Agent registration surface 和 client invocation reference adapters。 |
| Responsibilities | 创建和查询 Task；维护 Task lifecycle / TaskStateStore；兼容当前实现或历史文档中的 RunRepository / RunStateMachine 命名；维护 Session shell；绑定 tenant、actor、trace、idempotency、cost attribution key；承载 Agent / AgentRegistry SPI；输出开发态 Task timeline 和运行态查询面；通过 SSE 向外实时流式输出结果；在多 Agent 协作中维护 parent / child Task relationship；同一 service 进程内的多 Agent 协作、join 和结果合并由本模块闭环。 |
| Non-Responsibilities | 不拥有 bus physical channel；不把 engine 作为远程微服务调用；不拥有 engine SPI canonical definition；不拥有 model / skill / memory / vector / prompt / advisor 等 middleware SPI 的全局语义；不定义客户数据源细粒度权限；不拥有客户业务状态或客户内部工具成本。 |
| Owned State | Task Execution State；Session State；IdempotencyRecord；Agent registry draft；Task tree relationship；LLM cost attribution reference；HTTP 对外入口 trace / tenant request carrier；client invocation reference。 |
| Read-only State | engine envelope；middleware contract；bus channel contract；customer auth reference metadata；capacity / sandbox governance docs。 |
| Forbidden State Mutations | 外部模块不得直接写 Task Execution State；`service.runtime` 不得依赖 `service.platform` ThreadLocal tenant；不得把客户业务状态、客户数据源权限模型或客户工具成本写成平台状态；不得新增独立于 Task 的 Run State owner。 |
| Capability Placement Role | 记录每次 Task / step 的 execution locus；为 platform hosted service 创建 hosted Task；为 local client / business service handoff 维护 cursor、trace 和 resume validation；为 federation remote 建立 child Task relationship。 |
| A2A / Bus Role | 同一 `agent-service` 进程内维护 parent / child Task tree 和 join；跨 service / 跨部门 / 跨部署边界通过 `agent-bus` 发送或接收 A2A control command；不绕过 bus 私连远端 Agent。 |
| Cost Role | 记录并暴露 LLM cost attribution key，可按 tenant / app / agent / parent-child Task 聚合；不对客户内部工具成本定价。 |
| Provided Capabilities | Gateway 背后的 HTTP 对外入口；Service SSE Stream；Agent Service；Runtime Governance；Observability 的 service 侧；Context Engine 的 service 侧；Task tree query。 |
| Consumed Capabilities | Workflow / Orchestrator；Tool Gateway；Enterprise Middleware Integration；Bus S2C / A2A；Capability Placement。 |
| Provided Contracts | TaskStateStore；历史 RunRepository 兼容入口；Session shell；ContextProjector；Agent；AgentRegistry；HTTP 对外入口 contract；Task tree query draft。 |
| Consumed Contracts | engine-envelope；engine-hooks；s2c-callback；a2a / federation envelope；middleware SPI contracts；ICD-CS-Capability-Placement。 |
| Participating Scenarios | BA-001, BA-002, BA-003; technical S1, S2, S3, S5, S6 |
| Platform / Business Ownership | 平台拥有 runtime control、Task lifecycle、LLM cost attribution 和观测面；业务通过 AgentDefinition、SPI、客户数据源授权引用和配置接入。 |
| Evolution Direction | 保持 Task lifecycle single owner，逐步把历史 Run 命名收敛为 client invocation 兼容，把 design_only agentic surface、Task tree query、developer evidence query 绑定到 runtime。 |
| Development Pack | [agent-service/README.md](agent-service/README.md) |
| Open Issues | Agent / runtime adapter contract、Task tree query、developer evidence query 仍需要正式 ICD 和 harness；详见 [agent-service/open-issues.md](../l1/agent-service/10-governance/open-issues.md)。 |

## MOD-002 `agent-execution-engine`

| Field | Value |
|---|---|
| Module Name | `agent-execution-engine` |
| Classification | Architecture Module |
| Runtime Position | 平台 hosted runtime 或强部门本地 runtime 的执行编排层；与 `agent-service` 同进程运行；不作为入口层。 |
| Purpose | 定义执行和编排 SPI：orchestration、engine adapter、planner、suspend/resume checkpoint。 |
| Responsibilities | Orchestrator；Checkpointer；RunContext；SuspendSignal；ExecutorDefinition；ExecutorAdapter；GraphExecutor；AgentLoopExecutor；EngineHookSurface；Planner SPI；把 Agent step 结果表达为状态转换意图、tool intent、context request、local capability request 或 child-task / delegation intent。 |
| Non-Responsibilities | 不拥有 HTTP 对外入口；不直接写 Task Execution State；不拥有业务 skill / model provider；不拥有客户数据源权限模型；不直接调用远端 Agent 绕过 bus；不作为独立微服务对外暴露；不通过远程调用依赖 `agent-service`。 |
| Owned State | 无业务持久状态；拥有执行侧契约、编排 SPI、checkpoint SPI 语义和 engine envelope 语义。 |
| Read-only State | RunContext；engine envelope；planner request / plan contract；placement profile；customer auth reference metadata。 |
| Forbidden State Mutations | 不得绕过 `agent-service` 受控入口写 Task Execution State；不得持久化客户业务状态；不得把 LLM cost 直接结算为业务账单。 |
| Capability Placement Role | 根据 step result 触发 S3 / S4 / S5 / S6；本地 context / memory / retriever / tool / approval UI 必须表达为 S2C / Yield，不直接读取本地资源。 |
| A2A / Bus Role | 只能产生 child-task / delegation intent；同一 service 内由 `agent-service` 建立 Task tree 与 join，跨边界再由 `agent-bus` 传递控制指令。 |
| Cost Role | 在 model step evidence 中输出 token / model route / cost input；平台归集由 `agent-service` / Observability 完成。 |
| Provided Capabilities | Workflow / Orchestrator；Agent Execution；Planner；Suspend / Resume mechanism。 |
| Consumed Capabilities | RuntimeMiddleware hook surface；S2C / A2A bus contracts；Agent Service Task state owner；Capability Placement；Observability。 |
| Provided Contracts | orchestration SPI；engine SPI；planner SPI；checkpoint SPI；SuspendSignal contract。 |
| Consumed Contracts | engine-hooks；s2c-callback；a2a / federation envelope；run-event draft；ICD-CS-Capability-Placement。 |
| Participating Scenarios | BA-001, BA-002, BA-003; technical S2, S5, S6 |
| Platform / Business Ownership | 平台定义执行 SPI；强部门可在业务侧部署等价 runtime，但跨平台协作仍遵守 bus / contract。 |
| Evolution Direction | 从 reference sync execution 走向 durable async orchestration、heterogeneous engine adapters 和 child-task join semantics。 |
| Open Issues | durable scheduler / async orchestration binding、join policy、checkpoint production binding 仍需后续波次。 |

## MOD-003 `agent-middleware`

| Field | Value |
|---|---|
| Module Name | `agent-middleware` |
| Classification | Architecture Module |
| Runtime Position | 平台中间件能力层；也可作为强部门或 adapter 侧 SPI 契约依赖。 |
| Purpose | 定义跨切面和企业中间件 SPI，承接模型、工具、memory、retriever、prompt、advisor 和 runtime hook。 |
| Responsibilities | RuntimeMiddleware；HookPoint；ModelGateway；Skill / SkillRegistry；MemoryStore；VectorStore；Retriever；EmbeddingModel；PromptTemplate；ChatAdvisor；tool / model / memory 的 policy、capacity、audit、trace evidence。 |
| Non-Responsibilities | 不拥有 Task lifecycle；不写 Task State；不承载业务工具内部实现；不依赖 `agent-service` / `agent-execution-engine`；不定义客户数据源细粒度权限；不直接拥有客户业务数据。 |
| Owned State | 中间件 SPI 语义；tool / model / memory / retriever interaction contract；provider adapter boundary；policy decision evidence shape；tool call record draft。 |
| Read-only State | RunContext / Task context；tenant policy；skill / memory / model metadata；customer auth reference；execution locus。 |
| Forbidden State Mutations | 不直接写 Task State；不直接拥有业务状态；不绕过客户 auth reference 访问客户数据源；不把 provider adapter telemetry 作为唯一 observability sink。 |
| Capability Placement Role | 执行 platform middleware / platform hosted service 的 model、tool、memory、retriever、prompt 能力；对 business service / local client 能力只发出受控 intent / handoff，不伪装为平台本地调用。 |
| A2A / Bus Role | 对 federation policy、capacity、release control hook、audit 提供治理能力；不传递 A2A 控制消息本身。 |
| Cost Role | ModelGateway 产出 token usage、model route 和 LLM cost evidence；客户内部工具成本只记录 usage reference / trace。 |
| Provided Capabilities | Tool Gateway；Context Engine 的 middleware 侧；Enterprise Middleware Integration；Runtime policy hooks；ModelGateway。 |
| Consumed Capabilities | Workflow / Orchestrator；Observability；Runtime Governance；Capability Placement。 |
| Provided Contracts | middleware SPI；model / skill / memory / vector / retrieval / prompt / advisor contracts；policy decision evidence draft。 |
| Consumed Contracts | engine-hooks；skill-capacity；workflow-observability draft；customer auth reference profile；ICD-CS-Capability-Placement。 |
| Participating Scenarios | BA-001, BA-002, BA-003; technical S2, S3, S4, S5, S6 |
| Platform / Business Ownership | 平台定义 SPI 和治理边界；业务实现 Skill / provider-specific behavior；客户保留数据源权限体系。 |
| Evolution Direction | 把 design_only SPI surface 逐步推进到 runtime enforced adapter，并补齐 policy / capacity / audit evidence。 |
| Open Issues | Tool Gateway / Context Engine 是能力聚合，不是独立模块；customer auth reference 和 local capability result schema 需要正式化。 |

## MOD-004 `agent-bus`

| Field | Value |
|---|---|
| Module Name | `agent-bus` |
| Classification | Architecture Module |
| Runtime Position | 平台跨边界控制通道；强部门 service、弱部门平台托管 service、client callback 都通过它进行受控控制交互。Gateway 入口能力从 Bus 中拆出，当前仅代理 `agent-service`。 |
| Purpose | 拥有 S2C、A2A、federation、resume 和 bus channel 的契约面；主要传递控制指令和数据引用。 |
| Responsibilities | S2cCallbackTransport；A2A control command；Federation control surface；control / data-reference / rhythm 三通道声明；callback / completion / failure / timeout envelope；传递 URI / object reference / metadata；后续 mailbox / wake pulse contract。 |
| Non-Responsibilities | 不执行 engine；不拥有 Task State；不解释业务结果；不承载 provider adapter；不接管同一 `agent-service` 进程内的多 Agent 调度；不传输大型多模态 payload；不承载逐 token stream；不替代微服务 Gateway；不替审批人或运营人员做发布批准动作。 |
| Owned State | bus channel contract；s2c / a2a / federation envelope semantics；data reference envelope semantics；后续 mailbox / wake pulse state。 |
| Read-only State | tenant / trace / task identity carried in envelopes；parentTaskId / childTaskId correlation；execution locus；历史 runId / parentRunId 兼容字段。 |
| Forbidden State Mutations | 不直接写 Task State；不直接变更业务状态；不绕过 `agent-service` 创建 child Task；不把跨边界 A2A 写成 service / engine 私连；不把同一 service 内协作错误外包给 Bus。 |
| Capability Placement Role | 为 local client、business service、platform hosted service、federation remote 之间的 handoff 传递控制消息和结果 envelope；大型数据只传 URI / object reference / metadata。 |
| A2A / Bus Role | 跨 service / 跨部门 / 跨部署 A2A 控制指令的唯一平台通道；强部门 service 与弱部门 hosted service 互联都必须经过本模块契约；同一 service 进程内协作不经过 Bus。 |
| Cost Role | 只承载 cost attribution correlation，不计算或结算成本。 |
| Provided Capabilities | S2C callback；Agent Swarm / Federation；A2A control channel；Data Reference Path；Bus Governance。 |
| Consumed Capabilities | Runtime Governance；Observability；Task tree identity from Agent Service。 |
| Provided Contracts | s2c-callback；a2a-control-command；federation-envelope；bus-channels；data-reference-envelope draft。 |
| Consumed Contracts | Task identity semantics and client invocation reference from agent-service；resume validation contract；ICD-CS-Capability-Placement。 |
| Participating Scenarios | BA-001, BA-002, BA-003; technical S1, S5, S6 |
| Platform / Business Ownership | 平台拥有 bus contract 和控制面；业务 service / client 作为受控 endpoint 接入。 |
| Evolution Direction | 从 schema / SPI surface 演进到物理隔离的 control / data-reference / rhythm bus implementation、durable wake-pulse 和 federation runtime binding。 |
| Open Issues | control / data-reference / rhythm 的物理 transport、A2A wire schema、durable wake-pulse 仍为后续实现；A2A 场景中调用方要求被调方流式返回时，是否经由 service-to-service SSE、专用 stream channel 或其他机制仍需技术确认，不默认走 Bus 逐 token 事件流。 |

## MOD-005 `agent-client`

| Field | Value |
|---|---|
| Module Name | `agent-client` |
| Classification | Runtime Component |
| Runtime Position | 业务应用或个人本地环境中的 SDK / local capability endpoint。 |
| Purpose | 面向集成 `agent-client` 的应用开发者，提供入口封装、cursor / callback 消费、本地 capability 执行和开发态调试接入点。 |
| Responsibilities | client-side request packaging；tenant / actor / trace carrier；Task Cursor consumption；SSE stream consumption；S2C callback receive / result return；本地 context、memory、retriever、tool、approval UI 的执行入口；开发态 debug evidence 展示或导出。 |
| Non-Responsibilities | 不直接调用 compute_control HTTP route；不导入 `agent-service` / `agent-execution-engine` / `agent-middleware`；不写 server-side Task / Session state；不伪造平台 policy decision。 |
| Owned State | client-local cursor / callback state draft；local capability execution cache / reference；local debug evidence view。 |
| Read-only State | published client contract；ingress response；S2C / Yield instruction；Task timeline query result。 |
| Forbidden State Mutations | 不写 server-side Task / Session state；不直接修改 platform audit / trace；不绕过 S2C contract 返回本地工具结果。 |
| Capability Placement Role | 可以承载 local context、local memory、local retriever、local tool、approval UI；必须通过 S2C / Yield 返回受控 result，不把本地业务事实直接交给平台持久化。 |
| A2A / Bus Role | 作为 client endpoint 接收 S2C，不作为 A2A control command 的平台 owner。 |
| Cost Role | 可展示平台返回的 LLM cost attribution；不计算平台成本，不把客户内部工具成本归集为平台 LLM 成本。 |
| Provided Capabilities | Developer SDK；Local Capability Endpoint；Developer Experience entry。 |
| Consumed Capabilities | agent-service SSE stream；agent-bus S2C；Observability correlation；Capability Placement policy。 |
| Provided Contracts | client SDK contract draft；local capability result profile draft。 |
| Consumed Contracts | service-sse-stream draft；ingress-envelope；s2c-callback；ICD-CS-Capability-Placement；Task timeline query draft。 |
| Participating Scenarios | BA-001, BA-002, BA-003; technical S1, S3, S4, S5 |
| Platform / Business Ownership | 平台提供 SDK 和 contract；业务应用消费 SDK 并拥有本地工具、业务数据和本地执行环境。 |
| Evolution Direction | W3+ runtime SDK binding；普通业务请求经 Gateway / `agent-service` 入口，S2C / callback 经 Bus contract，并增强本地能力调试体验。 |
| Open Issues | 当前主要是 skeleton / placeholder；local capability result schema、debug evidence query 和 SDK harness 需要正式化。 |

## MOD-006 `agent-evolve`

| Field | Value |
|---|---|
| Module Name | `agent-evolve` |
| Classification | Runtime Component |
| Runtime Position | Evolution plane，非主执行链路。 |
| Purpose | Evolution plane Java adapter shell，用于后续 online / offline evolution 和 Python ML pipeline 对接。 |
| Responsibilities | evolution export boundary；online/offline evolution SPI；后续 Python ML pipeline adapter；消费经过治理的 TaskEvent / Trace / Audit export。 |
| Non-Responsibilities | 不写 Task State；不默认持久化 out-of-scope events；不依赖 compute_control、bus、client modules；不直接读取客户业务数据；不成为 Observability owner。 |
| Owned State | evolution adapter contract；export scope semantics；evolution job reference draft。 |
| Read-only State | TaskEvent export decision；trace / telemetry samples；redacted replay-safe fixture。 |
| Forbidden State Mutations | 不修改 Task / Session lifecycle state；不回写业务状态；不绕过 export scope 读取敏感数据。 |
| Capability Placement Role | 无主执行链路 placement 决策权；只能消费经过脱敏和授权的 export evidence。 |
| A2A / Bus Role | 不参与 A2A 控制面。 |
| Cost Role | 可消费汇总后的 LLM cost evidence 作为分析信号；不产生计费口径。 |
| Provided Capabilities | Evolution；Offline / Online Learning Boundary。 |
| Consumed Capabilities | Observability export；TaskEvent taxonomy draft；Replay-safe Fixture governance。 |
| Provided Contracts | evolution SPI draft；export scope draft。 |
| Consumed Contracts | evolution-scope；run-event draft；replay-safe fixture draft。 |
| Participating Scenarios | 后续 evolution BA scenario；不参与 BA-001 / BA-002 / BA-003 主执行链路。 |
| Platform / Business Ownership | 平台。 |
| Evolution Direction | 后续接入 Python ML pipeline，所有输入必须经过 observability export 和数据脱敏治理。 |
| Open Issues | 当前非主执行链路，主要是边界和 design_only surface；需要单独 BA 场景再进入核心验证闭环。 |

## Appendix A: Build Artifact `spring-ai-ascend-dependencies`

| Field | Value |
|---|---|
| Classification | Build Artifact / dependency governance |
| Purpose | BoM 和依赖版本管理；不作为 L0 架构模块。 |
| Responsibilities | 管理平台模块和第三方依赖版本；发布 dependency management；支持 release lockstep。 |
| Non-Responsibilities | 不包含 runtime code；不提供 SPI；不承载业务能力；不拥有 runtime state。 |
| Owned State | dependency version set。 |
| Read-only State | root parent POM properties；module artifacts。 |
| Forbidden State Mutations | 不改变 runtime state。 |
| Provided Capabilities | Dependency Governance；Configuration / Extensibility support。 |
| Consumed Capabilities | none at runtime。 |
| Provided Contracts | Maven dependency management。 |
| Consumed Contracts | release / compatibility policy。 |
| Participating Scenarios | none directly |
| Platform / Business Ownership | 平台。 |
| Evolution Direction | 随 release lockstep 更新。 |
| Open Issues | 无运行时 harness；验证重点是 dependency / compatibility；应归入 build governance 或 release checklist。 |

## Appendix B: Packaging Artifact `spring-ai-ascend-graphmemory-starter`

| Field | Value |
|---|---|
| Classification | Packaging Artifact / adapter scaffold |
| Purpose | GraphMemory sidecar adapter starter；不作为 L0 架构模块。 |
| Responsibilities | 消费 memory SPI；提供 GraphMemory / Graphiti 类 adapter scaffold；默认关闭；作为下游 memory adapter 示例。 |
| Non-Responsibilities | 不拥有 Memory SPI；不拥有 Task / Session；不写 Task State；不进入主控制链路模块边界。 |
| Owned State | starter-local configuration and adapter wiring。 |
| Read-only State | GraphMemoryRepository SPI；provider configuration；memory-store contract。 |
| Forbidden State Mutations | 不写平台 lifecycle state；不绕过 memory SPI；不直接拥有客户业务状态。 |
| Provided Capabilities | Enterprise Middleware Adapter；Memory Integration。 |
| Consumed Capabilities | Context Engine 的 memory side；Configuration Governance。 |
| Provided Contracts | starter config contract。 |
| Consumed Contracts | memory-store / GraphMemoryRepository SPI。 |
| Participating Scenarios | BA-001; technical S3 as downstream memory adapter |
| Platform / Business Ownership | 平台 adapter；业务提供 external service config。 |
| Evolution Direction | 从 scaffold 演进到真实 GraphMemory adapter。 |
| Open Issues | 当前不是主控制链路模块；应归入 adapter implementation constraint 或 starter appendix。 |
