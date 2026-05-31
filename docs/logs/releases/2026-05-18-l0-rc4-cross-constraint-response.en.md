---
release_tag: v2.0.0-rc5
release_date: 2026-05-18
release_type: additive_uplift
supersedes_tag: v2.0.0-rc4
retracts_tag: null
authority: docs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review.en.md
response_doc: docs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review-response.en.md
---

# v2.0.0-rc5 — rc4 cross-constraint review response (2026-05-18)

> **Historical artifact frozen at SHA 604ec3a** — this release note documents the state at v2.0.0-rc5 (2026-05-18). Subsequent state lives in `docs/releases/2026-05-18-l0-rc6-post-response.en.md` (v2.0.0-rc6) which supersedes this note for current-baseline claims. rc5 is NOT retracted; this note is preserved as the canonical rc5 snapshot.

## Baseline counts (post-rc5)

| metric | count |
|---|---|
| §4 constraints | 65 |
| Active ADRs | 79 |
| Layer-0 governing principles | 13 |
| Active engineering rules | 39 |
| Active gate rules | 68 |
| Gate self-test cases | 129 |
| Enforcer rows | 98 |
| Maven tests GREEN (under `./mvnw verify`) | 306 |
| Architecture graph | 323 nodes / 445 edges |

Canonical structured single-source: [`docs/governance/architecture-status.yaml#architecture_sync_gate.baseline_metrics`](../../governance/architecture-status.yaml).

**Deltas vs `v2.0.0-rc4`** (additive uplift only — no rule retracted, no production Java changed):

- Active engineering rules **+4** — Rules 80, 81, 82, 83 (rc4 cross-constraint review response prevention wave)
- Active gate rules **+4** — `s2c_callback_signal_historical_only_in_authority`, `skeleton_module_has_no_production_java`, `baseline_metrics_single_source`, `design_only_contract_registered_in_catalog`
- Gate self-tests **+8** — 2 per Rule 80-83 (positive + negative)
- Enforcer rows **+4** — E113, E114, E115, E116
- Active ADRs **+2 (correction)** — rc4 prose advertised 77 ADRs but ADR-0078 (Phase C consolidation) and ADR-0079 (engine extraction) were already on disk; corrected to 79 at rc5
- Architecture graph **+8 nodes / +12 edges** — Rules 80-83 + cards + plan_projection_contract capability row + ADR-0074 amendment edges
- §4 constraints / Layer-0 principles / Maven tests — no change (no production Java in this wave)

## Summary

v2.0.0-rc5 is an **additive uplift** on rc4 that closes the five findings from the Codex L0 cross-constraint architecture review (`docs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review.en.md`). The runtime architecture itself was judged sound by the review; this wave is corpus-integrity work plus four narrow prevention gates. **rc4 is NOT retracted.**

## Closure of the five findings

- **P0-1 (S2C authority drift)** — ADR-0074 + `docs/contracts/s2c-callback.v1.yaml` + `enforcers.yaml` E82 amended so the deleted `S2cCallbackSignal` Java type appears only in historical / rc3-unification / amendments paragraphs. The accepted implementation is now uniformly described as the checked `SuspendSignal.forClientCallback(...)` variant introduced at v2.0.0-rc3.
- **P0-2 (engine-extraction status drift)** — `agent-execution-engine` is now described as the ADR-0079 end-state ("engine SPI + EngineRegistry + EngineEnvelope extracted; reference adapters remain in `agent-service.runtime`") across `README.md`, root `ARCHITECTURE.md`, `agent-execution-engine/ARCHITECTURE.md`, `agent-execution-engine/pom.xml`, and `architecture-status.yaml`. `skeleton_modules` count dropped 5 → 3.
- **P1-1 (baseline count drift)** — New structured `baseline_metrics:` block landed under `architecture_sync_gate:` in `architecture-status.yaml` with 10 named keys (`active_engineering_rules`, `active_gate_checks`, `gate_executable_test_cases`, `enforcer_rows`, `architecture_graph_nodes`, `architecture_graph_edges`, etc.). `README.md`, `AGENTS.md`, and `gate/README.md` now point to this block instead of carrying their own copies.
- **P1-2 (contract catalog staleness)** — `docs/contracts/contract-catalog.md` regenerated. §2 SPI table regrouped by current module (post-ADR-0078 / ADR-0079) listing 11 SPI interfaces with actual packages. New §3 lists the 5 schema-first YAML domain contracts. §7 BoM table reflects the current reactor.
- **P1-3 (PlanProjection W2/W4 staging ambiguity)** — ADR-0032 amended with a new `### PlanProjection staging note (2026-05-18 amendment per rc4 review P1-3)` section. New `plan_projection_contract` capability row added to `architecture-status.yaml` with `status: design_only`, `runtime_enforced: false`, `promotion_trigger: "W2 — first non-in-memory scheduler ships"`. `plan-projection.v1.yaml` now listed in the contract catalog.

