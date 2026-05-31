---
affects_level: L0
affects_view: process
proposal_status: review
authors: ["Codex Java Microservices and Agent Architecture Review"]
related_adrs:
  - ADR-0019
  - ADR-0032
  - ADR-0052
  - ADR-0068
  - ADR-0072
  - ADR-0074
  - ADR-0078
  - ADR-0079
related_rules:
  - Rule 25
  - Rule 28
  - Rule 32
  - Rule 34
  - Rule 43
  - Rule 46
  - Rule 48
  - Rule 79
affects_artefact:
  - AGENTS.md
  - README.md
  - ARCHITECTURE.md
  - docs/contracts/contract-catalog.md
  - docs/contracts/s2c-callback.v1.yaml
  - docs/contracts/plan-projection.v1.yaml
  - docs/governance/architecture-status.yaml
  - docs/governance/architecture-graph.yaml
  - docs/adr/0032-scope-based-run-hierarchy-and-planner-contract-minimal.md
  - docs/adr/0074-s2c-capability-callback.yaml
  - agent-execution-engine/ARCHITECTURE.md
  - agent-execution-engine/pom.xml
  - gate/README.md
---

# L0 rc4 Cross-Constraint Architecture Review

## Executive decision

Do not publish a clean "L0 is complete" release note yet. The current runtime
architecture direction is sound: the engine envelope, hook surface, checked
suspension, skill capacity matrix, memory ownership boundary, and design-only
plan projection are the right L0 shapes and do not look over-designed at the
agent-runtime level.

The remaining risk is corpus integrity. Several authoritative or entrypoint
documents now contradict the current code and the newest ADRs. The gate still
passes, which means the defects are in the current governance blind spots, not
in basic build health.

Fresh verification evidence from this review:

- `bash gate/test_architecture_sync_gate.sh` -> `Tests passed: 121/121`.
- `python gate/build_architecture_graph.py` -> `315 nodes, 433 edges`.
- `bash gate/check_architecture_sync.sh` -> `GATE: PASS`.

## Findings

### P0-1: ADR-0074 still specifies the deleted unchecked S2C signal

Root cause: the rc3 S2C refactor updated CLAUDE.md, Rule 46, Java code, and
some status ledgers, but did not update ADR-0074 or the S2C contract header
that remain accepted authority for the protocol.

Evidence:

- `CLAUDE.md:276` says the waiting Run must suspend through
  `SuspendSignal.forClientCallback(...)` and that the prior unchecked
  `S2cCallbackSignal` was deleted.
- `agent-runtime-core/src/main/java/ascend/springai/service/runtime/orchestration/spi/SuspendSignal.java:16`
  through `:33` documents the checked client-callback variant and preserves
  ADR-0019's checked-suspension doctrine.
- `agent-service/src/main/java/ascend/springai/service/runtime/orchestration/inmemory/SyncOrchestrator.java:88`
  through `:97` catches `SuspendSignal` and branches on `isClientCallback()`.
- `docs/adr/0074-s2c-capability-callback.yaml:21` through `:24` still says
  the protocol introduces a `RuntimeException` subtype named
  `S2cCallbackSignal`.
- `docs/adr/0074-s2c-capability-callback.yaml:75` through `:98`,
  `:126` through `:127`, and `:136` through `:145` still describe and defend
  the deleted unchecked design.
- `docs/contracts/s2c-callback.v1.yaml:17` through `:23` still says
  `SyncOrchestrator` catches `S2cCallbackSignal`.

Why this matters:

ADR-0074 is accepted and relates to ADR-0019. A new engine or S2C transport
author reading the ADR can legitimately implement the old unchecked exception
path, contradicting Rule 46 and the actual Java SPI. This is a direct
authority conflict, not a cosmetic stale comment.

Recommended remediation:

1. Amend ADR-0074 in place, or add an explicit superseding note in ADR-0074
   that the accepted implementation is now the checked
   `SuspendSignal.forClientCallback(...)` variant.
