---
affects_level: L1
affects_view: logical
proposal_status: proposed
authors: ["Flash (Agent)", "LucioIT (Core Architect)"]
related_adrs: [ADR-0072, ADR-0079, ADR-0088, ADR-0100]
related_rules: [Rule-R-M, Rule-R-C.e]
affects_artefact: []
---

# Architecture Proposal: Resolution of Core Frictions between agent-execution-engine and agent-service (Wave 1.2)

> **Date:** 2026-05-22
> **Status:** Proposed
> **Affects:** L1 (Covers Logical, Process, Development, and Physical views)

## 1. Background

During the materialization of the L1 high-level design for the `agent-execution-engine` module, the development team identified three core architectural frictions between the agent service (`agent-service`) and the execution engine (`agent-execution-engine`) regarding control flow representation, statelessness boundaries, and physical deployment topologies (Mode-A and Mode-B). To preserve the architectural purity of the `spring-ai-ascend` platform and adhere to the L0 top-level design principles, this proposal is submitted to fundamentally resolve these frictions and unify the interaction contracts and physical boundaries of both modules at the L1 layer.

## 2. Scope Statement

This change primarily affects the **L1 Logical View and Development View**, and secondarily affects the **Process View and Physical View**.
- **L1 Logical & Process**: Redefines the ownership of Task-level states (Service) and Run-level states (Engine); completely abolishes exception-based suspension mechanisms and establishes a value-based suspension contract aligned with the A2A protocol.
- **L1 Development & Physical**: Clarifies that the `client -> service -> engine` invocation sequence remains a physical topology invariant across both Platform-Centric (Mode-A) and Business-Centric (Mode-B) deployment modes. The Engine always maintains its pure, stateless computation-chip positioning.

## 3. Root Cause / Strongest Interpretation (Rule 1)

1. **Observed failure / motivation**: The existing design attempts to use a checked exception (`SuspendSignal`) to represent asynchronous suspension and interruption of the execution engine, and suffers from a vague understanding of the Engine's state-management boundary and independence under the Business-Centric deployment mode (Mode-B).
2. **Execution path**: `com.huawei.ascend.engine.spi.ExecutorAdapter.execute(...)` throwing the checked exception `SuspendSignal`, propagating through the reactive stream.
3. **Root cause**: 
   - Suspension is a normal and expected business control flow branch. Treating it as an exception violates the architectural best practice of "exceptions should not be used for control flow" and forces normal flows into the reactive error channel (by wrapping them in `Mono.error()`), which pollutes telemetry and system health monitoring.
   - Misunderstanding of the "Business-Centric Mode" led to an attempt to introduce database/persistence dependencies inside the Engine module, violating its pure "stateless computation chip" nature.
4. **Evidence**: 
   - `com.huawei.ascend.engine.orchestration.spi.SuspendSignal` extending `Exception`;
   - Discrepancy between the interface definition in `agent-execution-engine/ARCHITECTURE.md` §5 and ADR-0100.

## 4. Proposed Change

### 4.1 Friction Resolution 1: Clarifying the "Service-Oriented Encapsulation" between Service and Engine
- **Statelessness Partitioning**:
  - **Engine (Execution Engine)**: Its statelessness is **passively stripped**. The Engine only handles micro-level **Run-level state transitions** within a Task. It is completely unaware of long-term state; all required inputs must be explicitly assembled into an `InjectedContext` and injected by the Service, and the Engine must immediately output a `StateDelta` and yield when blocked.
  - **Service (Agent Service)**: Its statelessness is **actively persisted** (similar to standard microservices). The Service manages **Task-level state control** and everything above it (Session and Memory), delegating to external middleware (Redis/Temporal/Postgres) for state serialization and elastic scaling.
- **Service-Oriented Encapsulation**: The relationship between `service` and `engine` is not a simple "Java interface-implementation" relationship, but a **service-oriented encapsulation and orchestration** relationship.

### 4.2 Friction Resolution 2: Abolishing "Exception-Based Suspension" and Establishing A2A Value-Based Suspension Contract
- **Eliminating Exception-Based Interruption**:
  - The checked exception `com.huawei.ascend.engine.orchestration.spi.SuspendSignal` (extending `Exception`) is no longer used for normal engine suspension/yield flow.
  - Suspension is a **normal and expected** control flow and must be returned as a **first-class value (Value-based)**.
