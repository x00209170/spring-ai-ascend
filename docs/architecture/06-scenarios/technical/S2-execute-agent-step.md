---
level: L2
view: scenarios
status: draft
---

# S2: Execute Agent Step (Technical Sub-scenario)

## 目的

验证 Agent step 如何在 Task State single owner、Engine SPI、ModelGateway、Tool Gateway、trace 和 capability placement 约束下受控执行。本文是技术子场景，不替代 BA-* 业务活动场景。

## 适用读者

Agent Service、Execution Engine、ModelGateway、Tool Gateway、Observability、harness 生成器。

## 维护规则

- Engine adapter 不得成为 Task State owner。
- Agent / ModelGateway / tool runtime binding 仍为 draft 或 design_only 时，必须在本场景和 Verification Matrix 中标清。
- step 执行必须能输出开发态 evidence，不允许只留下日志字符串。
- `agent-service` 与 `agent-execution-engine` 的目标运行形态是同进程组合；本场景不引入 service-to-engine 远程调用边界。

## 支撑的业务活动

| BA | S2 在其中验证什么 |
|---|---|
| BA-001 | Agent 读取上下文、调用模型、决定是否调用工具或返回结果。 |
| BA-002 | 工具调用前后的 step 可以进入 suspend / resume，而不是阻塞线程。 |
| BA-003 | 父 Agent 可以产生 child-task / delegation intent，子 Agent step 可以独立执行并回写结果。 |

## 参与模块边界

| 模块 | 责任 | 不负责 |
|---|---|---|
| `agent-service` | 加载 Task、生成 step execution envelope、同进程调用 engine、提交状态转换意图。 | 不直接绕过受控状态入口写非法状态。 |
| `agent-execution-engine` | 解析 AgentDefinition，执行 step，调用 ModelGateway / Tool Gateway / delegation intent。 | 不拥有 Task State；不直接持久化业务状态；不作为独立微服务入口。 |
| `agent-middleware` | 承接模型、工具、context、policy、capacity 等横切能力。 | 不决定业务流程的最终语义。 |
| `agent-bus` | 承载异步控制消息、S2C / 跨边界 A2A 指令、data reference envelope 和 trace propagation。 | 不执行 step；不接管同一 service 内 Agent 协作；不承载大型 payload 或逐 token stream。 |

## 主流程

1. `agent-service` 读取 Task 和 current step cursor，确认状态可执行。
2. `agent-service` 构造 execution envelope，包含 tenant、trace、placement profile、cost attribution key、allowed capabilities。
3. `agent-service` 同进程调用 `agent-execution-engine`；engine 解析 AgentDefinition，选择 engine adapter。
4. Engine 根据 step 需要调用 S3 context build、ModelGateway、S4 Tool Gateway 或 S6 child-task federation。
5. Engine 返回 step result：completed、failed、suspend-required、tool-required、child-task-required 或 terminal response。
6. `agent-service` 通过受控状态入口提交合法状态转换，并记录 step evidence。
7. Observability 输出 trace、metrics、audit 和可供 replay 的结构化 evidence。

## 部署形态约束

| 形态 | S2 要保证的执行语义 |
|---|---|
| Weak Department PaaS | 平台托管 engine 和 AgentDefinition 版本；平台统一统计 LLM 调用成本。 |
| Strong Department Service | 业务方 service 可承载局部编排，但与平台协作的 step 必须有平台 trace / Task reference。 |
| Local Client Capability | 本地 client 可以执行 context、memory、retriever、tool、approval UI；Engine 通过 S2C / Yield 指令表达，不直接读取本地资源。 |
| Cross-boundary A2A | Agent step 可产生 child-task intent；同一 service 内由 `agent-service` 闭环，跨 service / 跨部门 / 跨部署控制指令经 `agent-bus` 传递。 |

## 变体和失败流

| 类型 | 场景 | 期望行为 |
|---|---|---|
| ModelGateway unavailable | 模型 provider 超时或限流。 | step 记录 retryable / non-retryable reason，状态转换由 Agent Service 决定。 |
| local capability required | step 需要本地 context、memory 或 tool。 | 返回 S2C / Yield 指令，进入 S5 suspend。 |
| child-task intent | step 需要委派给其他 Agent。 | 进入 S6，创建 child Task 或 federation command。 |
| invalid transition | Engine 返回与当前 Task 状态不兼容的结果。 | Agent Service 拒绝状态写入并记录 error evidence。 |
| duplicate attempt | 同一 attempt 被重复执行。 | 不得重复 terminal side effect 或 LLM cost attribution。 |

## 开发态调试证据

| 证据 | 用途 |
|---|---|
| step envelope | 看清 step 输入、AgentDefinition 版本、placement profile。 |
| model decision evidence | 看清 prompt reference、model route、token usage、failure reason。 |
| tool / context / child-task intent | 解释为什么进入 S3、S4、S5 或 S6。 |
| state transition proposal | 开发者能看到 Engine 建议和 Agent Service 最终接受 / 拒绝的差异。 |

## 运行态指标和调用链

| 类型 | 最小要求 |
|---|---|
| Metrics | `agent_step_total`、`agent_step_latency`、`agent_step_error_total`、`model_call_token_usage`、`model_call_cost`。 |
| Trace | `engine.dispatched`、`model.requested`、`step.completed_or_failed`、`task.state_transition`。 |
| Cost | LLM 调用成本由平台统一统计；业务工具或外部数据源成本只记录 reference，不由平台定价。 |
| Audit | 记录 AgentDefinition version、engine adapter、model route、policy decision。 |

## Assertions

```yaml
scenario: S2-ExecuteAgentStep
assertions:
  - engine_registry_resolves_before_execution: true
  - task_state_write_goes_through_agent_service_owner: true
  - service_invokes_engine_in_process: true
  - illegal_transition_rejected: true
  - failed_step_records_reason: true
  - local_capability_request_uses_s2c_or_yield: true
  - same_service_child_task_handled_by_agent_service: true
  - cross_boundary_child_task_intent_uses_bus_control_channel: true
  - llm_cost_recorded_by_platform: true
  - trace_contains:
      - engine.dispatched
      - step.completed_or_failed
      - task.state_transition
```

## 开放问题

| ID | 问题 | 影响 | 建议归宿 |
|---|---|---|---|
| OI-S2-001 | Agent.invoke / ModelGateway 的完整 runtime binding 仍需补充。 | step harness 只能验证契约方向，不能声明生产级模型执行闭环。 | ModelGateway ICD / runtime binding。 |
| OI-S2-002 | step evidence 查询面尚未正式归入 Developer Experience ICD。 | 开发态调试体验不稳定。 | Observability / Developer Experience ICD。 |
