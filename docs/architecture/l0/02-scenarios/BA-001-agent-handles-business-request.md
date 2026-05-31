---
level: L1
view: scenarios
status: draft
---

# BA-001: Agent Handles Business Request

## 目的

用一个“应用开发者集成 Agent 能力到业务系统”的完整业务活动串起核心架构模块，检验模块特性是否齐备、模块划分是否合理、ICD 是否覆盖真实交互、harness 是否能驱动端到端开发和运行态运维。

## 适用读者

集成 `agent-client` 的应用开发者、架构师、模块负责人、AI agent、harness 生成器、评审者、运维和运营负责人。

## 维护规则

- 本文描述业务活动级核心场景，不替代 `technical/` 下的机制验证场景。
- 如果某个步骤无法映射到明确模块、状态 owner、ICD 或 verification，必须记录为缺口。
- 每次修改本场景时，必须同时检查开发态体验和运行态体验；不能只描述平台内部控制流。

## Business Activity

一个业务应用团队希望把 Agent 能力集成到自己的客服、订单或运营系统中。直接第一用户不是最终提问的客户，而是集成 `agent-client` 的应用开发者：他们要把业务流程、知识库、工具调用、权限策略和观测能力编排成一个可上线、可 debug、可运维的 Agent 功能。

典型活动是：开发者在开发态定义 AgentDefinition、接入知识库和订单查询工具，反复调试提示词、工具选择、上下文装配和异常路径；上线后，最终用户在业务系统中提交“让 Agent 根据知识库和订单系统回答客户问题”的请求。平台创建 Task，装配上下文，调用模型，必要时查询业务工具，生成回复，并产生可供开发者和运维人员使用的 trace、audit、metrics 和 Task evidence。需要实时体验时，`agent-service` 通过 SSE 向外流式输出阶段性结果或最终响应；client 侧可以把这次调用叫作 run / invocation，但服务端 canonical 状态是 Task。

## User Story

作为集成 `agent-client` 的应用开发者，我希望用平台提供的 Agent runtime 增强业务系统能力，而不是自己拼接模型、工具、上下文、状态机和观测系统。我需要在开发态清楚看到一次 Agent 执行到底经过了哪些步骤、为什么选择某个工具、上下文里放入了什么、在哪一步失败；也需要在运行态看到宏观指标和调用链追踪，帮助我运营、优化和排障整个业务功能。

作为业务系统的最终用户，我提交一个客户问题，希望 Agent 在受治理的运行环境中读取上下文、调用必要工具、生成可审计回复，而不是让业务系统暴露内部工具或绕过权限边界。

## Direct User Profile

| User | Primary Jobs | Success Signal |
|---|---|---|
| 应用开发者 | 集成 SDK；定义 Agent；接入业务工具；调试 prompt / context / tool flow；处理异常路径。 | 能在本地和测试环境复现一次 Task / invocation，看到完整执行路径和失败原因。 |
| 弱部门应用负责人 | 没有独立 service / engine 部署能力，希望通过平台托管能力快速配置 Agent、接入企业公共数据和工具。 | 不需要自建 runtime，也能完成 Agent 配置、发布、观测、权限和成本管理。 |
| 业务系统负责人 | 判断 Agent 是否真的提升业务流程效率和质量。 | 能看到业务维度的成功率、人工转接率、工具调用效果和成本趋势。 |
| 运维 / SRE | 保障上线后稳定性、容量、错误可见性和调用链追踪。 | 能从 metrics / trace / audit 快速定位租户、Task、工具、模型或外部系统问题。 |
| 平台模块负责人 | 维护模块边界和契约，避免业务集成绕过 runtime governance。 | 每个用户可见能力都能追踪到模块、状态 owner、ICD、harness 和 verification。 |

## Development-time Journey

