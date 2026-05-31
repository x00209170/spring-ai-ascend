---
level: L1
view: process
status: draft
---

# Agent Service Process Design

## 目的

把旧 L1 `process.md` 的序列和失败流翻译为当前架构口径，用于生成可执行 harness。本文关注流程、状态推进、跨模块协作和失败语义，不写具体接口字段和方法签名。

## 流程索引

| Process ID | 名称 | 覆盖切片 | 对应场景 |
|---|---|---|---|
| P1 | 标准业务请求进入并创建 Task | AS-SLICE-001, 002 | BA-001, S1 |
| P2 | 同进程执行 Agent step | AS-SLICE-003, 004 | S2 |
| P3 | 需要本地能力或审批时 suspend / resume | AS-SLICE-005, 006 | BA-002, S5 |
| P4 | 同一 service 内多 Agent child Task / join | AS-SLICE-007 | BA-003, S6 |
| P5 | 跨边界 A2A 控制指令和 data reference | AS-SLICE-008 | BA-003, S6 |
| P6 | SSE 实时输出 | AS-SLICE-009 | BA-001 |
| P7 | cancel / terminal 竞争处理 | AS-SLICE-003, 011 | S1, S2, S5 |

## P1 标准业务请求进入并创建 Task

```mermaid
sequenceDiagram
    autonumber
    participant Client as agent-client / External Client
    participant Gateway as Microservice Gateway
    participant Service as agent-service HTTP 对外入口
    participant Idem as Idempotency Manager
    participant Task as TaskStateStore
    participant Obs as Observability

    Client->>Gateway: business request
    Gateway->>Service: proxy to agent-service
    Service->>Service: bind tenant / actor / auth reference / trace
    Service->>Idem: claimOrFind(tenant, idempotencyKey, requestHash)
    alt idempotency hit
        Idem-->>Service: existing task reference
        Service-->>Client: existing invocation reference
    else fresh request
        Idem-->>Service: claim accepted
        Service->>Task: create Task(Pending)
        Task-->>Obs: task.created evidence
        Service-->>Client: invocation reference / cursor
    end
```

### P1 断言

- Gateway 只代理到 `agent-service`，不写 Task State。
- 同 tenant + equivalent request + idempotency key 只创建一个 Task。
- tenant mismatch 不泄露其他租户 Task 信息。
- 创建失败不得产生半创建状态。

## P2 同进程执行 Agent step

```mermaid
sequenceDiagram
    autonumber
    participant Service as agent-service
    participant Task as TaskStateStore
    participant Engine as agent-execution-engine (in-process)
    participant Middleware as agent-middleware
    participant Obs as Observability

    Service->>Task: transition Pending -> Running
    Service->>Engine: invoke in-process with engine envelope
    Engine->>Engine: resolve executor / planner / adapter
    alt needs model / tool / memory / retriever
        Engine-->>Service: governed intent
        Service->>Middleware: call through middleware contract
        Middleware-->>Service: result + decision evidence
        Service->>Engine: continue with result
    end
    Engine-->>Service: step result / terminal response / suspend-required / child-task intent
    Service->>Task: controlled transition
    Service->>Obs: step evidence + task.state_transition
```

### P2 断言

- `agent-service` 与 engine 是同进程调用，不引入远程 service-to-engine 边界。
- engine 不直接写 Task State。
- middleware 调用经过 service / middleware contract，不由 engine 私自绕过治理。
- step result 必须产生结构化 evidence。

## P3 suspend / resume

```mermaid
sequenceDiagram
    autonumber
    participant Engine as agent-execution-engine
    participant Service as agent-service
    participant Task as TaskStateStore
    participant Bus as agent-bus
    participant Client as agent-client / business service / operator

    Engine-->>Service: suspend-required(reason, checkpointRef, expectedActor)
    Service->>Task: transition Running -> Suspended
    Service->>Bus: send control command / callback expectation
    Bus-->>Client: deliver S2C / approval / business-service request
    Client-->>Bus: resume event with result or data reference
    Bus-->>Service: resume event
    Service->>Service: revalidate tenant / actor / attempt / payload schema
    alt valid resume
        Service->>Task: transition Suspended -> Running
        Service->>Engine: resume in-process
    else invalid resume
        Service->>Task: transition Suspended -> Failed
    end
```

