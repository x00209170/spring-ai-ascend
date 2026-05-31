---
level: L1
view: harness
status: draft
---

# Agent Service Harness Design

## 目的

把 `agent-service` 的状态模型、流程模型和开发切片转成 harness 生成输入。本文比 `08-harness/agent-service-harness-spec.md` 更细，面向模块负责人和 AI agent 直接拆测试。

## Harness 分层

| Harness Layer | 覆盖内容 | 依赖 |
|---|---|---|
| H1 Entry Harness | HTTP 对外入口、tenant、auth reference、idempotency、trace。 | AS-SLICE-001, 002 |
| H2 State Harness | Task 状态机、controlled transition、terminal idempotency、cancel race。 | AS-SLICE-003 |
| H3 Engine Harness | 同进程 engine dispatch、step result、suspend-required、terminal response。 | AS-SLICE-004 |
| H4 Placement Harness | context / tool / memory / retriever / approval UI placement。 | AS-SLICE-005 |
| H5 Resume Harness | S2C、approval、business service callback、timer、child Task resume。 | AS-SLICE-006 |
| H6 Task Tree Harness | same-service child Task、join、partial failure。 | AS-SLICE-007 |
| H7 Federation Harness | cross-boundary A2A control、data reference、timeout、duplicate completion。 | AS-SLICE-008 |
| H8 SSE Harness | service SSE event、terminal event、error event、trace correlation。 | AS-SLICE-009 |
| H9 Evidence Harness | Task timeline、step evidence、decision evidence、metrics、audit、cost attribution。 | AS-SLICE-010, 011 |
| H10 Replay Harness | replay-safe fixture export、脱敏、租户隔离。 | AS-SLICE-012 |

## Mock / Stub / Fixture

| 类型 | 名称 | 用途 |
|---|---|---|
| Mock | Gateway request mock | 模拟 Gateway 代理到 `agent-service` 的业务请求。 |
| Mock | Auth / tenant mock | 模拟 JWT claim、tenant header、tenant mismatch。 |
| Stub | TaskStateStore stub | 验证创建、状态转换、CAS / atomic transition。 |
| Stub | Historical RunRepository compatibility stub | 验证旧命名不会形成第二套状态 owner。 |
| Stub | In-process engine stub | 返回 completed、failed、suspend-required、tool-required、child-task-required。 |
| Stub | Middleware stub | 返回 model / tool / context decision evidence。 |
| Stub | Bus control stub | 只接受 S2C / callback / A2A control command 和 data reference envelope。 |
| Stub | SSE sink | 捕获 SSE event，验证 taskId / traceId / eventType。 |
| Fixture | Duplicate request fixture | 同 tenant + idempotency key + equivalent body。 |
| Fixture | Tenant mismatch fixture | token、header 或 task owner 不一致。 |
| Fixture | Large payload fixture | 多模态或长文本内容只允许以 URI / object reference 进入 envelope。 |
| Fixture | Child task fixture | parentTaskId、childTaskId、joinPolicy、failurePolicy。 |
| Fixture | Resume fixture | valid / invalid / duplicate / after terminal resume。 |

## 核心测试矩阵