| Step | Developer Need | Platform Behavior | Architecture Implication |
|---|---|---|---|
| 1. 引入 SDK | 开发者希望用 `agent-client` 或 HTTP contract 以最小成本接入业务应用。 | 提供稳定入口 contract、tenant / actor / trace carrier、idempotency 语义。 | `agent-client` 和 `agent-bus` 不应暴露 compute 内部路由。 |
| 2. 定义 Agent | 开发者希望声明模型、memory、retriever、skill、advisor 和业务 prompt。 | 平台校验 AgentDefinition，区分平台配置和业务扩展。 | `agent-service` 承载注册面，`agent-middleware` 承载 model / skill / memory SPI。 |
| 2a. PaaS 托管配置 | 弱部门用户希望不部署 service，只在平台中配置 Agent、数据源、工具权限、预算和发布环境。 | 平台提供 hosted agent service、配置治理、默认 adapter、权限模板和发布审批。 | Platform-Centric 不只是部署形态，还必须提供托管 onboarding 和 tenant-scoped configuration。 |
| 3. 本地 dry run | 开发者希望在不真实调用高风险工具的情况下验证流程。 | harness 提供 model fake、tool stub、memory fixture、policy fixture。 | BA harness 必须支持 fixture 组合，而不是只测 happy path。 |
| 4. 调试上下文 | 开发者希望知道 prompt 里放入了哪些 Session、Memory、Retriever 片段。 | 产生 context package evidence，能标记来源、裁剪、过滤和拒绝原因。 | Context Engine capability 必须输出可解释的 projection evidence。 |
| 5. 调试工具选择 | 开发者希望知道为什么调用某个 skill，为什么被拒绝或等待人工确认。 | 产生 tool decision trace：candidate、policy、capacity、approval、idempotency、audit。 | Tool Gateway capability 必须把决策过程暴露为观测事件。 |
| 6. 调试失败路径 | 开发者希望复现 timeout、权限拒绝、外部系统失败、重复提交。 | 提供可重放 Task evidence 和 failure injection fixture。 | Verification Matrix 需要覆盖 failure semantics，不只覆盖成功响应。 |
| 7. 上线前验收 | 开发者希望确认模块边界、状态写入、观测字段都满足平台约束。 | BA-001 harness 给出端到端 assertions 和 golden trace。 | Harness-first 是开发者体验的一部分，不是测试团队的后置动作。 |

## Runtime / Operations Journey

| Step | Runtime Need | Platform Behavior | Architecture Implication |
|---|---|---|---|
| 1. 宏观健康度 | 运维希望看到请求量、成功率、错误率、延迟、模型调用次数、工具调用次数。 | 输出按 tenant / app / agent / tool / model 聚合的 metrics。 | Observability capability 需要指标维度，不只记录 trace。 |
| 2. 实时结果输出 | 业务前端或集成 client 希望边生成边展示，而不是等待完整 Task 结束。 | `agent-service` 提供 SSE stream endpoint，按 Task / invocation reference 输出受治理的流式事件。 | 实时输出属于 service 对外接口能力，不把逐 token 事件压到 Bus。 |
| 3. 调用链追踪 | 排障时希望从业务请求跳到 Task、model call、tool call、external system。 | trace 贯穿 `agent-client`、ingress、service、engine、middleware、tool。 | 所有跨模块调用必须传播 tenantId、traceId、taskId；历史 runId 只作兼容关联字段。 |
| 4. 业务效果观察 | 业务负责人希望判断 Agent 是否减少人工处理、提高解决率或降低成本。 | 输出可关联业务 outcome 的 audit / event，但不拥有业务状态。 | 平台只记录业务 outcome evidence，不成为业务系统 state owner。 |
| 5. 成本和容量 | 运维希望看到 token、模型耗时、tool capacity、retry、suspend 数量。 | metrics 和 trace 中暴露 resource usage 与 capacity pressure。 | Runtime Governance 需要和 Observability 打通。 |
| 6. 安全和审计 | 审计人员希望知道谁触发、用了什么上下文、调用了哪个工具、结果如何。 | audit record 关联 actor、tenant、policy decision、tool outcome。 | Tool Call Record、Audit Record 和 Trace 需要可关联但 owner 分离。 |
| 7. 线上回放 | 开发者希望用线上失败样本生成脱敏 fixture，在测试环境复现。 | 提供 replay-safe evidence export，隐藏 PII 和跨租户数据。 | replay 只能导出证据和 fixture，不导出业务系统真实状态。 |

## Deployment / Capability Placement Variants

本场景必须支持多种部署和能力执行位置。部署位置改变的是数据驻留、工具执行、上下文装配和观测边界，不改变 L0 模块口径。

