---
level: L0
view: process
release_id: v2.0.0-rc10 (retracted)
tag: v2.0.0-rc10 (retracted)
date: 2026-05-19
status: retracted
retracted: true
retracted_by_tag: v2.0.0-rc11
retracted_date: 2026-05-19
retracted_reason: "rc10 post-corrective architecture review response P1-1 (Rule 41 kernel overclaim) / P1-2 (deleted-module-name leaks in operational runbooks) / P1-3 (Rule 96 kernel-vs-impl drift) / P2-1 (shadow-corpus prose drift) + user-elected Rule 94 kernel/impl widening — see ADR-0085 and docs/releases/2026-05-19-l0-rc11-corrective.en.md"
historical_baseline_note: "Historical artifact frozen at SHA 94ac17b (rc10 wave commit). Counts in this document (110 active gate rules / 165 self-tests / 138 enforcer rows / 84 ADRs / 53 active engineering rules / 376 graph nodes / 535 edges) reflect the state at rc10 publication time and are NOT retroactively updated — see canonical baseline in docs/governance/architecture-status.yaml#architecture_sync_gate.baseline_metrics for rc11 values."
supersedes_tag: v2.0.0-rc9
authority_refs:
  - ADR-0084
  - "docs/reviews/2026-05-18-l0-rc8-post-corrective-architecture-review.en.md (category-sweep follow-up)"
  - "docs/reviews/2026-05-19-l0-rc8-post-corrective-architecture-review-response.en.md (rc10 appendix appended)"
---

# v2.0.0-rc10 (retracted) — Corpus Truth + Prevention Widening (rc8 Category-Sweep Follow-Up)

> **Historical artifact frozen at SHA 94ac17b (rc10 wave commit).** This release is **RETRACTED** as of 2026-05-19 by the v2.0.0-rc11 wave per ADR-0085. Baseline counts in this document (110 active gate rules / 165 self-tests / 138 enforcer rows / 84 ADRs / 53 active engineering rules / 376 graph nodes / 535 edges) reflect the state at rc10 publication time and are NOT retroactively updated. Current canonical baseline lives in `docs/governance/architecture-status.yaml#architecture_sync_gate.baseline_metrics` and in `docs/releases/2026-05-19-l0-rc11-corrective.en.md`.

> **rc10 post-corrective architecture review response (rc11 supersession context).**
> The findings against rc10 (Codex review, 2026-05-19):
>
> - P1-1: Rule 41 kernel still overclaims end-to-end runtime suspension (shipped code returns a decision envelope, not a `Run` state transition).
> - P1-2: Deleted-module-name leaks remained in active operational Markdown runbooks (`ops/runbooks/*.md` + `ops/compose/sidecar-mem0.yml`) outside Rule 98's scope.
> - P1-3: Rule 96's kernel and implementation disagreed on whether the kernel block alone or EITHER the kernel block OR the rule card must cite a deferred sub-clause.
> - P2-1: `gate/rules/` file-count prose ("110 files total") didn't match the actual file count (108 files, keyed by unique rule id).
>
> rc11 closes all four findings + applies user-elected Rule 94 kernel/impl widening. See [docs/releases/2026-05-19-l0-rc11-corrective.en.md](2026-05-19-l0-rc11-corrective.en.md) (retracted) for the corrective wave and [docs/reviews/2026-05-19-l0-rc10-post-corrective-architecture-review-response.en.md](../reviews/2026-05-19-l0-rc10-post-corrective-architecture-review-response.en.md) for the per-finding closure.
>
> Historical content preserved below.

## TL;DR

The rc9 wave closed all 8 cited findings of the **rc8 post-corrective architecture review**
(Codex, 2026-05-18) and went green on CI (run 26055792484) with branch protection live on `main`.
The rc10 wave is **not** a fresh review response — it is a **category-driven re-audit** the user
requested: group the 8 rc8 findings into mechanistic defect families, then sweep each family
across the current post-rc9 corpus for hidden defects rc9's prevention rules missed.

