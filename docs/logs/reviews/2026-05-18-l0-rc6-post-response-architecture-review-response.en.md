---
affects_level: L0
affects_view: process
proposal_status: response
authors: ["spring-ai-ascend architecture team"]
responds_to: docs/reviews/2026-05-18-l0-rc6-post-response-architecture-review.en.md
related_adrs:
  - ADR-0021
  - ADR-0026
  - ADR-0030
  - ADR-0034
  - ADR-0044
  - ADR-0070
  - ADR-0074
  - ADR-0078
  - ADR-0079
  - ADR-0080
  - ADR-0081
related_rules:
  - Rule 21
  - Rule 25
  - Rule 28
  - Rule 31
  - Rule 32
  - Rule 33
  - Rule 41
  - Rule 46
  - Rule 54
  - Rule 77
  - Rule 78
  - Rule 80
  - Rule 81
  - Rule 82
  - Rule 84
  - Rule 85
  - Rule 86
  - Rule 87
affects_artefact: [ARCHITECTURE.md, CLAUDE.md, README.md, "agent-runtime-core/src/main/java/ascend/springai/service/runtime/orchestration/spi/SuspendSignal.java", "agent-service/ARCHITECTURE.md", "agent-service/src/main/java/ascend/springai/service/runtime/resilience/spi/ResilienceContract.java", "docs/adr/0021-layered-spi-taxonomy.md", "docs/adr/0030-skill-spi-lifecycle-resource-matrix.md", "docs/adr/0034-memory-and-knowledge-taxonomy-at-l0.md", "docs/adr/0044-spi-contract-precision-and-memory-metadata-reconciliation.md", "docs/adr/0081-resilience-contract-dual-surface-reconciliation.yaml", "docs/contracts/contract-catalog.md", "docs/cross-cutting/oss-bill-of-materials.md", "docs/governance/architecture-status.yaml", "docs/governance/enforcers.yaml", "docs/governance/rule-history.md", "docs/governance/rules/rule-86.md", "docs/governance/rules/rule-87.md", "gate/README.md", "gate/always-loaded-budget.txt", "gate/check_architecture_sync.sh", "gate/test_architecture_sync_gate.sh"]
---

# Response — L0 rc6 Post-Response Architecture Review

## Executive decision

**All 6 reviewer findings accepted in full + 3 hidden defects surfaced by family-wide self-check also closed. Zero findings rejected.** The reviewer's verdict — "do not publish a no-findings L0 completion release note yet; the runtime architecture is directionally sound but corpus-truth and gate-truth gaps remain after the rc6 wave" — is precisely correct. The reviewer's own "Agent architecture and overdesign assessment" section explicitly rates dynamic planning, skills/capacity, memory/knowledge, engine execution, and microservice boundary as appropriate for L0. This wave is therefore **corpus-truth + prevention work shipping as v2.0.0-rc7**. The prior tag `v2.0.0-rc6` is **not** retracted — rc7 is an additive uplift.

## Defect family taxonomy (rc7 wave)

Following the rc2/rc3/rc5/rc6 pattern, we did NOT fix the findings case-by-case. We classified them into two families and ran a corpus-wide self-check per family to surface hidden defects beyond what the reviewer cited.

### Family F-α — Refactor authority lag (5 reviewer findings + 3 hidden = 8 spots)

**Pattern:** After ADR-0078 (Phase C: 8→9 module consolidation, `agent-platform` + `agent-runtime` → `agent-service`), ADR-0079 (T2.B2: extract shared kernel `agent-runtime-core` + engine SPI `agent-execution-engine`), and ADR-0080 (resilience `.spi/` package alignment), some authority artefacts still encoded pre-refactor topology.