| Variant | Typical User | Placement | Tool / Context Behavior | Platform Responsibility | Key ADR |
|---|---|---|---|---|---|
| Mode A: Platform-Centric | 轻量业务应用或 SaaS 集成方。 | `agent-client` 在业务侧；`agent-service`、engine、bus、middleware 在平台侧。 | 工具、context、memory、retriever 多数通过平台 middleware / adapter 访问。 | 平台完成集中治理、调度、观测、配额和公共中间件集成。 | ADR-0101 |
| Weak Department / PaaS Tenant | 没有独立 service 部署能力的业务部门或低代码集成团队。 | `agent-service`、engine、bus、middleware 全部由平台托管；业务侧只配置 Agent、数据源、权限和轻量入口。 | 上下文装配、工具治理、模型调用和公共中间件访问默认在平台侧完成；业务私有数据通过授权数据源、delegated adapter 或受控导入接入。 | 平台提供 hosted service、配置面、默认运维面、租户隔离、配额、审计和成本治理；业务方负责业务规则、数据授权和发布验收。 | ADR-0101, ADR-0049, ADR-0051, ADR-0063 |
| Protected Local Capability | 强业务方、金融/政企核心系统，不愿把核心数据暴露到平台侧。 | 核心业务数据和本地工具留在 C-Side；平台 service / engine 只持有 TaskCursor、规则子集、skill limit 和执行轨迹。 | 当 service 需要敏感工具或本地 context 时，通过 S2C / Yield 指令返回给 client；client 本地装配 context 或调用工具，再返回脱敏或受控结果。 | 平台不得解释业务 cursor，不得持久化业务事实；只维护 Task trajectory、audit、cost、trace 和治理证据。 | ADR-0049, ADR-0051, ADR-0074 |
| Mode B: Business-Centric / Federated | 强业务部门、自托管或边缘部署团队。 | `agent-client` + `agent-service` + engine 可在业务侧；平台保留 bus / middleware / federation hub。 | 本地 service / engine 完成低延迟执行闭环；需要平台公共能力时通过 federation / middleware proxy 调用。 | 平台提供跨网络能力 RPC、状态同步、公共中间件、观测规范和 federation contract。 | ADR-0033, ADR-0101 |
| Hybrid Enterprise Individual | 企业内个人或部门应用开发者。 | 个人本地 client 持有本地文件、桌面工具或私有凭据；企业公共服务通过平台 middleware 暴露。 | 本地工具走 client-mediated call；企业知识库、审批、模型网关、公共 DB / cache / vector 服务走平台 adapter。 | 平台需要按能力粒度区分 local tool、platform tool、delegated memory 和 public middleware，并提供统一 trace。 | ADR-0049, ADR-0051, ADR-0063, ADR-0123, ADR-0127 |

## Capability Placement Decision Rules

| Decision Question | If Yes | If No |
|---|---|---|
| 数据是否属于 C-Side 核心业务事实或业务 ontology？ | 留在 C-Side；平台只能接收 placeholder、BusinessFactEvent 或 delegated result。 | 可进入平台 context / memory，但仍需 tenant、retention、redaction 约束。 |
| 工具是否需要本地凭据、本地文件、桌面环境或私有网络？ | 通过 S2C / client-mediated local tool call。 | 可通过 agent-middleware 的 Skill / MCP / platform adapter 执行。 |
| 企业是否已经有公共中间件服务？ | 通过平台 middleware adapter 接入，形成统一治理和 observability。 | 业务方可先以本地 tool / stub 接入，后续迁移为平台 adapter。 |
| 业务方是否没有独立 service / engine 部署能力？ | 使用平台 hosted service / engine；业务方只提交配置、授权和发布请求。 | 可选择 Protected Local Capability 或 Business-Centric 形态。 |
| 是否允许平台持久化上下文或业务事实？ | 必须有 delegation contract，声明 retention、visibility、redaction、delete/export 语义。 | 平台只保存轨迹、指标、审计和 replay-safe evidence。 |
| 是否需要跨 service / 跨部门 / 跨部署 Agent 协作？ | 使用 federation / bus contract，并保留 parent-child Task tree。 | 同一 `agent-service` 进程内协作由 service 闭环；单 Agent 执行保持单 Task。 |
| 是否需要传递大型数据包、多模态内容或重载结果？ | 走 data reference path：内容先写入第三方对象存储或客户指定存储，控制指令只携带 URI / object reference / metadata。 | 小型结构化控制参数可随 envelope 传递，但不得把 Bus 当作大 payload 通道。 |
| 是否需要对外实时展示生成结果？ | 使用 `agent-service` SSE stream endpoint。 | 非实时查询走 Task query；Bus 不作为逐 token stream 通道。 |