## New prevention gates

| Rule | Slug | Closes | Enforcer |
|---|---|---|---|
| 80 | `s2c_callback_signal_historical_only_in_authority` | P0-1 prevention | E113 |
| 81 | `skeleton_module_has_no_production_java` | P0-2 prevention | E114 |
| 82 | `baseline_metrics_single_source` | P1-1 prevention | E115 |
| 83 | `design_only_contract_registered_in_catalog` | P1-3 prevention | E116 |

Each rule lands as one inline section in `gate/check_architecture_sync.sh` with one positive + one negative self-test in `gate/test_architecture_sync_gate.sh` (8 new self-tests total). Cards: `docs/governance/rules/rule-80.md` … `rule-83.md` (`kernel:` byte-matches `CLAUDE.md` per Rule 68).

## Architecture baseline (post-rc5)

Canonical structured baseline lives in [`docs/governance/architecture-status.yaml#architecture_sync_gate.baseline_metrics`](../../governance/architecture-status.yaml). Headline numbers:

- 39 active engineering rules (rc4 baseline 35 + rc5 wave +4 Rules 80-83).
- 13 Layer-0 governing principles (P-A..P-M, unchanged).
- 68 active gate rules (rc4 baseline 64 + rc5 wave +4).
- 129 gate self-tests (rc4 baseline 121 + rc5 wave +8).
- 98 enforcer rows (rc4 baseline 94 + rc5 wave +4 E113-E116).
- 65 §4 ARCHITECTURE.md constraints (unchanged).
- 79 ADRs (rc4 baseline; ADR-0078 + ADR-0079 already on disk, count corrected at rc5).
- 306 Maven tests GREEN under `./mvnw verify` (no production Java changes this wave).
- 315 architecture-graph nodes / 433 edges (rc4 verified; regenerates idempotently after this wave with new rule + contract nodes).

## Four competitive pillars (P-B baselines)

This wave preserves all four pillar baselines unchanged. Pillar names as declared in `docs/governance/competitive-baselines.yaml`:

- **performance** — no runtime code changes; baseline preserved.
- **cost** — no per-call cost changes; BoM unchanged.
- **developer_onboarding** — quickstart preserved; contract catalog regenerated (improves time-to-first-integration discoverability).
- **governance** — +4 prevention gates (Rules 80-83); +8 self-tests; structured `baseline_metrics:` block now the single source.

See [`docs/governance/competitive-baselines.yaml`](../../governance/competitive-baselines.yaml) for the named baselines (P-B / Rule 30).

## What did NOT change

- No runtime Java code changes. No SPI signature changes. No new Maven modules.
- ADR-0074's accepted decision is preserved (prose amended to match rc3 implementation).
- ADR-0032's W4 planner ownership is preserved (prose amended to make W2 scheduler-admission carve-out explicit).
- rc4 is NOT retracted; rc5 is purely additive.

## Verification commands

```bash
bash gate/test_architecture_sync_gate.sh   # expect: Tests passed: 129/129
bash gate/check_architecture_sync.sh       # expect: GATE: PASS (68 active rules)
python gate/build_architecture_graph.py    # expect: idempotent regeneration; node/edge count adjusts for new rules + contracts
./mvnw -T 1C verify                         # expect: BUILD SUCCESS (306+ tests GREEN; no regression)
```

## Trajectory

- L1 release Phase L per ADR-0060.
- Telemetry Vertical L1.x per ADR-0061 / 0062 / 0063.
- Layer-0 governing principles P-A..P-D per ADR-0064 / 0065 / 0066 / 0067; P-E..P-L per ADR-0069; P-M per ADR-0071.
- W1 Layered 4+1 + Architecture Graph + Phase M per ADR-0068.
- W1.x Phase 1 L0 ironclad rules per ADR-0069.
- W1.x Phases 8+9 cursor flow + ResilienceContract per ADR-0070.
- W2.x Engine Contract Structural Wave per ADR-0071..0077.
- 2026-05-18 Phase C agent-service consolidation per ADR-0078.
- 2026-05-18 T2.B2 engine extraction with shared agent-runtime-core per ADR-0079.
- 2026-05-18 Beyond-SDD Telemetry-First Debugging response — Rule 79 + E112.
- **2026-05-18 rc4 cross-constraint review response (THIS WAVE)** — Rules 80-83 + E113-E116; structured `baseline_metrics:` block; ADR-0074 + ADR-0032 amendments.

## Authority chain

- Review: [`docs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review.en.md`](../reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review.en.md).
- Response: [`docs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review-response.en.md`](../reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review-response.en.md).
- Release note: this file.

## Retracted tags

`docs/governance/retracted-tags.txt` is unchanged. `v2.0.0-w2x-final` remains retracted from earlier waves. rc1–rc4 remain superseded but NOT retracted.