2. Rewrite the decision, consequences, alternatives, and verification blocks
   so `S2cCallbackSignal` appears only in a clearly historical paragraph.
3. Update `docs/contracts/s2c-callback.v1.yaml` to describe `SuspendSignal`,
   not the deleted signal.
4. Add a small gate rule that rejects `S2cCallbackSignal` in active ADR,
   contract, CLAUDE, README, and module-architecture files unless the same
   paragraph is explicitly marked historical.

### P0-2: AgentExecutionEngine extraction is complete in code but still described as a skeleton

Root cause: ADR-0079 completed the T2.B2 extraction through
`agent-runtime-core`, but the module maturity and root entrypoint prose from
the earlier materialization wave were not swept.

Evidence:

- `docs/adr/0079-engine-extraction-runtime-core.yaml:38` through `:64`
  records the chosen shared-core extraction and the completed move.
- `docs/adr/0079-engine-extraction-runtime-core.yaml:66` through `:75`
  says engine extraction completes and that the engine code now lives in
  `agent-execution-engine`.
- Actual files exist under `agent-execution-engine/src/main/java/`, including
  `ascend/springai/service/runtime/engine/EngineRegistry.java`,
  `EngineEnvelope.java`, and the `ascend/springai/engine/spi/*` interfaces.
- `agent-execution-engine/ARCHITECTURE.md:17` through `:23` says the module
  is a deliberately empty skeleton and that engine code stays elsewhere.
- `ARCHITECTURE.md:89` through `:98` says `agent-execution-engine` remains a
  skeleton with T2.B2 deferred.
- `ARCHITECTURE.md:131` through `:133` says `EngineRegistry`,
  `EngineEnvelope`, and `ExecutorAdapter` remain pending T2.B2.
- `README.md:30` through `:40` repeats that this is still a skeleton.
- `docs/governance/architecture-status.yaml:12` and `:20` say T2.B2 is
  deferred and that `agent-execution-engine` still awaits T2.B2.
- `agent-execution-engine/pom.xml:5` through `:7` says the code is still to be
  extracted, while the same POM already depends on `agent-runtime-core` and
  `agent-middleware` for the extracted engine implementation.

Why this matters:

This breaks team ownership and dependency-direction reasoning. A contributor
following the root L0 architecture will treat AgentExecutionEngine as an empty
workspace, while the reactor already builds it as the owner of the engine
surface. This also undermines Rule 31 and Rule 33 because module identity,
module architecture, and physical code no longer agree.

Recommended remediation:

1. Update `README.md`, root `ARCHITECTURE.md`,
   `agent-execution-engine/ARCHITECTURE.md`, and
   `agent-execution-engine/pom.xml` comments to the ADR-0079 state:
   "engine SPI plus EngineRegistry/EngineEnvelope extracted; reference
   in-memory executors remain in agent-service."
2. Update `docs/governance/architecture-status.yaml#repository_counts`.
   `skeleton_modules` should no longer count `agent-middleware` or
   `agent-execution-engine` as empty skeletons.
3. Add a data-driven gate rule: a module described as "empty skeleton" must
   contain no production Java type beyond `package-info.java` or placeholder
   SPI stubs explicitly waived by an ADR.

### P1-1: Canonical baseline counts still have multiple contradictory sources

Root cause: baseline values are embedded in prose fields and several markdown
entrypoints instead of a single structured machine-readable block that all
human-facing docs render from.

Evidence:

- `README.md:15` advertises rc4 as 35 active engineering rules, 64 active gate
  rules, 94 self-tests, 94 enforcer rows, and 306 Maven tests.
- `README.md:111` still says CLAUDE.md has 34 active engineering rules.
- `AGENTS.md:21` still carries a historical "34 active engineering rules"
  count inside the section whose stated purpose is to stop carrying counts.
- `docs/governance/architecture-status.yaml:89` advertises the current rc4
  counts for rules and tests, but still says the architecture graph is
  `249 graph nodes / 326 edges`.
