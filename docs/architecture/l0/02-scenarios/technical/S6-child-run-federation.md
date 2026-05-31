---
level: L2
view: scenarios
status: draft
---

# S6: Child Task / Federation (Technical Sub-scenario)

## 目的

验证多 Agent 委派、child Task、同一 service 内协作、跨部门 A2A federation、join、timeout、partial failure 和 Task tree observability。本文是 BA-003 的核心技术子场景，重点验证同一 `agent-service` 进程内协作由 service 闭环，跨 service / 跨部门 / 跨部署 Agent 协作时控制指令统一通过平台 `agent-bus` 传递。

## 适用读者

Agent Service、agent-bus、Execution Engine、Federation / A2A 负责人、Observability、发布治理负责人、harness 生成器。

## 维护规则

- S6 只验证 child-task / federation 机制，不替代 BA-003 的业务活动。
- 同一 `agent-service` 进程内的多 Agent 协作由 service 管理 child Task、等待和 join；Bus 不接管本地调度。
- 跨 service / 跨部门 / 跨部署 A2A 控制指令必须走平台 Bus；强部门 service 和弱部门平台托管 service 都通过同一控制通道互联。
- Bus 主要承载控制指令和 data reference envelope；大型多模态内容、文件、长文本结果等必须走对象存储或客户指定存储，Bus 只传 URI / object reference / metadata。
- A2A 调用方要求被调方流式返回时，不默认通过 Bus 逐 token 事件实现；具体技术方案需要单独确认。
- child Task 必须在 Task tree 中可见，且成本、trace、failure ownership 可聚合到 parent Task。

## 支撑的业务活动

| BA | S6 在其中验证什么 |
|---|---|
| BA-003 | 父 Agent 委派子 Agent、跨部门 Agent 协作、等待结果、合并输出。 |
| BA-001 | 业务请求内部可能触发多 Agent 协作，但仍保持一个可观测业务 Task tree。 |
| BA-002 | 审批后的工具调用可能触发下游 Agent 或跨部门服务。 |

## 参与模块边界

| 模块 | 责任 | 不负责 |
|---|---|---|
| `agent-service` | 创建 parent / child Task 关系，维护 Task tree 查询和状态聚合；同一 service 内负责本地多 Agent dispatch / wait / join。 | 不直接执行远端 Agent；不拥有 bus physical channel。 |
| `agent-bus` | 跨边界传递 A2A 控制指令、federation envelope、completion / failure / timeout 事件和 data reference envelope。 | 不拥有 Task State；不解释业务结果；不接管同一 service 内协作；不承载大型 payload；不承载逐 token stream。 |
| `agent-execution-engine` | 父 Agent 产生 delegation intent；子 Agent 执行自己的 step。 | 不绕过 Bus 直连其他部门 Agent；不写 Task State。 |
| `agent-middleware` | 治理 federation policy、capacity、audit、metrics、release control hook。 | 不代替运营人员做审批动作。 |
| 强部门 service | 承载自有 Agent runtime 或业务能力。 | 不脱离平台 Bus 与跨边界其他 Agent 私连。 |
| 平台托管 service | 承载弱部门 PaaS Agent runtime。 | 不接管客户数据源细粒度权限定义。 |

## Federation 形态

| 形态 | 说明 | 平台责任 |
|---|---|---|
| 平台内同一 service 多 Agent | 多个 Agent 都运行在同一个平台托管 `agent-service` 进程。 | `agent-service` 维护 Task tree、local dispatch、join、trace、capacity、LLM cost、audit；不需要经过 Bus。 |
| 平台内跨 service 多 Agent | 多个 Agent 都运行在平台托管 runtime，但不在同一个 service 边界内。 | Bus 控制通道、Task tree、trace、capacity、LLM cost、audit。 |
| 强部门 service -> 平台托管 service | 强部门 Agent 委派给弱部门租用的平台 service。 | Bus 控制通道、federation envelope、发布控制、trace 聚合。 |
| 平台托管 service -> 强部门 service | 弱部门 Agent 请求强部门能力。 | Bus 控制通道、tenant / policy 检查、结果回传。 |
| 强部门 service -> 强部门 service | 两个自有 service 之间协作。 | 平台仍维护 Bus、Task tree、A2A 控制指令、LLM cost 归集。 |

## 主流程

1. S2 中父 Agent 产生 delegation intent，声明 target agent、target department、input envelope、join policy、timeout、failure policy；如输入包含大型数据或多模态内容，input envelope 只携带 URI / object reference / metadata。
2. `agent-service` 为 child Task 创建 Task tree relationship，记录 parentTaskId、delegation reason、cost attribution key；历史 parentRunId 可作为兼容字段。
3. 如果目标 Agent 在同一 service 内，`agent-service` 本地派发 child Task 并管理 completion / failure / timeout。
4. 如果目标 Agent 跨 service / 跨部门 / 跨部署，`agent-bus` 发送 A2A control command 或 federation envelope；Bus envelope 只承载控制语义、identity、trace、policy 和 data reference。
5. 目标 Agent 所在 runtime 接收命令：可能是平台托管 service，也可能是强部门自有 service。
6. child Agent 独立执行 S1 / S2 / S3 / S4 / S5；同一 service 内由 service 回传 completion，跨边界通过 Bus 回传 completion、failure、partial result reference 或 timeout。
7. parent Task 在 S5 中等待 child completion；满足 join policy 后恢复执行。
8. Observability 聚合 parent / child trace、LLM cost、failure ownership 和 audit。