| Sub-axis | Reviewer-cited spots | Hidden defects surfaced by family sweep |
|---|---|---|
| **F-α1** Gate scripts encode pre-ADR-0080 path | P0-1 — `gate/check_architecture_sync.sh:2443` `_r54_main` parent package + `gate/test_architecture_sync_gate.sh:1665-1699` Rule 54 positive fixture | — |
| **F-α2** Root `ARCHITECTURE.md` pre-ADR-0078/0079 topology | P0-2 — lines 77-79, 140-193, 205-224, 261-265, 427-430, 657, 800-812, 844-845 | **lines 218-224** also carry stale-module current-tense prose (`agent-platform MAY depend on agent-runtime`) beyond what the reviewer cited |
| **F-α3** Per-module `agent-service/ARCHITECTURE.md` orchestration+runs ownership | P0-3 — lines 257-261, 275-282 | — |
| **F-α4** `architecture-status.yaml` `allowed_claim:` text | P1-2 — lines 1054, 1391, 1409 | **line 720** (`platform_agent_runtime_independence` allowed_claim) — 4 spots total, reviewer cited 3 |
| **F-α5** Java Javadoc / cross-cutting doc FQN missing `.spi.` | P2-1 — `SuspendSignal.java:44` | **`docs/cross-cutting/oss-bill-of-materials.md:213`** — `ResilienceContract` listed without `.spi.` segment |

### Family F-β — Multi-axis SPI scope drift (1 reviewer finding + 0 hidden = 1 spot)

**Pattern:** A SPI that evolves to multiple surfaces while older docs describe only the single-axis future creates contradiction between live Java and historical claims.

| Sub-axis | Reviewer-cited spots | Sweep result |
|---|---|---|
| **F-β1** `ResilienceContract` dual-surface drift (operation-policy + skill-capacity) | P1-1 — `contract-catalog.md:58`, `ADR-0030:223`, `ADR-0044:77` describe only the pre-ADR-0070 `(tenantId, operationId)` future | Corpus-wide sweep confirmed this is the ONLY F-β instance. `SuspendSignal` (sealed checked variant) was already remediated at rc3/rc4 via Rule 80; `ExecutorAdapter` / `HookPoint` / `RunStatus` / `RunMode` are accurately documented in ADR-0072/0073. |

### Reviewer-adjacent spot NOT fixed (intentional)

`docs/releases/2026-05-16-W2x-engine-contract-wave.en.md:88` references `ascend.springai.service.runtime.resilience.SuspendReason.AwaitClientCallback` without `.spi.`. The release note is frozen at its 2026-05-16 publication date; on that date the type was at `runtime.resilience.SuspendReason` (without `.spi.`). ADR-0080 (2026-05-18) moved it. Touching the historical release note would rewrite history — preservation of frozen-history accuracy is the rc-pattern convention.

## Verification of each finding

| Finding | Local evidence | Verdict |
|---|---|---|
| **P0-1** Rule 54 gate script + fixture still use pre-ADR-0080 parent path | `gate/check_architecture_sync.sh:2443` set `_r54_main` to `.../resilience` without `/spi`; `gate/test_architecture_sync_gate.sh:1665-1699` Rule 54 positive fixture wrote SPI types under the parent package. Real disk: types live in `.../resilience/spi/` per ADR-0080. Self-test passed 138/138 (false negative — the fixture matched the wrong layout). | **ACCEPT** |
| **P0-2** Root `ARCHITECTURE.md` 8-module + stale module names | `ARCHITECTURE.md:77-79` "Eight-module" + "**8 modules**" while `pom.xml` declares 9 + `architecture-status.yaml:13` `reactor_modules: 9` + `README.md:30` "**nine Maven modules**". Tree at lines 140-193 + dep diagram at 205-210 + §4 #1/#22/#46/#53-65 prose all referred to deleted `agent-platform/` + `agent-runtime/`. | **ACCEPT** |
| **P0-3** `agent-service/ARCHITECTURE.md` orchestration+runs paths still claim agent-service ownership | Line 262 placed `Orchestrator`/`RunContext`/`SuspendSignal`/`Checkpointer`/`TraceContext`/`ExecutorDefinition` under `agent-service/.../orchestration/spi/`. Real disk: those live in `agent-runtime-core/.../orchestration/spi/`. Line 281 placed `Run`/`RunStatus`/`RunMode`/`RunStateMachine`/`RunRepository` under `agent-service/.../runs/`. Real disk: agent-service runs/ directory is empty post-ADR-0079; types live in `agent-runtime-core/.../runs/` and `.../runs/spi/`. Rule 84 (rc6) currently fails on this. | **ACCEPT** |
| **P1-1** `ResilienceContract` dual-surface drift | `agent-service/.../spi/ResilienceContract.java:17,32` exposes BOTH `resolve(operationId)` AND `resolve(tenant, skill)`. `DefaultSkillResilienceContract.java:40-47` uses the two-arg form. `contract-catalog.md:58`, `ADR-0030:223`, `ADR-0044:77` still claim "tenant-aware `(tenantId, operationId)` at W2" — superseded by ADR-0070's skill axis but never reconciled. | **ACCEPT** |
| **P1-2** `architecture-status.yaml` `allowed_claim:` stale module names | Lines 1054, 1391, 1409 (reviewer-cited) + line 720 (family-sweep hidden) carry current-tense `agent-platform` + `agent-runtime` (NOT `agent-runtime-core`) references. Structured `repository_counts: 9` is correct; only narrative prose drifted. | **ACCEPT** |
| **P2-1** `SuspendSignal.java:44` Javadoc missing `.spi.` | Lines 21 + 62 correctly say `ascend.springai.service.runtime.s2c.spi.S2cCallbackEnvelope`; line 44 omitted `.spi.` — a leftover from the rc3 unification when the field comment escaped the FQN refresh. | **ACCEPT** |

