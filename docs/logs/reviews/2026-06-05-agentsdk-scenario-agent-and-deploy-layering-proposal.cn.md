# Proposal — AgentSDK（配置化场景智能体）+ 部署/服务承载层：对齐 AgentScope-Runtime-Java

> 形态：design-only 提案，提交架构师 + 工程团队评审。**不含任何实现代码，不急于开工。**
> 关联：ADR-0128（Agent 一等实体 / AgentDefinition）、ADR-0152（Feature 注册表 schema）、ADR-0156（产品权威）、ADR-0158（EnginePort）、ADR-0159（runtime/service 重分区，2026-06-03 settled）、`docs/logs/reviews/2026-06-04-agent-service-facade-registration-collaboration-proposal.cn.md`（§11 待裁问题）、`docs/reviews/2026-06-04-ai-native-rd-paradigm-reflection.zh.md`
> 对标实证来源：`agentscope-ai/agentscope-runtime-java` + `agentscope-ai/agentscope-java`（GitHub，Apache-2.0）的**真实源码逐文件核对**（非记忆、非网络搜索）；本机 clone 位于 `D:\ai-research\agent-platforms-survey\`

---

## 0. 摘要（TL;DR）

产品负责人提出两层缺口：(1) AgentRuntime 与"基于框架开发的业务 Agent"之间应有一层承载 `AgentHandler`、并支持把"runtime + 框架基础 handler"打包成**行业场景 handler** 的 **AgentSDK**；(2) runtime 本身需要一个 **AgentService** 把它**拉起来并管理**。

逐文件核对 AgentScope-Runtime-Java 后确认：**两层在它那里都真实存在**，产品负责人的措辞与其实现几乎一一对应。同时，产品权威 **PC-001 早已把"配置化 YAML 开发"写成第一卖点**，且仓库里**已有** `agent-definition.v1.yaml`（ADR-0128）作为声明式 Agent 契约——所以"配置化场景智能体"不是新发明，而是把已有契约接到 SDK 上。

**本提案的核心主张**：
- **不重新打开 ADR-0159**（它 2 天前才 settle，并刚刚配套删了 8.3k 行未达代码）。agent-runtime 保持"可独立启动"。
- 采取**加法、而非迁移**：命名三条 design-only 接缝——(A) 加厚的 **AgentSDK handler 承载面**、(B) 复用 **AgentScope 式 `DeployManager` 接缝**的**部署/拉起设施**、(C) **配置化场景智能体层**（YAML `AgentDefinition` → 装配成场景 handler）。
- 三条接缝的形状**从现存脊柱样例 `samples/agent-service-a2a-llm-e2e` 派生**（spine-first），不从纯逻辑推。
- **超越 AgentScope 的差异化点**正是 (C)：AgentScope 的配置钩子是"反射选 provider 类"，很粗；我们用富声明式 `AgentDefinition` 支撑金融垂域大量业务规则——这是 PC-001 + PC-003 的兑现。

---

## 1. 背景与触发（为什么是现在）

- `d6812ed1`（2026-06-04）以"reachability audit"为由，删掉脊柱触达不到的一切：**`agent-client`（SDK 骨架）、`agent-evolve`、graphmemory-starter 三个模块 + ~90 个 design-only SPI + 36 个契约 + `samples/finance-loan-review`（层③ 业务 agent 示例）**，reactor 7→5。
- 因此"**好像我们没有这一层了**"在字面上**成立**：承载 handler 的 SDK 制品和唯一的业务 agent 示例都被切掉了——且切得没错（它们当时无人触达）。
- 真正要回答的不是"是否切错"，而是：**当脊柱长到需要一个真实行业 agent 时，承载它的"配置化 SDK 接缝"和"拉起/管理接缝"应该长什么样**。这与 AI-native 宪章"让脊柱暴露最小接缝再冻结"一致。

---

## 2. 实证：AgentScope-Runtime-Java 的真实分层（逐文件核对）

```
①框架        io.agentscope.core.ReActAgent              （仓库 agentscope-java）
              ReActAgent.builder().sysPrompt().toolkit().model().memory().build()
                   ↑ 被②包装