## 数据与流式边界

| 主题 | 规则 |
|---|---|
| 控制指令 | Bus 负责传递 delegation、completion、failure、timeout、resume 等控制语义。 |
| 大型 payload | 大型多模态内容、文件、长文本结果和重载中间产物先写入第三方对象存储、客户指定存储或受控数据服务；Bus 只传 URI / object reference / metadata。 |
| 数据流 | 需求方根据 data reference 和客户授权直接拉取内容；这条路径是 data path，不是 Bus control path。 |
| A2A 流式回传 | 若调用方需要被调方实时流式返回，不能默认每 token 发 Bus 事件；候选方案可能是 service-to-service SSE、专用 stream channel 或其他受治理通道，需后续技术确认。 |
| Task evidence | Task tree 和 trace 可以记录 data reference、摘要、大小、content type、授权边界和拉取结果，但不得把敏感正文放入 metrics label 或 Bus envelope。 |

## 发布和审批控制

| 主题 | 规则 |
|---|---|
| 发布控制能力 | 平台必须提供 Agent-to-Agent 协作关系的发布、启停、回滚和审计能力。 |
| 实际审批动作 | 审批人是运营平台人员或被授权的运营角色；平台提供控制面，不自动替人批准。 |
| 数据源授权 | 目标 Agent 访问客户数据源时仍接入客户既有鉴权体系，平台不配置细粒度业务权限。 |
| 成本统计 | 平台统一统计所有 LLM 调用成本，并可按 parent Task / child Task / tenant / agent 聚合。 |

## 失败和降级流

| 场景 | 期望行为 |
|---|---|
| target agent unavailable | parent Task 根据 failure policy 进入 retry、fallback、partial success 或 failed。 |
| child timeout | 同一 service 内由 service 产生 timeout event；跨边界由 Bus 产生 timeout event；parent Task 通过 S5 resume 后执行 join failure path。 |
| partial failure | 可保留成功 child result，失败 child result 进入 evidence 和 failure ownership。 |
| cross-tenant rejection | federation command 被拒绝且不得泄露目标租户信息。 |
| duplicate completion | 不重复合并 child result，不重复统计 terminal side effect。 |

## 开发态调试证据

| 证据 | 用途 |
|---|---|
| delegation decision evidence | 解释父 Agent 为什么委派、委派给谁、输入是什么。 |
| federation envelope | 解释 Bus 传递的 A2A 控制指令和目标 runtime。 |
| data reference evidence | 解释大型 payload 存放在哪里、谁有权拉取、Bus envelope 中传递了哪个 URI / object reference。 |
| task tree timeline | 开发者查看 parent / child Task 的创建、等待、完成、合并过程。 |
| join evidence | 解释 parent Task 何时恢复、为什么成功或失败。 |

## 运行态指标和调用链

| 类型 | 最小要求 |
|---|---|
| Metrics | `child_task_created_total`、`federation_command_total`、`child_task_timeout_total`、`join_latency`、`task_tree_llm_cost`。 |
| Trace | `delegation.requested`、`child_task.created`、`a2a.command.sent`、`data_reference.created_or_received`、`child_task.completed_or_failed`、`join.completed`。 |
| Audit | 记录 source tenant、target tenant、source agent、target agent、release version、approver reference、policy decision。 |
| Cardinality | metrics 维度必须控制 agent / department / tenant 组合基数，禁止把自然语言 task 内容作为 label。 |

## Assertions

```yaml
scenario: S6-ChildTaskFederation
assertions:
  - child_task_has_parent_task_id: true
  - same_service_multi_agent_collaboration_closed_by_agent_service: true
  - cross_boundary_a2a_control_instruction_uses_agent_bus: true
  - bus_carries_control_and_data_reference_only: true
  - large_payload_uses_external_storage_reference: true
  - a2a_token_streaming_requires_separate_design: true
  - strong_and_weak_department_services_use_same_bus_contract: true
  - parent_task_waits_by_suspend_not_thread_hold: true
  - join_policy_recorded_before_child_dispatch: true
  - duplicate_child_completion_not_merged_twice: true
  - llm_cost_aggregated_to_parent_and_child_task: true
  - platform_provides_release_control_but_human_operator_approves: true
  - trace_contains:
      - delegation.requested
      - child_task.created
      - a2a.command.sent
      - child_task.completed_or_failed
      - join.completed
```

## 开放问题

| ID | 问题 | 影响 | 建议归宿 |
|---|---|---|---|
| OI-S6-001 | federation envelope 的 machine-readable schema 尚未正式化。 | 跨部门 A2A harness 只能验证目标语义，不能锁定 wire contract。 | A2A / Federation ICD。 |
| OI-S6-002 | Task tree cost receipt 与计费口径需要治理定义。 | LLM cost 可统计，但如何展示、分摊、对账还需产品 / 运营规则。 | Cost governance contract。 |
| OI-S6-003 | 发布控制的审批角色和权限模型需要与运营平台对齐。 | A2A 发布能力需要控制面，但审批动作不应由 runtime 自动完成。 | Release governance / operator approval ADR。 |
| OI-S6-004 | A2A 流式回传的技术方案尚未确定。 | 如果每 token 都走 Bus，可能给控制通道带来高频事件压力，也会混淆 control path 和 stream path。 | A2A stream contract / service-to-service SSE / dedicated stream channel ADR。 |
