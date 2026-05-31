---
affects_level: L0
affects_view: process
proposal_status: response
authors: ["spring-ai-ascend architecture team"]
responds_to: docs/reviews/2026-05-18-l0-rc5-post-response-architecture-review.en.md
related_adrs:
  - ADR-0030
  - ADR-0032
  - ADR-0070
  - ADR-0072
  - ADR-0078
  - ADR-0079
  - ADR-0080
related_rules:
  - Rule 25
  - Rule 32
  - Rule 33
  - Rule 41
  - Rule 46
  - Rule 77
  - Rule 78
  - Rule 80
  - Rule 81
  - Rule 82
  - Rule 83
  - Rule 84
  - Rule 85
affects_artefact:
  - ADR-0080
  - ARCHITECTURE.md
  - CLAUDE.md
  - README.md
  - agent-service/ARCHITECTURE.md
  - agent-service/module-metadata.yaml
  - "agent-service/src/main/java/ascend/springai/service/runtime/resilience/spi/*.java"
  - docs/contracts/contract-catalog.md
  - docs/contracts/plan-projection.v1.yaml
  - docs/dfx/agent-service.yaml
  - docs/governance/architecture-status.yaml
  - docs/governance/enforcers.yaml
  - docs/governance/principle-coverage.yaml
  - docs/governance/rule-history.md
  - docs/governance/rules/rule-82.md
  - docs/governance/rules/rule-84.md
  - docs/governance/rules/rule-85.md
  - gate/README.md
  - gate/check_architecture_sync.sh
  - gate/test_architecture_sync_gate.sh
---

# Response — L0 rc5 Post-Response Architecture Review

## Executive decision

All four findings (P0-1, P1-1, P1-2, P2-1) are **accepted in full**. **No findings are rejected.** The reviewer's verdict — "do not publish a no-findings L0 completion release note yet; the runtime architecture itself is sound, but three active-corpus inconsistencies plus one stale comment are release-blocking for an L0 architecture-truth claim" — is precisely correct. The runtime architecture (engine extraction, S2C checked suspension, PlanProjection L0 abstraction level, skill capacity / memory ownership) was net-positive in the review; this wave is corpus-truth + prevention work shipping as **v2.0.0-rc6**. The prior tag `v2.0.0-rc5` is **not** retracted — rc6 is an additive uplift.

## Verification of each finding

We replicated the review's evidence locally before drafting fixes. Each citation matched. Verification of the gate's claim-vs-reality gap (the gate passing while drift remained) also reproduced.

