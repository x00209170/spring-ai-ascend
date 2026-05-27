---
level: L1
view: modules
status: draft
---

# Agent Service Development Pack

## 目的

把 `agent-service` 从 L0 模块责任卡推进到可并行开发的 L1 模块设计包。本文档包不是复制 `docs/L1/agent-service/*`，而是按当前架构口径对旧设计做准入判定、改写和下沉，使模块负责人、AI agent、harness 生成器和评审者可以围绕同一套边界启动开发。

## 适用读者

`agent-service` 模块负责人、`agent-execution-engine` / `agent-bus` / `agent-middleware` 对接负责人、AI agent、harness 生成器、架构评审者。

## 维护规则

- 不修改 `docs/L1/**` 原始设计文档；旧文档只作为输入来源。
- 进入本文档包的内容必须先经过 [accepted-design-map.md](accepted-design-map.md) 准入判定。
- `Task` 是服务端 canonical 状态；历史 `Run` / `RunRepository` / `RunStateMachine` 只能作为实现兼容名或迁移备注。
- `agent-service` 与 `agent-execution-engine` 的目标运行形态是同进程组合；不得把 engine 写成默认远程服务。
- Gateway 是微服务入口能力，只代理 `agent-service`；Bus 只承载 S2C / callback / 跨边界 A2A 控制指令和 data reference envelope。
- 本包只写到能够启动模块开发、harness 和评审的 L1 深度；具体类、方法签名、表结构、topic、timeout 数值进入后续 L2 / implementation spec。

## 文档索引

| 文档 | 用途 |
|---|---|
| [4plus1-view.md](4plus1-view.md) | 以 4+1 视图组织本模块详细设计，提供关键图、约束和实现者阅读路径。 |
| [accepted-design-map.md](accepted-design-map.md) | 对旧 L1 设计项做准入判定，决定采纳、改写、下沉或转开放问题。 |
| [logical-design.md](logical-design.md) | 定义当前架构口径下 `agent-service` 的内部职责层、状态边界和跨模块协作。 |
| [state-model.md](state-model.md) | 定义 Task / Session / Idempotency / TaskTree / TaskEvent 语义，以及历史 Run 兼容映射。 |
| [process-design.md](process-design.md) | 定义 Task 创建、step 执行、suspend/resume、SSE、A2A 和 cancel race 流程。 |
| [development-view.md](development-view.md) | 管理包结构、层依赖、允许 / 禁止依赖和 L2 下沉边界。 |
| [development-slices.md](development-slices.md) | 将模块设计拆成可并行开发切片，给出输入、输出、依赖和 harness 断言。 |
| [harness-design.md](harness-design.md) | 细化 mocks、stubs、fixtures、failure injection、golden trace 和测试矩阵。 |
| [open-issues.md](open-issues.md) | 汇总不阻塞当前 L1 启动、但进入 L2 / 实现前必须澄清的问题。 |

## 上游和下游

| 类型 | 文件 |
|---|---|
| 上游 L0 / L1 约束 | `docs/architecture/00-overview/architecture-overview.md`, `docs/architecture/02-modules/module-responsibility-cards.md`, `docs/architecture/03-state/state-ownership-matrix.md` |
| 场景依据 | `docs/architecture/06-scenarios/BA-001-agent-handles-business-request.md`, `BA-002`, `BA-003`, `technical/S1`, `S2`, `S5`, `S6` |
| Harness 依据 | `docs/architecture/08-harness/agent-service-harness-spec.md` |
| 旧设计输入 | `docs/L1/agent-service/logical.md`, `process.md`, `physical.md`, `development.md`, `scenarios.md`, `spi-appendix.md` |

## 启动标准

`agent-service` 的模块开发可以启动，但每个开发切片必须满足：

- 能映射到一个 development slice。
- 能声明写入或读取的状态 owner。
- 能说明是否触发 `agent-execution-engine`、`agent-middleware`、`agent-bus` 或 `agent-client`。
- 能至少绑定一个 technical scenario 或 BA scenario。
- 能给出 harness assertion，而不是只给实现描述。
