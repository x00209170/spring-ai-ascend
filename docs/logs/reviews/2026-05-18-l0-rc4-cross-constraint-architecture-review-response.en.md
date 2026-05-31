---
affects_level: L0
affects_view: process
proposal_status: response
authors: ["spring-ai-ascend architecture team"]
responds_to: docs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review.en.md
related_adrs:
  - ADR-0032
  - ADR-0052
  - ADR-0072
  - ADR-0073
  - ADR-0074
  - ADR-0078
  - ADR-0079
related_rules:
  - Rule 25
  - Rule 28
  - Rule 31
  - Rule 32
  - Rule 33
  - Rule 46
  - Rule 79
  - Rule 80
  - Rule 81
  - Rule 82
  - Rule 83
affects_artefact: [ADR-0032, ADR-0074, ARCHITECTURE.md, AGENTS.md, README.md, CLAUDE.md, "docs/contracts/contract-catalog.md", "docs/contracts/s2c-callback.v1.yaml", "docs/governance/architecture-status.yaml", "docs/governance/enforcers.yaml", "docs/governance/principle-coverage.yaml", "docs/governance/rule-history.md", "docs/governance/rules/rule-80.md", "docs/governance/rules/rule-81.md", "docs/governance/rules/rule-82.md", "docs/governance/rules/rule-83.md", "agent-execution-engine/ARCHITECTURE.md", "agent-execution-engine/pom.xml", "gate/README.md", "gate/check_architecture_sync.sh", "gate/test_architecture_sync_gate.sh"]
---

# Response — L0 rc4 Cross-Constraint Architecture Review

## Executive decision

All five findings (P0-1, P0-2, P1-1, P1-2, P1-3) are **accepted in full**. **No findings are rejected.** The runtime architecture itself was judged sound by the review; remediation is corpus-integrity work plus four narrow prevention gates (Rules 80–83). The previous release tag `v2.0.0-rc4` is NOT retracted; this wave is an additive uplift shipping as `v2.0.0-rc5`.

## Verification of each finding

We replicated the review's evidence locally before drafting fixes. Each citation matched.

| Finding | Evidence confirmed locally | Verdict |
|---|---|---|
| **P0-1** ADR-0074 + `s2c-callback.v1.yaml` still describe the deleted `S2cCallbackSignal` design | ADR-0074 lines 21–31, 49, 70–77, 90–94, 117 named `S2cCallbackSignal` as the current ship. `s2c-callback.v1.yaml` line 22 said `SyncOrchestrator catches S2cCallbackSignal`. `enforcers.yaml` E82 `asserts:` line 722 said the same. CLAUDE.md Rule 46 body, `SuspendSignal.java:16-33`, `SyncOrchestrator.java:88-97`, and `agent-service/ARCHITECTURE.md:321-327` already correctly marked the rc3 refactor as historical — the three drifted authority files were missed at rc3. | **ACCEPT** |
| **P0-2** AgentExecutionEngine extraction complete in code but described as skeleton | `agent-execution-engine/src/main/java/ascend/springai/engine/spi/` already contains `AgentLoopExecutor`, `EngineHookSurface`, `EngineMatchingException`, `ExecutorAdapter`, `GraphExecutor` + `package-info`. `.../service/runtime/engine/` contains `EngineEnvelope`, `EngineRegistry`. ADR-0079 records the completed extraction. But module ARCHITECTURE.md frontmatter said `skeleton-receiving-extraction`, pom.xml comment said "Status: skeleton", root ARCHITECTURE.md table cell + tree comments said "skeleton pending T2.B2", README.md module table said "skeleton; code-extraction deferred", and `architecture-status.yaml#skeleton_modules` was `5` (counting `agent-middleware` and `agent-execution-engine` as skeletons). | **ACCEPT** |
| **P1-1** Baseline counts contradict across README / AGENTS / architecture-status / gate-README | README.md:15 advertised rc4 baseline `35 / 64 / 94 / 94 / 306`; README.md:111 still said `34 active engineering rules`; AGENTS.md:21 carried `34 active engineering rules` inside a section whose stated purpose is to NOT carry counts; architecture-status.yaml:89 said graph `249 / 326`; gate/README.md carried `66` (line 3), `63` (lines 18, 51), `92` (line 20), `98` (lines 53, 68) simultaneously. `bash gate/test_architecture_sync_gate.sh` returns `Tests passed: 121/121`; `python gate/build_architecture_graph.py` returns `315 nodes / 433 edges`. | **ACCEPT** |
| **P1-2** `contract-catalog.md` stale (pre-W2.x, pre-Phase-C names; missing new SPIs and YAML contracts) | `contract-catalog.md:1-4` claimed single-source-of-truth status, last refreshed 2026-05-13 (pre-W2.x). §2 SPI table listed 7 SPIs all under `agent-runtime` / `agent-platform` (Phase C consolidated those per ADR-0078; ADR-0079 added `agent-runtime-core`). §6 BoM listed `agent-platform` + `agent-runtime` as active. All W2.x YAML contracts (`engine-envelope.v1.yaml`, `engine-hooks.v1.yaml`, `s2c-callback.v1.yaml`, `plan-projection.v1.yaml`, `evolution-scope.v1.yaml`) were absent. | **ACCEPT** |
| **P1-3** PlanProjection W2 timing collides with ADR-0032 W4 planner timing | `plan-projection.v1.yaml:60-65` declared W2 trigger "first non-in-memory scheduler ships" + Java `PlanProjection` + `PlanProjector` SPI + `SkillResourceMatrix.admit(...)`. ADR-0032 lines 78-80 said no `PlanState` / `RunPlanRef` code ships until W4. Without an explicit "scheduler projection ≠ full planner" amendment, the corpus could be read either way. | **ACCEPT** |

