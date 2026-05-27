---
level: L1
view: logical
status: draft
---

# State Ownership Matrix

## 目的

明确核心状态的唯一 owner、允许 writer、允许 reader、生命周期、持久化、回放、审计、幂等和 forbidden writers。

## 适用读者

模块负责人、harness 生成器、测试负责人、评审者。

## 维护规则

- 每个 State Name 只能有一个 Owner。
- 若需要多个 writer，必须新增 Conflict 记录和 ADR。
- 与模块责任卡不一致时，以本矩阵为待修复信号。
- `Task` 是服务端 canonical 执行状态；历史 `Run` / `runId` / `RunRepository` 命名只作为实现兼容或 client invocation reference，不得新增独立 Run State owner。

| State ID | State Name | Owner | Allowed Writers | Allowed Readers | Lifecycle | Persistence Requirement | Replay Requirement | Audit Requirement | Idempotency Requirement | Related Module | Related Scenario | Forbidden Writers |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| STATE-001 | Task Execution State | agent-service TaskStateStore / controlled lifecycle entry | agent-service controlled transition path | Gateway, Orchestrator, Observability, agent-client query surface | Pending -> Running -> Suspended -> Running -> Succeeded / Failed / Cancelled / Expired | durable in non-dev posture | replay by taskId, tenantId and compatible runId when present | transition event and audit record | create / cancel / resume must be idempotent where contract requires | agent-service | BA-001, BA-002, BA-003; technical S1, S2, S5 | Gateway, Tool Gateway, Engine adapter, Bus, client direct write |
| STATE-002 | Client Invocation Reference | agent-client + agent-service query surface | agent-service creates authoritative reference; agent-client stores local handle / cursor | client, Gateway, Observability | created -> queried / resumed -> completed / expired | client-local plus service query target where supported | maps to Task identity; no independent state replay | query / callback audit when crossing boundary | duplicate client invocation maps to the same Task when idempotency contract applies | agent-client, agent-service | BA-001, BA-002; technical S1, S5 | any writer treating this as a second server-side Run State |
| STATE-003 | Agent Local State | business Agent implementation | business-owned Agent / SPI implementation | Orchestrator, Observability | implementation-specific | business-defined | business-defined | business-defined, platform trace required | business-defined | business module via SPI | BA-001, BA-003; technical S2 | platform runtime unless explicitly delegated |
| STATE-004 | Workflow Checkpoint | Checkpointer SPI implementation | Orchestrator / Checkpointer | Orchestrator, Recovery harness | before suspend -> resume / clear | durable target for long horizon | required for crash recovery | checkpoint saved / loaded event | checkpoint key must prevent duplicate irreversible work | agent-execution-engine, agent-service | BA-002, BA-003; technical S5 | Gateway, Tool Gateway |
| STATE-005 | Context Package | Context Engine capability | ContextProjector / Memory retriever pipeline | Engine, Tool Gateway, Observability | build -> consume -> expire | draft, depends on context source | reproducible from Session / Memory inputs where possible | projection event | package id or equivalent draft | agent-service, agent-middleware | BA-001; technical S3 | Gateway, TaskStateStore / historical RunRepository |
| STATE-006 | Context Version | Context Engine capability | ContextProjector / MemoryStore writer | Engine, Agent, Observability | versioned per projection or memory write | target durable for memory-backed context | compare versions for repeatable harness | context-version event | write idempotency for irreversible memory writes | agent-middleware | BA-001; technical S3 | Engine adapter direct write |
| STATE-007 | Tool Call Record | Tool Gateway capability | Skill runtime / RuntimeMiddleware | Orchestrator, Observability, Audit | requested -> authorized -> executed -> recorded | durable target for irreversible side effects | replay record, not side effect | mandatory for irreversible tool calls | tool-call idempotency key required | agent-middleware, agent-service | BA-001, BA-002; technical S4 | Agent implementation bypassing Tool Gateway |
| STATE-008 | Tool Execution State | Tool Gateway capability | Skill executor wrapper | Orchestrator, Observability | pending -> running -> succeeded / failed / suspended | depends on skill class | replay from Tool Call Record and checkpoint | tool outcome audit | duplicate attempt detection | agent-middleware | BA-001, BA-002; technical S4, S5 | Gateway, Engine adapter direct mutation |
| STATE-009 | Approval State | Runtime Governance capability | approval callback handler / S2C transport | Orchestrator, Audit | requested -> approved / denied / expired | durable target | resume must re-validate tenant | approval actor and timestamp | callback correlation id | agent-bus, agent-service | BA-002; technical S5 | Tool implementation direct write |
| STATE-010 | Audit Record | Observability / Governance capability | platform audit writer | reviewers, operators | append-only | durable target | replay by tenant / task / trace | mandatory | append idempotency for duplicate event producer | agent-service | BA-001..BA-003; technical S1..S6 | business code overwriting records |
| STATE-011 | Trace / Span / Event | Observability capability | TraceContext / RuntimeMiddleware / TaskEvent emitter | all modules | started -> emitted -> exported / sampled | target per posture | golden trace replay | required for core scenarios | event idempotency draft | agent-service, agent-middleware | BA-001..BA-003; technical S1..S6 | provider adapter direct telemetry sink bypass |
| STATE-012 | Business State | business system | business system owner | platform only through explicit SPI / tool result | business-defined | business-defined | business-defined | business audit plus platform trace | business-defined | external business module | BA-001, BA-002; technical S4 | Agent runtime direct ownership |
| STATE-013 | Tenant / Permission / Policy State | Runtime Governance capability | auth / policy owner | Gateway, Tool Gateway, Orchestrator | configured -> evaluated -> audited | durable config target | policy version replay | policy decision audit | policy decision key draft | agent-service, agent-middleware | BA-001, BA-002, BA-003; technical S1, S4, S5 | skill implementation bypass |
| STATE-014 | Configuration State | Platform config owner | configuration binding / operator | all modules via typed properties | boot -> active -> changed by governance | durable external config target | config version replay | change record | change idempotency by CR | all modules | BA-001..BA-003; technical S1..S6 | runtime code mutating config silently |
| STATE-015 | Task Tree Relationship | agent-service Task tree owner | agent-service when creating same-service child Task or accepting cross-boundary federation result | Observability, agent-bus, operations view, developer debug view | parent created -> child created -> waiting / joined -> aggregated / failed / timed out | durable target for multi-Agent scenarios | replay parent / child relation, join policy and failure ownership | delegation / join / timeout event | duplicate child creation must be guarded by delegation id / attempt id | agent-service | BA-003; technical S6 | agent-bus, engine adapter, remote service direct write |

## 冲突检查

任意 PR 或 AI-generated change 若触碰以下行为，必须打开 ADR / CR：

- 新增 Task Execution State writer，或把历史 `Run` 命名恢复成独立服务端状态 owner。
- 把 Client Invocation / Run Alias 写成可独立推进生命周期的服务端状态。
- 把同一 `agent-service` 进程内的多 Agent 协作错误外包给 `agent-bus`。
- 把跨 service / 跨部门 / 跨部署 A2A 写成业务 service 或 engine 私有直连。
- 把 Context Package 写成 agent-service 独有状态而忽略 Memory / Retriever。
- 让 Tool Gateway 写业务状态。
- 让 Observability 修改状态机结果。
- 让业务 Agent 修改平台运行状态。
