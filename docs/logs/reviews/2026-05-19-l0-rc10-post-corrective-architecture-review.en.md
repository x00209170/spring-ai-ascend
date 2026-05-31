---
affects_level: L0
affects_view: process
proposal_status: review
authors: ["Codex architecture review"]
responds_to:
  - docs/reviews/2026-05-19-l0-rc8-post-corrective-architecture-review-response.en.md
  - docs/releases/2026-05-19-l0-rc10-corrective.en.md
related_adrs:
  - ADR-0052
  - ADR-0054
  - ADR-0070
  - ADR-0083
  - ADR-0084
related_rules:
  - Rule 41
  - Rule 46
  - Rule 92
  - Rule 94
  - Rule 96
  - Rule 98
affects_artefact:
  - CLAUDE.md
  - README.md
  - gate/README.md
  - gate/check_architecture_sync.sh
  - gate/rules/
  - docs/CLAUDE-deferred.md
  - docs/governance/architecture-status.yaml
  - docs/governance/enforcers.yaml
  - docs/governance/rules/rule-41.md
  - docs/governance/rules/rule-92.md
  - docs/governance/rules/rule-96.md
  - docs/governance/rules/rule-98.md
  - docs/governance/skill-capacity.yaml
  - docs/releases/2026-05-19-l0-rc10-corrective.en.md
  - docs/adr/0070-cursor-flow-and-skill-capacity-runtime.yaml
  - agent-service/src/main/java/ascend/springai/service/runtime/resilience/DefaultSkillResilienceContract.java
  - agent-service/src/main/java/ascend/springai/service/runtime/resilience/spi/SkillResolution.java
  - agent-service/src/test/java/ascend/springai/service/runtime/resilience/SkillCapacityResolutionIT.java
  - ops/compose/sidecar-mem0.yml
  - ops/runbooks/README.md
  - ops/runbooks/dr.md
  - ops/runbooks/digest-pin.md
---

# L0 rc10 Post-Corrective Architecture Review

## Executive verdict

Do **not** publish a no-findings L0 completion release note yet.

The rc9/rc10 corrective waves materially improved the corpus. The original
rc8 findings are mostly closed: the state ledger was archived, orphan DFX was
removed, `SkillCapacityRegistry` is now cataloged, Rule 42/46 kernels are
deferred-aware, deleted-module path truth is broader, and the executable gate
baseline now matches `check_parallel.sh`.

The Java microservice and agent-component architecture itself remains
directionally sound for L0. I did not find evidence that the agent-facing design
is materially over-designed: dynamic planning remains design-only, skills are
modeled through a narrow capacity/resilience SPI, memory and knowledge ownership
are explicitly split between C-side and S-side authority, engine dispatch is
registry-based rather than a universal DSL, and S2C callbacks keep the checked
suspension shape.

The remaining issues are contract and prevention-rule precision defects. They
are smaller than the rc8 defects, but still block a clean "L0 complete" note
because they affect what implementers believe is currently enforced.

Root cause in one sentence: rc10 widened several truth gates, but the same
mechanism reappears at the next boundary: active prose still says "is suspended"
where shipped code only returns a would-suspend decision, and broad-corpus
guards still miss active operational Markdown surfaces.

## Findings

### P1-1 - Rule 41 still overclaims end-to-end runtime suspension

Rule 41's active kernel says over-capacity callers are `SUSPENDED`, not
rejected. The shipped Java surface is narrower: `ResilienceContract.resolve`
consults the matrix and returns `SkillResolution.admitted=false` with
`SuspendReason.RateLimited`. The actual Run or dependent-step suspension is
still W2 orchestrator wiring.

Evidence:

| Artifact | Evidence |
|---|---|
| `CLAUDE.md:239` / `docs/governance/rules/rule-41.md:12` | Active kernel says "over-cap callers are SUSPENDED, not rejected". |
| `agent-service/src/main/java/ascend/springai/service/runtime/resilience/DefaultSkillResilienceContract.java:16-19` | Javadoc says callers translate the returned `SkillResolution` into `RunStatus.SUSPENDED`. |
| `agent-service/src/main/java/ascend/springai/service/runtime/resilience/spi/SkillResolution.java:10-12` | Javadoc states the caller is responsible for the actual `Run.withSuspension(...)` transition, and labels that as W2 orchestrator wiring. |
| `agent-service/src/test/java/ascend/springai/service/runtime/resilience/SkillCapacityResolutionIT.java:21-23` | Test proves capacity exhaustion maps to "would-suspend", not an actual Run status transition. |
| `docs/adr/0070-cursor-flow-and-skill-capacity-runtime.yaml:52-55`, `:99-101` | ADR-0070 says over-cap callers receive a `SkillResolution`; W2 adds `Run.suspendReason` when it actually transitions runs to `SUSPENDED`. |
| `docs/governance/architecture-status.yaml:466` | The saturation-suspends-Run semantics are still listed as design-only, deferred to W2. |