## Defect family taxonomy

We classified the five findings into two families to make recurrence easier to spot:

- **D-α — authority-vs-code drift after a refactor.** The code moved (rc3 S2C unification; ADR-0079 engine extraction), but accepted ADRs / contract YAMLs / module status prose lagged behind. Manifestations: P0-1, P0-2. Prevention: Rules 80 (S2C historical-only) + 81 (skeleton-module-no-prod-Java).
- **D-β — entrypoint count drift + catalog staleness.** Numeric baselines and module/SPI lists were duplicated across README, AGENTS.md, gate/README.md, contract-catalog.md, and architecture-status.yaml without a single structured source. Manifestations: P1-1, P1-2, P1-3. Prevention: Rules 82 (baseline-metrics-single-source) + 83 (design-only-contract-registered) + the introduction of the structured `baseline_metrics:` block under `architecture_sync_gate:`.

## Closure mapping to the review's 5 closure criteria

| Criterion (review §"Proposed closure criteria") | Closure artefact |
|---|---|
| 1. ADR-0074 + `s2c-callback.v1.yaml` no longer describe the deleted unchecked `S2cCallbackSignal` as current. | ADR-0074 now carries `last_amended: 2026-05-18` + a top-level `amendments:` block. `decision_one_liner`, `triggering_inputs.internal_signal`, `decision`, `consequences`, `alternatives_considered`, and `verification.manual` rewritten so `S2cCallbackSignal` appears only in historical / 2026-05-16 / rc3-unification / 2026-05-18-amendment / deletion paragraphs. `s2c-callback.v1.yaml` status comment now says `SyncOrchestrator catches SuspendSignal where isClientCallback()`. `enforcers.yaml` E82 `asserts:` says the same. Gate Rule 80 (`s2c_callback_signal_historical_only_in_authority`, enforcer E113) prevents recurrence. |
| 2. AgentExecutionEngine status consistent across README, root ARCHITECTURE, module architecture, POM comments, architecture-status. | `agent-execution-engine/ARCHITECTURE.md` frontmatter `status: extracted-spi-and-registry`; `## Status` section names both extracted package roots (`ascend.springai.engine.spi.*` + `ascend.springai.service.runtime.engine.*`) and explains why reference adapters stay in `agent-service.runtime`. `agent-execution-engine/pom.xml` comment block rewritten. Root `ARCHITECTURE.md` module table cell + tree comments updated. README.md module table row updated; description paragraph rewritten ("six-team-facing-modules materialization (Phase C + engine extraction complete)"). `architecture-status.yaml#skeleton_modules` dropped from `5` to `3`. Gate Rule 81 (`skeleton_module_has_no_production_java`, enforcer E114) prevents recurrence. |
| 3. Baseline metrics structured once; every entrypoint either renders from that source or avoids numeric claims. | New `baseline_metrics:` block added under `architecture_sync_gate:` with 10 named keys (`active_engineering_rules`, `active_engineering_rules_post_rc5`, `active_governing_principles`, `active_gate_checks`, `gate_executable_test_cases`, `enforcer_rows`, `section_4_constraints`, `adr_count`, `maven_tests_green`, `architecture_graph_nodes`, `architecture_graph_edges`). README.md baseline paragraph rewritten to point to the block + summarise rc5 wave closures; line 111 + line 122 count claims replaced with pointer. AGENTS.md historical-rationale sentence replaced with count-free wording. gate/README.md reconciled to `64 active gate rules / 121 self-tests` with pointer. Gate Rule 82 (`baseline_metrics_single_source`, enforcer E115) prevents recurrence. |
| 4. `contract-catalog.md` reflects current modules, SPI packages, and YAML contracts, or is explicitly demoted. | Catalog regenerated 2026-05-18. §2 SPI table regrouped by current module (`agent-runtime-core`, `agent-service`, `agent-execution-engine`, `agent-middleware`, `spring-ai-ascend-graphmemory-starter`) listing 11 SPI interfaces with their actual packages. NEW §3 "YAML domain contracts" subsection lists all 5 schema-first contracts with their `status:` aligned to the Rule 28 / Rule 62 taxonomy. §7 Maven BoM table lists the post-ADR-0078 / ADR-0079 reactor (drops `agent-platform` + `agent-runtime`; adds `agent-runtime-core`). Last-refreshed header bumped to 2026-05-18 (post-rc4 cross-constraint review). |
| 5. PlanProjection staging reconciled with ADR-0032 and ADR-0052; W2 scheduler work and W4 planner work unambiguous. | ADR-0032 amended with a new section `### PlanProjection staging note (2026-05-18 amendment per rc4 review P1-3)` stating: W2 owns `PlanProjection` ONLY as the scheduler-admission contract when the first non-in-memory scheduler ships; W4 still owns the full dynamic planner toolset (`PlanState`, `RunPlanRef`, planner DAG, `RunScope` field). New `plan_projection_contract` capability row added to `architecture-status.yaml` with `status: design_only`, `runtime_enforced: false`, `promotion_trigger: "W2 — first non-in-memory scheduler ships"`. `plan-projection.v1.yaml` listed in `contract-catalog.md` §3. Gate Rule 83 (`design_only_contract_registered_in_catalog`, enforcer E116) prevents recurrence. |