- `docs/governance/architecture-graph.yaml:21` through `:22`, the graph build
  output, and `docs/releases/2026-05-18-beyond-sdd-review-response.en.md:24`
  all say the graph is `315 nodes / 433 edges`.
- `gate/README.md:3` says 66 active gate rules and 98 self-tests.
- `gate/README.md:18` through `:20` says 63 active gate rules and 92
  self-tests.
- `gate/README.md:51` says the canonical gate has 63 active rules, while
  `gate/README.md:68` says 98 self-test cases and the executable
  `gate/test_architecture_sync_gate.sh:43` declares `TOTAL=121`.

Why this matters:

The repository now has enough meta-governance that imprecise count vocabulary
becomes an architectural defect. A human or agent cannot tell whether "active
gate rules" means numbered rule scripts, top-level gate checks, active
engineering rules with gate enforcers, or the smaller release-baseline metric.
The full gate passes, so the current gate does not catch this drift.

Recommended remediation:

1. Add a structured `baseline_metrics:` block under
   `docs/governance/architecture-status.yaml#architecture_sync_gate` with
   first-class scalar fields for each metric.
2. Forbid raw baseline numbers in AGENTS.md entirely. Replace the historical
   "34 active" rationale with count-free wording such as "CLAUDE.md has
   evolved since the earlier subset."
3. Define separate names for:
   `active_engineering_rules`, `active_gate_checks`,
   `release_baseline_self_tests`, and `gate_executable_test_cases`.
   Then update README, gate/README, release notes, and architecture-status to
   use those exact names.
4. Add a gate rule that scans always-loaded entrypoints for stale numeric
   baseline claims, or generate the count paragraphs from the structured
   ledger.

### P1-2: `docs/contracts/contract-catalog.md` is no longer a contract catalog

Root cause: W2.x moved to schema-first domain contracts and module extraction,
but the contract catalog remained a pre-W2, hand-maintained snapshot while
still calling itself the single source of truth.

Evidence:

- `docs/contracts/contract-catalog.md:1` through `:4` calls the file the
  single source of truth and says it was last refreshed on 2026-05-13.
- `docs/contracts/contract-catalog.md:22` through `:32` lists exactly seven
  active SPI interfaces under the old `agent-runtime` module vocabulary.
- The current code includes additional public SPI surfaces such as
  `ascend.springai.engine.spi.ExecutorAdapter`,
  `ascend.springai.engine.spi.EngineHookSurface`,
  `ascend.springai.middleware.spi.RuntimeMiddleware`, and
  `ascend.springai.service.runtime.s2c.spi.S2cCallbackTransport`.
- `docs/contracts/contract-catalog.md:95` through `:103` still lists
  `agent-platform` and `agent-runtime` as active BoM artifacts, while the
  current reactor modules are declared in `pom.xml:34` through `:44`.
- The catalog omits the W2.x contract YAML surfaces that README now points to:
  `engine-envelope.v1.yaml`, `engine-hooks.v1.yaml`,
  `s2c-callback.v1.yaml`, and `plan-projection.v1.yaml`.

Why this matters:

This file is the first place a downstream integrator would look for public
contracts. It now under-reports the SPI surface, reports deleted modules, and
does not catalog the new schema-first domain contracts. That is a direct
Rule 25 architecture-text-truth risk.

Recommended remediation:

1. Either regenerate the contract catalog from `pom.xml`, module metadata,
   `*.spi` Java packages, and `docs/contracts/*.v1.yaml`, or demote this file
   from "single source of truth" to a historical snapshot.
2. Split the catalog into "Java SPI interfaces", "Java structural carriers",
   "YAML domain contracts", and "configuration contracts", each with a
   `status:` aligned to Rule 28's constraint-state taxonomy.
3. Update gate Rule 17 so it is data-driven. A hard-coded "7 known SPIs"
   assertion is now structurally obsolete.

### P1-3: PlanProjection timing conflicts with the planner ADR

Root cause: `plan-projection.v1.yaml` was added to bridge ADR-0032 and
ADR-0052, but ADR-0032's implementation timing was not amended to distinguish
"scheduler projection" from "full dynamic planner."