- **Reactive Contract Refactoring**:
  - The core stateless engine contract (`StatelessEngineExecutor`) is refactored as follows:
    ```java
    package com.huawei.ascend.engine.spi;

    import reactor.core.publisher.Mono;

    public interface StatelessEngineExecutor {
        /**
         * Executes stateless computation reactively: inputs the task definition and projected context,
         * and outputs the state delta.
         * Normal completion and suspension are both returned as values via StateDelta;
         * no control-flow exceptions are thrown.
         */
        Mono<StateDelta> execute(TaskSpec task, InjectedContext ctx);
    }
    ```
- **A2A Strong-Typed Interrupt Alignment**:
  - A nullable `InterruptSignal` is embedded in `StateDelta`. If the Engine yields, it returns a non-null `InterruptSignal`; on completion, it returns `null`.
  - Interrupt signal types are strictly aligned with the **A2A protocol specification**:
    * `INPUT_REQUIRED`: User interaction/approval suspension.
    * `SUB_TASK_AWAIT`: Sub-agent spawning/external A2A peer collaboration.
    * `TOOL_EXECUTION`: Physical tool execution interception.
    * `DELAY_AWAIT`: Time-window/timer suspension.
    * `POLICY_APPROVAL`: Guardrails, budget limit, or security sandbox approval suspension.

### 4.3 Friction Resolution 3: Invariant Boundaries Across Deployment Loci
- **Module Invocation Invariance**: The single-direction invocation chain `client -> service -> engine` remains a **physical topology invariant**.
  - Across all deployment modes (Mode-A Platform-Centric / Mode-B Business-Centric), the functional boundaries of each module remain unchanged.
  - In Mode-B, the business application only integrates `client` on the dependency level, while `service` and `engine` are co-located in the business physical boundary (same JVM or local Pod).
  - The `client` can never bypass the `service` to call the `engine`, and the `engine` can never run naked without the `service`.
- **Pure Computation Chip Invariance**: Because the `service` is always present in the execution path, the `engine` does not need to integrate local persistence engines (such as `InMemoryCheckpointer` etc.) even in Mode-B. It always remains a pure, stateless computation chip.

## 5. Alternatives Considered

| Alternative | Why Rejected |
|---|---|
| **Option A**: Keep the checked exception `SuspendSignal` and throw it from the Engine. | 1. Destroys the reactive declarative chain of Project Reactor. Handling checked exceptions inside reactive lambda operators is extremely painful.<br>2. Confuses "control flow signals" with "runtime failures," leading to false positives in system health and telemetry monitoring. |
| **Option B**: Allow the Client to bypass the Service and directly call the local Engine in Mode-B. | 1. Causes the Engine to degrade by forcing it to implement complex state serialization, dehydration, and rehydration, violating its "pure computation chip" nature.<br>2. Violates L0 high-cohesion and low-coupling design principles, breaking the microservice governance boundary when localized. |

## 6. Verification Plan

- [x] Static compilation verification: The refactored `agent-execution-engine` compiles successfully with no purity violations introduced.
- [ ] Architecture guard tests: `SpiPurityGeneralizedArchTest` (Rule E48) and `RuntimeMustNotDependOnPlatformTest` remain 100% green.
- [ ] Integration tests:
  - [ ] Verify that `execute` returns a `StateDelta` with a non-null `interruptSignal` aligned with the A2A spec when intercepting an interrupt (e.g., `TOOL_EXECUTION`).
  - [ ] Verify that no tests rely on throwing exceptions to control suspension flows.

## 7. Rollout

- **Current Wave**: W1.2 / W2 (Immediate refactoring during the current wave)
- **Freeze Impact**: This proposal does not unfreeze L0 physical artifacts, but requires a synchronized lightweight code refactoring of the `agent-execution-engine` skeleton, SPI declarations, and `agent-service` adapters.

## 8. Self-Audit (Rule 9)

- Any unresolved ship-blocking issues? **No**. This proposal eliminates a potential P0-level reactive throughput degradation risk by abolishing exception-based control flow.

---

## Authority

- CLAUDE.md Rule 33 (Layered 4+1 Discipline)
- CLAUDE.md Rule 34 (Architecture-Graph Truth)
- ADR-0068 (Layered 4+1 Twin Sources of Truth)