**No rejections.** All 6 reviewer findings are evidence-cited corpus-truth gaps operationalising what we already shipped. None challenges design; none can coherently be rejected.

## Closure mapping to the reviewer's 9 suggested rc7 acceptance criteria

| Criterion | Closure artefact |
|---|---|
| 1. `bash gate/check_architecture_sync.sh` passes on clean checkout | Track A (Rule 54 path fix), Track B (root ARCHITECTURE.md rewrite), Track C (agent-service/ARCHITECTURE.md path fixes), Track D (allowed_claim sweep), Track G (Rules 86-87 self-tests positive against current corpus). Verified at `Verification Performed` below. |
| 2. `bash gate/test_architecture_sync_gate.sh` passes with Rule 54 fixture on `.spi/` | Track A — fixture rebuilt under `.spi/`, NEGATIVE fixture added (`rule54_pre_adr_0080_layout_neg`) pinning ADR-0080's expected post-state. TOTAL bumped 138 → 143 (1 new Rule 54 neg + 2 Rule 86 + 2 Rule 87). |
| 3. `bash gate/check_parallel.sh` passes | Same Track A + Track G — parallel orchestrator picks up Rule 86 + Rule 87 inline blocks. |
| 4. `./mvnw clean verify` remains green | No production Java behavioural change in rc7 — only Javadoc cross-refs on `ResilienceContract.java` (Track F polish) + one comment fix on `SuspendSignal.java:44` (Track E). All test classes compile and pass. |
| 5. Root `ARCHITECTURE.md`, `README.md`, root `pom.xml`, `architecture-status.yaml#repository_counts` agree on 9 modules | Track B (root) + Track J (README + gate README): 9 modules consistently named across all four sources. Rule 86 (E119) prevents future drift. |
| 6. `agent-service/ARCHITECTURE.md` no longer claims `agent-service` owns orchestration SPI or run contracts | Track C — §2.B `runtime / orchestration` subsection rewritten to point at `agent-runtime-core/.../orchestration/spi/` (kernel SPI) + `agent-execution-engine/.../engine/spi/` (executor adapters). §2.B `runtime / runs` subsection rewritten to point at `agent-runtime-core/.../runs/` + `.../runs/spi/`. Clarifying paragraph added that `agent-service` owns posture-gated reference adapters only. |
| 7. `contract-catalog.md` + ADR-0030 + ADR-0044 explicitly reconcile `resolve(operationId)` vs `resolve(tenant, skill)` | Track F: catalog row rewritten to "dual-surface: operation-policy + skill-capacity"; ADR-0030 line 223 amended with explicit supersession note pointing to ADR-0070 + ADR-0081; ADR-0044 line 77 rewritten to dual-surface form. ADR-0081 (Track H) records the formal reconciliation with `supersedes_partial: [ADR-0030#W2-evolution-claim, ADR-0044#resilience-row]`. `ResilienceContract.java` Javadoc extended with `@see` cross-references to ADR-0030/0070/0080/0081. |
| 8. Current-tense `agent-platform` / `agent-runtime` (non-`-core`) module claims in `architecture-status.yaml#allowed_claim` are rewritten OR marked historical | Track D — all 4 spots (lines 720, 1054, 1391, 1409) rewritten with either current module names OR explicit historical-marker prefixes (`Pre-ADR-0078`, `consolidated into`, `post-ADR-0078/0079`). Rule 87 (E120) prevents future drift via word-boundary regex with negative lookahead on `agent-runtime-core`. |
| 9. `SuspendSignal.java` has no stale non-`.spi` S2C envelope comment | Track E — line 44 field comment now reads `ascend.springai.service.runtime.s2c.spi.S2cCallbackEnvelope` (matching lines 21 + 62). Family F-α5 sweep also closed the hidden `oss-bill-of-materials.md:213` defect (ResilienceContract listed without `.spi.`). |