| Finding | Evidence confirmed locally | Verdict |
|---|---|---|
| **P0-1** `agent-service/ARCHITECTURE.md` still describes pre-ADR-0079 engine and S2C ownership as current | `agent-service/ARCHITECTURE.md:44` (path convention rooted at `agent-service/...`), `:304-317` (engine section listing `EngineRegistry`/`EngineEnvelope`/`ExecutorAdapter` under `service.runtime.engine/`), `:319-327` (S2C SPI listed under `agent-service/.../s2c/spi/`), `:496-498` (engine extraction marked "scheduled post-Phase-C" out-of-scope bullet), `:585-588` ("Engine code in transit" risk register), `:592` (dead `docs/STATE.md` link). Real code locations: `EngineRegistry`/`EngineEnvelope` at `agent-execution-engine/.../service/runtime/engine/`; `ExecutorAdapter` + friends at `agent-execution-engine/.../engine/spi/`; `S2cCallbackEnvelope`/`S2cCallbackTransport`/`S2cCallbackResponse` at `agent-runtime-core/.../runtime/s2c/spi/`. | **ACCEPT** |
| **P1-1** Rule 82 does not enforce the baseline single-source invariant it claims to enforce | `gate/check_architecture_sync.sh:3733-3762` performs substring-only check for `architecture_sync_gate.baseline_metrics`. `gate/README.md:3`, `:18-20`, `:51`, `:53`, `:68` still claimed `64 active gate rules` / `121 self-tests` even though `architecture-status.yaml:95-96` declared `active_gate_checks: 68` / `gate_executable_test_cases: 129`. `bash gate/test_architecture_sync_gate.sh` returned `129/129`, `bash gate/check_architecture_sync.sh` returned `GATE: PASS` with the drift fully present — i.e. the rule passed the gate while violating its own intent. | **ACCEPT** |
| **P1-2** `ResilienceContract` is simultaneously treated as a shipped SPI and excluded from SPI governance | `docs/contracts/contract-catalog.md:22-31` listed `ResilienceContract` in "Active SPI interfaces (11 total)" at package `ascend.springai.service.runtime.resilience` (no `.spi` token). `:43` counted `agent-service` as having 2 SPIs (`GraphMemoryRepository`, `ResilienceContract`). `architecture-status.yaml:319-330` called it an SPI. `agent-service/ARCHITECTURE.md:284-290` titled the section "Operation-routing SPI". Real source at `agent-service/src/main/java/ascend/springai/service/runtime/resilience/ResilienceContract.java:1` — package without `.spi`. `agent-service/module-metadata.yaml:13-14` and `docs/dfx/agent-service.yaml:14-15` both declared only `ascend.springai.service.runtime.memory.spi`. Rules 77 + 78 passed vacuously for the resilience surface. | **ACCEPT** — promote to SPI (reviewer's preferred path) |
| **P2-1** `plan-projection.v1.yaml` still carries an rc1 "no enforcer ships" comment after Rule 83 shipped | `docs/contracts/plan-projection.v1.yaml:3-4` said "DESIGN ONLY at v2.0.0-rc1. No Java type, no runtime self-validation, no enforcer ships in this hotfix." Rule 83 + enforcer E116 ship at v2.0.0-rc5 and assert this contract appears in the catalog with at least one existing ADR — that IS an enforcer for this contract, just at the architecture-corpus level rather than runtime-self-validation. | **ACCEPT** |

**No rejections.** The reviewer's overdesign-smell observation — "narrow substring checks create a false sense of closure" — is closed structurally by the three negative self-tests added to Track B (pointer-with-stale-count, historical-marker-exempt) plus the bidirectional negative coverage in the new Tracks A and C self-tests.

## Defect family taxonomy (rc6 wave)

We classified the four findings into two families. Each family closes with a structural prevention gate so the same drift class cannot recur silently.

- **E-α — module-level authority drift after a refactor.** Code moved (ADR-0078 Phase C; ADR-0079 engine extraction), but a module-level `ARCHITECTURE.md` path claim or risk-register line lagged behind because rc5's prevention rule (Rule 81) only fires on `status: skeleton` — the `agent-service` module status is `active`, so Rule 81 was vacuous on the stale prose. **Manifestation:** P0-1. **Prevention:** Rule 84 — Active Module ARCHITECTURE Path Truth (the bidirectional complement of Rule 81; active module path claims must resolve OR be marked historical/moved/extracted-per-ADR).
- **E-β — gate enforces the wrong invariant.** rc5 shipped Rule 82 with a substring-presence check, but the actual drift class (stale adjacent count next to a correct pointer) escaped because numeric agreement was never asserted. Similarly, `ResilienceContract`'s catalog row claimed SPI status without any cross-check against `module-metadata.yaml#spi_packages` or `docs/dfx/<module>.yaml#spi_packages`. **Manifestations:** P1-1, P1-2. **Prevention:** Rule 82 strengthening (numeric-agreement check on entrypoint count phrases, with marker-based historical exemption) + Rule 85 — Catalog SPI Row Matches Module SPI Metadata.

P2-1 is a one-line comment correction with no prevention gate — low recurrence risk; the stale comment was a leftover from rc1 publishing before Rule 83 existed, not a pattern likely to repeat.

## Closure mapping to the review's 6 suggested rc6 acceptance criteria

| Criterion | Closure artefact |
|---|---|
| 1. `agent-service/ARCHITECTURE.md` no longer claims engine/S2C SPI ownership that moved to `agent-execution-engine` or `agent-runtime-core`. | `agent-service/ARCHITECTURE.md` path-convention line (around 44) extended with a post-ADR-0078 / ADR-0079 caveat; the engine section (around 304-317) rewritten as a consumer paragraph that names `agent-execution-engine` as the SPI + registry/envelope home; the S2C section (around 319-327) rewritten to name `agent-runtime-core` as the SPI home with `InMemoryS2cCallbackTransport` remaining at `agent-service`; §8 "Out of scope at L1" engine-extraction-deferred bullet deleted; §9.2 risk register "Engine code in transit" rewritten as a closure note citing ADR-0079; §10 dead `docs/STATE.md` link replaced with `docs/governance/architecture-status.yaml`. Gate Rule 84 (`active_module_architecture_path_truth`, enforcer E117) prevents recurrence. |
| 2. `gate/README.md` either contains no active raw baseline counts or all active counts match `architecture_sync_gate.baseline_metrics`. | All inline counts at `gate/README.md:3`, `:18-20`, `:51`, `:53`, `:68` bumped to the post-rc6 baseline (`70 active gate rules` / `138 self-tests`); the pointer to `architecture_sync_gate.baseline_metrics` stays in line 3 and is now consistent with the surrounding numbers. The strengthened Rule 82 now enforces that this can never silently drift again. |
| 3. Rule 82 has a negative test for "pointer present but stale adjacent count". | `gate/test_architecture_sync_gate.sh` adds `rule82_pointer_present_but_stale_count_neg` (reproduces reviewer evidence exactly — pointer + `64 active gate rules` stale adjacent count must FAIL) and two companion tests: `rule82_numeric_agreement_pos` and `rule82_historical_marker_exempts_neg` (regression guard preventing false positives in historical prose). |
| 4. `ResilienceContract` has a single classification: governed SPI or internal runtime contract, with catalog, metadata, DFX, and prose aligned. | Reviewer's **preferred path** chosen — promotion to governed SPI. ADR-0080 ("ResilienceContract `.spi` package alignment") records the decision. Five source files (`ResilienceContract`, `ResiliencePolicy`, `SkillResolution`, `SuspendReason`, `SkillCapacityRegistry`) moved from `ascend.springai.service.runtime.resilience` to `ascend.springai.service.runtime.resilience.spi`. Implementations (`DefaultSkillResilienceContract`, `YamlResilienceContract`, `YamlSkillCapacityRegistry`) stay in the parent package — same split pattern as ADR-0079's `engine.spi.*` vs `service.runtime.engine.*`. `agent-service/module-metadata.yaml#spi_packages` and `docs/dfx/agent-service.yaml#spi_packages` both gain the new `resilience.spi` entry; `docs/contracts/contract-catalog.md` row updated; `agent-service/ARCHITECTURE.md` resilience section rewritten; `architecture-status.yaml#resilience_contract.implementation` lists all 5 SPI files + 3 impl files separately. Gate Rule 85 (`catalog_spi_row_matches_module_spi_metadata`, enforcer E118) prevents recurrence. |
| 5. `plan-projection.v1.yaml` distinguishes runtime enforcement from Rule 83 catalog-registration enforcement. | `docs/contracts/plan-projection.v1.yaml` comment block (lines 3-26) rewritten. New text says: "DESIGN ONLY since v2.0.0-rc1. No Java type, no runtime self-validation ships at W1; the orchestrator does NOT consume the projection yet. Architecture-corpus enforcement (active since v2.0.0-rc5): Rule 83 (design_only_contract_registered_in_catalog, enforcer E116) asserts this contract appears in docs/contracts/contract-catalog.md §3 and cites at least one existing ADR. This is corpus-registration enforcement, NOT runtime self-validation. Runtime enforcement is deferred to the W2 scheduler wave per ADR-0032 (PlanProjection staging note, 2026-05-18 amendment)." The wave-trajectory bullets are kept; an "active now" row added at the v2.0.0-rc5 line. |
| 6. The architecture team reruns the gate scripts + Maven verify. | See **Verification Performed** below. |

## Hidden defects mined during the audit

Beyond the four named findings, we found and closed the following adjacent defects:

1. **`agent-service/ARCHITECTURE.md:592` referenced `docs/STATE.md`.** That file does not exist (and never did — the README clarified its absence at rc5 but the module file kept the dangling pointer). Replaced with the actual current-delivery-state source `docs/governance/architecture-status.yaml`.
2. **`agent-service/src/test/java/.../PlatformImportsOnlyRuntimePublicApiTest.java:46-48` comment + `:56` `because:` clause said `resilience..` is the published-SPI surface.** Updated to name `resilience.spi.*` (the public SPI) plus `resilience.{Default,Yaml}*` (the implementations) explicitly, reflecting ADR-0080. The ArchUnit rule itself does not need to change — `resilience..` matches both `.spi` and impl sub-packages; only the documentation prose was sharpened.
3. **`agent-runtime-core/.../S2cCallbackResponse.java:15` `@link` FQN referenced `ascend.springai.service.runtime.resilience.SuspendReason.AwaitClientCallback`.** Updated to `ascend.springai.service.runtime.resilience.spi.SuspendReason.AwaitClientCallback` so the cross-module Javadoc reference points at the new SPI home.
4. **`agent-service/module-metadata.yaml` previously declared only one SPI package (`memory.spi`).** rc6 adds `resilience.spi` so the catalog's "11 total SPIs" count is now actually backed by metadata for all 11 (the rc4 audit's hidden defect 6 about ResilienceContract is closed by this change combined with ADR-0080).
5. **Rule 82's previous prose-only "drift mode" clause in `docs/governance/rules/rule-82.md` (lines 72-74)** said "A future numeric claim that does NOT cite the structured block will fail the gate." This was aspirational — the gate did not actually do that check. Rule 82's strengthened kernel + new self-tests now operationalise the prose; the rule body needs no edit beyond the kernel re-statement.
6. **`docs/governance/architecture-status.yaml#baseline_metrics` previously had `active_engineering_rules: 35` + `active_engineering_rules_post_rc5: 39` but no rc6 marker.** Added `active_engineering_rules_post_rc6: 41` to preserve the wave-by-wave traceability pattern; also bumped `active_gate_checks`, `gate_executable_test_cases`, `enforcer_rows`, `adr_count`, `architecture_graph_nodes`, `architecture_graph_edges` in place to their post-rc6 values.

