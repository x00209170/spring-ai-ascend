---
release_tag: v2.0.0-rc6
release_date: 2026-05-18
release_type: additive_uplift
supersedes_tag: v2.0.0-rc5
retracts_tag: null
authority: docs/reviews/2026-05-18-l0-rc5-post-response-architecture-review.en.md
response_doc: docs/reviews/2026-05-18-l0-rc5-post-response-architecture-review-response.en.md
---

# v2.0.0-rc6 — rc5 post-response review response (2026-05-18)

> **Historical artifact frozen at SHA 78b436f** — superseded by v2.0.0-rc7 (`docs/releases/2026-05-18-l0-rc7-corrective.en.md`). Baseline counts below reflect the rc6 wave (80 ADRs, 70 gate rules, 138 self-tests, 41 active engineering rules); current canonical baseline is in `docs/governance/architecture-status.yaml#architecture_sync_gate.baseline_metrics`. This release is **not retracted** — the rc7 corrective wave is additive uplift only.

## Baseline counts (post-rc6)

| metric | count |
|---|---|
| §4 constraints | 65 |
| Active ADRs | 80 |
| Layer-0 governing principles | 13 |
| Active engineering rules | 41 |
| Active gate rules | 70 |
| Gate self-test cases | 138 |
| Enforcer rows | 100 |
| Maven tests GREEN (under `./mvnw verify`) | 371 |
| Architecture graph | 335 nodes / 463 edges |

Canonical structured single-source: [`docs/governance/architecture-status.yaml#architecture_sync_gate.baseline_metrics`](../../governance/architecture-status.yaml).

**Deltas vs `v2.0.0-rc5`** (additive uplift only — no rule retracted; ResilienceContract package rename is mechanical with zero behavioural change):

- Active engineering rules **+2** — Rules 84, 85 (rc5 post-response review response prevention wave).
- Active gate rules **+2** — `active_module_architecture_path_truth`, `catalog_spi_row_matches_module_spi_metadata`. Rule 82 is **strengthened**, not added — `baseline_metrics_single_source` now performs a numeric-agreement check on entrypoint count phrases (substring-pointer check is retained as the structural floor).
- Gate self-tests **+9** — 3 per Rule 84/85 (positive + negative + marker-exemption) + 3 for Rule 82 numeric-agreement strengthening.
- Enforcer rows **+2** — E117, E118. E115 `asserts:` widened in place.
- Active ADRs **+1** — ADR-0080 (`ResilienceContract .spi package alignment + Rule 85 prevention`).
- Architecture graph **+12 nodes / +18 edges** — Rules 84/85 + their cards + E117/E118 + ADR-0080 + ResilienceContract SPI source nodes + capability row updates.
- §4 constraints / Layer-0 principles — no change (no production Java behavioural change in this wave).
- Maven tests GREEN — count refreshed from 306 (rc5 baseline; stale, inherited from rc4) to **371** (277 surefire + 94 failsafe; the actual measured count at rc6 head). rc6 added 0 production Java behavioural changes — ResilienceContract package rename is mechanical, same test set with refreshed imports.

## Summary

v2.0.0-rc6 is an **additive uplift** on rc5 that closes the four findings from the Codex L0 rc5 post-response architecture review (`docs/reviews/2026-05-18-l0-rc5-post-response-architecture-review.en.md`). The runtime architecture (engine extraction, S2C checked suspension, PlanProjection L0 abstraction level, skill capacity / memory ownership) was judged net-positive by the review; this wave is corpus-truth + prevention work. **rc5 is NOT retracted.**