## Participating Architecture Modules

| Module | Role in Activity |
|---|---|
| `agent-client` | 业务应用侧 SDK / future external entry；开发者集成、传递 trace carrier、消费 cursor / callback / status / SSE stream。 |
| `agent-bus` | S2C / callback / 跨边界 A2A 控制通道；传递控制指令、trace / identity carrier 和 data reference envelope，不承载大型 payload 或逐 token stream。 |
| `agent-service` | HTTP 对外入口、SSE stream endpoint、Agent registration surface、Task / Session identity、client invocation reference、idempotency、tenant、trace、Task state owner、developer-facing Task query surface。 |
| `agent-execution-engine` | 与 `agent-service` 同进程运行的 Orchestrator / engine envelope / execution dispatch；生成 step timeline 和 suspend / resume evidence。 |
| `agent-middleware` | ModelGateway、Skill、Memory、Retriever、Prompt、Advisor、RuntimeMiddleware；暴露 context / model / tool decision evidence。 |

## Module Collaboration Flow

1. 开发者在业务应用中集成 `agent-client`，配置 tenant、actor、AgentDefinition、tool binding 和 observability carrier。
2. 开发态 dry run 通过 harness 注入 model fake、tool stub、memory fixture 和 policy fixture，生成一次可解释的 Task timeline。
3. 上线后，`agent-client` 或外部 client 提交业务请求。
4. 微服务 Gateway / ingress 将外部请求代理到 `agent-service` HTTP 对外入口；`agent-bus` 只参与 S2C、callback 或跨边界 A2A 控制通道，不作为通用业务入口。
5. `agent-service` 创建 Task / Session identity，并返回 client invocation reference；Task state 由 agent-service 受控入口写入，历史 RunRepository 命名只作为实现兼容。
6. 同进程内的 `agent-execution-engine` Orchestrator 将请求转换为 engine envelope，并产生 step-level timeline。
7. `agent-service` + `agent-middleware` 组装上下文：Session、Memory、Retriever、PromptTemplate，并记录 context evidence。
8. 根据 Capability Placement Decision Rules 判定 context / tool / memory 的执行位置：platform hosted service、platform middleware、C-Side client、本地 service / engine 或 delegated adapter。
9. 平台侧能力由 `agent-middleware` 通过 ModelGateway / Advisor / RuntimeMiddleware 执行模型调用，并记录 model decision / usage evidence。
10. 本地敏感能力通过 S2C / YieldResponse 交给 `agent-client`，client 本地装配 context 或调用工具后返回受控结果。
11. 如需平台工具，`agent-middleware` Skill 边界执行权限、capacity、幂等和 audit，并记录 tool decision trace。
12. 如果模型或工具结果包含大型数据包、多模态内容或重载结果，生产方先写入第三方对象存储或客户指定存储，并在 Task evidence / A2A control envelope 中只携带 URI / object reference / metadata。
13. `agent-service` 通过 SSE stream endpoint 向外输出实时结果事件，并通过受控状态入口写入 terminal state。
14. Observability 记录 TaskEvent、trace、audit、metrics 和 golden trace evidence，并标记每一步的 execution locus。
15. 开发者和运维人员通过 Task query / trace / metrics 定位问题、评估效果并生成 replay-safe fixture。

## Feature Coverage

| Feature | Covered by |
|---|---|
| Task lifecycle | S1, S2 |
| Context package assembly | S3 |
| Model invocation governance | S2, S4 |
| Tool / Skill governance | S4 |
| Tenant / idempotency / trace | S1, S2, S4 |
| Golden trace and audit | S1..S4 |
| Developer debug timeline | BA-001, S1, S2, S3, S4 |
| Runtime metrics and operations insight | BA-001, S1..S4 |
| Service SSE realtime output | BA-001, S1, S2 |
| Data reference path for large payload | BA-001, S3, S4, S6 |
| Replay-safe fixture export | BA-001, S1..S4 |
| Deployment locus / capability placement | BA-001, S3, S4, S5 |
| PaaS hosted service onboarding | BA-001, S1, S3, S4 |