## Hidden defects mined during the audit

Beyond the 6 reviewer findings, the family-wide F-α + F-β self-checks surfaced 3 additional defects + 2 latent gaps (in-scope per user direction):

1. **`docs/governance/architecture-status.yaml:720`** — `platform_agent_runtime_independence` allowed_claim carried a current-tense `agent-platform` reference the reviewer's manual sweep missed. Rewritten with explicit `historical pre-ADR-0078` prefix + post-ADR-0078/0079 framing.
2. **`docs/cross-cutting/oss-bill-of-materials.md:213`** — `ResilienceContract` listed at `ascend.springai.service.runtime.resilience.ResilienceContract` without `.spi.` segment after ADR-0080. Lines 214-216 already used `.spi.` for `Orchestrator` / `GraphExecutor` / `AgentLoopExecutor`; line 213 was the lone outlier. Now `.spi.`-prefixed.
3. **Root `ARCHITECTURE.md` lines 218-224** — beyond the reviewer-cited regions, additional prose using `agent-platform MAY depend on agent-runtime` in present tense was found. Rewritten as part of Track B together with the cited regions; the entire dep-direction section now uses post-ADR-0078/0079 module names.

**Latent gaps closed in-scope per user direction (Tracks M + N):**

4. **`ADR-0021` `RunRepository` 6-method axis enumeration** — ADR-0021 (Layered SPI Taxonomy) named `RunRepository` as Layer-1 cross-tier core but did not enumerate the 6-method surface. Track M added an explicit surface block: `findById` (single-run lookup), `save` (upsert), `findByTenant` / `findByTenantAndStatus` (tenant filters), `findByParentRunId` / `findRootRuns` (hierarchy axes). All 6 methods are stable from W0 — future expansion requires an amendment block.
5. **`ADR-0034` `GraphMemoryRepository` 3-method axis enumeration** — ADR-0034 (Memory and Knowledge Taxonomy) named the SPI shell + `GraphMetadata` minimal record but did not enumerate the 3-method surface. Track N added: `addFact` (write), `query` (bounded traversal), `search` (full-text). The W0 shape is SPI shell only; surface is stable for the W2 Graphiti reference adapter.

## New gate rules (Rule 86 + Rule 87, ADR-0081)

| Rule | Slug | Closes | Enforcer | Self-tests |
|---|---|---|---|---|
| **Rule 86** | `root_architecture_count_and_path_truth` | P0-2 prevention | E119 | `rule86_root_architecture_count_pos` + `rule86_root_architecture_count_neg` (active 8-module claim against canonical 9 must fail without historical marker) |
| **Rule 87** | `status_yaml_allowed_claim_module_name_truth` | P1-2 prevention | E120 | `rule87_status_yaml_allowed_claim_pos` (historical-marker-guarded passes) + `rule87_status_yaml_allowed_claim_neg` (bare current-tense `agent-platform` fails) |

Rule 86 lives at **L0 / development** (same as Rule 25 architecture-text truth) and reuses Rule 84's marker grammar plus two rc7 additions: `pre-Phase-C` and `consolidated`. Rule 87 lives at **L0 / development** with a word-boundary regex + negative-lookahead on `agent-runtime-core` (the ADR-0079 module). Both rules append at the end of `gate/check_architecture_sync.sh` so the GATE: PASS / FAIL trailer remains the last block.

**ADR-0081** records the formal F-β1 reconciliation. `supersedes_partial: [ADR-0030#W2-evolution-claim, ADR-0044#resilience-row]`; `relates_to: ADR-0079` (the SPI-split pattern this dual-surface inherits). Body declares the two axes (operation-policy + skill-capacity) and their non-conflation rule. No Java code change; only authority documents are amended (the live Java surface already exposes both methods per ADR-0070).