### P3 断言

- suspend 前必须有 checkpointRef 或等价 resume payload。
- resume 必须重新校验 tenant、actor、attempt 和 payload schema。
- Bus 只传控制和 data reference，不传大型 payload。
- duplicate resume 不重复副作用。

## P4 同一 service 内多 Agent child Task / join

```mermaid
sequenceDiagram
    autonumber
    participant Parent as Parent Agent step
    participant Service as agent-service
    participant Task as TaskStateStore
    participant Engine as agent-execution-engine

    Parent-->>Service: child-task intent
    Service->>Task: create child Task with parentTaskId
    Service->>Engine: dispatch child Task in-process
    Service->>Task: parent Running -> Suspended(await_child_task)
    Engine-->>Service: child terminal result
    Service->>Task: record child terminal + join evidence
    alt join policy satisfied
        Service->>Task: parent Suspended -> Running
        Service->>Engine: resume parent
    else join failed
        Service->>Task: parent terminal by failure policy
    end
```

### P4 断言

- 同一 `agent-service` 进程内多 Agent 协作不经过 Bus。
- child Task 必须有 parentTaskId、delegation reason、join policy。
- duplicate child completion 不重复合并。

## P5 跨边界 A2A 控制指令和 data reference

```mermaid
sequenceDiagram
    autonumber
    participant ServiceA as source agent-service
    participant Bus as agent-bus
    participant Store as external / customer storage
    participant ServiceB as target agent-service

    ServiceA->>ServiceA: create child Task relationship
    alt large or multimodal input
        ServiceA->>Store: write content
        Store-->>ServiceA: URI / object reference
    end
    ServiceA->>Bus: A2A control command + data reference
    Bus-->>ServiceB: deliver control command
    ServiceB->>Store: pull content by reference if needed
    ServiceB-->>Bus: completion / failure / timeout + result reference
    Bus-->>ServiceA: deliver controlled result
    ServiceA->>ServiceA: validate result and update Task tree
```

### P5 断言

- 跨 service / 跨部门 / 跨部署 A2A 控制指令必须走 Bus。
- Bus envelope 只包含控制语义、identity、trace、policy 和 data reference。
- 大型内容由需求方按授权直接从 data path 拉取。
- A2A 流式回传不默认走 Bus 逐 token 事件。

## P6 SSE 实时输出

```mermaid
sequenceDiagram
    autonumber
    participant Client as agent-client / UI
    participant Service as agent-service SSE endpoint
    participant Task as TaskStateStore
    participant Engine as agent-execution-engine

    Client->>Service: open SSE stream(task / invocation reference)
    Service->>Task: validate tenant and task visibility
    Engine-->>Service: partial result / step progress / terminal response
    Service-->>Client: SSE event with taskId / traceId / eventType
    alt terminal
        Service-->>Client: completion event
        Service-->>Client: close or keep-alive according to contract
    end
```

### P6 断言

- SSE event 必须关联 taskId、tenant scope 和 traceId。
- SSE 不写 Task State，只发布实时输出。
- Bus 不承载逐 token stream。

## P7 cancel / terminal 竞争处理

```mermaid
sequenceDiagram
    autonumber
    participant Client as cancel requester
    participant Service as agent-service
    participant Task as TaskStateStore
    participant Engine as concurrent terminal writer

    par cancel request
        Client->>Service: cancel task
        Service->>Task: atomic transition to Cancelled if not terminal
    and terminal result
        Engine-->>Service: terminal result
        Service->>Task: atomic transition to Succeeded / Failed if not terminal
    end
    Task-->>Service: winning terminal state
    Service->>Service: loser re-read terminal state
    Service-->>Client: idempotent success or illegal transition
```

### P7 断言

- cancel / complete 竞争只有一个获胜写入。
- 输掉竞争的一方必须重新读取 Task 终态。
- 同状态 terminal 请求可幂等成功；不同 terminal 返回 illegal transition。
- 所有路径都有 audit / evidence。

