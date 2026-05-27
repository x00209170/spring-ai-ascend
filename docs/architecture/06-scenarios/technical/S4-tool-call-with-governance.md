---
level: L2
view: scenarios
status: draft
---

# S4: Tool Call With Governance (Technical Sub-scenario)

## 目的

验证工具 / Skill / MCP / 业务 adapter 调用必须经过权限、capacity、幂等、审计、trace、approval 和 capability placement 治理。本文是技术子场景，支撑 BA-001 和 BA-002。

## 适用读者

Tool Gateway、agent-middleware、Agent Service、agent-client、业务 Skill 实现者、测试负责人。

## 维护规则

- 不可逆副作用必须有幂等、防重、audit 和 failure semantics。
- 业务 Skill 不得直接写 Task State。
- 本地工具、平台公共服务、业务方 service 工具都必须通过同一治理语义表达 execution locus。

## 支撑的业务活动

| BA | S4 在其中验证什么 |
|---|---|
| BA-001 | Agent 调用业务工具或平台工具时，治理、审计和 trace 形成闭环。 |
| BA-002 | 高风险工具调用进入 approval / suspend，审批结果决定是否继续执行。 |
| BA-003 | 多 Agent 委派中的工具调用仍遵守 parent / child Task 的治理边界。 |

## 参与模块边界

| 模块 | 责任 | 不负责 |
|---|---|---|
| `agent-middleware` | Tool Gateway、policy、capacity、audit、Skill / MCP adapter。 | 不拥有 Task State。 |
| `agent-service` | 根据工具结果推进 Task 状态或进入 suspend。 | 不直接执行工具副作用。 |
| `agent-client` | 执行本地工具、approval UI、私有网络工具、本地文件工具。 | 不跳过平台工具治理结果。 |
| 业务方 service | 执行客户侧核心工具，使用客户自己的鉴权和业务权限。 | 不要求平台维护业务工具内部权限模型。 |

## Capability placement

| 执行位置 | 示例 | 治理要求 |
|---|---|---|
| `platform_middleware` | 平台公共工具、企业中间件 adapter、通用 MCP server。 | 平台 policy、capacity、audit、trace 全量生效。 |
| `business_service` | 强业务方核心业务操作。 | 平台发出受控 tool intent；业务方 service 用自有鉴权执行并返回结果。 |
| `local_client` | 本地文件、桌面应用、个人工具、内网资源。 | 通过 S2C / Yield handoff；client 本地执行后返回受控 result。 |
| `platform_hosted_service` | 弱部门租用的平台托管 runtime。 | 平台托管工具编排和公共 adapter，但仍接入客户数据源鉴权。 |

## 主流程

1. S2 产生 tool request，包含 Task、step、tool key、risk level、execution locus、attempt id。
2. Tool Gateway 校验 tenant、policy、capacity、幂等 key 和 approval requirement。
3. 如果需要人工确认，返回 approval-required，S5 进入 suspend。
4. 如果工具位于本地 client，Tool Gateway 生成 S2C / Yield 指令，经 `agent-bus` 传递给 client。
5. 如果工具位于业务方 service，平台传递受控 tool intent，业务方使用自身鉴权执行。
6. Tool result 返回后，Tool Gateway 记录 outcome、audit、trace 和 side-effect reference。
7. `agent-service` 根据结果推进 Task，而不是 Tool Gateway 直接写状态。

## 成本和权限边界

| 主题 | 规则 |
|---|---|
| LLM 成本 | 统一由平台统计，关联 Task / step / tenant / agent / model。 |
| 工具成本 | 平台不对客户内部工具成本定价；只记录 tool usage reference 和 trace。 |
| 数据源权限 | 平台使用客户认证信息调用数据源，不维护客户内部细粒度权限。 |
| 审批动作 | 平台提供发布 / 调用控制能力；实际审批人可以是运营平台人员或客户侧授权人员。 |

## 开发态调试证据

| 证据 | 用途 |
|---|---|
| tool decision evidence | 解释工具为什么允许、拒绝、限流或需要审批。 |
| execution locus evidence | 解释工具在 platform、business service 还是 local client 执行。 |
| approval evidence | 解释审批请求、审批人、审批结果和 resume 关系。 |
| side-effect evidence | 支持不可逆副作用的 replay-safe 断言。 |

## 运行态指标和调用链

| 类型 | 最小要求 |
|---|---|
| Metrics | `tool_request_total`、`tool_denied_total`、`tool_latency`、`tool_approval_required_total`、`local_tool_handoff_total`。 |
| Trace | `tool.requested`、`tool.authorized_or_denied`、`tool.local_handoff`、`tool.completed_or_failed`。 |
| Audit | 记录 tenant、taskId、stepId、toolKey、execution locus、policy decision、approver、outcome；runId 仅作历史兼容关联字段。 |

## Assertions

```yaml
scenario: S4-ToolCallWithGovernance
assertions:
  - permission_denied_has_no_side_effect: true
  - duplicate_attempt_does_not_repeat_side_effect: true
  - approval_required_before_irreversible_side_effect: true
  - local_tool_call_uses_s2c_or_yield: true
  - business_service_tool_uses_customer_auth_boundary: true
  - tool_gateway_does_not_write_task_state: true
  - audit_contains:
      - tenantId
      - taskId
      - skillKey
      - executionLocus
      - policyDecision
      - outcome
```

## 开放问题

| ID | 问题 | 影响 | 建议归宿 |
|---|---|---|---|
| OI-S4-001 | full skill lifecycle runtime enforcement 仍需后续落地。 | 工具注册、版本、下线、兼容性测试尚不完整。 | Skill lifecycle ICD / Tool Gateway harness。 |
| OI-S4-002 | local client tool result schema 需要与 S2C callback schema 合并或引用。 | client / service 可能产生重复协议。 | S2C local capability profile。 |