Evidence:

- `docs/contracts/plan-projection.v1.yaml:10` through `:14` says W2 promotes
  the projection to schema-shipped and W2.x.1 makes the orchestrator consult
  it before submitting steps to the SkillResourceMatrix.
- `docs/contracts/plan-projection.v1.yaml:60` through `:65` makes the W2
  trigger "first non-in-memory scheduler ships" and requires a Java
  `PlanProjection`, `PlanProjector` SPI, and `SkillResourceMatrix.admit(...)`.
- `docs/adr/0032-scope-based-run-hierarchy-and-planner-contract-minimal.md:78`
  through `:80` says no `PlanState` or `RunPlanRef` code ships until W4 when
  the planner subsystem is scheduled.
- `docs/adr/0032-scope-based-run-hierarchy-and-planner-contract-minimal.md:99`
  through `:101` warns the planner record shapes must not be treated as stable
  API before W4.
- `docs/governance/architecture-status.yaml:555` through `:561` keeps the
  full planner toolset in W4, while `:1075` says bidding-protocol Java types
  are deferred W2-W3.

Why this matters:

This is the most important agent-driven architecture gap left. The design
itself is good: a planner-to-scheduler projection is exactly the missing join
between dynamic planning and skill/resource arbitration. The problem is that
the corpus does not say whether W2 owns only the projection/admission SPI or
whether W4 owns the first planner binding. Teams could either overbuild the
full planner early or underbuild the scheduler projection.

Recommended remediation:

1. Add an ADR amendment or a new ADR for PlanProjection staging.
2. State explicitly: W2 owns `PlanProjection` only as a scheduler-admission
   contract when the first non-in-memory scheduler ships; W4 still owns the
   full dynamic planner toolset if that remains the intended roadmap.
3. Register `plan-projection.v1.yaml` in `contract-catalog.md` and in
   `architecture-status.yaml` with `status: design_only`,
   `runtime_enforced: false`, and the exact promotion trigger.
4. Add a gate rule that every design-only contract named under
   `docs/contracts/*.v1.yaml` appears in the catalog and has an explicit ADR
   authority that does not contradict its target wave.

## Overdesign assessment

The core L0 agent architecture is not over-designed in the runtime sense:

- EngineEnvelope is shallow and correctly avoids becoming a universal DSL.
- HookOutcome consumption is explicitly deferred, which prevents premature
  middleware semantics before real consumers exist.
- S2C uses a typed envelope and checked suspension, which is appropriate for a
  server-sovereign callback boundary.
- Skill capacity and PlanProjection are the right abstractions for joining
  dynamic planning with resource arbitration, provided the W2/W4 timing is
  clarified.
- Memory and knowledge ownership remains appropriately conservative:
  business ontology stays C-side by default, while S-side trajectory memory
  and delegated memory are explicitly scoped.

The overdesign risk is in the governance layer: too many human-written
entrypoints repeat counts and status labels that should be generated or kept
in a single structured ledger. That is why the gate can be green while the
corpus still tells different stories.

## Proposed closure criteria

Before publishing a clean L0 completion release note, the architecture team
should close these five items:

1. ADR-0074 and `s2c-callback.v1.yaml` no longer describe the deleted
   unchecked `S2cCallbackSignal` design as current.
2. AgentExecutionEngine status is consistent across README, root
   ARCHITECTURE.md, module architecture, POM comments, and
   architecture-status.
3. Baseline metrics are structured once and every entrypoint either renders
   them from that source or avoids numeric claims.
4. `contract-catalog.md` reflects current modules, SPI packages, and YAML
   domain contracts, or is explicitly demoted from single-source status.
5. PlanProjection staging is reconciled with ADR-0032 and ADR-0052 so W2
   scheduler work and W4 planner work are unambiguous.

After these are fixed, I would support an L0 release note that says the L0
architecture is complete for current scope, with W2/W3/W4 runtime promotions
clearly tracked as design-only or schema-shipped contracts rather than hidden
gaps.
