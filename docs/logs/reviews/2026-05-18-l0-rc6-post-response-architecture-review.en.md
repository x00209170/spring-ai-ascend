---
affects_level: L0
affects_view: process
proposal_status: review
authors: ["Codex architecture review"]
responds_to: docs/releases/2026-05-18-l0-rc6-post-response.en.md
related_adrs:
  - ADR-0026
  - ADR-0030
  - ADR-0044
  - ADR-0070
  - ADR-0074
  - ADR-0078
  - ADR-0079
  - ADR-0080
related_rules:
  - Rule 21
  - Rule 31
  - Rule 32
  - Rule 33
  - Rule 41
  - Rule 46
  - Rule 77
  - Rule 78
  - Rule 82
  - Rule 84
  - Rule 85
affects_artefact:
  - ARCHITECTURE.md
  - agent-service/ARCHITECTURE.md
  - agent-runtime-core/src/main/java/ascend/springai/service/runtime/orchestration/spi/SuspendSignal.java
  - agent-service/src/main/java/ascend/springai/service/runtime/resilience/spi/ResilienceContract.java
  - docs/adr/0030-skill-spi-lifecycle-resource-matrix.md
  - docs/adr/0044-spi-contract-precision-and-memory-metadata-reconciliation.md
  - docs/adr/0070-cursor-flow-and-skill-capacity-runtime.yaml
  - docs/adr/0080-resilience-contract-spi-package-alignment.yaml
  - docs/contracts/contract-catalog.md
  - docs/governance/architecture-status.yaml
  - docs/releases/2026-05-18-l0-rc6-post-response.en.md
  - gate/check_architecture_sync.sh
  - gate/test_architecture_sync_gate.sh
---

# L0 rc6 Post-Response Architecture Review

## Executive verdict

Do **not** publish a no-findings L0 completion release note yet.

The agent-runtime architecture is directionally sound: the split between
`agent-runtime-core`, `agent-service`, `agent-execution-engine`, middleware SPI,
memory SPI, S2C checked suspension, and skill-capacity arbitration is not
over-designed for L0. The remaining blockers are corpus-truth and gate-truth
blockers: active authority documents and gate checks still encode pre-ADR-0079
or pre-ADR-0080 paths while rc6 claims those moves are closed.

Strongest interpretation of the rc6 release is that the architecture team wanted
to close the rc5 findings without changing the runtime design shape. That goal is
valid. The root cause of the remaining release blocker is narrower: the rc6 wave
updated the resilience SPI package home, but did not update every authority
artifact and enforcer that reasons about the same package and module topology.
Concrete evidence: `gate/check_architecture_sync.sh:2442-2456` still searches
for skill-capacity SPI types in the pre-ADR-0080 parent resilience package, while
`agent-service/ARCHITECTURE.md:286-293` and
`docs/governance/architecture-status.yaml:327-335` correctly place those types
under `runtime.resilience.spi`.

## Verification performed

| Command | Result |
|---|---|
| `bash gate/check_architecture_sync.sh` | **FAIL**. Rule 54 still expects `SkillCapacityRegistry.java`, `SkillResolution.java`, and `SuspendReason.java` under the old parent resilience package; Rule 84 also catches a stale `agent-service/ARCHITECTURE.md` orchestration path. |
| `bash gate/check_parallel.sh` | **FAIL** with the same two rule families as the serial gate: Rule 54 stale resilience SPI package assumptions and Rule 84 stale `agent-service/ARCHITECTURE.md` path truth. |
| `bash gate/test_architecture_sync_gate.sh` | PASS, 138/138. This is a problem: the self-test corpus passes while the real repository fails, proving the Rule 54 positive fixture was not updated for ADR-0080. |
| `python gate/build_architecture_graph.py` | PASS, 335 nodes / 463 edges, no working-tree diff. |
| `.\mvnw.cmd clean verify` | PASS, full 10-project Maven reactor succeeds (parent plus 9 declared modules). Java/runtime tests are green; the blockers are architecture corpus and gate truth, not compile/runtime regressions. |

## Findings

### P0-1 - The canonical architecture gate fails after rc6

`docs/releases/2026-05-18-l0-rc6-post-response.en.md` claims the architecture
sync gate passed, but the current repository fails `bash gate/check_architecture_sync.sh`.

Evidence:

| Artifact | Evidence |
|---|---|
| `gate/check_architecture_sync.sh:2442-2456` | Rule 54 sets `_r54_main` to `agent-service/src/main/java/ascend/springai/service/runtime/resilience` and then requires `SkillCapacityRegistry.java`, `SkillResolution.java`, and `SuspendReason.java` directly under that directory. |
| Actual source tree | Those three types now live under `agent-service/src/main/java/ascend/springai/service/runtime/resilience/spi/`, which is correct after ADR-0080. |
| `agent-service/ARCHITECTURE.md:286-293` | The rc6 architecture doc states that `ResilienceContract`, `ResiliencePolicy`, `SkillResolution`, `SuspendReason`, and `SkillCapacityRegistry` live in the `.spi` package while implementations stay in the parent package. |
| `docs/governance/architecture-status.yaml:327-335` | The architecture ledger lists the same `.spi` paths. |
| `bash gate/test_architecture_sync_gate.sh` | 138/138 self-tests pass even though the real repo fails, because the Rule 54 fixture still models the pre-ADR-0080 layout. |

Impact:

The gate is the canonical L0 synchronization mechanism. A failing canonical gate
invalidates the rc6 "self-check passed" claim and blocks a no-findings release
note even if the Java code compiles.

Recommendation:

Update Rule 54 to model the two-package shape introduced by ADR-0080:

- `_r54_spi=agent-service/src/main/java/ascend/springai/service/runtime/resilience/spi`
  should contain `SkillCapacityRegistry.java`, `SkillResolution.java`,
  `SuspendReason.java`, `ResilienceContract.java`, and `ResiliencePolicy.java`.
- `_r54_impl=agent-service/src/main/java/ascend/springai/service/runtime/resilience`
  should contain `DefaultSkillResilienceContract.java` and the YAML-backed
  implementations.
- The grep for `resolve(String, String)` and `tryAcquire(...)` should remain on
  `DefaultSkillResilienceContract.java`, but imports should be allowed to point
  at `.resilience.spi.*`.
- Update `gate/test_architecture_sync_gate.sh` so the positive fixture uses the
  post-ADR-0080 `.spi` package. Add a negative fixture where the old parent
  package layout is present but the `.spi` package is absent.
- Re-run `bash gate/test_architecture_sync_gate.sh`,
  `bash gate/check_architecture_sync.sh`, and `bash gate/check_parallel.sh`.

### P0-2 - Root `ARCHITECTURE.md` still describes deleted modules as current

The root architecture document is still carrying an eight-module, pre-shared-core
view while the reactor, README, and architecture ledger all declare nine Maven
modules.

Evidence:

| Artifact | Evidence |
|---|---|
| `ARCHITECTURE.md:77-79` | Says "Eight-module post-Phase-C state" and "The reactor declares 8 modules today." |
| `pom.xml:34-44` | Declares 9 modules, including `agent-runtime-core`. |
| `README.md:29-30` | Correctly says the reactor ships 9 Maven modules and names `agent-runtime-core` as the shared kernel introduced by ADR-0079. |
| `docs/governance/architecture-status.yaml:13-19` | Canonical repository counts say `reactor_modules: 9` and `total_reactor_modules: 9`. |
| `ARCHITECTURE.md:140-193` | The tree still lists deleted `agent-platform/` and `agent-runtime/` directories as current module roots. |
| `ARCHITECTURE.md:205-224` | Dependency diagram and prose still describe `agent-platform`, `agent-runtime`, and `agent-runtime -> agent-platform` as the active topology. |
| `ARCHITECTURE.md:261-265`, `:427-430`, `:657`, `:800-812`, `:844-845` | Later constraints continue to use the old module names and old ArchUnit framing. |

Impact:

Root `ARCHITECTURE.md` is an authoritative L0 entrypoint. Leaving it stale means
new contributors, architects, and downstream teams will be taught the wrong module
ownership for runtime kernel, HTTP edge, engine, telemetry, tenant propagation, and
microservice boundaries.

Recommendation:

Refresh root `ARCHITECTURE.md` as part of rc7:

- Replace the "eight-module" section with the nine-module layout from
  `pom.xml`, `README.md`, and `architecture-status.yaml`.
- Add `agent-runtime-core` to the module table and tree.
- Remove current-tense `agent-platform/` and `agent-runtime/` trees; if the old
  names must remain, mark them explicitly as historical pre-ADR-0078 names.
- Rewrite the dependency-direction section around current Maven modules and
  current package boundaries: `service.platform.*`, `service.runtime.*`,
  `agent-runtime-core`, `agent-execution-engine`, and `agent-middleware`.
- Extend Rule 84 or add a companion rule so root `ARCHITECTURE.md` is scanned for
  active path claims and module-count phrases, not only `agent-*/ARCHITECTURE.md`.

### P0-3 - `agent-service/ARCHITECTURE.md` still assigns orchestration and run contracts to the wrong module

The rc6 response fixed the engine and S2C path claims, but the same file still
contains stale ownership for orchestration SPI and run contracts.

Evidence:

