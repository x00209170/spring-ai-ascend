---
level: L1
view: scenarios
status: draft
---

# BA-002: 人工确认工具调用

## 目的

用需要人工确认的工具调用业务活动检验 suspend/resume、S2C callback、tool governance、Task state owner、开发态调试证据和运行态审计是否形成闭环。

## 适用读者

集成 `agent-client` 的应用开发者、架构师、agent-service、agent-bus、agent-middleware、harness 生成器、运维和审计负责人。

## 维护规则

- 本场景必须证明长等待不持有执行线程或客户端连接。
- 人工确认结果不能绕过 Tool Gateway 和 `agent-service` 的 Task 状态受控入口。
- 场景必须说明开发者如何调试 approval-required 决策，以及运行态如何审计确认链路。

## 业务活动

集成 `agent-client` 的应用开发者把一个高风险业务工具接入 Agent，例如“取消订单”或“提交退款”。开发态他们需要验证：什么条件会触发人工确认、确认前是否真的没有副作用、重复 callback 是否幂等、拒绝和超时如何反馈给业务系统。运行态他们需要看到每一次 approval request / response / resume / tool outcome 的调用链和审计证据。

上线后，Agent 需要执行高风险业务工具时，平台在工具调用前触发人工确认，Task 进入 Suspended；用户确认后，平台恢复 Task，执行工具，记录审计并返回结果。

## 直接用户关注点

| 用户 | 开发态关注点 | 运行态关注点 |
|---|---|---|
| 应用开发者 | 能用 stubbed approval UI 验证 approve / deny / timeout / duplicate callback。 | 能从业务请求定位到 approval request、审批人、resume 结果和工具 outcome。 |
| 运维 / SRE | 能用 failure fixture 复现 callback 丢失、乱序、重复和超时。 | 能看到 suspended Task 数量、平均等待时长、resume 失败率、callback 延迟。 |
| 审计负责人 | 能检查确认策略是否覆盖高风险工具。 | 能审计 actor、policy decision、approval decision、tool side effect。 |

## 参与架构模块

| 模块 | 在活动中的角色 |
|---|---|
| `agent-service` | Task state owner；approval request identity；resume entry；client invocation reference query。 |
| `agent-execution-engine` | Orchestrator 处理 suspend / resume intent。 |
| `agent-middleware` | Skill governance、permission、tool-call idempotency、audit。 |
| `agent-bus` | S2C callback request / response channel。 |
| `agent-client` | future approval UI / callback consumer。 |

## 模块协作流

1. Agent step 触达需要人工确认的工具调用。
2. Tool Gateway 将调用判定为 approval-required，并产出受治理的决策。
3. Orchestrator 将等待转换为 SuspendSignal / suspend intent。
4. `agent-service` 通过受控状态入口将 Task 从 Running 转为 Suspended。
5. `agent-bus` 将 S2C 审批请求传递给 client。
6. client 返回 approve / deny / timeout 等审批结果。
7. resume 路径重新校验 tenant、actor 和 callback correlation。
8. approve 路径通过 Tool Gateway 执行工具；deny 路径记录拒绝结果并进入相应状态转换。
9. Observability 记录 approval request、response、resume 和 tool outcome。

## 特性覆盖

| 特性 | 覆盖场景 |
|---|---|
| suspend instead of hold | S5 |
| S2C callback boundary | S5 |
| 工具治理 | S4 |
| 审批审计 | S4, S5 |
| Task state single owner | S2, S5 |
| 开发态审批调试 | BA-002, S4, S5 |
| 运行态审批指标 | BA-002, S5 |

## 面向开发者的调试证据

| 证据 | 最小内容 |
|---|---|
| 审批决策 trace | skill name、risk reason、policy rule、approval required flag。 |
| suspend 证据 | taskId、from/to status、checkpoint 或 resume payload 可用性；runId 仅作历史兼容关联字段。 |
| callback 证据 | callback correlation、actor、decision、receivedAt、duplicate / expired flag。 |
| 工具副作用证据 | approval 前未执行工具；approved path 有幂等和审计；denied path 没有副作用。 |

## 运行态指标和调用链要求

| 领域 | 必要信号 |
|---|---|
| 审批规模 | 按 tenant / app / skill 聚合的 approval requested / approved / denied / expired 数量。 |
| 等待健康度 | suspended Task 数量、approval wait duration、resume latency。 |
| 可靠性 | duplicate callback、out-of-order callback、resume failure、approval 后工具失败。 |
| 可审计性 | approval actor、policy rule、tool outcome、traceId / taskId linkage；runId 仅作历史兼容关联字段。 |

## 涉及契约