②适配/handler  engine-core/adapters/AgentHandler
              注释原文："adapt different Agent Framework Types: AgentScope, Spring AI Alibaba, Langchain4j"
              getFrameworkType()/start()/stop()/streamQuery()→Flux/getStreamAdapter()/getMessageAdapter()/getSandboxService()
              abstract AgentScopeAgentHandler implements AgentHandler   ← 每框架一个"基础 handler"
                  持 StreamAdapter+MessageAdapter；注入 State/Session/Memory/Sandbox 服务
                   ↑ 被②.5驱动
②.5引擎       engine-core/engine/Runner(AgentHandler)     流式 created→in_progress→stream→completed
              engine/DeployManager { deploy(Runner); undeploy(); }   ← 中立"部署接缝"，在 runtime 核心里
                   ↑ 被③实现/拉起
③部署/服务    web/AgentApp(handler).run(host,port)         开发者入口（AppLifecycleHook 生命周期）
              web/LocalDeployManager implements DeployManager
                  deploy(Runner) = SpringApplicationBuilder 真起一个 Spring Boot，暴露 A2A + ResponseAPI；undeploy()=ctx.close()
                   ↑ 被④消费
④业务agent    examples/MyAgentScopeAgentHandler extends AgentScopeAgentHandler
                  在 streamQuery() 里 build 真正的 ReActAgent（prompt/tools/model/memory）
              main: new AgentApp(handler).cors(..).run(10001)
              依赖：spring-boot-starter-runtime-a2a + agentscope-runtime-agentscope(agents) + io.agentscope.core