## Developer-facing Debug Evidence

| Evidence | Who Uses It | Minimum Content | Forbidden Content |
|---|---|---|---|
| Task timeline | 应用开发者、SRE | step id、module、start/end、status、error class、trace/span link。 | 直接泄露其他租户或未脱敏业务数据。 |
| Context evidence | 应用开发者 | context source、retrieval query summary、selected fragment id、filter / rejection reason、token estimate。 | 原始 PII、未经授权的 knowledge fragment。 |
| Model evidence | 应用开发者、运营 | model provider logical name、prompt version、advisor chain、token usage、latency、finish reason。 | provider secret、完整敏感 prompt dump。 |
| Tool decision evidence | 应用开发者、安全审计 | skill name、policy decision、capacity decision、idempotency key ref、approval status、tool outcome。 | 工具内部私有实现或业务系统凭证。 |
| Failure evidence | 应用开发者、SRE | error category、retry owner、retryable flag、fallback decision、user-visible error mapping。 | 用 ambiguous error 隐藏真实失败类别。 |
| Replay fixture | 应用开发者、测试 | 脱敏 request、stubbed context、stubbed tool outcome、expected trace assertions。 | 真实业务数据库快照或跨租户数据。 |

## Runtime Metrics and Tracing Requirements

| Area | Required Signals | Primary Consumer |
|---|---|---|
| Traffic | request count、accepted / rejected、duplicate intent、tenant / app / agent dimension。 | 运维、业务负责人 |
| Latency | end-to-end latency、model latency、tool latency、context build latency、queue / suspend duration。 | SRE、应用开发者 |
| Quality | success / failed / cancelled / fallback、human escalation、tool denied、context missing。 | 业务负责人、开发者 |
| Cost | token usage、model call count、tool call count、retry count、cache hit if available。 | 运营、平台负责人 |
| Reliability | timeout、provider failure、capacity full、policy denial、resume failure。 | SRE、平台模块负责人 |
| Traceability | traceId、taskId、spanId、tenantId、actor、agent version、prompt version；runId 仅作为历史兼容关联字段。 | 开发者、SRE、审计 |

## State Changes

| State | Expected Owner | Activity Expectation |
|---|---|---|
| Task Execution State | `agent-service` TaskStateStore / controlled lifecycle entry | Created once, transitioned through controlled entry only。 |
| Client Invocation Reference | `agent-client` + `agent-service` query surface | Maps client-side invocation / legacy run handle to Task identity, without becoming a second state owner。 |
| Session State | `agent-service` Session / ContextProjector | Provides context projection input。 |
| Context Package | Context Engine capability | Built from Session / Memory / Retriever。 |
| Tool Call Record | Tool Gateway capability | Records governed tool call outcome。 |
| Trace / Audit | Observability capability | Full evidence trail exists。 |
| Metrics | Observability capability | Aggregated by tenant / app / agent / model / tool without owning business state。 |
| Replay Fixture | Harness / Observability capability | Exported as sanitized test evidence, not as production state。 |

## Contracts Exercised

- ICD-Gateway-Workflow
- ICD-Workflow-AgentService
- ICD-AgentService-ContextEngine
- ICD-AgentService-ToolGateway
- ICD-Workflow-Observability
- ICD-CS-Capability-Placement

## Technical Sub-scenarios

- [S1 Create Task / Invocation](technical/S1-create-run.md)
- [S2 Execute Agent Step](technical/S2-execute-agent-step.md)
- [S3 Build Context Package](technical/S3-build-context-package.md)
- [S4 Tool Call With Governance](technical/S4-tool-call-with-governance.md)

## Assertions