## New gate rules + Rule 82 strengthening (Rules 84, 85 + E115/E117/E118)

| Rule | Slug | Enforcer | Self-tests (positive / negative pairs) |
|---|---|---|---|
| **Rule 82** (strengthened) | `baseline_metrics_single_source` | E115 (asserts widened) | `rule82_baseline_metrics_single_source_pos` / `_neg` (existing) + `rule82_numeric_agreement_pos` + `rule82_pointer_present_but_stale_count_neg` + `rule82_historical_marker_exempts_neg` (NEW) |
| **Rule 84** | `active_module_architecture_path_truth` | E117 | `rule84_path_claim_resolves_pos` + `rule84_path_claim_does_not_resolve_neg` + `rule84_path_claim_historical_marker_exempts` |
| **Rule 85** | `catalog_spi_row_matches_module_spi_metadata` | E118 | `rule85_catalog_row_matches_metadata_pos` + `rule85_catalog_row_missing_from_metadata_neg` + `rule85_internal_marker_exempts_neg` |

`docs/governance/rules/rule-84.md` + `rule-85.md` cards published; `principle-coverage.yaml` extends P-C with Rule 84 and P-D with Rule 85; `rule-history.md` records the rc5 post-response review response wave entry alongside the rc4 cross-constraint wave.