```

**两个关键观察**：
1. **`DeployManager` 是放在 runtime 核心（engine-core）里的中立接缝**，实现（`LocalDeployManager`）在 `web`。这把"runtime 怎么被拉起"定义在 runtime 侧、把"用什么拉起"留给上层——一手回答了产品负责人的两个问题。
2. **AgentScope 的"配置化"很粗**：`AgentApp(String[] args)` 读 `.env`，靠 `AgentHandlerProvider`/`ComponentProvider<T>` **反射 `Class.forName` 选 provider 类**。它没有富声明式 agent 定义——这正是我们可以差异化的地方。

---

## 3. 我们今天的状态（ADR-0159 + d6812ed1 之后）

| 维度 | 现状（已核对） |
|---|---|
| 模块（5） | `dependencies` / `agent-bus` / `agent-middleware` / `agent-runtime` / `agent-service` |
| agent-runtime | 自包含、**可独立启动**（`AgentRuntimeApplication`）；owns Run/session/tenant/dispatch/engine/planner；pom 同时产**普通库 jar + boot-classifier jar** |
| agent-service | **骨架** façade；`service.platform.*`（RunController/JWT/幂等/观测）+ 预留注册发现（§Decision.7 deferred，无 SPI） |
| `AgentHandler` | `dispatch.spi.AgentHandler`——**很薄**：`agentId()/isHealthy()/execute()→Stream/resultAdapter()`；无 start/stop 生命周期、不注入 runtime 服务、无 stream/message adapter |
| 框架适配基类 | `OpenJiuwenAgentHandler`（存活在 agent-runtime） |
| 声明式 agent 契约 | **已存在** `docs/contracts/agent-definition.v1.yaml`（ADR-0128）：`agentId/tenantId/modelBinding/toolBindings/memoryBindings/systemPrompt/safetyPolicy/plannerBinding/advisorBindings/metadata`；status=design_only |
| 脊柱 | `samples/agent-service-a2a-llm-e2e`（`OpenJiuwenA2aAgentServiceApplication` + `OpenJiuwenReactAgentA2aE2eTest`）——**这个样例自己装配 handler 并 boot**，即它已经是"AgentApp/SDK"的手搓原型 |
| 依赖方向（锁定） | `agent-service → agent-runtime → {agent-bus, agent-middleware}`；**`agent-runtime ↛ agent-service` 禁止**（E1 / Rule R-C.1） |

**对照 AgentScope 的两点差异**：
- 我们的 `AgentHandler` 比 AgentScope 薄一个数量级（缺生命周期、服务注入、双 adapter）。
- AgentScope 把 boot 放在部署层（engine-core 是纯库）；我们 ADR-0159 把 boot 放进了 agent-runtime 自身。**但脊柱实际是经由 `samples/.../OpenJiuwenA2aAgentServiceApplication`（一个样例自带的 bootable app）拉起的**——也就是说"AgentApp（装配+拉起）"角色今天由样例临时扮演，尚无可复用设施。

---

## 4. 提案（三条 design-only 接缝）

### 4.1 接缝 A — AgentSDK：handler 承载面（加厚 + 可依赖）

把 `AgentHandler` 契约**加厚到 AgentScope 同级**，并明确成业务可干净依赖的 SDK 面：
- 生命周期 `start()/stop()/isHealthy()`；
- 注入 runtime 服务（session / memory / state / 工具沙箱）——即产品负责人说的"**把 AgentRuntime 打包进去**"，AgentScope 正是用基类注入服务实现的；
- per-framework **基础 handler**（`OpenJiuwenAgentHandler`、未来 `SpringAiAgentHandler` 等），对应 PC-004 多框架异构；
- 业务"**行业场景 handler**"只继承基础 handler，在执行体里 build 具体 agent（= AgentScope 的 `MyAgentScopeAgentHandler` 模式）。

> **是否抽成独立模块**留作待裁（见 §5-A）。最小变更是"留在 agent-runtime 内做公开稳定 API"；最干净是"抽 `agent-sdk` 可依赖制品"。

### 4.2 接缝 B — 部署/拉起设施（"把它拉起来并管理"）

复用 AgentScope 的 **`DeployManager { deploy(Runner); undeploy(); }`** 模式，把今天散落在脊柱样例里的"装配 handler + 起 Spring Boot + 暴露协议 + 生命周期"提炼成**可复用设施**（AgentApp 等价物）。

**与 ADR-0159 调和（关键）**：**不把 boot 从 agent-runtime 搬走**。`AgentRuntimeApplication` 保留（它本质就是一个内置的 "local deploy"，服务 Form-1 standalone + dev）。本接缝是**加法**：
- runtime 侧定义**中立 deploy 接缝**（最小：`deploy/undeploy`，归属待裁——见 §6-Q1）；
- agent-service 侧提供 **AgentApp 等价装配/拉起设施**（ADR-0159 已规定 service "drives" runtime，故装配/拉起天然归 service 侧）；企业部署换 deploy 实现，对接 2026-06-04 提案的注册/发现/路由。

### 4.3 接缝 C — 配置化场景智能体层（YAML → 场景 handler）★产品负责人重点

行业智能体业务规则密集 → 存在**一系列场景智能体**（loan-review、AML 筛查、欺诈识别、KYC、客服……，金融垂域 v1.0 已点名 `loan-review-assistant`）。AgentSDK 应支持**用 YAML 等格式把参数/业务规则传入场景智能体，使场景智能体可被配置化开发**。

落地方式：**复用已有 `agent-definition.v1.yaml`（ADR-0128），不新造契约**。SDK 提供一个**装配器**，把一份 YAML `AgentDefinition`（systemPrompt / modelBinding / toolBindings(按 skillKey 引用) / memoryBindings / safetyPolicy / 业务参数入 `metadata`）装配成一个注册到 runtime 的 `AgentHandler`。

**承认配置→代码的光谱（诚实，避免"规则引擎"幻觉）**：

| 层级 | 形态 | 谁来做 | 何时用 |
|---|---|---|---|
| 纯配置 | YAML `AgentDefinition` → 通用 `ConfigDrivenAgentHandler` 装配，**零 Java** | Persona-C / 领域专家 | prompt/工具引用/模型/护栏/业务参数能声明清楚的场景 |
| 配置 + 钩子 | YAML + 少量自定义 tool/validator bean（按 `skillKey` 引用） | Persona-C（少量 Java） | 业务规则需要一段校验/取数逻辑 |
| 全代码 | 手写 `AgentHandler` 子类（= AgentScope `MyAgentScopeAgentHandler`） | Persona-E | 复杂编排/多 agent |

> 这条光谱直接兑现 PC-001（配置化）+ PRODUCT.md "**Config-driven YAML is the abstraction; UI is downstream / customer-built**"，并把"规则引擎/工作流编排器"这种 ceremony 风险挡在外面：复杂规则下沉到被引用的工具或 Tier-2/3 代码，而不是在 YAML 里发明一套 DSL。
> 治理挂钩：每个场景智能体可登记为一个 FEAT- 元素，绑 `saa.productClaim=PC-001|PC-003`、`saa.aiBoundary.sandboxPolicyRef=financial_default`（ADR-0152 + sandbox-policies.yaml），使配置化产物**可被 gate 校验、AI 可读**。

### 4.4 目标分层（对照图）

```
①框架  openjiuwen / Spring AI / ...                         （pom 依赖，PC-004）
②SDK   AgentSDK：加厚 AgentHandler + per-framework 基础 handler + YAML 装配器   ← 接缝A+C
         ├─ ConfigDrivenAgentHandler（YAML AgentDefinition 装配，零 Java）       ← 接缝C
         └─ 业务行业场景 handler（继承基础 handler，可纯配置/配置+钩子/全代码）
