---
level: L2
view: scenarios
status: draft
---

# S1: Create Task / Invocation (Technical Sub-scenario)

## 目的

验证入口请求如何在租户、幂等、trace、Task State single owner 和部署形态约束下创建一个可治理的 Task，并向 client 返回 invocation reference。本文是技术子场景，只验证机制；业务活动入口见 `../BA-001-agent-handles-business-request.md`、`../BA-002-human-approval-tool-call.md` 和 `../BA-003-multi-agent-delegation.md`。

## 适用读者

微服务 Gateway、Agent Service、Workflow / Task lifecycle 负责人、agent-bus 负责人、harness 生成器、架构评审者。

## 维护规则

- S1 只描述 Task 创建和 client invocation reference 机制，不描述完整业务目标。
- 入口语义、幂等语义、Task 初始状态变化时，必须同步 ICD、State Matrix、Verification Matrix。
- 不得把 Gateway、Bus、Client 或 Engine 写成 Task State owner；历史 `Run` 命名只能作为兼容字段。

## 支撑的业务活动

| BA | S1 在其中验证什么 |
|---|---|
| BA-001 | 应用开发者发起业务请求后，平台创建可追踪、可查询、可审计的 Task。 |
| BA-002 | 高风险工具调用前的业务请求仍先进入标准 Task lifecycle，后续才能 suspend / resume。 |
| BA-003 | 父 Task 或 child Task 的创建都必须保持同一套租户、幂等、trace 和 Task tree 约束。 |

## 参与模块边界

| 模块 | 责任 | 不负责 |
|---|---|---|
| `agent-client` | 提交业务意图、携带 tenant / user / auth reference / idempotency key，接收 cursor 或 invocation reference。 | 不直接写 Task State；不决定平台侧调度状态。 |
| 微服务 Gateway | 对外暴露统一入口并代理到 `agent-service`。 | 不拥有 Task aggregate；不代理 engine / middleware / bus 成为业务入口。 |
| `agent-bus` | 承载后续 S2C / callback / 跨边界 A2A 控制通道和 trace / identity carrier。 | 不作为普通业务请求入口；不执行 engine；不拥有 Task aggregate；不承载逐 token stream。 |
| `agent-service` | 创建 Task，执行幂等 claim，维护 Task 初始状态、查询面和 SSE stream reference。 | 不直接执行模型或工具副作用。 |
| `agent-execution-engine` | 在 Task 被创建后由 `agent-service` 同进程调用并消费执行意图。 | 不创建入口 Task；不绕过 Agent Service 写状态；不作为远程入口服务。 |

## 部署形态约束

| 形态 | S1 要保证的入口语义 |
|---|---|
| Platform-Centric / Weak Department PaaS | 平台托管 Agent runtime，Task 由平台 Agent Service 创建；平台统一统计 LLM 调用成本。 |
| Business-Centric / Strong Department | 业务方 service 可作为入口调用平台，但平台仍要为协作、trace、A2A 和 LLM cost 创建平台 Task reference。 |
| Protected Local Capability | 本地 client 可承载 context / tool / memory / retriever / approval UI，但入口 Task 仍要有平台可关联的 cursor 和 trace。 |
| Cross-boundary A2A | child Task 创建由 `agent-service` 维护 Task tree；跨 service / 跨部门 / 跨部署控制指令必须通过平台 Bus 传递。 |

## 主流程

1. `agent-client` 或业务方 service 提交业务意图，携带 tenant、caller identity、idempotency key、trace parent 和 deployment profile。
2. 微服务 Gateway 接收入站业务请求并代理到 `agent-service`；`agent-bus` 不作为普通业务请求入口。
3. `agent-service` 校验 tenant、request shape、幂等 key 和调用方身份。
4. `agent-service` 执行 idempotency claim：相同 tenant + equivalent request + idempotency key 不得创建重复 Task。
5. `agent-service` 创建 Task，初始状态进入 `Pending` 或等价的可调度状态。
6. `agent-service` 记录 placement profile、caller reference、traceId、cost attribution key 和可查询 cursor。
7. 后续调度由 `agent-service` 同进程调用 Workflow / Engine 消费 Task，而不是入口层直接执行。

## 变体和失败流

| 类型 | 场景 | 期望行为 |
|---|---|---|
| 重复请求 | client 超时后重试同一 idempotency key。 | 返回同一语义 Task reference，不创建重复 Task。 |
| 弱部门 PaaS | 租户没有自有 service。 | 平台创建 hosted Task，并记录 tenant-scoped config / release version。 |
| 强部门 service | 业务方 service 发起 Task。 | 平台记录业务方 service identity，但不接管业务数据库权限定义。 |
| child Task | BA-003 中父 Agent 发起委派。 | 创建 child Task，并绑定 parentTaskId / delegation reason；历史 parentRunId 可作为兼容字段。 |
| tenant mismatch | token、tenant、request scope 不一致。 | 拒绝并不得泄露其他租户 Task 信息。 |
| repository unavailable | Task store 不可用。 | 返回可观测错误，不得产生半创建状态。 |

## 开发态调试证据

| 证据 | 用途 |
|---|---|
| intake envelope | 开发者确认 client / service 提交的业务意图、tenant、idempotency key 是否正确。 |
| idempotency decision | 解释本次是新建 Task、命中已有 Task，还是发生冲突。 |
| placement profile | 解释本 Task 后续哪些 capability 可能在平台、业务 service 或本地 client 执行。 |
| trace origin | 确认入口 trace 如何贯穿 S2/S3/S4/S5/S6。 |
| cost attribution key | 确认 LLM 调用成本会归属到哪个 tenant / app / agent / Task。 |

## 运行态指标和调用链

| 类型 | 最小要求 |
|---|---|
| Metrics | `task_create_total`、`task_create_error_total`、`idempotency_hit_total`、`task_create_latency`。 |
| Trace | 必须包含 `intake.requested`、`idempotency.claimed_or_reused`、`task.created`。 |
| Audit | 记录 caller、tenant、app、agent definition version、deployment profile、release version。 |
| Cost | 平台统一维护 LLM cost attribution key；非 LLM 的业务系统成本不由平台定价。 |

## 涉及契约

| 契约 | 作用 |
|---|---|
| ICD-Gateway-Workflow | 入口意图与 Workflow / Agent Service 的边界。 |
| ICD-Workflow-AgentService | Task State owner 与状态转换约束。 |
| ICD-CS-Capability-Placement | 记录本 Task 的 capability placement profile。 |
| ICD-Workflow-Observability | 入口 trace、audit、metrics 事件语义。 |

## Assertions

```yaml
scenario: S1-CreateTaskInvocation
assertions:
  - task_created_once_for_duplicate_request: true
  - initial_task_state:
      allowed:
        - Pending
  - gateway_or_bus_does_not_write_task_state: true
  - gateway_proxies_business_request_only_to_agent_service: true
  - bus_is_not_general_business_ingress: true
  - tenant_mismatch_not_visible: true
  - placement_profile_recorded: true
  - llm_cost_attribution_key_recorded: true
  - trace_contains:
      - intake.requested
      - idempotency.claimed_or_reused
      - task.created
```

## 开放问题

| ID | 问题 | 影响 | 建议归宿 |
|---|---|---|---|
| OI-S1-001 | cursor / invocation reference 的 production binding 仍需与正式 ICD 对齐。 | client 查询、超时重试、debug replay。 | ICD-Gateway-Workflow。 |
| OI-S1-002 | 弱部门 PaaS 的 tenant config / release version 最小字段需要治理契约。 | hosted runtime 发布、回滚、审计。 | PaaS tenant configuration governance。 |