Impact:

This is the same class as the earlier Rule 42/46 problem, now at Rule 41. The
contract shape is good, but the active kernel is one step stronger than shipped
behavior. A W1 implementer could reasonably believe that calling
`ResilienceContract.resolve(tenant, skill)` currently performs or triggers a
Run suspension, while the code only returns a decision envelope.

Recommendation:

1. Narrow Rule 41's active kernel to: the YAML matrix must exist, the runtime
   resolver must consult it, and over-capacity resolution must return
   `SkillResolution.reject(SuspendReason.RateLimited)` rather than a failure.
2. Move the actual Run/dependent-step suspension transition to a deferred
   Rule 41 sub-clause, or split the current Rule 46.b "post-review
   strengthening" paragraph into a dedicated Rule 41.c because it covers
   generic skill saturation, not only S2C callbacks.
3. Add a prevention check parallel to Rule 96 that catches active kernels using
   end-state terms such as "are SUSPENDED" when the shipped code/test evidence
   only proves "would suspend" or "returns a suspension reason".

### P1-2 - Deleted-module-name leaks remain in active operational runbooks

rc10 correctly widened deleted-module-name coverage for Helm, compose,
OpenAPI, and module metadata. However, active operational Markdown runbooks
under `ops/runbooks/` are still outside the new Rule 98 scope and still use
current-tense `agent-platform` / `agent-runtime` names.

Evidence:

| Artifact | Evidence |
|---|---|
| `ops/runbooks/README.md:32` | Dev topology says "Single JVM (agent-platform + agent-runtime)". |
| `ops/runbooks/dr.md:11` | Scope still says "agent-platform deployment". |
| `ops/runbooks/digest-pin.md:31` | Verification command still scans `springaiascend/agent-platform:<tag>`. |
| `ops/compose/sidecar-mem0.yml:8` | Comment still says port 8001 avoids collision with `agent-platform` on 8080. |
| `CLAUDE.md:520` / `docs/governance/rules/rule-98.md:12` | Rule 98 scans `ops/**/*.{yaml,yml,tpl}`, `docs/contracts/*.yaml`, and `**/module-metadata.yaml`, but not `ops/**/*.md`; YAML comments can also remain misleading. |

Impact:

This is not a runtime architecture defect, but it is operational-authority drift.
Runbooks are exactly where on-call engineers and release operators look during
incidents. Leaving current-tense `agent-platform` labels there weakens the
post-ADR-0078 topology truth that rc10 is trying to protect.

Recommendation:

1. Update the runbooks to use `agent-service` and `agent-runtime-core` with
   historical markers only where needed.
2. Extend Rule 98 or add a sibling rule that scans `ops/**/*.md` and active
   operational YAML comments for deleted-module names outside historical
   markers.
3. Include a negative self-test with an `ops/runbooks/*.md` fixture containing
   bare `agent-platform`.

### P1-3 - Rule 96's kernel and implementation disagree on the source that must cite deferred sub-clauses

Rule 96's kernel says the matching `CLAUDE.md` kernel block must contain the
literal deferred sub-clause reference. The implemented gate accepts either the
CLAUDE kernel or the rule card. The response document explains the broader
"either surface" intent, but the active kernel and the enforcer are not aligned.

Evidence:

| Artifact | Evidence |
|---|---|
| `CLAUDE.md:504` / `docs/governance/rules/rule-96.md:12` | Kernel requires the matching `CLAUDE.md` kernel block to contain the literal `Rule N.<letter>` reference. |
| `gate/check_architecture_sync.sh:4617-4628` | Implementation marks coherence satisfied if either the CLAUDE kernel or the matching rule card contains the reference. |
| `docs/governance/rules/rule-96.md:40` | Enforcement prose still describes only the kernel-positive fixture. |

Impact:

