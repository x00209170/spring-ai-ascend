---
level: L2
view: scenarios
status: draft
---

# S5: Suspend / Resume (Technical Sub-scenario)

## 目的

验证长等待、人工审批、本地能力执行、capacity pressure、timer、child Task join 或 client callback 可以通过 suspend/resume 表达，而不占用物理线程、客户端连接或 engine execution context。本文支撑 BA-002 和 BA-003，也为 BA-001 的本地能力 handoff 提供机制。

## 适用读者

Workflow、Agent Service、Execution Engine、agent-bus、Tool Gateway、agent-client、Observability 负责人。

## 维护规则

- Suspend 前必须持久化 checkpoint / resume payload / resume reason。
- Resume 必须重新验证 tenant、actor、state、attempt id 和 payload schema。
- Bus 只传递 S2C / 跨边界 A2A / resume 控制消息和 data reference envelope，不执行 engine；不承载大型 payload 或逐 token stream；同一 service 内 child Task join 由 `agent-service` 管理。

## 支撑的业务活动

| BA | S5 在其中验证什么 |
|---|---|
| BA-001 | Agent 等待本地 context、memory、retriever、tool 或 approval UI 时释放资源。 |
| BA-002 | 高风险工具等待人工确认，审批完成后恢复执行。 |
| BA-003 | 父 Task 等待 child Task 或跨部门 Agent 返回结果。 |

## Suspend reason 类型

| Reason | 典型来源 | Resume 来源 |
|---|---|---|
| `await_local_capability` | 本地 context / memory / retriever / tool / approval UI。 | `agent-client` 经 S2C callback 返回。 |
| `await_platform_approval` | 弱部门 PaaS 或平台托管审批面。 | 运营平台审批人动作。 |
| `await_business_service` | 强业务方 service 执行核心工具或装配上下文。 | 业务方 service callback。 |
| `await_child_task` | BA-003 child Task / federation call。 | 同一 service 内由 `agent-service` 接收 child completion；跨边界由 `agent-bus` 接收 completion / failure / timeout。 |
| `rate_limited` | capacity pressure。 | capacity wake-pulse / timer。 |
| `await_timer` | 延迟任务或重试窗口。 | scheduler / timer event。 |

## 主流程

1. S2、S3、S4 或 S6 返回 suspend-required，包含 typed reason、checkpoint、resume payload schema、timeout 和 expected actor。
2. `agent-service` 校验可挂起状态，通过受控状态入口将 Task 从 `Running` 转为 `Suspended`。
3. `agent-bus` 投递 S2C、A2A、approval 或 timer 控制消息；如果 resume 结果包含大型 payload，只投递 URI / object reference / metadata。
4. 外部 actor 完成本地能力、审批、业务 service 调用或 child Task 后，提交 resume event。
5. `agent-service` 重新验证 tenant、actor、payload、attempt id、Task current state。
6. 合法 resume 将 Task 从 `Suspended` 转回 `Running` 或进入 terminal state。
7. Execution Engine 从 checkpoint 恢复，不重复已完成副作用。

## 部署形态约束

| 形态 | S5 要保证的语义 |
|---|---|
| Weak Department PaaS | 平台提供托管审批 / 发布控制能力；实际审批动作由运营平台人员或授权人员完成。 |
| Strong Department | 业务方 service 或 client 可作为 resume actor，但必须通过平台 Bus / callback contract 返回。 |
| Local Client | client 可执行完整本地能力，不限于工具调用；包括 context、memory、retriever、approval UI。 |
| Cross-boundary A2A | parent Task 等待 child Task 时也使用 suspend/resume，不占用线程；跨边界回调经 Bus，同一 service 内回调由 service 闭环。 |

## 开发态调试证据

| 证据 | 用途 |
|---|---|
| suspend decision evidence | 解释为什么挂起、挂起前 checkpoint 是什么。 |
| callback envelope | 看清 S2C / A2A / approval / business service callback 的输入输出。 |
| data reference envelope | 看清大型 payload 的存储位置、授权边界和结果引用。 |
| resume validation evidence | 解释 resume 被接受或拒绝的原因。 |
| duplicate resume evidence | 证明重复 callback 不会重复副作用。 |

## 运行态指标和调用链

| 类型 | 最小要求 |
|---|---|
| Metrics | `task_suspended_total`、`task_resume_total`、`resume_rejected_total`、`suspend_duration`、`callback_timeout_total`。 |
| Trace | `suspend.requested`、`task.suspended`、`resume.requested`、`resume.accepted_or_rejected`、`task.resumed`。 |
| Audit | 记录 suspend reason、expected actor、actual actor、execution locus、timeout、approval reference。 |

## Assertions

```yaml
scenario: S5-SuspendResume
assertions:
  - task_state_sequence:
      - Running
      - Suspended
      - Running
      - Terminal
  - checkpoint_or_resume_payload_available_before_suspended: true
  - resume_tenant_and_actor_revalidated: true
  - bus_does_not_execute_engine: true
  - bus_does_not_carry_large_payload_or_token_stream: true
  - duplicate_resume_does_not_duplicate_side_effect: true
  - local_capability_resume_supported:
      - context
      - memory
      - retriever
      - tool
      - approval_ui
  - trace_contains:
      - suspend.requested
      - task.suspended
      - resume.requested
      - task.resumed
```

## 开放问题

| ID | 问题 | 影响 | 建议归宿 |
|---|---|---|---|
| OI-S5-001 | durable wake-pulse 和部分 S2C runtime binding 仍需落地。 | capacity / callback 高压场景不能只靠设计证明。 | S2C transport / ResilienceContract / Workflow harness。 |
| OI-S5-002 | 平台托管审批和本地 client 审批的统一 schema 需要定稿。 | BA-002 的多部署形态 harness 会分裂。 | approval placement profile。 |
