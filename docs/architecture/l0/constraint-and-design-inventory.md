---
level: L0
view: scenarios
status: draft
---

# 约束与设计清单

## 目的

把现有权威文档中的目标、能力、模块、状态、契约、场景、实现建议、冲突和开放问题分类，作为本目录后续文档的输入清单。

## 适用读者

架构师、模块负责人、AI agent、评审者。

## 维护规则

- 每条清单项必须指向一个来源。
- 无法确认的内容标记为 `Open Issue` 或 `Unclear`。
- 冲突不得在下游文档中静默消失，必须保留 Conflict ID。

## 分类清单

| ID | 类型 | 内容 | 来源 | 下游文档 |
|---|---|---|---|---|
| INV-GOAL-001 | Goal | 平台要支撑自托管 agent runtime，服务外部 Spring 开发者和受监管行业的自托管操作者。 | `ARCHITECTURE.md` §1 | Overview, Capability Map |
| INV-GOAL-002 | Goal | 架构文档需要能转化为模块边界、契约、场景、harness 和验证。 | `docs/architecture/task.md` | 本目录全量 |
| INV-PRINCIPLE-001 | Principle | 平台与业务解耦，业务扩展必须走 SPI 和配置，不 patch 平台实现。 | `CLAUDE.md` Rule R-A | Principles, Invariants |
| INV-PRINCIPLE-002 | Principle | 独立模块演进，每个模块必须有 metadata、DFX、允许和禁止依赖。 | `CLAUDE.md` Rule R-C.1 | Module Cards |
| INV-PRINCIPLE-003 | Principle | 跨服务内部通信分为 control、data、rhythm 三条通道。 | `CLAUDE.md` Rule R-E | ICD, Scenarios |
| INV-PRINCIPLE-004 | Principle | 长任务必须返回 cursor 或 suspend/resume，不持有客户端连接。 | `CLAUDE.md` Rule R-F | Scenarios, Harness |
| INV-PRINCIPLE-005 | Principle | 外部 I/O、等待和恢复必须遵守非阻塞、Chronos hydration 和资源释放边界。 | `CLAUDE.md` Rule R-G, R-H | Invariants |
| INV-CAP-001 | Capability | Agent Execution | `docs/contracts/contract-catalog.md`, ADR-0128 | Capability Map |
| INV-CAP-002 | Capability | Workflow Orchestration | `agent-execution-engine/module-metadata.yaml`, `docs/architecture/l0/l1/agent-service/` | Capability Map |
| INV-CAP-003 | Capability | Context Management | ADR-0123, ADR-0133, `agent-service/module-metadata.yaml` | Capability Map |
| INV-CAP-004 | Capability | Tool / Skill Governance | ADR-0122, ADR-0127 | Capability Map |
| INV-CAP-005 | Capability | Observability | ADR-0061, `docs/telemetry/policy.md` | Capability Map |
| INV-CAP-006 | Capability | Developer Experience / Operational Insight | ADR-0017, ADR-0063, ADR-0061 | Capability Map |
| INV-CAP-007 | Capability | Deployment Locus / Capability Placement，包含 weak department PaaS、protected local capability、business-centric 和 hybrid enterprise individual。 | ADR-0033, ADR-0049, ADR-0051, ADR-0074, ADR-0101 | Capability Map, ICD-CS-Capability-Placement |
| INV-MOD-001 | Module Responsibility | `agent-service` 同时承载 HTTP 对外入口、Task lifecycle、client invocation reference adapters 和若干 agent/customer-facing SPI；历史 Run 命名只作兼容。 | `agent-service/module-metadata.yaml` | Module Cards |
| INV-MOD-002 | Module Responsibility | `agent-execution-engine` 拥有 engine SPI、orchestration SPI 和 planner SPI。 | `agent-execution-engine/module-metadata.yaml` | Module Cards |
| INV-MOD-003 | Module Responsibility | `agent-middleware` 拥有 RuntimeMiddleware、ModelGateway、Skill、Memory、Vector、Prompt、Advisor 等中间件 SPI。 | `agent-middleware/module-metadata.yaml` | Module Cards |
| INV-MOD-004 | Module Responsibility | `agent-bus` 拥有 S2C、callback、跨边界 A2A / federation 控制指令和 data reference envelope 等跨平面控制面 SPI；Gateway 入口能力已从 Bus 中拆出。 | `agent-bus/module-metadata.yaml` | Module Cards |
| INV-STATE-001 | State Ownership | Task Execution State 的单一 owner 是 agent-service 的 TaskStateStore / controlled lifecycle entry；RunRepository 只作为实现兼容名。 | ADR-0142, `docs/architecture/l0/l1/agent-service/logical.md` | State Matrix |
| INV-STATE-002 | State Ownership | Task 是服务端 canonical 控制状态，Session 是上下文状态，Run 是 client invocation / 历史实现命名。 | ADR-0100, ADR-0136 | State Matrix, Glossary |
| INV-CONTRACT-001 | ICD Candidate | Gateway 到 Workflow 的 Task intake、cancel、resume 和 client invocation reference 语义需要独立 ICD。 | `docs/contracts/openapi-v1.yaml`, task.md | ICD |
| INV-CONTRACT-002 | ICD Candidate | Workflow 到 Agent Service / Engine 的执行语义需要独立 ICD。 | `engine-envelope.v1.yaml`, ADR-0072 | ICD |
| INV-CONTRACT-003 | ICD Candidate | Agent Service 到 Tool Gateway 的工具治理语义需要独立 ICD。 | ADR-0122, ADR-0127 | ICD |
| INV-CONTRACT-004 | ICD Candidate | C-Side / S-Side 能力放置、本地工具 handoff、本地上下文装配和平台 middleware 调用需要独立 ICD。 | ADR-0049, ADR-0051, ADR-0074, ADR-0101 | ICD-CS-Capability-Placement |
| INV-SCENARIO-BA-001 | Business Activity Scenario | Agent Handles Business Request | user review feedback, task.md goal | Scenario BA-001 |
| INV-SCENARIO-BA-002 | Business Activity Scenario | Human Approval Tool Call | user review feedback, ADR-0074, ADR-0122 | Scenario BA-002 |
| INV-SCENARIO-BA-003 | Business Activity Scenario | Multi-Agent Delegation | user review feedback, ADR-0053, ADR-0101 | Scenario BA-003 |
| INV-SCENARIO-001 | Technical Scenario Candidate | Create Task / Invocation | task.md, `docs/architecture/l0/l1/agent-service/scenarios.md` | Technical Scenario S1 |
| INV-SCENARIO-002 | Technical Scenario Candidate | Execute Agent Step | task.md, `docs/architecture/l0/l1/agent-service/scenarios.md` | Technical Scenario S2 |
| INV-SCENARIO-003 | Technical Scenario Candidate | Build Context Package | task.md, ADR-0123, ADR-0133 | Technical Scenario S3 |
| INV-SCENARIO-004 | Technical Scenario Candidate | Tool Call With Governance | task.md, ADR-0122, ADR-0127 | Technical Scenario S4 |
| INV-SCENARIO-005 | Technical Scenario Candidate | Suspend / Resume | task.md, ADR-0074, ADR-0137 | Technical Scenario S5 |