The implemented policy is reasonable because cards have more room than
always-loaded kernels. The problem is that the policy as implemented is not the
policy as written. This is a small but direct Code-as-Contract drift in a rule
whose whole job is preventing kernel/deferred drift.

Recommendation:

1. Choose one policy. Prefer the implemented one: "either CLAUDE.md kernel or
   the rule card must acknowledge the deferred sub-clause".
2. Update CLAUDE.md, `rule-96.md`, and E133/E134 wording to match that policy.
3. Add a positive self-test where only the rule card cites `Rule N.b`, proving
   the card-only path is intentional.

### P2-1 - `gate/rules/` file-count prose is still imprecise

The canonical executable gate now reports 110 sections and passes. The
checked-in `gate/rules/` directory has 108 unique files because multiple
canonical sections share a rule id/file stem. Rule 92 verifies that every
unique header id has a matching file, but rc10 prose states the directory has
"110 files total".

Evidence:

| Artifact | Evidence |
|---|---|
| `bash gate/check_parallel.sh` | PASS; trailer says `parallel_summary: executed 110 rules; serial source defined 110 rules`. |
| Local file inventory | `gate/rules/` contains 108 `rule-*.sh` files. |
| `docs/releases/2026-05-19-l0-rc10-corrective.en.md:144` | Says `gate/rules/` was regenerated by `extract_rules.sh` with "110 files total". |
| `gate/README.md:51` | File table still says `check_architecture_sync.sh` is the canonical release gate with "108 active rules". |
| `docs/reviews/2026-05-19-l0-rc8-post-corrective-architecture-review-response.en.md:169` | Out-of-scope section says Rule 92 enforces file-vs-header parity, but the implementation enforces id-presence parity rather than one-file-per-section parity. |

Impact:

This is lower severity because the production gate runs from the canonical
monolith and passes. The issue is still worth fixing because it is the exact
shadow-corpus vocabulary rc9/rc10 set out to clarify.

Recommendation:

1. Change "110 files total" to the actual file count, or state explicitly that
   `gate/rules/` is keyed by unique rule id and therefore can have fewer files
   than executable sections.
2. Update `gate/README.md:51` from 108 to 110.
3. If the desired contract is truly one-file-per-executable-section, fix
   `extract_rules.sh` and Rule 92 to distinguish duplicate section ids instead
   of coalescing by rule id.

## Overdesign assessment

No new overdesign concern was found in the agent architecture itself. The most
important residual risk is the opposite: a few contracts describe W2 runtime
behavior as if it is already fully materialized. The component boundaries remain
appropriate for L0:

| Area | Assessment |
|---|---|
| Dynamic planning | Still appropriately design-only through `plan-projection.v1.yaml`; no premature W4 planner in shipped code. |
| Skills | The capacity matrix + `ResilienceContract` / `SkillCapacityRegistry` split is compact and useful, but Rule 41 must distinguish decision-envelope behavior from actual scheduler suspension. |
| Memory and knowledge | C-side/S-side ownership remains explicit; `GraphMemoryRepository` ownership in `agent-service` is coherent. |
| Engine dispatch | `EngineEnvelope` + `EngineRegistry` remains a good L0 contract; not over-generalized. |
| S2C callbacks | Checked suspension shape is correct; remaining S2C capacity and non-blocking behavior are explicitly deferred. |
| Governance | The main complexity pressure is in the prevention gate layer. It is valuable, but several rules need precise scope wording so the gate does not become a second, partially divergent architecture corpus. |

## Suggested corrective wave

Recommended scope for the next architecture-team response:

1. **Rule 41 semantic narrowing** - change active wording from "callers are
   SUSPENDED" to "resolver returns a would-suspend `SkillResolution`"; move
   actual step/run suspension to a deferred Rule 41 sub-clause or a clearly
   scoped W2 scheduler admission clause.
2. **Operational runbook sweep** - update `ops/runbooks/*.md` and
   `ops/compose/sidecar-mem0.yml`; extend Rule 98 coverage or add a sibling
   ops-runbook deleted-name guard.
3. **Rule 96 authority alignment** - make kernel/card/enforcer wording match
   the implemented "either CLAUDE kernel or rule card" policy.
4. **Shadow corpus wording cleanup** - correct `gate/rules/` file count prose
   and `gate/README.md` 108-vs-110 line.

After those are corrected and the standard gate/test/Maven set remains green,
a clean L0 completion release note should be appropriate.