Two families (I-α numeric drift, I-ε deleted-module-name leakage) yielded 9 hidden defects across
the live OpenAPI contract, the Helm chart triplet, the BoM module-metadata description, the dev
compose file, the rc9 release note's graph node/edge counts, and the live enforcer row count.
rc10 fixes all 9 surfaces, strengthens Rule 91 in-place to additionally check `enforcer_rows`
against the live source, and adds Rules 97 + 98 to mechanically close the two families' broader
mechanisms.

## Counts

| Item | rc10 | Δ vs rc9 |
|---|---|---|
| Reactor modules | 9 | unchanged |
| §4 constraints | 65 | unchanged |
| Active ADRs | 84 | +1 (ADR-0084) |
| Layer-0 governing principles | 13 | unchanged |
| Active engineering rules | 53 | +2 (Rules 97, 98) |
| Active gate rules | 110 | +2 (Rules 97, 98 sections) |
| Gate self-test cases | 165 | +4 (2 per Rule 97-98) |
| Enforcer rows | 138 | +4 (E135-E138); rc10 reconciliation of rc9 declared 116 vs actual 134 → live count of 138 after additions |
| Maven tests GREEN (under `./mvnw verify`) | 371 | unchanged (rc10 changes are docs/yaml/gate-script, no production code) |
| Architecture graph | 376 nodes / 535 edges | +7 nodes / +15 edges (rc9 baseline 369 / 520; Rules 97-98 + cards + ADR-0084 + 4 enforcer rows E135-E138 + cross-reference edges) |

## Family Taxonomy (rc10)

| Family | Mechanism | rc9 prevention rule | rc10 hidden defects | rc10 prevention |
|---|---|---|---|---|
| **I-α** | Numeric-claim drift | Rule 91 (only `active_gate_checks`) | enforcer_rows 116→134; rc9 release-note 360 nodes / 510 edges | Rule 91 strengthened; **Rule 97** added |
| **I-β** | Orphan authority | Rule 93 + STATE.md archive | Clean (BOM exempt per Rule 78) | n/a |
| **I-γ** | Active kernel overclaims deferred | Rule 96 | Clean (Rules 42, 46 narrowed; spot-audit clean) | n/a |
| **I-δ** | One-direction contract completeness | Rule 95 | Clean (12 SPI rows verified) | n/a |
| **I-ε** | Deleted-module-name leakage | Rule 94 (narrow scope) | 7 leaks: Helm chart triplet, OpenAPI live + pinned, BoM module-metadata, ops/compose.yml | **Rule 98** added (sibling rule, broader file scope) |
| **I-ζ** | Dual-authority drift | Rule 89 narrowed | Clean (sampled Rules 1-10, 33-48, 80-96 align) | n/a |
| **I-η** | Shadow corpus described as durable | Rule 92 | Clean (all generated trees properly marked) | n/a |

## Findings closed (hidden defects rc9 missed)

### I-α-1 — `enforcer_rows: 116` declared, `134` actual (delta +18)