- ICD-AgentService-ToolGateway
- ICD-Workflow-AgentService
- ICD-Workflow-Observability
- ICD-CS-Capability-Placement
- s2c-callback contract

## 技术子场景

- [S1 Create Task / Invocation](technical/S1-create-run.md)
- [S2 Execute Agent Step](technical/S2-execute-agent-step.md)
- [S4 Tool Call With Governance](technical/S4-tool-call-with-governance.md)
- [S5 Suspend / Resume](technical/S5-suspend-resume.md)

## 断言

```yaml
scenario: BA-002-HumanApprovalToolCall
assertions:
  - approval_required_tool_does_not_execute_before_approval: true
  - task_state_sequence:
      - Running
      - Suspended
      - Running
      - Terminal
  - resume_revalidates_tenant_and_callback_correlation: true
  - denied_approval_has_no_tool_side_effect: true
  - approved_tool_call_has_idempotency_and_audit: true
  - developer_can_replay_approval_paths_with_stubs: true
  - runtime_metrics_include_suspended_count_and_approval_wait_duration: true
  - trace_contains:
      - approval.requested
      - task.suspended
      - approval.received
      - task.resumed
      - tool.completed_or_denied
```

## 模块能力缺口检查

| 检查项 | 结果 |
|---|---|
| Tool Gateway 能否在审批前阻止工具执行？ | Draft；需要 tool governance harness。 |
| bus 能否只承载审批请求而不执行 engine？ | Draft；S2C runtime binding 部分 deferred。 |
| resume 能否安全重新进入执行流程？ | S5 draft 覆盖。 |
| 集成开发者能否调试 approval-required 决策？ | Draft；需要 approval decision trace contract。 |
| 运维能否衡量 suspended backlog 和 approval latency？ | Draft；需要 metrics contract。 |

## 模块边界适配性检查

| 边界 | 适配信号 |
|---|---|
| Tool Gateway vs Orchestrator | 健康信号：Tool Gateway 返回受治理决策，Orchestrator 拥有 suspend / resume intent。 |
| Bus vs Task state owner | 健康信号：bus 只承载 callback，不写 Task State。 |

## Harness 生成说明

- 构造 approval-required skill stub。
- 构造 fake S2C callback transport，覆盖 approve / deny / timeout 变体。
- 断言 approval 前没有副作用。
- 断言重复 approval response 保持幂等。
- 断言开发者可以看到 approval decision trace。
- 断言系统发出 suspended backlog 和 approval wait 指标。

## 开放问题

| ID | 问题 | 为什么重要 | 影响范围 | 建议归宿 |
|---|---|---|---|---|
| OI-BA002-001 | S2C capacity enforcement 和 non-blocking wake-pulse 部分 deferred。 | BA-002 假设审批等待不会占用执行线程，也不会让大量等待中的 callback 压垮系统；如果 S2C capacity 和 wake-pulse 没有 runtime binding，只能证明语义方向，不能证明生产容量治理。 | S2C callback、suspend/resume、approval backlog、capacity、timeout、resume latency。 | L1 保留为 `design_only` / deferred；L2/L3 在 S2C transport、ResilienceContract、workflow harness 中补 capacity enforcement 和 wake-pulse 测试。 |
| OI-BA002-002 | Approval debug timeline 尚未正式定义。 | 应用开发者需要知道工具为什么进入 approval-required、approval 前是否有副作用、审批结果如何影响 resume；如果没有统一 timeline，开发者只能看分散日志。 | developer debug evidence、Tool Gateway decision trace、Task timeline、approval replay fixture。 | 扩展 Observability / Developer Experience ICD，定义 approval decision、task.suspended、approval.received、task.resumed、tool.completed_or_denied 的查询语义。 |
| OI-BA002-003 | 审批相关 metrics dimensions 需要明确 contract。 | 运维要看 suspended Task 数量、审批等待时长、resume 失败率，但维度设计不清会导致指标无法定位或高基数爆炸。 | metrics、tenant / app / skill 维度、SRE dashboard、容量治理、审计报表。 | 在 observability contract 中定义 approval metrics 的 mandatory dimensions、禁止维度、采样策略和聚合窗口。 |
| OI-BA002-004 | 本地 client 审批和平台托管审批的边界需要补充。 | 强业务方可能要求审批发生在本地 client；弱部门 PaaS 用户可能使用平台托管审批面。两者都叫 approval，但数据、凭据和审计 owner 不同。 | agent-client、agent-bus、hosted service、S2C callback、审计、数据驻留。 | 在 `ICD-CS-Capability-Placement` 中补 approval placement profile：`local_client_approval`、`platform_hosted_approval`、`delegated_approval_adapter`。 |