## Implementation Note 下沉区

| ID | 内容 | 不放入 L0 的原因 | 应去向 |
|---|---|---|---|
| IMPL-NOTE-001 | 具体数据库表、RLS migration、SQL CAS 语句 | 属于 L2/L3 实现细节 | `docs/architecture/l0/l1/agent-service/` 或实现设计 |
| IMPL-NOTE-002 | 具体 Redis key、MQ topic、TTL、retry 次数 | task.md 明确禁止进入 L0 | 对应 ICD 或实现设计 |
| IMPL-NOTE-003 | 具体 Spring AI adapter 类名 | 多数仍为 design_only shell，不能写成已交付能力 | ADR / contract catalog / L2 adapter spec |

## 冲突记录

| Conflict ID | Description | Involved Documents | Involved Modules | Impact | Recommended Resolution | Required ADR | Status |
|---|---|---|---|---|---|---|---|
| C-001 | task.md 使用 Gateway / Workflow / Context Engine / Tool Gateway 等通用模块名，当前仓库的真实模块是 agent-service、agent-execution-engine、agent-middleware、agent-bus 等。 | task.md, module-metadata.yaml | 多模块 | 如果照抄通用名，会产生不存在的模块边界。 | 在本目录中把通用名作为“能力视角”，并映射到真实模块；不得新增不存在的 reactor module。 | ADR-0100, ADR-0142 | Open |
| C-002 | “Workflow owns Run lifecycle”的历史通用表达需要对齐为 Task canonical。 | task.md, ADR-0142, docs/architecture/l0/l1/agent-service/logical.md | agent-service, agent-execution-engine | 可能误导 agent-execution-engine 或 Orchestrator 直接写 Task state，或恢复第二套 Run State owner。 | 改写为：Workflow/Orchestrator 发起状态意图，agent-service TaskStateStore / controlled lifecycle entry 是唯一写入口；RunRepository 只作兼容名。 | ADR-0142 | Resolved in this doc set |
| C-003 | Context Engine 不是独立模块，能力分布在 Session / ContextProjector / MemoryStore / Retriever / VectorStore 等 SPI。 | task.md, ADR-0123, ADR-0124, ADR-0133 | agent-service, agent-middleware | 可能生成错误的独立模块 harness。 | 作为 capability 聚合处理，harness 用 mocks/stubs 聚合多个 SPI。 | ADR-0123, ADR-0133 | Open |
| C-004 | Tool Gateway 尚未作为独立模块存在，实际为 Skill SPI、ModelGateway、RuntimeMiddleware 和 service integration adapter 的组合。 | task.md, ADR-0122, ADR-0127 | agent-middleware, agent-service | 可能生成不存在的代码路径。 | 作为 capability 聚合处理，责任卡明确 owning module 和 participating modules。 | ADR-0127 | Open |

## Open Issues

| ID | 问题 | 当前处理 |
|---|---|---|
| OI-001 | Context Package 的稳定机器可读 contract 尚未在本目录中绑定到生产 contract catalog。 | 本目录先用 draft YAML 表达 harness 需要，后续迁移到 `docs/contracts/` 前必须补 ADR 和 catalog。 |
| OI-002 | Tool Call With Governance 的完整 runtime binding 依赖 W2/W3 的 ModelGateway、SkillRegistry、advisor 和 hook 实现。 | 场景与 harness 标记为 design_only / draft，不声明已交付行为。 |
| OI-003 | Agent Swarm / A2A 协作需要更多 S3 以后的 contract test 形状。 | 本次放入 BA-003 作为业务活动级 draft，后续补充独立 technical child-run / federation scenario。 |