## Hidden defects mined during the audit

Beyond the five named findings, we found and closed the following adjacent defects:

1. **`architecture-status.yaml#repository_counts.note`** still said "T2.B2 engine-code extraction deferred to follow-up PR per the back-dep snag documented at agent-execution-engine/ARCHITECTURE.md" — superseded by ADR-0079. Rewrote to "T2.B2 engine-code extraction COMPLETED at v2.0.0-rc5 (2026-05-18) per ADR-0079 — the back-dep cycle was resolved by introducing the shared agent-runtime-core module".
2. **README.md "Reading order" item 5** said `ADR-0001 … ADR-0077` — stale. Updated to `ADR-0001 … ADR-0079` to reflect ADR-0078 (Phase C) + ADR-0079 (engine extraction).
3. **`agent-execution-engine/ARCHITECTURE.md` H1 heading** carried "(skeleton, receiving extraction)" — would have contradicted the new Status section after the fix. Updated to "(SPI + registry + envelope extracted)".
4. **agent-execution-engine uses two different package roots** in production code: `ascend.springai.engine.spi.*` (SPI surface) and `ascend.springai.service.runtime.engine.*` (registry/envelope implementation). Documented explicitly in the module's `## Status` section and the POM comment so future contributors don't try to "unify" the namespaces without an ADR — the split is intentional (Rule 76 / 77 isolate the SPI surface from the implementation home).
5. **`evolution-scope.v1.yaml` lives in `docs/governance/`, not `docs/contracts/`.** The contract catalog now flags this explicitly so readers don't go looking in the wrong directory.
6. **`ResilienceContract` lives in `agent-service.runtime.resilience` (no `.spi` sub-package).** This is the one shipped SPI interface that does NOT live under a `.spi` package — a Rule 77 compliance edge that the new catalog flags. Out of scope for this wave (no architecture change to ship), but logged here for future audit attention.
7. **Gate self-test count was claimed as `92` / `94` / `98` in different docs.** The actual `bash gate/test_architecture_sync_gate.sh` count was `121/121` at rc4. The structured `baseline_metrics:` block now reflects `121` at rc4 baseline and `129` post-rc5 wave (+8 from Rules 80-83 self-tests).
8. **ADR count drift.** `architecture-status.yaml#allowed_claim` advertised `77 ADRs (0001-0077)` at rc4. Actual disk count is `0001…0079` (79 ADRs) — ADR-0078 (Phase C consolidation) and ADR-0079 (engine extraction with shared core) shipped during Phase C + Beyond-SDD but the `allowed_claim` numeric reference was not updated. The new `baseline_metrics.adr_count: 79` is the corrected canonical value.

