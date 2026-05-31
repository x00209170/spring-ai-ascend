# 0032. Scope-Based Run Hierarchy and Planner Contract Minimal

**Status:** accepted
**Deciders:** architecture
**Date:** 2026-05-13
**Technical story:** Sixth reviewer (LucioIT L1) found no scope discriminator on `Run`; any nested
run is implicitly swarm-capable. Seventh reviewer (P1.4) found `AgentLoopDefinition.planningEnabled(true)`
claimed in three places but absent in code. Cluster 1 self-audit surfaced 8 hidden defects around
hierarchy vocabulary and planning contract. This ADR names the scope axis and the minimal planner
contract without shipping code at W0.

## Context

`Run` expresses hierarchy through `parentRunId` and `RunMode`, but has no explicit scope
discriminator. A run nested via `SuspendSignal` could be a step-local sub-task or a peer agent in
a swarm delegation — the intent is invisible from the record.

`AgentLoopDefinition.planningEnabled` was mentioned in three documentation locations but does not
exist in `ExecutorDefinition.java`. The planning capability has no minimal type contract to anchor
future design.

## Decision Drivers

- Sixth reviewer L1: hierarchy should express scope (STEP_LOCAL vs SWARM), not just mode (GRAPH vs AGENT_LOOP).
- Seventh reviewer P1.4: removing a false claim and naming the minimal planner contract prevents W2 surprise.
- Hidden defects 1.1–1.8: `AgentLoopDefinition` accumulates 3 future-breaking changes (typed payload, plan-state, scope discriminator) without a unified migration plan.
- Rule G-2 sub-clause .a (Architecture-Text Truth): claims in docs must reflect reality.

## Considered Options

1. **Name scope axis + minimal planner contract (design-only at W0)** — this decision.
2. **Add RunScope Java field at W0** — introduces a DB column before Postgres schema exists.
3. **Defer entirely** — leaves reviewer L1 unaddressed and planningEnabled claim dangling.

## Decision Outcome

**Chosen option:** Option 1.

### RunScope taxonomy (§4 #29)

```
RunScope {
  STEP_LOCAL — a sub-task dispatched by the parent within the same logical flow.
               Lifecycle is bound to the parent; termination propagates upward.
  SWARM      — a peer agent delegated a goal independently. Lifecycle is autonomous;
               the parent awaits a result signal but does not own the child's lifecycle.
}
```

Field addition deferred to W2 alongside Postgres schema revision. At W0 the taxonomy is
named in ADR and §4 only. `SuspendReason.SwarmDelegation` variant addition also deferred
(sealed interface `SuspendReason` does not exist at W0 as runnable code).

### Planner contract minimal (§4 #29 extension)

`PlanState` and `RunPlanRef` are named as design contracts for the planner subsystem:

```java
// Design-only (W4+). No production code at W0.

/** Represents the decomposed execution plan for a goal. */
public record PlanState(
    UUID planId,
    String goal,
    List<PlanStep> steps,  // ordered list of RunPlanRef entries
    PlanStatus status      // PENDING | IN_PROGRESS | COMPLETED | FAILED
) {}

/** A reference from a parent Run to a planned child Run. */
public record RunPlanRef(
    UUID parentRunId,
    UUID plannedRunId,
    String stepKey,
    RunScope scope  // STEP_LOCAL or SWARM
) {}
```

`AgentLoopDefinition.planningEnabled` claim is removed from all active documents.
No `PlanState` or `RunPlanRef` code ships at W0; first implementation binding at W4
when the planner subsystem is scheduled.

### PlanProjection staging note (2026-05-18 amendment per rc4 review P1-3)

The v2.0.0-rc4 cross-constraint architecture review (`docs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review.en.md` finding P1-3) called out that `docs/contracts/plan-projection.v1.yaml` and this ADR carry overlapping prose about when planner-related Java types ship, and a reader could not tell whether W2 or W4 owns first implementation. The two are NOT the same wave:

- **W2 owns `PlanProjection` ONLY as a scheduler-admission contract.** The contract `docs/contracts/plan-projection.v1.yaml#deferred_runtime_binding` ships its Java record (`PlanProjection`), the `PlanProjector` SPI, and `SkillResourceMatrix.admit(PlanProjection) → AdmissionDecision` when the first non-in-memory scheduler ships. A stub planner that emits projections is sufficient — the W2 scheduler does not require the full dynamic planner toolset to admit a step.
- **W4 still owns the full dynamic planner toolset.** `PlanState`, `RunPlanRef`, the planner DAG, and any code that decomposes a goal into steps ship at W4 per this ADR. The `RunScope` field addition to `Run` is also W4.

A planned step's projection (a snapshot of required skills, budget envelope, memory scope, estimated duration) is what the W2 scheduler consumes; the planner that produced it can be a stub at W2 and become a real subsystem at W4 without breaking the projection contract. This separation is what `plan-projection.v1.yaml` is for.

`PlanProjection` is therefore registered in `docs/governance/architecture-status.yaml#capabilities.plan_projection_contract` with `status: design_only`, `runtime_enforced: false`, and `promotion_trigger: "W2 — first non-in-memory scheduler ships"`.

### RunRepository.findRootRuns (shipped W0)

```java
/** Returns top-level runs for a tenant — runs with no parent (parentRunId == null). */
List<Run> findRootRuns(String tenantId);
```

Shipped in `RunRepository.java` + `InMemoryRunRegistry` (W0). Supports the scope
hierarchy principle by enabling callers to enumerate the root of each run tree.

### Consequences

**Positive:**
- Scope discriminator prevents W2/W3 schema decisions that conflate STEP_LOCAL and SWARM semantics.
- Planner contract naming prevents three more docs from accreting false claims about `planningEnabled`.
- `findRootRuns` ships now — small, low-risk, directly supports the hierarchy taxonomy.

**Negative:**
- `RunScope` field deferred to W2; `Run` record remains narrower than the named taxonomy until then.
- Two new record shapes (`PlanState`, `RunPlanRef`) are named but not implemented; must not be treated as stable API before W4.

## References

- Sixth reviewer L1: `docs/logs/reviews/2026-05-12-architecture-LucioIT-wave-1-request.en.md`
- Seventh reviewer P1.4: `docs/logs/reviews/2026-05-13-l0-architecture-readiness-agent-systems-review.en.md`
- ADR-0019: SuspendReason taxonomy (SwarmDelegation variant deferred here)
- ADR-0028: CausalPayloadEnvelope (initialContext + payload migration context)
- ADR-0039: Payload migration adapter strategy (Object → Payload → CausalPayloadEnvelope)
- `RunRepository.java`, `InMemoryRunRegistry.java` — findRootRuns shipped W0
- `architecture-status.yaml` rows: `scope_based_run_hierarchy`, `planner_contract_minimal`
