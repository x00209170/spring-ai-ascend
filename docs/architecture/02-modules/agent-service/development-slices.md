---
level: L1
view: development
status: draft
---

# Agent Service Development Slices

## 目的

把 `agent-service` 的 L1 设计拆成可并行开发、可生成 harness、可评审的切片。每个切片都必须有明确状态 owner、上下游契约、场景来源和最小断言。

## 切片总览

| Slice ID | 名称 | 目标 | 主要依赖 | 可并行性 |
|---|---|---|---|---|
| AS-SLICE-001 | Entry Context & Trace Binding | 建立 HTTP 对外入口的 tenant / actor / auth reference / trace 绑定。 | Gateway, Observability | 可先行 |
| AS-SLICE-002 | Task Creation & Idempotency | 创建 / 复用 Task，维护 idempotency 和 client invocation reference。 | AS-SLICE-001, TaskStateStore | 可先行 |
| AS-SLICE-003 | Task Controlled Transition | 所有 Task 状态变化走受控入口，兼容历史 RunRepository 命名。 | AS-SLICE-002 | 可先行 |
| AS-SLICE-004 | In-process Engine Dispatch | 同进程调用 `agent-execution-engine` 并接收 step result。 | AS-SLICE-003, engine envelope | 与 engine 协同 |
| AS-SLICE-005 | Context / Tool Placement Coordinator | 根据 placement profile 发起 platform middleware、local client 或 business service handoff。 | AS-SLICE-004, middleware contracts, S2C | 与 middleware / client 协同 |
| AS-SLICE-006 | Suspend / Resume Coordinator | 处理本地能力、审批、业务 service、child Task 的 suspend / resume。 | AS-SLICE-003, AS-SLICE-005, bus callback | 与 bus / client 协同 |
| AS-SLICE-007 | Task Tree & Same-service Multi-Agent Join | 同一 service 内创建 child Task、等待、join 和结果合并。 | AS-SLICE-003, AS-SLICE-004 | 与 engine 协同 |
| AS-SLICE-008 | Cross-boundary A2A Control | 通过 Bus 发送 / 接收跨 service / 跨部门 A2A 控制指令和 data reference。 | AS-SLICE-006, AS-SLICE-007, agent-bus | 与 bus 协同 |
| AS-SLICE-009 | Service SSE Stream | 对外输出实时 SSE 事件。 | AS-SLICE-002, AS-SLICE-004, AS-SLICE-006 | 可与查询面并行 |
| AS-SLICE-010 | Developer Evidence Query | 提供 Task timeline、step evidence、context / tool / model decision evidence 查询面。 | AS-SLICE-003..009, Observability | 可渐进 |
| AS-SLICE-011 | Runtime Metrics / Audit / Cost Attribution | 输出运维指标、审计、LLM cost attribution key。 | AS-SLICE-001..010 | 横切 |
| AS-SLICE-012 | Replay-safe Fixture Export | 从失败 Task 生成脱敏 fixture。 | AS-SLICE-010, governance | 后置 |

## 切片详情

### AS-SLICE-001 Entry Context & Trace Binding

| Field | Value |
|---|---|
| Scope | Gateway 背后的 `agent-service` HTTP 对外入口；tenant、actor、auth reference、trace parent、idempotency key 的解析和传递。 |
| Owns State | HTTP 对外入口 trace / tenant request carrier。 |
| Does Not Own | Gateway；Task State；Bus channel。 |
| Scenarios | BA-001, S1 |
| Harness Assertions | tenantId / traceId 必须存在；tenant mismatch 不泄露其他租户信息；HTTP 对外入口不调用 engine。 |

### AS-SLICE-002 Task Creation & Idempotency

| Field | Value |
|---|---|
| Scope | 创建 Task；创建或复用 client invocation reference；执行 idempotency claim。 |
| Owns State | Task Execution State initial record；IdempotencyRecord；Client Invocation Reference。 |
| Does Not Own | 独立 Run State；业务状态。 |
| Scenarios | S1, BA-001 |
| Harness Assertions | 重复请求返回同语义 Task reference；Gateway / Bus 不写 Task State；repository unavailable 不产生半创建状态。 |

### AS-SLICE-003 Task Controlled Transition

| Field | Value |
|---|---|
| Scope | Pending / Running / Suspended / Terminal 等状态转换；terminal idempotency；历史 RunRepository / RunStateMachine 兼容。 |
| Owns State | Task Execution State。 |
| Does Not Own | Engine result semantics；Tool execution state。 |
| Scenarios | S2, S5 |
| Harness Assertions | 合法转换通过；非法转换拒绝；terminal 后重复 resume 不重复副作用；历史 Run 命名不形成第二套 owner。 |

### AS-SLICE-004 In-process Engine Dispatch

| Field | Value |
|---|---|
| Scope | 构造 engine envelope；同进程调用 `agent-execution-engine`；接收 step result。 |
| Owns State | 无新增持久状态；只提交状态转换意图给 AS-SLICE-003。 |
| Does Not Own | Engine SPI canonical definition；模型 / 工具 provider 语义。 |
| Scenarios | S2 |
| Harness Assertions | service invokes engine in process；engine 不直接写 Task State；step evidence 记录 engine dispatch。 |

