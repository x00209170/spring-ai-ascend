# Governance Waves — Consolidated History

> Historical record of governance-rule waves from rc1 through rc11. Authority: this document supersedes ADR-0083 / ADR-0084 / ADR-0085 as the active narrative for rcN-closure history (those ADRs migrate to `docs/logs/adr-amendment-narratives/` at Wave 4 of the migration plan).
>
> Each row records: wave name, date, ADR (if any), rules added (engineering-rule namespace), enforcers added (E-numbers), key findings closed, hidden defects surfaced, and a one-line reason the wave existed.

| Wave | Date | ADR | Rules added | Enforcers | Findings closed | Hidden defects | Reason |
|---|---|---|---|---|---|---|---|
| L1 Rule-28 expansion + Phase K + L1.x Telemetry | 2026-04..05 | ADR-0049, ADR-0055 | 1-29 + 28a-28k sub-checks + 30-44 | E1-E84 | initial L0 + telemetry contract spine | (none cited) | First active rule cohort; pre-rc namespace |
| W1.x L0 ironclad-rule wave | 2026-05-15 | ADR-0069 | 45-52 | E85-E92 | LucioIT W1 §6/§7 import (channel isolation, cursor flow, async I/O, Chronos hydration, five-plane topology, tenant isolation, skill capacity, sandbox subsumption) | — | Absorb LucioIT competitive-pillar rules into spring-ai-ascend |
| W1.x Phases 8-9 (HTTP reconciliation) | 2026-05-15..16 | ADR-0070 | 53-54 | E93 | W1 HTTP cursor migration; idempotency W0 | — | Migrate POST /v1/runs to 202+TaskCursor |
| W2.x Engine Contract Structural Wave | 2026-05-15..16 | ADR-0071, ADR-0072, ADR-0073, ADR-0074, ADR-0075, ADR-0076, ADR-0077 | 55-60 | — | Heterogeneous engine contract (envelope/matching/hooks/S2C/scope) | — | P-M principle operationalisation |
| v2.0.0-rc2 second-pass closure (F-α/F-β/F-γ family) | 2026-05-17 | (none) | 61-63 | — | 4 cited + 12 hidden via corpus audit; closed 13, rejected 4 with rationale | awk regex bug at validation surface | "Run rule on REAL file before ship" lesson |
| v2.0.0-rc3 cross-constraint audit (R-α/R-β/R-γ family) | 2026-05-17 | (amends ADR-0019, ADR-0021, ADR-0034) | (rule-redesign, no new numbers) | E94-E96 | 9 cited (5 already closed by rc2) | SuspendSignal sealed-checked-variant unifies S2C; s2c.spi package move makes "java.* + spi siblings" literally true | Constraint-system integrity vs document-truth |
| CLAUDE.md token-optimization wave | 2026-05-17 | ADR-0078 | 67-71 | E97-E101 | always-loaded byte budget; kernel-vs-card byte parity; orphan-card detection | — | Cut byte spend on AI-agent context window |
| Gate-script efficiency wave | 2026-05-17 | (none) | 72-73 | E102-E103 | per-rule duration regression; gate config schema | — | Catch performance regression in gate machinery |
| Beyond-SDD review response | 2026-05-18 | ADR-0079 (rule-by-rule), Linux-first | 74, 79 | E104, E112 | Linux-first dev environment; evidence-first debug sequence | WSL is 6-20× faster than Git Bash and surfaces real bugs Git Bash hides | Codify verification environment + debug discipline |
| SPI metadata integrity wave | 2026-05-18 | (none) | 75-78 | E105-E108 | SPI packages populated + no split packages + .spi convention + DFX-matches-metadata | — | SPI catalog ↔ module-metadata.yaml bidirectional truth |
| rc4 cross-constraint review response (D-α/D-β family) | 2026-05-18 | ADR-0074-amend, ADR-0032-amend | 80-83 | E113-E116 | 5 findings (P0-1..P1-3) closed; structured baseline_metrics block (Rule 82 single source); Rule 81 caught agent-middleware status drift | — | Constraint-system integrity prevention layer |
| rc5 post-response review response (E-α/E-β family) | 2026-05-18 | ADR-0080 | 84-85 | E117-E118 | 4 findings closed; ResilienceContract .spi package alignment | — | Active-module path truth + catalog-row-metadata parity |
| rc6 post-response review response (F-α/F-β family) | 2026-05-18 | ADR-0081 | 86-87 | E119-E120 | 6 findings + 3 hidden + 2 self-check-surfaced; ResilienceContract dual-surface reconciliation | — | Root-ARCHITECTURE count + path truth; status YAML allowed_claim truth |
| rc7 post-corrective review response (G-α/G-β family) | 2026-05-18 | ADR-0082 | 88-89; Rule 86 fenced-tree extension | E121-E122 | 5 cited + 14 hidden; GraphMemoryRepository canonical owner | Compound defect (em-dash/double-dash separator), regex-blind-spot (fenced-block exclusion), SSOT-can-be-wrong (Rule 82 vacuous because baseline encoded broken count) | Serial/parallel slug parity + self-test fail-closed coverage |
| rc8 post-corrective review response (H-α..H-ε family) | 2026-05-19 | ADR-0083 | 91-96 | E123-E134 | 7 findings + 6 hidden + CI defect cluster (NoOpAsyncRunDispatcher / IdempotencyStoreAutoConfiguration / WebSecurityConfig / PostureBindingIT / spring.ai eager-credential / RunResponse @Schema / Rule 69 pipe-race / jackson-yaml runtime scope / quickstart postgres service / arch-graph CI regen step) | first green CI run on main since rc1 (48/50 = 96% red); branch protection now requires Maven build + Quickstart smoke as status checks | GNU awk doesn't honor `\b` (use POSIX bracket-class); `@ConditionalOnBean/@ConditionalOnMissingBean` ordering hazard on Spring Boot 4 |
| rc8 post-corrective category-sweep follow-up (rc10) (I-α..I-η family) | 2026-05-19 | ADR-0084 (later retracted) | 97-98 + Rule 91 widening | E135-E138 | 9 hidden defects rc9 missed; 2 families had hidden (I-α numeric drift, I-ε deleted-name leakage) | sibling rule > widen-in-place when reviewer scope is at stake (Rule 98 vs Rule 94 widening) | rc10 RETRACTED per ADR-0085 — content carried forward through rc11 |
| rc10 post-corrective review response (rc11) (J-α..J-δ family) | 2026-05-19 | ADR-0085 (retraction + ratchet) | 99-100 + Rule 41 narrowed + Rule 96 aligned + Rule 92 clarified + Rule 94 impl widened + Rule 98 widened + Rule 41.c sub-clause | E139-E142 | 4 cited findings (P1-1 Rule 41 kernel overclaim, P1-2 ops/runbooks leaks, P1-3 Rule 96 kernel-vs-impl drift, P2-1 gate/rules file-count prose) + user-elected Rule 94 widening + rc10 retraction | CRITICAL hidden defect: docs/quickstart.md boot commands referenced DELETED agent-platform/agent-runtime modules — broken developer onboarding regression that survived 11+ waves because rc9 Rule 94 narrow scope explicitly exempted quickstart.md | Kernel-vs-impl drift in rules whose JOB is preventing drift; methodology: categorise→sweep→batch-fix→prevention is now codified |