```yaml
scenario: BA-001-AgentHandlesBusinessRequest
assertions:
  - all_runtime_steps_have_tenant_and_trace: true
  - task_state_written_only_by_agent_service_controlled_entry: true
  - context_package_built_before_model_invocation: true
  - tool_call_goes_through_governance_boundary: true
  - business_state_not_owned_by_runtime: true
  - final_response_has_trace_and_audit_evidence: true
  - developer_can_inspect_step_level_timeline: true
  - developer_can_explain_context_and_tool_decisions: true
  - sensitive_context_can_be_assembled_on_client_without_platform_persistence: true
  - local_tool_call_uses_s2c_or_yield_handoff: true
  - platform_public_service_uses_middleware_adapter: true
  - weak_department_can_use_hosted_service_without_local_deployment: true
  - hosted_service_configuration_is_tenant_scoped_and_audited: true
  - service_streams_realtime_output_via_sse: true
  - bus_does_not_carry_token_stream: true
  - large_payload_uses_data_reference_path: true
  - trace_marks_execution_locus_for_each_step: true
  - runtime_metrics_available_by_tenant_app_agent_tool_model: true
  - failed_task_can_generate_sanitized_replay_fixture: true
  - module_collaboration_crosses:
      - agent-service
      - agent-execution-engine
      - agent-middleware
```

## Missing Module Capability Check

| Check | Result |
|---|---|
| Can entry layer create a governed Task? | Covered by S1 draft。 |
| Can execution layer run an Agent step without owning Task State? | Covered by S2 draft。 |
| Can context be assembled without inventing a Context Engine module? | Covered by S3 draft。 |
| Can tool call be governed without business logic patching platform internals? | Covered by S4 draft。 |
| Can evidence reconstruct the full activity? | Draft; needs golden trace harness。 |
| Can an integrating developer debug context, model and tool decisions? | Draft; needs developer-facing Task timeline and evidence contract。 |
| Can operations observe macro metrics without owning business state? | Draft; metrics dimensions need contract and harness coverage。 |
| Can a failed production Task be converted into a replay-safe fixture? | Open Issue; requires sanitization and export governance。 |
| Can strong business users keep core data and tools local while still using platform orchestration? | Covered by ADR-0049 / ADR-0051 / ADR-0074; needs BA harness。 |
| Can enterprise individuals mix local tools with platform public middleware? | Draft; requires capability placement contract and trace locus field。 |
| Can weak departments use platform-hosted service without deploying their own runtime? | Covered by Platform-Centric / Mode A; needs hosted onboarding and configuration harness。 |
| Can realtime output be exposed without turning Bus into token stream? | Covered by service SSE boundary; needs stream contract and harness。 |
| Can large multimodal content cross module or A2A boundaries without putting payload on Bus? | Draft; data reference envelope needs contract。 |

## Module Boundary Fitness Check

| Boundary | Fitness Signal |
|---|---|
| `agent-service` vs `agent-execution-engine` | Healthy if Orchestrator never writes Task State directly。 |
| `agent-service` vs `agent-middleware` | Healthy if model/tool/memory SPI semantics stay in middleware and service only hosts adapters / state identity。 |
| `agent-bus` vs compute modules | Healthy if Bus carries control command / callback / data reference envelope, but does not execute engine, own Task State, carry large payload, or carry per-token stream。 |
| `agent-client` vs platform runtime | Healthy if client developers get debug / trace / status surfaces without importing server modules or bypassing ingress。 |
| Observability vs business system | Healthy if platform records evidence and metrics, but does not become the owner of business outcome state。 |
| Platform hosted service vs weak department tenant | Healthy if the platform owns runtime operations while the tenant owns business config, data authorization and release acceptance。 |

## Harness Generation Notes

生成 BA-001 harness 时，应组合：

- Gateway intent mock。
- TaskStateStore / historical RunRepository controlled transition fake。
- Context Engine stubs。
- Tool Gateway stubs。
- Observability in-memory collector。
- Golden trace assertion over the full activity。
- Developer debug timeline assertion。
- Metrics collector with tenant / app / agent / model / tool dimensions。
- Sanitized replay fixture export assertion。
- Local tool / local context fixture executed through fake S2C transport。
- Platform public middleware fixture executed through agent-middleware stub。
- Assertion that no C-Side business fact is persisted by platform without delegation contract。
- Assertion that every context/tool step carries `execution_locus` evidence。
- Assertion that `agent-service` SSE stream emits governed realtime events without writing Task State outside service。
- Assertion that Bus messages carry URI / object reference / metadata for large payloads, not inline multimodal content。
- Hosted service onboarding fixture: tenant config, AgentDefinition, public middleware binding, quota, release approval。
- Assertion that weak-department configuration is tenant-scoped, audited and reversible。

