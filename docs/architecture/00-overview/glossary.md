---
level: L0
view: logical
status: draft
---

# Glossary

## 目的

统一架构文档中的核心术语，避免 AI agent 和模块负责人把不同层级的概念混用。

## 适用读者

所有使用 `docs/architecture/` 的读者。

## 维护规则

- 新术语必须给出 owner、层级和禁止混淆项。
- 若术语已在 `docs/adr/` 或 `docs/contracts/` 中有定义，以权威来源为准。

| Term | 中文解释 | Owner / Home | 不要混淆为 |
|---|---|---|---|
| Task | 服务端 canonical 执行与控制状态，表达“要做的事是否完成、为什么停止、是否等待外部输入”。 | agent-service task layer | Session、Memory、业务订单、client invocation |
| Client Invocation / Run Alias | client 侧一次调用、SDK 函数名或历史实现命名。它可以映射到一个 Task，但不是独立服务端状态 owner。 | agent-client + agent-service query surface | Task State、Session、业务订单 |
| Session | 上下文状态，表达对话、变量、上下文投影。 | agent-service session layer | Memory、Task、client invocation |
| Memory | 知识或经验状态，按 MemoryStore / ConversationMemory 等 SPI 暴露。 | agent-middleware | Session 临时上下文 |
| Workflow / Orchestrator | 编排能力，发起执行、处理 suspend/resume 和状态转换意图；目标运行形态与 `agent-service` 同进程组合。 | agent-execution-engine SPI + agent-service control layer | Task state owner、远程微服务边界 |
| TaskStateStore / Historical RunRepository | Task lifecycle 的受控读写入口；当前代码或历史文档若仍使用 `RunRepository` 命名，按 Task 状态 owner 的实现兼容处理。 | agent-service | 普通 DAO、任意模块可写状态、第二套 Run State |
| Agent | 绑定 model、skills、memory、planner 和 advisors 的业务可注册实体。 | agent-service agent SPI | Orchestrator |
| ModelGateway | 平台模型调用边界。 | agent-middleware model SPI | 直接 Spring AI ChatModel 调用 |
| Skill | 统一 tool/skill 执行边界。 | agent-middleware skill SPI | 业务函数裸调用 |
| Tool Gateway | 本目录中的能力聚合名，代表 skill authz、capacity、audit、tool-call governance。 | agent-middleware + agent-service integration | 独立 reactor module |
| Context Engine | 本目录中的能力聚合名，代表 Session、ContextProjector、Memory、Retriever、Vector 上下文装配。 | agent-service + agent-middleware | 独立 reactor module |
| Gateway | 微服务框架总体入口能力；当前系统只有 `agent-service` 对外暴露服务，因此 Gateway 只代理 service，不承载业务编排。 | agent-service HTTP 对外入口 + infrastructure gateway | Engine executor、Bus、独立业务模块 |
| Integrating Developer | 直接第一用户，指在业务系统中集成 `agent-client`、定义 Agent、接入业务工具并负责上线效果的应用开发者。 | business application team | 最终业务用户、平台内部模块负责人 |
| C-Side | Business Application Side，业务方或客户端侧，拥有业务目标、业务规则、业务事实、业务工具和部分本地上下文。 | ADR-0049, ADR-0051 | 平台内部 Task / RunContext |
| S-Side | Platform Runtime Side，平台运行时侧，拥有执行轨迹、调度、治理、观测、配额、审计和平台中间件集成。 | ADR-0049, ADR-0051 | 业务事实 owner |
| Platform-Centric Deployment | Mode A / S-Cloud，`agent-client` 在业务侧，`agent-service` / engine / bus / middleware 在平台侧。 | ADR-0101 | 唯一部署形态 |
| Weak Department / PaaS Tenant | 没有独立 service / engine 部署能力的业务部门，使用平台托管 runtime，只负责业务配置、客户数据源授权引用、发布验收和效果运营。 | Platform-Centric Deployment | 强业务方自托管客户 |
| Business-Centric Deployment | Mode B / S-Edge / federated，`agent-client` + service + engine 可部署在业务侧，平台保留 bus / middleware 能力。 | ADR-0101 | 新增 L0 模块 |
| Client-mediated Local Capability | 平台运行到需要敏感工具或本地上下文时，通过 S2C / Yield 指令交给 client 执行，client 返回受控结果。 | ADR-0049, ADR-0074 | 平台直接读取核心业务数据 |
| Local Capability | 可在本地 client 执行的能力集合，包括本地 context、memory、retriever、tool、approval UI。 | `ICD-CS-Capability-Placement`, S3, S4, S5 | 只有工具调用 |
| Service SSE Stream | `agent-service` 面向外部 client 的实时输出通道，用于流式返回模型或 Agent 执行结果。 | agent-service | Bus token event、持久化状态、A2A 控制指令 |
| A2A Control Command | Agent-to-Agent 协作控制指令，承载 child Task / federation / completion / failure / timeout 等控制语义。跨 service / 跨部门 / 跨部署边界必须经 `agent-bus` 传递；同一 `agent-service` 进程内的多 Agent 协作由 service 闭环。 | agent-service + agent-bus, S6 | 业务结果数据、token stream、engine 直接私连远端 service |
| Data Reference Path | 大型数据包、多模态内容或重载结果先进入对象存储、客户存储或第三方存储，控制指令只携带 URI / object reference / metadata，需求方按授权直接拉取。 | agent-bus control envelope + external storage owner | Bus 传输大 payload、Task State、模型 token stream |
| Task Tree | parent Task 与 child Task 的可追踪关系，用于重建多 Agent 协作、join、成本和失败归属；历史 `Run tree` 命名仅作兼容。 | agent-service + observability, S6 | 单个 trace span |
| Customer Auth Reference | 客户已有认证 / 授权体系提供给平台使用的授权引用，平台用它调用客户数据源，但不定义客户内部细粒度权限。 | S3, S4, S6 | 平台自定义业务权限模型 |
| LLM Cost Attribution | 平台对模型调用 token、模型路由和成本的统一归集，可按 tenant、app、agent、Task tree 聚合。 | Observability + Runtime Governance | 客户内部工具成本、业务系统成本 |
| Platform-hosted Service | 平台为弱部门 PaaS 租户托管的 Agent runtime，包括 service、engine、bus、middleware 和运维面。 | BA-001, S1, S2 | 强部门自有 service |
| Hybrid Capability Placement | 同一次业务活动中，个人本地工具走 client，本企业公共服务或平台治理能力走 middleware / platform adapter。 | ADR-0033, ADR-0049, ADR-0051, ADR-0101 | 所有能力只能在平台侧或只能在本地侧 |
| Development-time Debug Evidence | 开发态用于理解一次 Agent 执行路径的证据，包括 Task timeline、context evidence、model evidence、tool decision evidence 和 failure evidence。 | Observability + Harness + agent-service query surface | 只给平台内部看的日志 |
| Runtime Operations Insight | 运行态用于运维和运营 Agent 功能的宏观指标与调用链，包括请求量、成功率、延迟、成本、错误、capacity、trace 和 audit。 | Observability capability | 单次 debug 日志、业务系统自有状态 |
| Replay-safe Fixture | 从线上失败或关键路径中导出的脱敏测试夹具，用于在开发态复现，不包含跨租户数据或真实业务状态快照。 | Harness + Observability governance | 生产数据备份 |
| Contract / ICD | 跨模块交互的语义契约，包含状态、错误、幂等、重试、权限、观测。 | `docs/architecture/05-contracts/` draft | API 字段清单 |
| Scenario Spec | 分为 BA-* 业务活动级核心场景和 technical sub-scenario，两者都必须有 assertions。 | `docs/architecture/06-scenarios/` | 只写流程图；把机制场景当核心场景 |
| Harness | 用 mocks、stubs、fixtures、contract tests、failure injection 驱动模块开发的测试壳。 | `docs/architecture/08-harness/` | 完整生产实现 |
| Invariant | 可检查的架构不变量。 | `docs/architecture/07-invariants/` | 口号 |
| design_only | 契约或 SPI 形状存在，但未形成 runtime enforced 行为。 | contract catalog / ADR | shipped |
| draft | 本目录中的工作草案，还未迁移到权威来源。 | `docs/architecture/` | authoritative |