| Artifact | Evidence |
|---|---|
| `agent-service/ARCHITECTURE.md:257-261` / `:262` by gate output | Claims the cognitive runtime SPI contracts ship under `agent-service/src/main/java/ascend/springai/service/runtime/orchestration/spi/`. |
| Actual source tree | `Orchestrator`, `RunContext`, `SuspendSignal`, `Checkpointer`, `TraceContext`, and `ExecutorDefinition` live under `agent-runtime-core/src/main/java/ascend/springai/service/runtime/orchestration/spi/`. |
| Actual source tree | `GraphExecutor` and `AgentLoopExecutor` live under `agent-execution-engine/src/main/java/ascend/springai/engine/spi/`. |
| `agent-service/ARCHITECTURE.md:275-282` / `:281` by gate output | Claims `Run`, `RunStatus`, `RunMode`, `RunStateMachine`, and `RunRepository` live under `agent-service/src/main/java/ascend/springai/service/runtime/runs/`. |
| Actual source tree | `Run`, `RunStatus`, `RunMode`, and `RunStateMachine` live under `agent-runtime-core/.../runtime/runs/`; `RunRepository` lives under `agent-runtime-core/.../runtime/runs/spi/`. |
| `bash gate/check_architecture_sync.sh` | Fails Rule 84 for the stale orchestration path. |

Impact:

This is the same defect family as the rc5 P0-1 finding, just in a neighboring
section. It directly affects agent-driven design ownership: orchestration, run
state, engine execution, and reference implementation boundaries are central L0
contracts.

Recommendation:

Rewrite the `runtime / orchestration` and `runtime / runs` subsections:

- State that `agent-runtime-core` owns durable kernel types and SPI contracts.
- State that `agent-execution-engine` owns `GraphExecutor` and `AgentLoopExecutor`
  after ADR-0079.
- State that `agent-service` owns posture-gated in-memory reference adapters and
  Spring wiring, not the shared SPI package roots.
- Add a Rule 84 self-test that covers `runtime/orchestration/spi` and
  `runtime/runs` path claims, not only engine-path examples.

### P1-1 - ResilienceContract scope language conflicts across active authority documents

The runtime now exposes two distinct `ResilienceContract` surfaces:
single-argument operation routing and two-argument skill-capacity arbitration.
The contract catalog and older ADR text still describe only the pre-ADR-0070
operation-scoped future.

Evidence:

| Artifact | Evidence |
|---|---|
| `docs/contracts/contract-catalog.md:58` | Says `ResilienceContract` is operation-scoped, has no tenant parameter at W0, and evolves to `(tenantId, operationId)` at W2. |
| `docs/adr/0030-skill-spi-lifecycle-resource-matrix.md:223` | Says `ResilienceContract.resolve(operationId)` extends to `(tenantId, operationId)` at W2. |
| `docs/adr/0044-spi-contract-precision-and-memory-metadata-reconciliation.md:77` | Repeats the same "no tenant param at W0" and W2 `(tenantId, operationId)` claim. |
| `CLAUDE.md:239` | Active Rule 41 requires `ResilienceContract.resolve(tenant, skill)` to consult `skill-capacity.yaml`. |
| `docs/adr/0070-cursor-flow-and-skill-capacity-runtime.yaml:50-57` | Defines the active two-argument `resolve(tenant, skill)` capacity surface. |
| `agent-service/src/main/java/ascend/springai/service/runtime/resilience/spi/ResilienceContract.java:17-32` | Contains both `resolve(String operationId)` and default `resolve(String tenant, String skill)`. |
| `DefaultSkillResilienceContract.java:40-47` | Implements `resolve(String tenant, String skill)` with `SkillCapacityRegistry.tryAcquire(...)`. |

Impact:

The design itself is reasonable, but the terminology now conflates two axes:
operation policy routing and skill capacity admission. This can mislead W2
implementers into adding `(tenantId, operationId)` as the next evolution while
the active L0 rule already ships `(tenant, skill)` for skill arbitration.

Recommendation:

Amend the contract catalog and older ADR references instead of changing the Java
surface:

- Define `resolve(operationId)` as the operation-policy routing surface.
- Define `resolve(tenant, skill)` as the skill-capacity arbitration surface
  activated by ADR-0070 and governed by Rule 41.
- Mark the older `(tenantId, operationId)` wording in ADR-0030 / ADR-0044 as
  superseded or narrowed to operation-policy routing only.
- Add an explicit note that these two axes must not be collapsed into one
  overloaded semantic claim.

### P1-2 - The architecture status ledger still carries pre-Phase-C module claims as current allowed claims

The top-level repository counts in `architecture-status.yaml` are correct, but
some active `allowed_claim` prose still names old module boundaries.

Evidence:

| Artifact | Evidence |
|---|---|
| `docs/governance/architecture-status.yaml:13-19` | Correctly declares 9 total reactor modules. |
| `docs/governance/architecture-status.yaml:1391` | Still says "each of the 4 reactor modules" for Rule 31 module metadata. |
| `docs/governance/architecture-status.yaml:1409` | Still says `agent-runtime` declares 2 SPI packages and `agent-platform + agent-runtime` publish DFX docs. |
| `docs/governance/architecture-status.yaml:1054` | Describes the service layer as `agent-platform` HTTP edge plus `agent-runtime` cognitive runtime, rather than current `agent-service` subpackages plus `agent-runtime-core`. |

Impact:

Because `architecture-status.yaml` is the canonical status ledger, active
allowed claims should not carry old module names unless they are explicitly
historical. This is a lower severity than P0-2 because the structured counts are
correct, but it still weakens L0's "single source of truth" claim.

Recommendation:

Sweep `allowed_claim` text for current-tense `agent-platform` / `agent-runtime`
module claims. Convert them to the current module names or mark them explicitly
as historical pre-ADR-0078 / pre-ADR-0079 context. Add a gate check that compares
current-tense module names in active status claims with `repository_counts` and
root `pom.xml`.

### P2-1 - A stale S2C package name remains in `SuspendSignal` Javadoc

Evidence:

| Artifact | Evidence |
|---|---|
| `agent-runtime-core/src/main/java/ascend/springai/service/runtime/orchestration/spi/SuspendSignal.java:21` | Correctly names `ascend.springai.service.runtime.s2c.spi.S2cCallbackEnvelope`. |
| `SuspendSignal.java:43-44` | The field comment still says the actual type is `ascend.springai.service.runtime.s2c.S2cCallbackEnvelope`, missing `.spi`. |
| `SuspendSignal.java:62` | Correctly names the `.spi` package again. |

Impact:

This is not a design blocker by itself, but it is a small contract-truth defect
inside an SPI type that reviewers and IDE hovers will see.

Recommendation:

Change the field comment at `SuspendSignal.java:43-44` to
`ascend.springai.service.runtime.s2c.spi.S2cCallbackEnvelope`.

## Agent architecture and overdesign assessment

No additional overdesign issue was found in the agent-driven architecture shape.

| Area | Assessment |
|---|---|
| Dynamic planning | The current PlanProjection / scheduler-admission posture is appropriately design-only at L0 and avoids shipping a premature full planner. Keep the W2 scheduler projection vs W4 planner distinction explicit. |
| Skills and capacity | Skill-capacity arbitration is a legitimate L0 primitive, not overdesign. The issue is vocabulary drift around `ResilienceContract`, not the abstraction itself. |
| Memory and knowledge | `GraphMemoryRepository` as an SPI shell with no W0 adapter remains an acceptable L0 contract boundary as long as docs keep adapter/runtime claims deferred. |
| Engine execution | Extracting engine SPI plus registry/envelope into `agent-execution-engine` and shared kernel types into `agent-runtime-core` is a reasonable way to break the back-dependency. The remaining problem is stale authority prose. |
| Microservice boundary | Six team-facing modules plus BoM, shared runtime core, and graphmemory starter is not excessive for the stated L0 ownership model. It does, however, require stronger corpus gates because duplicate module names across docs are now the main failure mode. |

## Proposed rc7 acceptance criteria

1. `bash gate/check_architecture_sync.sh` passes on a clean checkout.
2. `bash gate/test_architecture_sync_gate.sh` still passes, with Rule 54 fixtures updated to ADR-0080 `.spi` package truth.
3. `bash gate/check_parallel.sh` passes.
4. `.\mvnw.cmd clean verify` remains green.
5. Root `ARCHITECTURE.md`, `README.md`, root `pom.xml`, and
   `architecture-status.yaml#repository_counts` all agree on 9 Maven modules.
6. `agent-service/ARCHITECTURE.md` no longer claims that orchestration SPI or run
   contracts live under `agent-service` when they are owned by `agent-runtime-core`
   or `agent-execution-engine`.
7. `contract-catalog.md`, ADR-0030, and ADR-0044 explicitly reconcile
   `resolve(operationId)` with `resolve(tenant, skill)`.
8. Current-tense `agent-platform` / `agent-runtime` module claims are either
   rewritten to current modules or marked historical.
9. `SuspendSignal.java` has no stale non-`.spi` S2C envelope comment.

## Release-note recommendation

The next release note should be an rc7 corrective note, not an L0 completion
note. Suggested one-liner:

> v2.0.0-rc7 closes the rc6 post-response architecture-truth gaps by aligning
> the canonical gate, root architecture entrypoint, module architecture docs,
> and ResilienceContract scope language with the post-ADR-0079 / ADR-0080 module
> and SPI topology.
