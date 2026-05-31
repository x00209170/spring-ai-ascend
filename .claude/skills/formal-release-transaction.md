---
name: formal-release-transaction
description: |
  Use this skill when preparing, reviewing, or publishing a formal L0 release
  note. Delegates to `/refresh-architecture-doc` for the upstream authority
  surface refresh + template re-render stages; then validates the formal
  release transaction shape (evidence bundle present, formal_release
  frontmatter coherent, current-vs-forward claims expressed, recurring-family
  closures recorded). Replaces the previous single-stage post-author
  validator with an integrated flow.
scope: project
---

# /formal-release-transaction

## Purpose

A formal release note is the last derivative artefact in the
architecture-document chain. Before its content can be trusted, every
upstream authority surface it references MUST be refreshed and every
downstream rendered template MUST be byte-identical to those surfaces
(Rule G-13). This skill drives the integrated flow.

## Required workflow

### Stage A — Run `/refresh-architecture-doc` first

The 9-stage refresh-order discipline (pre-flight gate, ADRs, graph,
status.yaml, CLAUDE.md kernel + cards, families ledger, phase contracts,
template render idempotency, README + numeric claims, full gate)
applies to every release. Do not skip to stage B with refresh-skill
stages failing.

### Stage B — Freeze the candidate commit

Record the SHA in the release note frontmatter:

```yaml
---
formal_release: true
evidence_bundle: gate/release-ci-evidence/<release-id>.evidence.yaml
release_candidate_commit: <40-char SHA>
status: formal-release-candidate
---
```

### Stage C — Generate the evidence bundle

```bash
python3 gate/lib/build_release_evidence.py \
    --run-self-tests \
    --include-maven-reports \
    --output gate/release-ci-evidence/<release-id>.evidence.yaml
```

The bundle is the canonical numeric input for the release note. Hand-typed
counts are forbidden under Rule G-13.b — every count comes from the bundle
or from the render-context loader.

### Stage D — Validate the transaction

```bash
bash gate/check_formal_release_transaction.sh \
    --evidence gate/release-ci-evidence/<release-id>.evidence.yaml
```

Checks: scaffold files exist; release-readiness schema declares all five
core models PLUS the W1-added RenderContext / TemplatedArtifact / RenderSchema
models; evidence-bundle baseline_comparison shows all `matches: true`;
frontmatter `formal_release: true` ↔ `evidence_bundle:` reference is consistent.

### Stage E — Render the release note from template (W3+)

Once the release-note template lands in W3, hand-typed prose for the
generated tables (Evidence, Baseline, Family Closures, Authority Refresh)
is forbidden. Instead:

```bash
python3 gate/lib/load_render_context.py release_note \
    --seed gate/release-ci-evidence/<release-id>-narrative-seed.yaml \
    --run-self-tests --include-maven-reports \
    --output gate/release-ci-evidence/<release-id>-render-context.yaml
python3 -m gate.lib.render_template \
    docs/governance/templates/release-note.md.j2 \
    --data gate/release-ci-evidence/<release-id>-render-context.yaml \
    --output docs/logs/releases/<date>-l0-<release-id>.md
```

Pre-W3, hand-author the release note using the
`docs/governance/release-readiness/formal-release-note-template.en.md`
template; the W3 cutover migrates to `.md.j2`.

### Stage F — Current-vs-forward claims (hand-author, validated structurally)

For every staged behavior, write a `CurrentForwardClaim` record (per
the release-readiness.schema.yaml model):
- subject,
- current shipped behavior,
- current verified by (tests, gates, code paths),
- forward behavior,
- promotion trigger,
- phrase that must not be claimed before promotion.

### Stage G — Recurring-family closures

For every touched recurring family, write a `DefectFamilyClosure`:
- family id,
- cited findings,
- sibling surfaces checked,
- closure result (`closed | accepted_residual | not_ready`),
- residual risk (empty if closed).

### Stage H — Final gate

```bash
bash gate/check_parallel.sh
./mvnw clean verify
```

All 140+ rules + Maven verify MUST PASS.

## Release decision rule

No evidence bundle means no `formal_release: true` claim. A corrective RC
note may still be published, but it must not claim final L0 closure and
must mark itself `status: corrective` in frontmatter.

## Files to load when needed

- `docs/governance/release-readiness/release-readiness.schema.yaml`
- `docs/governance/release-readiness/formal-release-note-template.en.md` (pre-W3)
- `docs/governance/templates/release-note.md.j2` (post-W3)
- `docs/governance/recurring-defect-families.yaml`
- `docs/governance/architecture-status.yaml`
- latest file from `docs/logs/releases/`

## Composes with

- `/refresh-architecture-doc` — mandatory upstream stage.
- `/commit-mode` — system-commit phase contract exit criteria gate the
  final release-note commit.
- `/refresh-defect-archive` — companion for the recurring-defect ledger
  refresh.