`docs/governance/architecture-status.yaml:102` declared `enforcer_rows: 116` ("rc8 baseline 104 +
rc9 wave +12: E123-E134"). The live `^- id: E[0-9]+` count in
`docs/governance/enforcers.yaml` was actually **134** — undeclared rows had accreted across
rc7/rc8/rc9 CI-defect closure waves. Rule 91 (rc9) narrowly only checked `active_gate_checks`, so
the drift was invisible.

**Fix**: update field to 134 (then to 138 after E135-E138 added in this wave); **strengthen Rule
91** to additionally check `enforcer_rows` against the live `^- id: E[0-9]+` count.

### I-α-2 — rc9 release note declared 360 nodes / 510 edges, actual 369 / 520

`docs/releases/2026-05-19-l0-rc9-corrective.en.md:33` and `:87` declared "360 nodes / 510 edges"
with delta "+12 nodes / +24 edges". Live `architecture-graph.yaml#node_count` and `#edge_count`
were **369 / 520**, delta from rc8 baseline (348 / 486) **+21 / +34**. Both absolute and delta
arithmetic were wrong at rc9 write time. Rule 82 (`baseline_metric_matches_executable_manifest`)
doesn't enumerate "nodes" or "edges" in its canonical-key list, so no rule caught it.

**Fix**: correct both lines in the rc9 release note in place with `[rc10 correction]` markers;
**add Rule 97 (`release_note_numeric_truth`)** to enforce that the LATEST release note's absolute
node/edge claims match live `architecture-graph.yaml` values, with `rc[N] correction|snapshot|
first cut` and historical markers waiving inline historical references. Delta-formatted claims
(`+N nodes / +M edges`) are exempt by syntax.

### I-ε family — 7 deleted-module-name leaks Rule 94 missed

Rule 94's implementation (`gate/check_architecture_sync.sh:4527-4534`) scanned only three
surfaces: root `ARCHITECTURE.md`, `docs/governance/rules/*.md` (one-level),
`agent-*/src/test/java/**/*{Test,IT}.java`. The rule body claimed "every active `.md`, `.yaml`,
and `*.java` file" but explicitly exempted `docs/contracts/openapi-v1.yaml`,
`*/src/test/resources/*`, all `docs/adr/*`, and several `docs/` subtrees. The `ops/` directory was
NOT in the exemption list but was NOT in the file-discovery scope either — Rule 94 silently never
scanned it. This **kernel-vs-implementation drift** was itself an undiscovered defect.

The category sweep found 7 hidden leaks:

| # | File:Line | Leak |
|---|---|---|
| I-ε-1 | `spring-ai-ascend-dependencies/module-metadata.yaml:9` | BoM description: "pins agent-platform, agent-runtime, ..." |
| I-ε-2 | `ops/helm/spring-ai-ascend/values.yaml:7` | `repository: springaiascend/agent-platform` |
| I-ε-3 | `ops/helm/spring-ai-ascend/templates/deployment.yaml:18` | container `- name: agent-platform` |
| I-ε-4 | `ops/helm/spring-ai-ascend/Chart.yaml:3,9` | description + keyword |
| I-ε-5 | `docs/contracts/openapi-v1.yaml:287` | `x-contract-owner: agent-platform` |
| I-ε-6 | `docs/contracts/openapi-v1.yaml:294` | W1 note: "Integration test: agent-platform RunCursorFlowIT..." |
| I-ε-7 | `agent-service/src/test/resources/contracts/openapi-v1-pinned.yaml:265` | mirrored live `x-contract-owner` |
| I-ε-8 | `ops/compose.yml:1,26` | dev-compose service name (surfaced by Rule 98 self-validation) |

**Fix**: replace `agent-platform` → `agent-service` (and `agent-runtime` → `agent-runtime-core` /
post-Phase-C equivalent) in each surface, carrying `post-Phase-C / ADR-0078` markers for inline
historical context where appropriate. **Add Rule 98 (`broad_corpus_deleted_module_name_truth`)**
as a sibling to Rule 94 that scans `ops/**/*.{yaml,yml,tpl}`, `docs/contracts/*.yaml`,
`**/module-metadata.yaml` with the same word-boundary regex and ±3-line marker exemption.

## New prevention gate rules (Rules 97-98)

| # | Slug | Enforcers | Closes |
|---|---|---|---|
| 91 (strengthened) | `baseline_metric_matches_executable_manifest` (now also `enforcer_rows`) | E123, E124 + new sub-check | I-α-1 |
| 97 | `release_note_numeric_truth` | E135, E136 | I-α-2 |
| 98 | `broad_corpus_deleted_module_name_truth` | E137, E138 | I-ε family |

## Files changed

### Corpus truth (Track A)

- `docs/governance/architecture-status.yaml` — `enforcer_rows: 116 → 138`; `active_gate_checks: 108 → 110`; `gate_executable_test_cases: 161 → 165`; new `active_engineering_rules_post_rc10: 53` row.
- `docs/releases/2026-05-19-l0-rc9-corrective.en.md` — lines 33 + 87 corrected with `rc10 correction` inline marker.
- `spring-ai-ascend-dependencies/module-metadata.yaml:9` — description rewritten with current module list + `(pre-Phase-C / ADR-0078)` marker for the historical pin.
- `ops/helm/spring-ai-ascend/values.yaml:7` — `springaiascend/agent-platform` → `springaiascend/agent-service` + `post-Phase-C / ADR-0078` marker.
- `ops/helm/spring-ai-ascend/templates/deployment.yaml:18` — container name `agent-platform` → `agent-service`.
- `ops/helm/spring-ai-ascend/Chart.yaml:3,9` — description rewritten with post-Phase-C marker; keyword `agent-platform` → `agent-service`.
- `docs/contracts/openapi-v1.yaml:287,294` — `x-contract-owner: agent-platform` → `agent-service`; W1 note rewritten with post-Phase-C marker for the integration-test class path.
- `agent-service/src/test/resources/contracts/openapi-v1-pinned.yaml:265` — pinned snapshot updated to mirror live (the snapshot remains the contract-IT diff baseline).
- `ops/compose.yml:1,26` — service name `agent-platform` → `agent-service`; header comment with `post-Phase-C` marker.

### Prevention rules (Track B)

- `gate/check_architecture_sync.sh` — Rule 91 strengthened (new `_r91_enforcer_*` block); Rule 97 + Rule 98 added BEFORE the `# === END OF RULES ===` marker; Rule 94 `rule-98.md` added to per-card exemption list.
- `docs/governance/rules/rule-97.md` — NEW card (kernel + algorithm + cross-references).
- `docs/governance/rules/rule-98.md` — NEW card.
- `docs/governance/enforcers.yaml` — E135-E138 appended.
- `gate/test_architecture_sync_gate.sh` — 4 new fixtures: `test_rule_97_release_note_numeric_pos/neg`, `test_rule_98_broad_corpus_pos/neg`.
- `gate/rules/` — regenerated by `gate/lib/extract_rules.sh` (110 files total).

### Authority registration (Track C)

- `CLAUDE.md` — new `### rc8 post-corrective review category-sweep follow-up prevention wave` section with Rule 97 + Rule 98 kernel blocks.
- `docs/adr/0084-rc10-corpus-truth-and-prevention-widening.yaml` — NEW ADR (this wave's authority record).
- `docs/reviews/2026-05-19-l0-rc8-post-corrective-architecture-review-response.en.md` — rc10 appendix appended ("Category-sweep follow-up (rc10)").
- `README.md`, `gate/README.md` — count baselines refreshed.

## Verification

```bash
bash gate/check_parallel.sh             # GATE: PASS, parallel_summary: executed 110 rules
bash gate/test_architecture_sync_gate.sh # Tests passed: 165/165
python gate/build_architecture_graph.py # idempotent re-run
./mvnw -B -ntp verify                    # 371 tests GREEN (CI Linux + Docker)
gh.exe run view <rc10-run-id>            # conclusion: success
```

## Four pillars

- **performance**: unchanged at rc10 (rc9 baseline holds; no production code changed).
- **cost**: unchanged at rc10.
- **developer_onboarding**: improved — operational deploy artifacts (Helm + compose) now use the
  post-Phase-C module name; live API contract `x-contract-owner` field correctly reflects the
  consolidated module; BoM description tells contributors the actual module list to import.
- **governance**: improved — `enforcer_rows` numeric agreement now enforced (Rule 91 widening);
  release-note prose drift mechanically caught (Rule 97); deleted-module-name leakage covered
  across the operational + contract + metadata surfaces Rule 94 silently didn't scan (Rule 98).

## Authority

- ADR-0084 — accepts all rc10 hidden defects of the rc8 post-corrective review category-sweep
  follow-up (this wave's authority record).
- Response document (rc10 appendix appended):
  `docs/reviews/2026-05-19-l0-rc8-post-corrective-architecture-review-response.en.md`.
- Tag rc10 (retracted) supersedes rc9; rc9 NOT retracted (carries `superseded_by_tag: v2.0.0-rc10` (retracted) + inline
  `rc10 correction` markers on the two corrected lines).
