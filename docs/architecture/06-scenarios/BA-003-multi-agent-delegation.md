---
level: L1
view: scenarios
status: draft
---

# BA-003: 多 Agent 委派与协作

## 目的

用多 Agent 协作业务活动检验父子 Task、同一 service 内协作边界、跨边界 Agent 委派、统一 Task tree、tenant / trace 传播、开发态调试路径和运行态运维洞察是否合理。

## 适用读者

集成 `agent-client` 的应用开发者、架构师、agent-bus、agent-service、agent-execution-engine、AI agent、运维和运营负责人。

## 维护规则

- 本场景当前包含较多 `design_only` / future work，必须诚实标记。
- 子 Agent 不得直接写父 Task terminal state。
- 场景必须说明开发者如何理解父子 Agent 的协作路径，以及运行态如何观察跨 Agent 成本、延迟和失败。

## 业务活动

集成 `agent-client` 的应用开发者希望把复杂业务流程拆给多个 Agent 协作，例如“生成客户问题分析并让另一个 Agent 检查合规风险”。开发态他们需要看到父 Agent 为什么委派、子 Agent 收到了什么上下文、等待和合并发生在哪里、失败后由谁负责重试或降级。运行态他们需要看到跨 Agent 的调用链、成本、延迟、失败率和业务 outcome。

主 Agent 收到复杂业务请求后，创建或委派子任务，等待子 Agent 结果，再合并输出。平台必须保证子任务出现在统一 Task tree 中，trace 和 audit 可重建，权限与 tenant 不被隐式转移。同一 `agent-service` 进程内的多 Agent 协作由 service 闭环；跨 service / 跨部门 / 跨部署时，A2A 控制指令必须走 `agent-bus`。

## 直接用户关注点

| 用户 | 开发态关注点 | 运行态关注点 |
|---|---|---|
| 应用开发者 | 能可视化父子 Task tree，理解委派原因、子 Agent 输入、join 结果。 | 能从一个业务请求追踪到所有子 Agent、模型调用、工具调用和失败点。 |
| 业务负责人 | 能判断拆分给多 Agent 是否真的提升质量。 | 能看到多 Agent 成本、耗时、成功率和人工介入率。 |
| 运维 / SRE | 能用 fixture 复现子任务失败、父任务等待、跨实例传输失败。 | 能看到子 Task 积压、federation 延迟、join 超时、部分失败。 |

## 参与架构模块

| 模块 | 在活动中的角色 |
|---|---|
| `agent-service` | 父子 Task identity、Task state owner、同一 service 内多 Agent 协作和 join owner。 |
| `agent-execution-engine` | Orchestrator 产生 child-task / delegation intent 和 join intent。 |
| `agent-bus` | federation、S2C / callback、跨 service / 跨部门 / 跨部署 A2A 控制指令通道；只传控制指令和 data reference envelope。 |
| `agent-middleware` | 子 Agent 依赖的 model、skill、memory、release control hook 等能力。 |
| `agent-client` | SDK 请求来源，也可以承载本地 capability。 |

## 模块协作流

1. 父 Agent 接收业务请求，并判断是否需要委派子任务。
2. Orchestrator 发出 child-task / 委派意图。
3. `agent-service` 在同一 tenant 下创建 child Task，并记录父子关联；若目标 Agent 在同一 service 进程内，service 负责本地 dispatch、等待和 join。
4. 如需跨 service、跨实例、跨部署或跨部门协作，`agent-bus` 承载 federation envelope / A2A control command；大型数据或多模态内容先写入对象存储或客户指定存储，Bus 只传 URI / object reference / metadata。
5. 子 Agent 执行沿用 BA-001 的基本路径。
6. 父 Task 进入 suspend 或等价的非持有等待，不占用物理线程或连接。
7. 子 Task 的 terminal event 唤醒或通知父 Task。
8. 父 Agent 合并子结果，并通过 `agent-service` 受控状态入口完成 terminal transition。

## 特性覆盖

| 特性 | 覆盖场景 |
|---|---|
| 父子 Task tree | S6 |
| 等待子 Task 时 suspend | S5 |
| Agent 执行 | S2 |
| tenant / trace 传播 | S1, S2 |
| federation / bus 边界 | S6 |
| 开发态 Task tree 调试 | BA-003, S1, S2, S6 |
| 运行态多 Agent 运维洞察 | BA-003, S6 |

## 面向开发者的调试证据

| 证据 | 最小内容 |
|---|---|
| 委派决策 | 父 Agent step、委派原因、目标 Agent 逻辑名、输入摘要。 |
| Task tree | 父 taskId、子 taskId、correlation、join point、terminal status；runId 仅作历史兼容关联字段。 |
| 子 Task 执行 trace | 子上下文摘要、模型 / 工具使用情况、失败类别、重试 owner。 |
| 合并证据 | 子结果摘要、合并策略、fallback 或 partial result decision。 |

## 运行态指标和调用链要求

| 领域 | 必要信号 |
|---|---|
| 协作规模 | 父 Task 数量、子 Task 数量、每个父 Task 的平均子 Task 数。 |
| 延迟 | 子 Task dispatch 延迟、子 Task 执行延迟、join 等待时长、federation 延迟。 |
| 可靠性 | 子 Task 失败率、join 超时、部分失败、resume 失败。 |
| 成本 | LLM 调用成本由平台统一按 parent / child Task 聚合；客户内部工具成本不由平台定价，只记录 usage reference / trace。 |
| 可追踪性 | 能按 tenant、traceId、parent taskId 重建完整 Task tree。 |