## Baseline metrics (post-rc6)

Wave-by-wave deltas (rc5 → rc6), all sourced from `docs/governance/architecture-status.yaml#architecture_sync_gate.baseline_metrics`:

| Metric | rc5 baseline | rc6 baseline | Δ |
|---|---:|---:|---:|
| `active_engineering_rules_post_rc6` | 39 | **41** | +2 (Rules 84, 85) |
| `active_gate_checks` | 68 | **70** | +2 (Rules 84, 85) |
| `gate_executable_test_cases` | 129 | **138** | +9 (3 per new rule × 2 + 3 for Rule 82 strengthening) |
| `enforcer_rows` | 98 | **100** | +2 (E117, E118) |
| `adr_count` | 79 | **80** | +1 (ADR-0080) |
| `architecture_graph_nodes` | 323 | **335** | +12 (Rules 84/85 nodes + cards + E117/E118 + ADR-0080 + ResilienceContract SPI source nodes + capability row updates) |
| `architecture_graph_edges` | 445 | **463** | +18 (rule→enforcer, rule→card, enforcer→artefact, ADR→relates_to, new SPI source-to-capability edges) |
| `maven_tests_green` | 306 (stale) | **371** | +65 (count refresh — rc5 carried a stale 306 inherited from rc4; rc6 measures 277 surefire + 94 failsafe at head. rc6 adds 0 production Java behavioural changes — ResilienceContract package rename is mechanical.) |

## Verification Performed

- `bash gate/test_architecture_sync_gate.sh` → `Tests passed: 138/138` (post-rc6 baseline; rc5 baseline 129 + 9 new self-tests, all green).
- `bash gate/check_architecture_sync.sh` → `GATE: PASS` (70 active gate rules; Rule 82 strengthened, Rules 84-85 active).
- `bash gate/check_parallel.sh` → `GATE: PASS`.
- `python gate/build_architecture_graph.py` → measured `335 nodes / 463 edges`; `Graph validation: OK` (Rule 34 idempotency).
- `./mvnw clean verify` → `BUILD SUCCESS`, 371 tests GREEN (277 surefire + 94 failsafe; rc5 baseline of 306 was stale, count refreshed at rc6 — no behavioural change). Track C SPI move imports cleanly across modules; `PlatformImportsOnlyRuntimePublicApiTest` continues to pass with the refreshed comment; `SpiPurityGeneralizedArchTest` accepts the new `resilience.spi` package without modification — it already imports only `java.*` + same-spi-package siblings.
- `./mvnw -pl agent-service -am test-compile -q` → BUILD SUCCESS (refreshed test imports for `ResilienceContractTest`, `ResilienceContractIT`, `SkillCapacityResolutionIT`, `S2cCallbackRoundTripIT`).