### AS-SLICE-005 Context / Tool Placement Coordinator

| Field | Value |
|---|---|
| Scope | 依据 placement profile 决定 platform middleware、local client、business service、delegated adapter。 |
| Owns State | execution locus evidence。 |
| Does Not Own | 客户数据源权限模型；middleware SPI 全局语义；本地 client 资源。 |
| Scenarios | BA-001, S3, S4 |
| Harness Assertions | 本地 context / tool 通过 S2C / Yield；平台工具通过 middleware governance；trace 标记 execution_locus。 |

### AS-SLICE-006 Suspend / Resume Coordinator

| Field | Value |
|---|---|
| Scope | 等待本地能力、人工审批、业务 service、timer、child Task；resume 重新校验 tenant、actor、attempt、payload。 |
| Owns State | Task Suspended / Running transition；resume validation evidence。 |
| Does Not Own | Approval UI 实现；Bus physical channel。 |
| Scenarios | BA-002, S5 |
| Harness Assertions | suspend 前 checkpoint / resume payload 存在；resume actor 重新校验；重复 resume 不重复副作用。 |

### AS-SLICE-007 Task Tree & Same-service Multi-Agent Join

| Field | Value |
|---|---|
| Scope | 同一 `agent-service` 进程内创建 parent / child Task relationship，本地 dispatch、wait、join、结果合并。 |
| Owns State | Task Tree Relationship。 |
| Does Not Own | 跨边界 Bus 控制通道；远端 Agent 执行。 |
| Scenarios | BA-003, S6 |
| Harness Assertions | child_task_has_parent_task_id；same_service_multi_agent_collaboration_closed_by_agent_service；duplicate_child_completion_not_merged_twice。 |

### AS-SLICE-008 Cross-boundary A2A Control

| Field | Value |
|---|---|
| Scope | 通过 `agent-bus` 发送 / 接收跨 service / 跨部门 / 跨部署 A2A control command、completion、failure、timeout 和 data reference envelope。 |
| Owns State | parent / child correlation in Task tree；resume validation evidence。 |
| Does Not Own | Bus physical channel；大型 payload；逐 token stream。 |
| Scenarios | BA-003, S5, S6 |
| Harness Assertions | cross_boundary_a2a_control_instruction_uses_agent_bus；large_payload_uses_external_storage_reference；a2a_token_streaming_requires_separate_design。 |

### AS-SLICE-009 Service SSE Stream

| Field | Value |
|---|---|
| Scope | 面向 `agent-client` 或外部 client 输出实时事件，包括阶段性结果、最终响应、错误和完成信号。 |
| Owns State | SSE stream event publication；不拥有 Task State。 |
| Does Not Own | Bus token stream；A2A 流式回传技术方案。 |
| Scenarios | BA-001, S1, S2 |
| Harness Assertions | service_streams_realtime_output_via_sse；bus_does_not_carry_token_stream；SSE event 关联 taskId / traceId。 |

### AS-SLICE-010 Developer Evidence Query

| Field | Value |
|---|---|
| Scope | 查询 Task timeline、step evidence、context evidence、tool decision evidence、model evidence、failure evidence。 |
| Owns State | 只读查询面；不拥有底层观测存储。 |
| Does Not Own | Observability 全局存储；业务 outcome state。 |
| Scenarios | BA-001, BA-002, BA-003 |
| Harness Assertions | developer_can_inspect_step_level_timeline；developer_can_explain_context_and_tool_decisions；敏感字段脱敏。 |

### AS-SLICE-011 Runtime Metrics / Audit / Cost Attribution

| Field | Value |
|---|---|
| Scope | 输出 tenant / app / agent / task / model / tool 维度的 metrics、audit 和 LLM cost attribution key。 |
| Owns State | service 侧 attribution reference；audit event emission。 |
| Does Not Own | 客户内部工具成本定价；业务 outcome state。 |
| Scenarios | BA-001, BA-003, S1..S6 |
| Harness Assertions | runtime_metrics_available_by_tenant_app_agent_tool_model；llm_cost_aggregated_to_parent_and_child_task；metrics label 不包含自然语言正文。 |

### AS-SLICE-012 Replay-safe Fixture Export

| Field | Value |
|---|---|
| Scope | 从失败 Task 生成脱敏 request、stubbed context、stubbed tool outcome 和 expected trace assertions。 |
| Owns State | replay-safe export decision / evidence reference。 |
| Does Not Own | 生产业务数据快照；跨租户数据导出。 |
| Scenarios | BA-001 |
| Harness Assertions | failed_task_can_generate_sanitized_replay_fixture；no C-Side business fact persisted without delegation contract。 |

## 并行开发建议

- 第一批：AS-SLICE-001、002、003、004、011。它们建立 service 主干、状态 owner 和观测底座。
- 第二批：AS-SLICE-005、006、007、009、010。它们补齐开发者体验、本地能力、SSE 和多 Agent 本地协作。
- 第三批：AS-SLICE-008、012。它们依赖 Bus / A2A / replay governance 的进一步契约。