## New gate rules (Rules 80-83)

| Rule | Slug | Enforcer | Self-tests |
|---|---|---|---|
| 80 | `s2c_callback_signal_historical_only_in_authority` | E113 | `test_rule80_s2c_callback_signal_historical_only_in_authority` (positive + negative) |
| 81 | `skeleton_module_has_no_production_java` | E114 | `test_rule81_skeleton_module_has_no_production_java` (positive + negative) |
| 82 | `baseline_metrics_single_source` | E115 | `test_rule82_baseline_metrics_single_source` (positive + negative) |
| 83 | `design_only_contract_registered_in_catalog` | E116 | `test_rule83_design_only_contract_registered_in_catalog` (positive + negative) |

Cards: `docs/governance/rules/rule-80.md` … `rule-83.md` (`kernel:` field byte-matches the corresponding `#### Rule NN` body in `CLAUDE.md` per Rule 68).

Principle mapping (per `docs/governance/principle-coverage.yaml`):

- Rule 80 → P-M (Heterogeneous Engine Contract & Server-Sovereign Boundary — S2C is part of P-M).
- Rule 81 → P-C (Code-as-Everything, Rapid Evolution, Independent Modules).
- Rule 82 → P-D (SPI-Aligned, DFX-Explicit, Spec-Driven, TCK-Tested — meta-governance application).
- Rule 83 → P-D (same — design-only contracts ARE the spec-driven surface).

## Verification (post-fix)

Expected post-fix gate state:

- `bash gate/test_architecture_sync_gate.sh` → `Tests passed: 129/129` (rc4 baseline `121` + rc5 wave +8 for Rules 80-83 pos/neg pairs).
- `bash gate/check_architecture_sync.sh` → `GATE: PASS` with 68 active gate rules (rc4 baseline `64` + rc5 wave +4).
- `python gate/build_architecture_graph.py` → regenerates idempotently; node/edge count adjusts to reflect the new rule + card + contract + capability-row nodes.
- `./mvnw -T 1C verify` → BUILD SUCCESS with 306+ Maven tests GREEN (no production Java changes in this wave; the bar is "no regression").
- `grep -nE 'S2cCallbackSignal' docs/adr/ docs/contracts/ CLAUDE.md README.md agent-*/ARCHITECTURE.md` → every remaining hit appears within ±5 lines of a `historical` / `deleted` / `refactored from` / `amendment` / `rc3-unification` marker. This is the literal Rule 80 invariant.

## What is NOT changed in this wave

- No runtime Java code changes. No SPI signature changes. No new modules.
- ADR-0074's accepted decision is preserved; we only amended the prose to match the rc3 implementation that already shipped.
- ADR-0032's W4 planner ownership is preserved; we only amended the prose to make the W2-scheduler-admission carve-out explicit.
- The Layer-0 governing principles P-A..P-M, the §4 constraints, and the existing engineering Rules 1–79 are untouched in scope. We added Rules 80-83 (prevention) and a `baseline_metrics:` block under `architecture_sync_gate:` (structured source).

## Open follow-ups (flagged for future review attention; not closed in this wave)

- `ResilienceContract` package convention vs Rule 77 — its current home is `agent-runtime-core/.../resilience` (no `.spi` sub-package). The module-metadata.yaml currently does not declare `...resilience` as an SPI package, so Rule 77 vacuously passes. If a future wave promotes ResilienceContract to a published SPI (e.g., tenant-aware variant at W2 per ADR-0030), the package home should move to a `.spi` sub-package.
- The gate-script header rule listing at lines 1–108 of `gate/check_architecture_sync.sh` is missing entries for Rules 72, 75, 76, 77, 78, 79 (only Rules 80-83 were added in this wave). A future cosmetic cleanup wave should backfill these without behaviour changes.

## Authority chain

- Review: [`docs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review.en.md`](2026-05-18-l0-rc4-cross-constraint-architecture-review.en.md).
- Response: this document.
- Release note: [`docs/releases/2026-05-18-l0-rc4-cross-constraint-response.en.md`](../releases/2026-05-18-l0-rc4-cross-constraint-response.en.md).
- Plan file: `D:\.claude\plans\d-chao-workspace-spring-ai-ascend-docs-vivid-hennessy.md`.