**Negative spot-checks (executed + reverted; not committed):**

a. Injected `64 active gate rules` into `gate/README.md`; reran `check_architecture_sync.sh`; confirmed Rule 82 FAILS citing `claims '64 active gate rules' but architecture_sync_gate.baseline_metrics.active_gate_checks = 70 -- Rule 82 / E115 (numeric drift)`. Reverted.

b. Injected stale `agent-service/src/main/java/ascend/springai/service/runtime/engine/ExecutorAdapter.java` claim into `agent-service/ARCHITECTURE.md`; confirmed Rule 84 FAILS citing the unresolved path with no historical marker. Reverted.

c. Removed `ascend.springai.service.runtime.resilience.spi` from `agent-service/module-metadata.yaml`; confirmed Rule 85 FAILS citing the catalog row for `ResilienceContract` whose package is no longer in `module-metadata.yaml#spi_packages`. Reverted.

## Open follow-ups for W2 (none expected)

- `ResilienceContract` was the last `.spi`-package edge case logged in the rc4 response's hidden defects (item 6). It is now closed. We are not aware of any remaining published SPI surface that fails Rule 77 / Rule 78 / Rule 85.
- `plan-projection.v1.yaml` runtime enforcement (Java record + `PlanProjector` SPI + orchestrator consumption) remains the legitimate W2 promotion. Rule 83 already keeps it registered at the catalog level until then; nothing changes there.
- The `gate/check_architecture_sync.sh` Rule 82 phrase map currently covers `active gate rules`, `active rules`, `self-tests`, `self-test cases`, `active engineering rules`, `enforcer rows`, `architecture-graph nodes`, `architecture-graph edges`, `graph nodes`, `graph edges`, `ADRs`, plus the `Tests passed: N/N` pattern. If a future wave introduces a new entrypoint phrase (e.g. `governing principles`), the map needs one row added — the architecture cost is small and the rule's intent generalises.

## Open closure questions answered for the reviewer

- **Why promote `ResilienceContract` to SPI rather than demote?** Three signals: (a) `docs/contracts/contract-catalog.md` already listed it as 1 of 11 active SPIs, making it a de-facto published commitment; (b) ADR-0030 (Skill-Dimensional Resource Arbitration) and ADR-0070 (tenant-aware two-arg `resolve(tenant, skill)`) publish the contract as the architectural boundary for skill capacity arbitration; (c) `SuspendReason` is referenced from `agent-runtime-core` Javadoc (`SuspendSignal.java`, `S2cCallbackResponse.java`) as a cross-module taxonomy — and ADR-0070's planned W2 cross-module callers will turn that documentation reference into an actual import. Demotion would erase a real boundary; promotion brings the metadata up to where the contract already sat.
- **Why split SPI vs implementation across the same module?** The same pattern ADR-0079 used for `agent-execution-engine` — the SPI surface lives under `<root>.<domain>.spi.*` (`engine.spi.*` / `resilience.spi.*`), implementations live under `<root>.service.runtime.<domain>.*` (`service.runtime.engine.*` / `service.runtime.resilience.*`). This keeps Rule 77 (.spi convention) + Rule 78 (DFX set-match) + Rule 32 (SPI co-design) lined up with the actual class file home and avoids a JPMS split-package problem when W3+ migrates to module-info.

## Tag + retraction posture

Tag **v2.0.0-rc6** supersedes v2.0.0-rc5. v2.0.0-rc5 is **not** retracted (additive uplift; no behavioural regression). v2.0.0-w2x-final remains retracted per `docs/governance/retracted-tags.txt`.

## Cross-references

- Review: `docs/reviews/2026-05-18-l0-rc5-post-response-architecture-review.en.md` (Codex Java Microservices and Agent Architecture Review).
- Release note: `docs/releases/2026-05-18-l0-rc6-post-response.en.md`.
- Prior wave response: `docs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review-response.en.md` (the rc4 closure this wave builds on).
- ADR-0080: `docs/adr/0080-resilience-contract-spi-package-alignment.yaml`.
- Rule cards: `docs/governance/rules/rule-82.md` (strengthened) · `rule-84.md` (new) · `rule-85.md` (new).
- Rule history: `docs/governance/rule-history.md` — 2026-05-18 rc5 post-response wave entry.
