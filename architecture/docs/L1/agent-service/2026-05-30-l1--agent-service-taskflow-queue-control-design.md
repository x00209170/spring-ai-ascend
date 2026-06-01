# L1 Agent Service Taskflow Queue/Control Design

## 1. Purpose

This wave keeps the taskflow surface intentionally small. It defines:

- a local in-memory queue component backed by the JDK;
- a static queue factory method for the current in-memory backend;
- a JavaBean-style `Task` model;
- one compact L4 `TaskControlClient` API with a single L1-facing `runTask` entry and runtime mark* signals.

It does not define any new taskflow SPI in this wave. SPI remains reserved for "this module defines the interface, another provider implements it" extension points. The current taskflow surface is internal API and local component code.

Morning update, 2026-06-01:

- L1-facing task control remains a single `runTask(RunTaskCommand)` method.
- `RUN`, `RESUME_INPUT`, and `CANCEL` are carried by `TaskAction`, not by separate handler methods.
- No `RuntimeQueueGateway` is defined. Runtime reports state through the adapter back to L4 `TaskControlClient.mark*`; any runtime detail that must be queued is first returned to L4.
- The document now lives under the active `architecture/docs/L1/agent-service/` authority root after the repository's architecture-tree migration.

## 2. Package Layout

```text
agent-service/src/main/java/com/huawei/ascend/service/taskflow/
  control/
    Task.java
    TaskState.java
    WaitingReason.java
    TaskFailureCode.java
    api/
      TaskControlClient.java
  queue/
    TaskQueue.java
    InMemoryTaskQueue.java
    QueueFactory.java
```

## 3. Queue Component

`TaskQueue<T>` is a thin local abstraction over queue operations:

```java
public interface TaskQueue<T> {
    String queueId();
    boolean offer(T value);
    Optional<T> poll();
    Optional<T> peek();
    List<T> snapshot();
    int size();
}
```

The default implementation is `InMemoryTaskQueue<T>`, backed by
`java.util.concurrent.LinkedBlockingQueue`.

`QueueFactory` is a final utility class, not an SPI:

```java
public final class QueueFactory {
    public static <T> TaskQueue<T> inMemoryQueue(String queueId) { ... }
}
```

The queue does not own task state and does not inspect item payload type.
The queue also does not expose a runtime gateway. L5 runtime code must not publish to or consume from taskflow queues directly.

## 4. Task Model

`Task` is a mutable JavaBean-style model with a no-arg constructor and standard getters/setters. The first implementation wave keeps it simple so later persistence, serialization, and controller code can reuse the same shape.

Core fields:

- `tenantId`
- `sessionId`
- `taskId`
- `agentId`
- `state`
- `revision`
- `waitingReason`
- `failureCode`
- `detail`
- `createdAt`
- `updatedAt`

`TaskState` contains:

```text
CREATED, RUNNING, WAITING, PAUSED, CANCELLING, COMPLETED, FAILED, CANCELLED
```

No `QUEUED`, `WAITING_FOR_TOOL`, or `EXPIRED` state is defined in this wave.

## 5. Control API

`TaskControlClient` is the internal task-control API in this wave. It is not registered as SPI because the current implementation direction is not "external provider implements the contract"; L4 owns the control service and callers invoke it.

```java
public interface TaskControlClient {
    CompletionStage<TaskResult> runTask(RunTaskCommand command);
    CompletionStage<TaskResult> markRunning(MarkTaskCommand command);
    CompletionStage<TaskResult> markWaiting(MarkTaskCommand command);
    CompletionStage<TaskResult> markSucceeded(MarkTaskCommand command);
    CompletionStage<TaskResult> markFailed(MarkTaskCommand command);
    CompletionStage<TaskResult> markCancelled(MarkTaskCommand command);
}
```

`RunTaskCommand` carries a `TaskAction` enum:

```text
RUN, RESUME_INPUT, CANCEL
```

This keeps the L1 `TaskHandler` shape to one method. Future control intent can extend the enum and command fields without adding another L1-facing method.

Command/result records live inside `TaskControlClient`:

- `RunTaskCommand`
- `TaskAction`
- `MarkTaskCommand`
- `TaskResult`

This keeps the public interface count small while preserving typed command/result semantics for future controller implementation.

## 6. Runtime Alignment

PR #100's `EngineDispatchSpi` remains the runtime/engine dispatch reference.

This wave does not add a second runtime dispatch SPI. Future L5 runtime adapter code can translate `TaskControlClient.runTask` intent plus `TaskAction` into the engine dispatch surface and report state changes back through `TaskControlClient.mark*`.

## 7. White-Box Test Scope

The first test wave pins:

- `Task` behaves as a JavaBean and validates required identifiers;
- `QueueFactory.inMemoryQueue` returns a FIFO in-memory queue backed by JDK semantics;
- white-box tests live under `agent-service/src/test/java/com/huawei/ascend/service/taskflow/test`;
- `TaskControlClient` nested command records validate required fields and defensively copy metadata.

## 8. Deferred

- controller implementation;
- task state transition policy implementation;
- durable queue backend;
- queue manager / registry;
- runtime adapter bridge to `EngineDispatchSpi`;
- integration tests across access, session, taskflow, and runtime.