| Test ID | 目标 | Given | When | Then |
|---|---|---|---|---|
| AS-H-001 | 创建 Task 幂等 | 同 tenant、同 idempotency key、等价请求 | 重复提交 | 返回同一 Task reference，不创建重复 Task。 |
| AS-H-002 | tenant mismatch 隐藏 | 请求 tenant 与 token / Task owner 不一致 | 查询或取消 Task | 不泄露其他租户 Task 信息，并产生审计。 |
| AS-H-003 | Task 状态受控 | engine 返回 terminal result | service 推进状态 | 只能通过 TaskStateStore controlled transition。 |
| AS-H-004 | 非法状态转换拒绝 | Task 已 terminal | resume / cancel / complete 再次到达 | 不重复副作用，返回幂等或 illegal transition。 |
| AS-H-005 | cancel / complete 竞争 | cancel 和 terminal result 并发 | 两者竞争写入 | 只有一个 terminal transition 获胜。 |
| AS-H-006 | 同进程 engine dispatch | Task Running | service 调 engine | 不出现 service-to-engine 远程调用假设，engine 不写状态。 |
| AS-H-007 | 本地能力 handoff | step 需要 local context / tool | service 生成 S2C / Yield | client 返回受控 result，trace 标记 execution_locus。 |
| AS-H-008 | suspend / resume 校验 | Task Suspended | resume event 到达 | 重新校验 tenant、actor、attempt、payload schema。 |
| AS-H-009 | 同 service child Task | parent 产生 child intent | 目标 Agent 同进程 | service 创建 child Task 并 join，不经过 Bus。 |
| AS-H-010 | 跨边界 A2A | target 跨 service / 部门 / 部署 | service 发出 A2A control command | 通过 Bus；payload 只含 data reference。 |
| AS-H-011 | 大型 payload 引用传递 | result 含多模态内容 | completion envelope 生成 | envelope 只含 URI / object reference / metadata。 |
| AS-H-012 | SSE 实时输出 | client 打开 stream | step 产生 partial result | SSE event 含 taskId / traceId，不写 Task State。 |
| AS-H-013 | 开发者 timeline | Task 执行失败 | 查询 evidence | 能看到 step、context、tool、model、failure reason。 |
| AS-H-014 | 成本归集 | parent / child 都调用 LLM | 任务完成 | LLM cost 可按 tenant / app / agent / parent-child Task 聚合。 |
| AS-H-015 | replay-safe export | 失败 Task 含敏感数据 | 导出 fixture | 只导出脱敏 request、stubbed context / tool outcome 和 expected assertions。 |

## Golden Trace 最小事件

| 场景 | Trace 事件 |
|---|---|
| Task 创建 | `intake.requested`, `idempotency.claimed_or_reused`, `task.created` |
| Step 执行 | `engine.dispatched`, `step.completed_or_failed`, `task.state_transition` |
| Suspend / Resume | `suspend.requested`, `task.suspended`, `resume.requested`, `resume.accepted_or_rejected`, `task.resumed` |
| Same-service child Task | `delegation.requested`, `child_task.created`, `join.completed` |
| Cross-boundary A2A | `a2a.command.sent`, `data_reference.created_or_received`, `child_task.completed_or_failed` |
| SSE | `sse.stream.opened`, `sse.event.sent`, `sse.stream.completed_or_failed` |

## Failure Injection

| Failure | 期望 |
|---|---|
| Task store unavailable | 返回可观测错误，不产生半创建状态。 |
| Idempotency store conflict | 返回 conflict，不重复创建 Task。 |
| Engine throws unexpected error | Task 进入 Failed 或可恢复路径，并记录 failure evidence。 |
| Middleware timeout | 根据 policy retry / suspend / fail，service 不直接吞错。 |
| Bus unavailable | 跨边界 control command 失败可观测；同 service 协作不受 Bus 影响。 |
| Client callback timeout | Task 进入 timeout / failed 路径，并记录 callback outcome。 |
| Duplicate child completion | 不重复 join，不重复 cost attribution。 |
| SSE client disconnect | 不影响 Task State，记录 stream closed。 |
| Metrics sink failure | 不影响 Task terminal transition，但记录 telemetry failure。 |

## Harness 生成约束

- 不需要真实 LLM。
- 不需要真实对象存储；data reference 可用 fake URI。
- 不需要真实 Bus；Bus stub 必须拒绝大型 payload 和 token stream。
- 不需要真实数据库；但 TaskStateStore stub 必须支持 atomic transition 模拟。
- 所有测试都必须带 tenantId、traceId、taskId。
- 历史 runId / RunRepository 只能作为 compatibility 字段或 stub，不得成为断言主键。