③runtime agent-runtime（保持 ADR-0159：Run/session/tenant/dispatch/engine/planner + 可独立启动）
④部署   agent-service：AgentApp 等价装配/拉起设施 + deploy 接缝实现 + 注册发现(2026-06-04)  ← 接缝B
⑤业务   samples/<scenario>/ 场景智能体（依赖 ②，按需 ④ 拉起）— 脊柱在此生长
```

---

## 5. 利弊分析（逐决策）

### 决策 A：加厚并（可选）抽出 AgentSDK 承载面
- **利**：兑现 PC-001/PC-004；业务 agent 有干净可依赖面（不再被迫依赖整个可启动 runtime）；对齐 AgentScope 已验证形态。
- **弊/风险**：抽独立模块 = 反向于"删 agent-client"的动作，架构师会高度敏感（见 §6-C1/§6-C3）。
- **建议**：**先加厚契约（design-only），是否抽模块由脊柱拉出第二个场景 agent 时再定**；若抽，必须被脊柱触达（reachability 审计），避免重蹈 agent-client 空骨架。

### 决策 B：部署/拉起设施 + deploy 接缝（vs ADR-0159 可启动 runtime）
- **利**：把脊柱样例里手搓的装配/拉起复用化；给企业部署（K8s/远程）一个换实现的接缝；对接 2026-06-04 注册发现。
- **弊/风险**：若表述为"把 boot 搬出 runtime"，直接撞 ADR-0159（2 天前 settle）→ 触发重新立法成本、违背"接缝故意贵、别轻易改"。
- **建议**：**纯加法**——保留 `AgentRuntimeApplication`，新增 deploy 接缝 + agent-service 侧装配设施；deploy 接缝归属（agent-bus 中立 SPI vs agent-runtime 暴露）走 §6-Q1 由架构师裁。

### 决策 C：配置化场景智能体（YAML）
- **利**：直接命中 PC-001 + 金融垂域业务规则密集的真实需求；复用已有 `agent-definition.v1.yaml`（零新契约）；领域专家可配置化产出场景 agent；可治理（FEAT- + sandbox 策略）。
- **弊/风险**：易膨胀成"规则引擎/工作流 DSL"（ceremony）；纯 YAML 表达不了所有业务规则。
- **建议**：**采纳，但严守配置→代码光谱**——SDK 只做"YAML→handler 装配 + 工具/技能按 ref 注入"，不发明规则 DSL；复杂规则走被引用工具或代码层。

### 决策 D：推进节奏
- **design-only（推荐）**：写一条新 ADR 命名三条接缝 + 在 workspace.dsl 留缝，不写代码。**利**：符合 design 阶段 + 宪章；**弊**：接缝形状未经运行验证。
- **spine-pull**：先把一个真实场景 agent（建议复活 `loan-review-assistant`）+ 配置化装配 + 拉起设施在脊柱里跑绿，让它逼出接缝真实形状。**利**：最符合"接缝从运行派生"；**弊**：要写运行代码，与"当前 design 阶段/不急"略有张力。
- **carve-now**：立刻抽模块 + 落骨架。**弊**：重蹈过度设计，最不推荐。
- **建议**：**design-only 命名接缝**为主线；若团队同意启动脊柱生长，则以 **spine-pull 复活 loan-review-assistant 作为第一个配置化场景 agent** 来验证接缝（二者不冲突：先 ADR 命名，再由脊柱填实并据此微调）。

---

## 6. 逐条回应架构师 concerns

> 取自 ADR-0158/0159 现行规范、2026-06-04 facade 提案 §11、AI-native 宪章、以及 d6812ed1 切除背后的治理约束。

**C1. SPI/模块膨胀风险（刚删了 90 个 SPI + 3 模块）。**
回应：本提案**净新增 SPI 趋近于零**——接缝 C 复用 `agent-definition.v1.yaml`（已存在）；接缝 A 是加厚现有 `AgentHandler`，非新 SPI；接缝 B 的 deploy 接缝最小化为 `deploy/undeploy`。承诺：任何新 SPI/模块**必须被脊柱 `samples/agent-service-a2a-llm-e2e` 触达**（codegraph reachability 审计），否则不落地。

**C2. 模块边界蠕变 / 新跨模块边。**
回应：不引入 `agent-runtime ↛ agent-service` 反向边。接缝 B 的装配设施归 agent-service（合法 `service→runtime`）。deploy 接缝若需中立化，候选归 agent-bus（与 EnginePort 同位）——此点列入 §6-Q1 由架构师裁，**不在本提案擅自加白名单边**。

**C3. SDK 模块打包 = 复活 agent-client？**
回应：**不复活 agent-client**。接缝 A 的最小形态是"agent-runtime 内公开稳定 API"，是否抽 `agent-sdk` 制品**推迟到脊柱拉出第二个场景 agent 时按 reachability 决定**。理由必须是"被脊柱触达"，而非"逻辑上应该有"。

**C4. EnginePort 中立性 + boot 迁移。**
回应：**不动 EnginePort 契约、不迁 boot**。ADR-0159 的"agent-runtime 可独立启动"保留。接缝 B 是 deploy 生命周期接缝，与 EnginePort（execute/stream/suspend/resume）是**正交关注点**，不改其签名、不破坏 Form-1/2/3 可表达性。

**C5. 配置化层 = ceremony 风险。**
回应：严守 §4.3 配置→代码光谱：SDK 只做 YAML→handler 装配，**不发明规则 DSL/工作流引擎**；产物必须装配进**现有 executor**（GraphExecutor / AgentLoopExecutor），不开新执行路径；且每个场景 agent 必须有脊柱内可跑的样例后才进 ADR（spine-first）。

**C6. 治理度量回归（graph/enforcer 计数）。**
回应：净新增契约=0（复用 0128）、净新增 SPI≈0；新增的是 design 文档 + 派生接缝。承诺过 baseline drift guard，不让 enforcer/契约计数无对应代码地膨胀。

**C7. 三种部署形态可表达性（Form 1/2/3）。**
回应：保留 `AgentRuntimeApplication` + 不动 EnginePort + 不要求按 form 分别构建 → 三形态不变。接缝 B 的企业 deploy 实现是 Form-1 的可选件，不破坏 Form-2/3。

**facade 提案 §11 待裁问题（本提案需要架构师一并裁定）**：
- **Q1**：deploy 接缝 + `InstanceRegistry/DiscoveryProvider` 归 agent-bus（bus_state）还是经 agent-runtime 暴露？（牵涉是否补依赖白名单）
- **Q2**：凭证 SPI（`SecretResolver/IdentityVerifier/PermissionBroker`）"解析在 runtime 使用点、校验在门面入口"切分是否确认？
- **Q5**：是否据本提案 + facade 提案合并立**一条新 ADR**（extends ADR-0159，relates ADR-0128/0158），编号由架构师指派？

---

## 7. 逐条回应工程团队 concerns

**E1. 我到底依赖什么？**
答：业务 agent 依赖 = AgentSDK 面（加厚 handler + 基础 handler + YAML 装配器）+ 框架 jar。纯配置场景甚至只需 YAML + starter。对齐 AgentScope 的 `starter + agents + framework` 三依赖。

**E2. 怎么写一个场景 agent——代码还是 YAML？**
答：按 §4.3 光谱。纯配置：写一份 `AgentDefinition` YAML；配置+钩子：再加少量 tool/validator bean；全代码：继承基础 handler。文档给三档模板。

**E3. 三层测试（Rule D-4）怎么测配置化 agent？**
答：YAML schema 校验（单元）+ 装配后 handler 行为（集成）+ 脊柱 A2A e2e（端到端，复活 loan-review-assistant 作为首个）。配置化产物必须可被同一三层测试覆盖，否则不算 shippable。

**E4. AgentScope 用反射 `Class.forName` 选 provider，脆弱。**
答：我们**不照搬反射**。用 Spring `@ConfigurationProperties` + `AgentDefinition` 强类型绑定（PC-001 明确"ConfigurationProperties-validated config"），按 `skillKey/modelGatewayId` 做 bean 引用解析，避免 classpath/反射脆弱。

**E5. 迁移：finance-loan-review 删了，要重建吗？**
答：建议把 `loan-review-assistant` 作为**第一个配置化场景 agent** 复活进 `samples/`（不挂主 reactor，按现有 sample 约定），既验证三接缝，又兑现 v1.0 金融垂域目标。

**E6. 构建/reactor 影响。**
答：design-only 阶段零构建影响。若 spine-pull，场景 agent 走"sample 不入主 reactor、按需显式加入"的现有约定，不动主线分发。

---

## 8. 本提案明确不做（YAGNI / 非目标）

- 不复活 `agent-client` 空骨架；不为"逻辑完整"而抽模块。
- 不把 boot 从 agent-runtime 搬走、不重开 ADR-0159、不改 EnginePort 签名。
- 不发明规则引擎 / 工作流 DSL / no-code 拖拽 UI（PRODUCT.md 明确 UI 是下游/客户侧）。
- 不引入 AgentScope 的 `AgentApp.run(port)` HTTP/SSE 部署细节之外的多协议端点（保持我们既有的队列 + 事件回写）。
- 不在本提案擅自加任何跨模块依赖白名单边（留架构师裁）。

---

## 9. 推荐决策（汇总）+ 下一步

| 决策 | 推荐 |
|---|---|
| A SDK 承载面 | 加厚 `AgentHandler`（生命周期+服务注入+per-framework 基类），**design-only**；是否抽独立模块待脊柱拉出第二个场景 agent 再定 |
| B 部署/拉起 | **加法**：保留 `AgentRuntimeApplication`；新增最小 deploy 接缝 + agent-service 侧装配设施；归属走 Q1 |
| C 配置化场景 agent | **采纳**，复用 `agent-definition.v1.yaml`；严守配置→代码光谱；治理挂 FEAT-/sandbox |
| D 节奏 | **design-only 命名三接缝（写新 ADR）**为主线；可选以 spine-pull 复活 `loan-review-assistant` 验证接缝 |

**下一步（待架构师 / 工程团队评审后执行）**：
1. 本提案即落于 `docs/logs/reviews/2026-06-05-agentsdk-scenario-agent-and-deploy-layering-proposal.cn.md`（与 2026-06-04 facade 提案同目录、同 .cn.md 约定；docs/logs 为 gate 排除的历史区，不受英文 lint）。
2. 与 2026-06-04 facade 提案合并，请架构师据 §6-Q1/Q2/Q5 立一条新 ADR（extends ADR-0159，relates ADR-0128/0158）。
3. （可选，经同意后）spine-pull：复活 `loan-review-assistant` 为首个配置化场景 agent，跑绿 A2A e2e，让脊柱派生并冻结三条接缝的真实形状。

---

## 10. 留给架构师拍板的开放问题

1. **接缝 B 的 deploy 接缝归属**：agent-bus 中立 SPI（同 EnginePort）还是经 agent-runtime 暴露？（facade Q1）
2. **接缝 A 是否抽 `agent-sdk` 独立制品**，还是只在 agent-runtime 内做公开稳定 API？触发条件（reachability）是否接受？
3. **是否合并 facade 提案立一条新 ADR**、编号与 extends/relates 链如何指派？（facade Q5）
4. **节奏**：纯 design-only 命名接缝，还是同时启动 spine-pull 复活 `loan-review-assistant`？

---

### 验证（本提案如何被检验为"对"）
- 实证可复核：源码 `agentscope-ai/agentscope-runtime-java`（`engine-core/adapters/AgentHandler.java`、`engine/Runner.java`、`engine/DeployManager.java`、`web/app/AgentApp.java`、`web/LocalDeployManager.java`、`examples/.../MyAgentScopeAgentHandler.java`）；本机 clone：`D:\ai-research\agent-platforms-survey\`。
- 治理可检验：任何落地都需过 E1 依赖方向、baseline drift guard、codegraph reachability、三层测试 + 脊柱 A2A e2e 绿。
- 价值可追溯：场景 agent → PC-001（配置化）/PC-003（金融生产级）/PC-004（多框架）/Persona-C/F；复用 ADR-0128 契约，零新契约。
