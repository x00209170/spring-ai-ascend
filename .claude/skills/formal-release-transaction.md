---
name: formal-release-transaction
description: |
  Use this skill when preparing, reviewing, or publishing a formal L0 release
  note. Validates the formal release transaction shape: evidence bundle present,
  `formal_release` frontmatter coherent, current-vs-forward claims expressed, and
  recurring-family closures recorded. (The former single-source render pipeline —
  /refresh-architecture-doc + .md.j2 template re-render — was removed in the
  2026-06-03 governance cleanup; release notes are hand-authored from the
  release-readiness template, validated structurally + by the gate.)
scope: project
---

# /formal-release-transaction

## Purpose

A formal release note is the last derivative artefact in the architecture-document
chain. Its numeric claims MUST come from the generated evidence bundle (never
hand-typed), and its frontmatter + claim structure MUST be coherent before the
note is trusted. This skill drives that validation flow.

## Required workflow

### Stage A — Freeze the candidate commit

Record the SHA in the release note frontmatter:

```yaml
---
formal_release: true
evidence_bundle: gate/release-ci-evidence/<release-id>.evidence.yaml
release_candidate_commit: <40-char SHA>
status: formal-release-candidate
---
```

### Stage B — Generate the evidence bundle

```bash
python3 gate/lib/build_release_evidence.py \
    --run-self-tests \
    --include-maven-reports \
    --output gate/release-ci-evidence/<release-id>.evidence.yaml
```

The bundle is the canonical numeric input for the release note — every count comes
from it, never hand-typed. `build_release_evidence.py` also runs the formal-release
transaction-shape validation (`gate/lib/check_formal_release_transaction.py`):
scaffold files exist; the release-readiness schema declares its core models;
the evidence-bundle `baseline_comparison` shows all `matches: true`; and the
frontmatter `formal_release: true` <-> `evidence_bundle:` reference is consistent.

### Stage C — Hand-author the release note

Author the note from `docs/governance/release-readiness/formal-release-note-template.en.md`,
filling the generated tables (Evidence, Baseline, Family Closures) from the
evidence bundle's values. Do not hand-type counts that diverge from the bundle.

### Stage D — Current-vs-forward claims (hand-author, validated structurally)

For every staged behavior, write a `CurrentForwardClaim` record (per the
release-readiness.schema.yaml model): subject; current shipped behavior; current
verified by (tests, gates, code paths); forward behavior; promotion trigger; the
phrase that must not be claimed before promotion.

### Stage E — Recurring-family closures

For every touched recurring family, write a `DefectFamilyClosure`: family id;
cited findings; sibling surfaces checked; closure result
(`closed | accepted_residual | not_ready`); residual risk (empty if closed).

### Stage F — Final gate

```bash
bash gate/check_parallel.sh
./mvnw clean verify
```

The active gate rules + Maven verify MUST PASS.

## Release decision rule

No evidence bundle means no `formal_release: true` claim. A corrective RC note may
still be published, but it must not claim final L0 closure and must mark itself
`status: corrective` in frontmatter.

## Files to load when needed

- `docs/governance/release-readiness/release-readiness.schema.yaml`
- `docs/governance/release-readiness/formal-release-note-template.en.md`
- `docs/governance/recurring-defect-families.yaml`
- `docs/governance/architecture-status.yaml`
- latest file from `docs/logs/releases/`

## Composes with

- `/commit-mode` — system-commit phase contract exit criteria gate the final
  release-note commit.
- `/refresh-defect-archive` — companion for the recurring-defect ledger refresh.