## 开放问题

| ID | 问题 | 为什么重要 | 影响范围 | 建议归宿 |
|---|---|---|---|---|
| OI-BA001-001 | ModelGateway / Skill runtime binding 仍有 `design_only` 面。 | BA-001 假设平台能受治理地调用模型和工具，但当前部分 SPI / adapter 只是契约形状，还没有完整 runtime enforcement。 | 模型调用、工具调用、capacity、policy、audit、failure semantics。 | 后续 L1/L2 能力细化中拆成 ModelGateway binding、SkillRegistry binding、Tool Gateway harness。 |
| OI-BA001-002 | ContextPackage 尚未迁移为 production contract。 | 开发者调试上下文时，需要知道哪些 Session、Memory、Retriever 片段进入了 prompt；如果 ContextPackage 只是草案，harness 无法稳定生成断言。 | Context evidence、context replay、RAG / memory adapter、prompt debug。 | 先保留 draft YAML；进入生产前补正式 contract catalog、ADR 引用和 contract test。 |
| OI-BA001-003 | 面向开发者的 Task timeline / evidence query contract 尚未定义。 | BA-001 强调开发态 debug，但还没有统一查询面说明“开发者如何拿到 step timeline、context evidence、tool decision evidence”。 | agent-client、agent-service query surface、Observability、MCP replay、debug harness。 | 新增或扩展 Observability / Developer Experience ICD，定义 `get_task_timeline`、`get_step_evidence` 这类只读查询语义；历史 `get_run_timeline` 可作为兼容别名。 |
| OI-BA001-004 | Runtime metrics 的最小维度、基数控制和隐私边界尚未定稿。 | 运维需要按 tenant / app / agent / model / tool 看指标，但维度太多会造成高基数和隐私风险；维度太少又无法定位问题。 | metrics、trace、成本统计、SRE dashboard、租户隔离。 | 在 observability contract 中定义 mandatory dimensions、禁止维度、采样策略和 PII redaction。 |
| OI-BA001-005 | Replay-safe fixture export 缺少脱敏、租户隔离和审计策略。 | 从线上失败 Task 生成 fixture 对开发者很有价值，但如果导出真实业务数据或跨租户数据，会直接破坏安全边界。 | debug replay、harness fixture、审计、C-Side business data。 | 定义 Replay-safe Fixture contract：只导出脱敏 request、stubbed context、stubbed tool outcome 和 expected trace assertions。 |
| OI-BA001-006 | Capability placement contract 仍是 draft，执行位置的 runtime binding 未细化。 | BA-001 已经支持 `platform_hosted_service`、`local_client`、`business_service`、`platform_middleware`、`delegated_adapter` 等位置，但还缺少运行时如何判定和执行的细节。 | 强业务方本地工具、弱部门 PaaS、企业个人 hybrid、平台公共中间件。 | 继续细化 `ICD-CS-Capability-Placement` 和 `cs-capability-placement.yaml`，补 placement policy、fallback 和 trace `execution_locus`。 |
| OI-BA001-007 | S2C local context assembly / local tool call 的 schema 需要与 `s2c-callback.v1.yaml` 对齐。 | 本地工具和本地上下文装配都依赖 S2C / Yield handoff；如果 schema 不统一，client 和 service 会各自实现一套 callback 语义。 | agent-client、agent-bus、S2C transport、本地工具、本地上下文。 | 在 S2C contract 中增加或引用 local capability request / result profile，避免重复定义 callback 协议。 |
| OI-BA001-008 | Weak Department / PaaS Tenant 缺少配置治理和发布治理契约。 | 弱部门不部署 service，平台托管 runtime；因此谁能改 AgentDefinition、谁能授权数据源、如何发布、如何回滚、如何计量成本，都必须是平台能力。 | hosted service、tenant config、AgentDefinition、数据源授权、release approval、cost allocation。 | 新增 PaaS tenant onboarding / configuration governance 场景或 ICD；BA-001 harness 先覆盖 tenant-scoped config、audit 和 rollback。 |