## Methodology (codified across waves)

The recurring pattern documented across rc4..rc11 is:

1. **External review** raises P0/P1/P2 findings (Codex or Claude reviewer; sometimes both).
2. **Defect families** categorised by surface and shape (e.g. F-α document-truth, F-β kernel-coherence, J-γ kernel-vs-impl-disjunction).
3. **Corpus sweep** for hidden instances of each family — almost always finds 2× to 3× more defects than the cited findings.
4. **Batch fix** within each family, not file-by-file.
5. **Prevention gate** added so the next iteration of the same family cannot recur.

This is the *categorise → sweep → batch-fix → prevention* discipline. It is now hard-codified in:
- The plan-mode workflow (this current migration follows the same pattern at a meta-level).
- Rule 99 / G-3.e (kernel-terminal-verb vs shipped-decision check) and Rule 100 / G-3.f (kernel-implementation disjunction truth) — both born from rc11 to catch the *next* iteration of "the rule's own kernel and impl disagree."

## Why this file exists

Before the Wave 4 migration ratchet, rcN-closure history was scattered across ADR-0083/0084/0085 (each a separate ADR per wave), inline `Closes rcN ...` annotations in 21+ rule kernels, and the cluster headers `### rcN ... wave` in CLAUDE.md. The ratchet consolidates all of this into a single rcN-tagged log file, freeing the normative read path of rcN literals.

For deep details on any wave, see:
- `docs/logs/releases/<date>-l0-rcN-*.en.md` — the release note for that wave.
- `docs/logs/reviews/<date>-l0-rcN-*-architecture-review*.en.md` — the review proposal + response.
- `docs/logs/waves/rcN-<topic>/findings.md` — extracted findings narratives (created in Wave 3 scrub).
- `docs/logs/adr-amendment-narratives/00<n>-*.md` — extracted ADR amendment blocks (created in Wave 4 ratchet).