## 涉及契约

- ingress-envelope
- federation-envelope
- 涉及 client callback 时使用 s2c-callback
- ICD-Workflow-AgentService
- ICD-Workflow-Observability
- ICD-CS-Capability-Placement

## 技术子场景

- [S1 Create Task / Invocation](technical/S1-create-run.md)
- [S2 Execute Agent Step](technical/S2-execute-agent-step.md)
- [S5 Suspend / Resume](technical/S5-suspend-resume.md)
- [S6 Child Task / Federation](technical/S6-child-run-federation.md)

## 断言

```yaml
scenario: BA-003-MultiAgentDelegation
assertions:
  - child_task_has_same_tenant_as_parent: true
  - child_task_visible_under_parent_task_tree: true
  - same_service_multi_agent_collaboration_closed_by_agent_service: true
  - cross_boundary_a2a_uses_agent_bus: true
  - parent_wait_uses_suspend_or_equivalent_non_holding_wait: true
  - child_agent_does_not_write_parent_terminal_state_directly: true
  - trace_links_parent_and_child: true
  - cross_workflow_handoff_requires_explicit_contract: true
  - developer_can_reconstruct_parent_child_timeline: true
  - runtime_metrics_group_cost_and_latency_by_task_tree: true
```

## 模块能力缺口检查

| 检查项 | 结果 |
|---|---|
| 子 Task 创建机制是否有技术子场景？ | S6 draft 覆盖。 |
| federation runtime binding 是否可用？ | 主要仍为 `design_only`。 |
| Task tree 重建是否有 golden trace 覆盖？ | Draft；需要 BA harness。 |
| 集成开发者能否调试委派决策？ | Open Issue；需要 child-run timeline evidence。 |
| 运维能否按 Task tree 聚合成本和延迟？ | Open Issue；需要 metrics dimension governance。 |

## 模块边界适配性检查

| 边界 | 适配信号 |
|---|---|
| 父 Orchestrator vs 子 Task owner | 健康信号：父侧只创建委派意图，`agent-service` 拥有子 Task identity 和状态写入。 |
| Bus vs execution engine | 健康信号：bus 只传输 envelope，不执行 Agent 逻辑。 |

## Harness 生成说明

- 构造 fake child-task executor。
- 构造 fake bus federation envelope。
- 构造跨部门 A2A control command，经 `agent-bus` 传递。
- 断言父子 Task 的 tenant 一致。
- 断言同一 service 内协作不经过 Bus，跨边界协作必须经过 Bus。
- 断言父 Task 的 terminal transition 依赖子 Task terminal event，而不是由子 Agent 直接改写。
- 断言可以从 trace evidence 重建父子 Task tree。
- 断言成本和延迟可以按父 Task tree 聚合。

## 开放问题

| ID | 问题 | 为什么重要 | 影响范围 | 建议归宿 |
|---|---|---|---|---|
| OI-BA003-001 | S6 已补充 child-task / federation 技术子场景，但 runtime binding 和 wire schema 仍未正式化。 | BA-003 已经有机制级子场景验证 child Task 创建、join、federation envelope、partial failure 和 timeout；下一步需要把 S6 的断言落到机器可读 contract 和 harness。 | federation contract、Task tree、join semantics、failure injection、A2A wire schema。 | 细化 `technical/S6-child-run-federation.md` 对应的 A2A / Federation ICD，并回填 contract tests。 |
| OI-BA003-002 | 多个 contract 仍为 `design_only`。 | 多 Agent 委派依赖 child-task creation、federation envelope、Task tree reconstruction、cross-agent handoff 等契约；如果这些契约没有 runtime binding，BA-003 只能作为目标架构校准，不能声明已交付。 | federation-envelope、a2a-envelope、TaskEvent、agent-bus、agent-service、agent-execution-engine。 | L0/L1 标记为 draft；L2/L3 按 contract 优先级逐步落地 runtime binding 和 contract tests。 |
| OI-BA003-003 | 面向开发者的 Task tree 调试证据尚未正式化。 | 应用开发者需要理解父 Agent 为什么委派、子 Agent 收到了什么、join 在哪里发生、失败后谁负责重试或降级；没有统一证据，就无法调试复杂 Agent 编排。 | developer debug evidence、Task timeline、parent / child correlation、trace replay、MCP replay。 | 扩展 Developer Experience / Observability ICD，定义 parent-child timeline、delegation decision evidence、merge evidence 查询语义。 |
| OI-BA003-004 | 多 Agent 指标需要 cardinality 和 tenant isolation 规则。 | 多 Agent 会天然增加 child Task、跨实例调用和 trace 维度；如果没有基数控制，会让 metrics 成本和 dashboard 可用性失控，同时可能暴露跨租户关联。 | metrics、trace、tenant isolation、cost allocation、SRE dashboard。 | 在 observability contract 中定义 Task tree metrics 的 mandatory dimensions、cardinality limit、tenant guard 和聚合粒度。 |
| OI-BA003-005 | 父子 Task 的成本和责任归属需要治理语义。 | 业务负责人关心一次业务请求的整体成本和质量，但平台底层会产生多个子 Task；如果成本、失败、取消和降级责任不清，运营和计费都会混乱。 | cost receipt、billing、quota、failure ownership、business degradation authority。 | 后续 ADR 或 governance contract 定义 parent Task cost aggregation、child Task quota attribution、failure ownership 和 degradation decision owner。 |
