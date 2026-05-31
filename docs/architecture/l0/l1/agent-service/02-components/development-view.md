---
level: L1
view: development
status: draft
---

# Agent Service Development View

## 目的

承接旧 L1 `development.md` 的包结构和层依赖信息，并按当前架构口径整理为可执行的开发约束。本文允许出现包名和实现区域，但不写具体方法签名或字段 schema。

## 当前代码包结构快照

以下目录来自当前 `agent-service/src/main/java/com/huawei/ascend/service`，用于帮助模块负责人定位实现区域。目录存在不等于架构边界；架构边界仍以模块责任卡和 logical design 为准。

```text
com.huawei.ascend.service
├── agent/
│   └── spi/
├── dispatcher/
├── engine/
│   ├── adapter/
│   └── spi/
├── integration/
│   └── springai/
├── orchestrator/
├── platform/
│   ├── auth/
│   ├── engine/
│   ├── idempotency/
│   ├── observability/
│   ├── persistence/
│   ├── posture/
│   ├── probe/
│   ├── resilience/
│   ├── tenant/
│   └── web/
│       └── runs/
├── runtime/
│   ├── evolution/
│   ├── idempotency/
│   ├── memory/
│   │   └── spi/
│   ├── orchestration/
│   │   └── inmemory/
│   ├── posture/
│   ├── probe/
│   ├── resilience/
│   │   └── spi/
│   ├── runs/
│   │   └── spi/
│   └── s2c/
├── session/
│   └── spi/
└── task/
    └── spi/
```

## L1 层与实现区域映射

| L1 Layer | 主要实现区域 | 当前解释 | 迁移 / 重命名约束 |
|---|---|---|---|
| AS-L1 Entry & Streaming Surface | `platform.web`, `platform.tenant`, `platform.auth`, `platform.idempotency`, `platform.observability` | `agent-service` 的 HTTP 对外入口、租户绑定、鉴权引用、幂等、trace 提取、SSE 入口。 | 文档中统一称“HTTP 对外入口”；不再写成独立 Gateway 模块。 |
| AS-L2 Task / Session / Idempotency Manager | `task`, `task.spi`, `session`, `session.spi`, `runtime.runs`, `runtime.runs.spi`, `runtime.idempotency` | Task / Session / 幂等和历史 Run 兼容实现区域。 | 新设计以 Task 为 canonical；`runtime.runs` 视为历史兼容区。 |
| AS-L3 Context & Placement Coordinator | `session.spi`, `integration.springai`, `runtime.memory`, `runtime.resilience` | 上下文投影、Spring AI 适配、memory / resilience 参考实现。 | Prompt / ModelGateway / Skill 全局语义归 `agent-middleware`；service 只做协调和 evidence。 |
| AS-L4 In-process Engine Control Adapter | `engine`, `engine.adapter`, `engine.spi`, `orchestrator`, `runtime.orchestration` | 同进程 engine 调用、orchestrator、adapter、checkpoint 参考实现。 | 不把 engine 写成远程 service；engine 不写 Task State。 |
| AS-L5 Cross-boundary Control Coordinator | `runtime.s2c`, `runtime.orchestration`, future A2A binding | S2C、resume、跨边界控制交互的 service 侧实现区域。 | Bus physical channel 不在 service；data channel 改写为 data reference。 |
| AS-L6 Evidence & Operations Surface | `platform.observability`, `runtime.evolution`, future evidence query | trace、metrics、audit、TaskEvent / evidence export。 | RunEvent 迁移为 TaskEvent / evidence event 兼容表达。 |

## 依赖规则

| Rule | 规则 | 原因 |
|---|---|---|
| DEV-R-001 | `platform.*` 可以调用受控的 runtime / task / session public surface；`runtime.*` 不得依赖 `platform.*` ThreadLocal tenant。 | 防止 runtime 依赖 HTTP 对外入口上下文。 |
| DEV-R-002 | engine adapter 只能返回状态转换意图、tool intent、context request、suspend-required、child-task intent 或 terminal response。 | 防止 engine 直接写 Task State。 |
| DEV-R-003 | `runtime.runs` 相关包只能作为历史兼容实现区，不得在新文档中恢复为独立 Run State owner。 | 保持 Task canonical。 |
| DEV-R-004 | `runtime.s2c` 只能对接 S2C / callback contract，不得承载大 payload 或逐 token stream。 | 保持 Bus 控制通道边界。 |
| DEV-R-005 | `integration.springai` 不拥有 RuntimeMiddleware / ModelGateway / Skill 的全局语义。 | 防止 service 抢占 middleware 边界。 |
| DEV-R-006 | 新增子包必须归入 AS-L1..AS-L6 或标记为 cross-cutting / implementation constraint。 | 防止包结构再次变成隐式架构。 |

## 旧开发视图的翻译

| 旧表述 | 当前表述 |
|---|---|
| Access Layer / HTTP 对外入口 | `agent-service` HTTP 对外入口 |
| Run aggregate owner | Task lifecycle owner；历史 Run 兼容实现 |
| Internal Event Queue | 内部异步意图分发待定，不作为已交付层 |
| Three-track bus data channel | data reference channel / envelope，不传大型 payload |
| External Runtime | 同进程 `agent-execution-engine` |
| RunEvent sealed hierarchy | TaskEvent / evidence event hierarchy；RunEvent 兼容命名 |

## L2 下沉边界

以下内容不在本 L1 直接定稿：

- 具体 Java 方法签名。
- `POST /tasks` 或兼容 `POST /runs` 的请求 / 响应字段。
- SSE event 的 machine-readable schema。
- A2A envelope 的 machine-readable schema。
- 数据库表、索引、RLS 迁移脚本。
- 具体 timeout、重试次数、队列 topic 或存储桶命名。
