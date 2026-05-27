---
level: L2
view: scenarios
status: draft
---

# S3: Build Context Package (Technical Sub-scenario)

## 目的

验证 Agent step 所需的上下文如何从 Session、Memory、Retriever、Vector、prompt variables、本地 client 或业务方 service 中装配成可追踪、可重放、受权限约束的 Context Package。本文是技术子场景，不新增名为 Context Engine 的 L0 架构模块。

## 适用读者

Agent Service、agent-middleware、Context / Memory / Retriever 负责人、agent-client 负责人、harness 生成器。

## 维护规则

- Context Engine 是能力聚合，不是新的 L0 架构模块。
- 平台不得替客户定义细粒度数据源权限；数据源授权应接入客户既有鉴权体系。
- ContextPackage draft 不能替代正式 contract catalog。

## 支撑的业务活动

| BA | S3 在其中验证什么 |
|---|---|
| BA-001 | Agent 在处理业务请求前装配业务上下文、历史记忆、检索结果和 prompt variables。 |
| BA-002 | 高风险工具审批前后，context evidence 能解释为什么需要审批。 |
| BA-003 | 父子 Agent 传递上下文时，能够避免跨租户泄露并保留 trace lineage。 |

## 参与模块边界

| 模块 | 责任 | 不负责 |
|---|---|---|
| `agent-service` | 发起 context projection 请求，绑定 Task / step / tenant / trace。 | 不绕过权限读取业务数据。 |
| `agent-middleware` | 聚合 Memory、Retriever、Vector、Prompt、Policy 等能力。 | 不成为业务数据 owner。 |
| `agent-client` | 在本地执行 context assembly、memory、retriever、文件读取或私有网络访问。 | 不伪造平台 trace；不绕过 S2C 返回结果。 |
| 业务方 service | 使用客户自身鉴权体系访问数据源，返回受控 context reference 或摘要。 | 不要求平台配置客户内部权限模型。 |

## Capability placement

| 执行位置 | 允许能力 | 约束 |
|---|---|---|
| `platform_hosted_service` | 平台托管 Memory / Retriever / prompt variables。 | 只能访问已接入且授权给平台调用的数据源。 |
| `business_service` | 强业务方 service 本地装配核心上下文。 | 使用客户自有认证信息和权限体系；平台只接收结果 reference / evidence。 |
| `local_client` | 本地文件、桌面环境、个人工具、本地 memory / retriever。 | 通过 S2C / Yield handoff；返回脱敏或受控结果。 |
| `platform_middleware` | 公共中间件、通用知识库、企业公共服务 adapter。 | 必须经过 tenant scope、policy 和 trace。 |

## 主流程

1. S2 触发 context build request，包含 Task、step、tenant、trace、allowed sources、placement profile。
2. `agent-service` / `agent-middleware` 判断每个 context source 的 execution locus。
3. 平台侧 source 通过 middleware 能力读取；业务方 source 使用客户既有 auth reference 调用；本地 source 通过 S2C / Yield 指令交给 client。
4. 每个 source 返回 source reference、版本、摘要、权限证明或失败原因。
5. Context projector 生成 ContextPackage metadata，不在 trace 中暴露敏感正文。
6. S2 使用 ContextPackage reference 继续执行。

## 权限和数据边界

| 规则 | 说明 |
|---|---|
| 客户权限体系优先 | 数据源授权接入客户现有认证和权限体系，平台不维护细粒度业务权限配置。 |
| 平台只使用授权引用 | 平台调用数据源时使用客户授予的 auth reference，不自行扩权。 |
| 本地能力充分开放 | local client 可执行 context、memory、retriever、tool、approval UI，但结果必须通过契约返回。 |
| 敏感内容最小暴露 | trace / metrics 记录 source reference、hash、version、size，不直接写入敏感正文。 |

## 开发态调试证据

| 证据 | 用途 |
|---|---|
| source selection evidence | 解释哪些 source 被选中、跳过或拒绝。 |
| auth reference evidence | 解释平台是否使用客户认证信息访问数据源。 |
| local handoff evidence | 解释哪些 context 在 client 本地装配，以及返回了什么受控 metadata。 |
| context package metadata | 支持 prompt debug 和 replay-safe fixture。 |

## 运行态指标和调用链

| 类型 | 最小要求 |
|---|---|
| Metrics | `context_build_total`、`context_build_latency`、`context_source_error_total`、`local_context_handoff_total`。 |
| Trace | `context.requested`、`context.source_selected`、`context.local_handoff`、`context.built_or_failed`。 |
| Audit | 记录 source type、execution locus、auth reference type、tenant、Task、step。 |
| Privacy | 不在 metrics label 或 trace inline payload 中放业务正文或 PII。 |

## Assertions

```yaml
scenario: S3-BuildContextPackage
assertions:
  - context_package_has_tenant_scope: true
  - context_build_does_not_write_task_state: true
  - platform_does_not_define_customer_data_permissions: true
  - customer_auth_reference_used_for_customer_datasource: true
  - local_context_assembly_uses_s2c_or_yield: true
  - forbidden_memory_category_rejected: true
  - sensitive_content_not_inlined_in_trace: true
  - trace_contains:
      - context.requested
      - context.built_or_failed
```

## 开放问题

| ID | 问题 | 影响 | 建议归宿 |
|---|---|---|---|
| OI-S3-001 | ContextPackage schema 仍需正式化。 | harness 难以稳定断言 context evidence。 | ContextPackage ICD / machine-readable schema。 |
| OI-S3-002 | customer auth reference 的最小抽象尚未定稿。 | 平台与客户数据源对接方式可能分裂。 | Data source authorization profile。 |
