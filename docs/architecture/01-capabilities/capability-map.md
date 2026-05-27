---
level: L0
view: logical
status: draft
---

# Capability Map

## 目的

先定义系统能力，再映射到真实模块、场景、ADR 和验证方式，避免按模块名重复建设能力。

## 适用读者

产品架构师、模块负责人、AI agent、测试和 harness 负责人。

## 维护规则

- Capability ID 稳定，后续新增只追加。
- Owning Module 必须是真实模块或明确的 capability 聚合。
- 未 runtime enforced 的能力标记为 `design_only`、`draft` 或 `deferred`。
- Related Scenarios 必须优先引用 BA-* 业务活动场景；S1-S6 只能作为 technical sub-scenario 补充。

| Capability ID | Capability Name | Description | Owning Module | Participating Modules | Current Stage | Target Stage | Platform / Business Ownership | Related Scenarios | Related ADRs | Verification Method |
|---|---|---|---|---|---|---|---|---|---|---|
| CAP-01 | Agent Execution | 接收 AgentDefinition，绑定 model、skills、memory、planner，并执行 agent step。 | agent-service | agent-execution-engine, agent-middleware | 部分 SPI design_only，reference path 存在 | runtime enforced agent invocation | 平台提供 Agent SPI，业务提供 AgentDefinition 和业务 skill | BA-001, BA-003; technical S2, S4 | ADR-0128, ADR-0121, ADR-0127 | Scenario Test, Contract Test |
| CAP-02 | Workflow Orchestration | 编排 Task 执行，处理 dispatch、suspend、resume、cancel、child Task join 和 terminal transition；目标运行形态与 `agent-service` 同进程组合；历史 Run 命名只作为 client invocation / implementation compatibility。 | agent-execution-engine | agent-service, agent-bus | Orchestrator SPI shipped，部分 async flow deferred | durable async orchestration | 平台 | BA-001, BA-002, BA-003; technical S1, S2, S5, S6 | ADR-0072, ADR-0100, ADR-0142 | State Machine Test, Failure Injection |
| CAP-03 | Context Management | 从 Session、Memory、Retriever、Vector、ConversationMemory 组装可执行上下文。 | agent-service + agent-middleware | agent-service, agent-middleware | SPI shape 与部分 reference impl | runtime projection and memory governance | 平台定义边界，业务提供知识内容 | BA-001; technical S3 | ADR-0123, ADR-0124, ADR-0133 | Contract Test, Projection Assertion |
| CAP-04 | Tool / Skill Governance | Skill 解析、授权、capacity、audit、idempotency 和 tool-call loop 语义。 | agent-middleware | agent-service, agent-execution-engine | Skill SPI design_only，capacity schema shipped | governed tool execution | 平台治理，业务实现 skill | BA-001, BA-002; technical S4, S5 | ADR-0122, ADR-0127, ADR-0134 | Contract Test, Security Review, Failure Injection |
| CAP-05 | Enterprise Middleware Integration | 对模型、memory、vector、prompt、advisor 等企业中间件提供统一 SPI 和 adapter 边界。 | agent-middleware | agent-service | 多个 SPI design_only | adapter runtime binding | 平台定义 SPI，业务配置 provider | BA-001; technical S2, S3, S4 | ADR-0121..ADR-0133 | Compatibility Test, Contract Test |
| CAP-06 | Observability | trace、span、TaskEvent、audit、golden trace 和 replay 边界；历史 RunEvent 只作兼容命名。 | agent-service | agent-middleware, agent-bus | TraceContext / traceparent shipped，TaskEvent design_only | full trace/event replay | 平台 | BA-001, BA-002, BA-003; technical S1..S6 | ADR-0061, ADR-0145 | Golden Trace Test, Static Check |
| CAP-07 | Runtime Governance | posture、tenant、idempotency、capacity、sandbox、hook failure safety、contract truth。 | agent-service | agent-middleware, agent-bus | 多个 W0/W1 guard 存在 | production posture enforcement | 平台 | BA-001, BA-002; technical S1, S4, S5 | ADR-0056, ADR-0057, ADR-0070, ADR-0110 | Static Check, Integration Test |
| CAP-08 | Security / Policy / Audit | 租户隔离、权限判断、审计记录、PII boundary、策略拒绝。 | agent-service | agent-middleware | 部分 shipped，部分 deferred | end-to-end authorization and tamper-evidence | 平台治理，业务策略输入 | BA-001, BA-002; technical S1, S4, S5 | ADR-0108, ADR-0110, ADR-0111 | Security Review, Scenario Test |
| CAP-09 | Agent Swarm / Multi-Agent Coordination | 父子 Task、同一 service 内多 Agent 协作、跨边界 A2A、S2C callback、federation、控制指令和数据引用路径。Bus 传控制和 URI / object reference，不传大 payload 或逐 token stream。 | agent-service + agent-bus | agent-client, agent-execution-engine | 多数 design_only | governed multi-agent execution | 平台 | BA-003; technical S6 | ADR-0053, ADR-0074, ADR-0101, ADR-0107 | Scenario Test, Contract Test |
| CAP-10 | Configuration / DSL / Extensibility | 通过 SPI、properties、contract YAML 和 DSL 组合平台能力。 | agent-service + agent-middleware | all modules | metadata / SPI / contract catalog active | stable developer-facing SDK | 平台定义扩展面，业务填充配置 | BA-001, BA-002, BA-003; technical S1..S6 | ADR-0066, ADR-0067, ADR-0120 | Compatibility Test, Static Check |
| CAP-11 | Developer Experience / Operational Insight | 面向集成 `agent-client` 的应用开发者提供开发态 debug timeline、context/tool/model decision evidence、运行态 metrics、trace、audit、SSE streaming 和 replay-safe fixture。 | agent-client + agent-service observability surface | agent-bus, agent-execution-engine, agent-middleware | 多数为 draft，需要 observability contract 细化 | developer-friendly integration and operations experience | 平台提供 debug / operations surface，业务消费并关联业务 outcome；实时输出由 service SSE 承载 | BA-001, BA-002, BA-003; technical S1..S6 | ADR-0061, ADR-0145, ADR-0120 | Golden Trace Test, Scenario Test, Harness Review |
| CAP-12 | Deployment Locus / Capability Placement | 根据数据敏感度、业务方能力、延迟和企业治理要求，决定工具调用、上下文装配和中间件访问发生在平台 hosted service、C-Side client、本地 service / engine、S-Side middleware 或 delegated adapter。 | agent-client + agent-bus | agent-service, agent-execution-engine, agent-middleware | ADR 已定义，本文档需补齐场景和 harness | policy-driven hosted / local / platform / hybrid capability placement | 弱部门使用平台托管 runtime；强业务方拥有敏感数据和本地工具；平台拥有治理、公共中间件、观测和 federation contract | BA-001, BA-002, BA-003; technical S3, S4, S5, S6 | ADR-0033, ADR-0049, ADR-0051, ADR-0074, ADR-0101 | Scenario Test, Contract Test, Security Review |

## Traceability Rules

每个 capability 至少要有：

```text
Owner
Participating modules
Related scenarios
Related ADRs
Verification method
```

缺失项必须在 Verification Matrix 中标记为 `Traceability Missing`。