The reviewer's central observation — *"the gate must catch the exact drift class it claims to prevent"* — is closed by two structural changes: (a) Rule 82's vacuous substring-only check is upgraded to a numeric-agreement check (negative self-test for "pointer present but stale adjacent count" reproduces the reviewer's exact evidence and fails the gate); (b) Rule 85's catalog-vs-metadata cross-check turns a published-SPI commitment into a structural assertion (a future catalog row that claims SPI status without metadata backing fails the gate).

## Closure of the four findings

- **P0-1 (module-level authority drift after refactor)** — `agent-service/ARCHITECTURE.md` no longer claims engine or S2C SPI ownership that moved to `agent-execution-engine` / `agent-runtime-core` per ADR-0079. Path-convention header (around 44) extended with a post-ADR-0078 / ADR-0079 caveat. Engine section (around 304-317) rewritten as a consumer paragraph naming `agent-execution-engine` as the SPI + registry/envelope home; the "scheduled to move" note deleted. S2C section (around 319-327) rewritten — `S2cCallbackEnvelope` / `S2cCallbackTransport` / `S2cCallbackResponse` now correctly named at `agent-runtime-core/.../runtime/s2c/spi/` with `InMemoryS2cCallbackTransport` remaining at `agent-service`. §8 "Out of scope at L1" engine-extraction-deferred bullet deleted. §9.2 risk register "Engine code in transit" rewritten as a closure note citing ADR-0079. §10 dead `docs/STATE.md` link replaced with `docs/governance/architecture-status.yaml`. **Prevention:** Rule 84 (`active_module_architecture_path_truth`, enforcer E117).
- **P1-1 (Rule 82 vacuous baseline single-source)** — Rule 82 strengthened with a numeric-agreement check. The map of canonical count phrases (`active gate rules` → `active_gate_checks`, `self-tests` → `gate_executable_test_cases`, `enforcer rows` → `enforcer_rows`, `ADRs` → `adr_count`, `architecture-graph nodes` → `architecture_graph_nodes`, `architecture-graph edges` → `architecture_graph_edges`, plus the `Tests passed: N/N` pattern) is parsed from `architecture-status.yaml#baseline_metrics`. Active count claims outside fenced code blocks and outside historical-marker lines (`historical` / `rc[N] baseline` / `pre-rc[N]` / `previous` / `deprecated` / `superseded`) must match the baseline value. `gate/README.md` raw counts (lines 3, 18-20, 51, 53, 68) bumped to the post-rc6 baseline (70 / 138). Negative self-test `rule82_pointer_present_but_stale_count_neg` reproduces reviewer evidence exactly.
- **P1-2 (ResilienceContract dual classification)** — Reviewer's **preferred path** chosen: promote to governed SPI. ADR-0080 (`ResilienceContract .spi package alignment + Rule 85 prevention`) records the decision. Five source files (`ResilienceContract`, `ResiliencePolicy`, `SkillResolution`, `SuspendReason`, `SkillCapacityRegistry`) moved from `ascend.springai.service.runtime.resilience` to `ascend.springai.service.runtime.resilience.spi` — same split pattern as ADR-0079's `engine.spi.*` vs `service.runtime.engine.*`. Implementations stay in the parent package. `agent-service/module-metadata.yaml#spi_packages` and `docs/dfx/agent-service.yaml#spi_packages` both gain the new `resilience.spi` entry; `docs/contracts/contract-catalog.md` row updated to the new package; `agent-service/ARCHITECTURE.md` resilience section rewritten; `architecture-status.yaml#resilience_contract.implementation` lists SPI files + impl files separately. ~10 caller import updates across agent-service main + tests + a Javadoc FQN fix in `agent-runtime-core/.../S2cCallbackResponse.java`. **Prevention:** Rule 85 (`catalog_spi_row_matches_module_spi_metadata`, enforcer E118).
- **P2-1 (plan-projection.v1.yaml stale comment)** — Comment block (lines 3-26) rewritten. New text says: "DESIGN ONLY since v2.0.0-rc1. No Java type, no runtime self-validation ships at W1; the orchestrator does NOT consume the projection yet. Architecture-corpus enforcement (active since v2.0.0-rc5): Rule 83 (`design_only_contract_registered_in_catalog`, enforcer E116) asserts this contract appears in the catalog and cites at least one existing ADR. This is corpus-registration enforcement, NOT runtime self-validation. Runtime enforcement is deferred to the W2 scheduler wave per ADR-0032 (PlanProjection staging note, 2026-05-18 amendment)."

## New prevention gates + Rule 82 strengthening

| Rule | Slug | Closes | Enforcer |
|---|---|---|---|
| 82 (strengthened) | `baseline_metrics_single_source` | P1-1 substantive + prevention | E115 (widened) |
| 84 | `active_module_architecture_path_truth` | P0-1 prevention | E117 |
| 85 | `catalog_spi_row_matches_module_spi_metadata` | P1-2 prevention | E118 |

Each rule lands as one inline section in `gate/check_architecture_sync.sh` with three self-tests in `gate/test_architecture_sync_gate.sh` (9 new self-tests total). Cards: `docs/governance/rules/rule-82.md` (kernel + body updated for strengthening), `rule-84.md` (new), `rule-85.md` (new). `CLAUDE.md` Rule 82 kernel byte-matches the card per Rule 68; Rules 84/85 added under a new `### rc5 post-response review response prevention wave (2026-05-18)` heading.

## New ADR

**ADR-0080** — `ResilienceContract .spi package alignment + Rule 85 prevention`. Extends ADR-0030 (skill-capacity arbitration), ADR-0070 (tenant-aware two-arg resolve). Relates to ADR-0072 (engine extraction T2.B2 origin) and ADR-0079 (the engine SPI split pattern this ADR mirrors for resilience).

## Architecture baseline (post-rc6)

Canonical structured baseline lives in [`docs/governance/architecture-status.yaml#architecture_sync_gate.baseline_metrics`](../../governance/architecture-status.yaml). Headline numbers:

- 41 active engineering rules (rc5 baseline 39 + rc6 wave +2 Rules 84-85).
- 13 Layer-0 governing principles (P-A..P-M, unchanged).
- 70 active gate rules (rc5 baseline 68 + rc6 wave +2).
- 138 gate self-tests (rc5 baseline 129 + rc6 wave +9).
- 100 enforcer rows (rc5 baseline 98 + rc6 wave +2 E117-E118).
- 65 §4 constraints in ARCHITECTURE.md (unchanged).
- 80 ADRs (rc5 baseline 79 + rc6 wave +1 ADR-0080).
- 371 Maven tests GREEN under `./mvnw verify` — 277 surefire + 94 failsafe. The rc5 baseline carried a stale `306` inherited from rc4; rc6 records the actual measured count at v2.0.0-rc6 head. ResilienceContract package rename is mechanical — no production Java behavioural changes in rc6.
- 335 architecture-graph nodes / 463 architecture-graph edges (rc5 baseline 323 / 445; +12 nodes / +18 edges from Rules 84/85 + cards + E117/E118 + ADR-0080 + ResilienceContract SPI source nodes + capability row updates; regenerates idempotently per Rule 34).

## Four competitive pillars (P-B baselines)

This wave preserves all four pillar baselines unchanged. Pillar names as declared in `docs/governance/competitive-baselines.yaml`:

- **performance** — no runtime code changes; baseline preserved.
- **cost** — no runtime code changes; baseline preserved.
- **developer_onboarding** — preserved (quickstart unchanged; the ResilienceContract package rename is invisible to first-agent execution).
- **governance** — baseline strengthened: Rule 82 numeric-agreement + Rules 84-85 close the rc5 review's three corpus-integrity findings + the one comment-update finding.

## Verification commands

```bash
bash gate/test_architecture_sync_gate.sh   # -> Tests passed: 138/138
bash gate/check_architecture_sync.sh       # -> GATE: PASS (70 active gate rules)
bash gate/check_parallel.sh                # -> GATE: PASS (~7min wall-clock)
python gate/build_architecture_graph.py    # -> 335 nodes / 463 edges; Graph validation: OK
./mvnw clean verify                        # -> BUILD SUCCESS (371 tests GREEN: 277 surefire + 94 failsafe)
```

Linux/WSL verification per Rule 74 (E2 NDJSON self-tests require `python3` in PATH; Git Bash for Windows may show 3 E2-only failures unrelated to the Rule 82/84/85 surface).

## Tag posture

Tag **v2.0.0-rc6** supersedes v2.0.0-rc5. v2.0.0-rc5 is **not** retracted (additive uplift; no behavioural regression). v2.0.0-w2x-final remains retracted per `docs/governance/retracted-tags.txt`.

## Cross-references

- Review: `docs/reviews/2026-05-18-l0-rc5-post-response-architecture-review.en.md`.
- Response: `docs/reviews/2026-05-18-l0-rc5-post-response-architecture-review-response.en.md`.
- Prior wave release note: `docs/releases/2026-05-18-l0-rc4-cross-constraint-response.en.md` (v2.0.0-rc5).
- ADR-0080: `docs/adr/0080-resilience-contract-spi-package-alignment.yaml`.
- Rule cards: `docs/governance/rules/rule-82.md` · `rule-84.md` · `rule-85.md`.
- Rule history: `docs/governance/rule-history.md`.