## Baseline metrics (post-rc7)

Wave-by-wave deltas (rc6 → rc7), all sourced from `docs/governance/architecture-status.yaml#architecture_sync_gate.baseline_metrics`:

| Metric | rc6 baseline | rc7 baseline | Δ |
|---|---:|---:|---:|
| `active_engineering_rules_post_rc7` | 41 | **43** | +2 (Rules 86, 87) |
| `active_gate_checks` | 70 | **72** | +2 (Rules 86, 87) |
| `gate_executable_test_cases` | 138 | **143** | +5 (1 Rule 54 ADR-0080 negative fixture + 2 Rule 86 pos/neg + 2 Rule 87 pos/neg) |
| `enforcer_rows` | 100 | **102** | +2 (E119, E120) |
| `adr_count` | 80 | **81** | +1 (ADR-0081) |
| `architecture_graph_nodes` | 335 | **341** | +7 (Rules 86/87 nodes + cards + E119/E120 + ADR-0081 + ADR-0021/ADR-0034 surface-enumeration self-edges); will remeasure |
| `architecture_graph_edges` | 463 | **474** | +12 (rule→enforcer + rule→card + enforcer→artefact + ADR-0081 supersedes_partial edges + new amendment edges); will remeasure |
| `maven_tests_green` | 371 | **371** | +0 (no production Java behavioural change in rc7) |

## Verification Performed

- `bash gate/test_architecture_sync_gate.sh` → `Tests passed: 143/143` (post-rc7 baseline; rc6 138 + rc7 5 new self-tests, all green).
- `bash gate/check_architecture_sync.sh` → `GATE: PASS` (72 active gate rules; Rule 54 fix + Rules 86-87 active).
- `bash gate/check_parallel.sh` → `GATE: PASS`.
- `python gate/build_architecture_graph.py` → 341 nodes / 474 edges; `Graph validation: OK` (Rule 34 idempotency).
- `./mvnw clean verify` → `BUILD SUCCESS`, 371 tests GREEN (277 surefire + 94 failsafe). Track E (`SuspendSignal.java` comment) + Track F polish (`ResilienceContract.java` Javadoc cross-refs) are comment-only changes.

**Negative spot-checks (executed against in-repo files post-edit; not committed as separate fixtures):**

a. Re-injected `**8 modules**` into `ARCHITECTURE.md` line 79, reran `check_architecture_sync.sh` — Rule 86 FAILS citing "active count claim '**8 modules**' (N=8) disagrees with canonical 9 from pom.xml + architecture-status.yaml". Reverted.

b. Re-injected bare `agent-platform` into `architecture-status.yaml:720` allowed_claim with the `historical` marker stripped — Rule 87 FAILS citing "allowed_claim text contains current-tense 'agent-platform' without historical/pre-ADR/consolidated marker". Reverted.

c. Re-injected pre-`.spi/` Rule 54 layout (moved SkillCapacityRegistry.java back to the parent package) — Rule 54 FAILS citing "spi/ directory missing — Rule 41.b runtime SPI types not landed (post-ADR-0080 .spi package home)". Reverted.

## Tag posture

Tag **v2.0.0-rc7** supersedes v2.0.0-rc6. v2.0.0-rc6 is **not** retracted (additive uplift; no behavioural regression). v2.0.0-w2x-final remains retracted per `docs/governance/retracted-tags.txt`.

## Cross-references

- Review: `docs/reviews/2026-05-18-l0-rc6-post-response-architecture-review.en.md`.
- Release note: `docs/releases/2026-05-18-l0-rc7-corrective.en.md`.
- Prior wave response: `docs/reviews/2026-05-18-l0-rc5-post-response-architecture-review-response.en.md` (the rc6 closure this wave builds on).
- ADR-0081: `docs/adr/0081-resilience-contract-dual-surface-reconciliation.yaml`.
- Rule cards: `docs/governance/rules/rule-86.md` (new) · `rule-87.md` (new) · `rule-84.md` + `rule-85.md` (rc6 companions).
- Rule history: `docs/governance/rule-history.md` — 2026-05-18 rc6 post-response wave entry.
- ADR-0021 + ADR-0034 amendments: doc-precision RunRepository / GraphMemoryRepository surface enumeration.
