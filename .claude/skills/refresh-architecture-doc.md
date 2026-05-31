---
name: refresh-architecture-doc
description: |
  Use this skill whenever an authority surface listed in any template's
  `context_loader:` is being refreshed — ADR, status.yaml, module-metadata,
  rule card, recurring-families.yaml, principle-coverage.yaml, enforcers.yaml,
  or pom.xml. Drives the refresh cascade in the correct order with
  stage-by-stage gates so the rendered templates stay byte-identical to
  authority surfaces (Rule G-13 / ADR-0119). Also invoked transitively
  by `/formal-release-transaction` for the upstream stages.
scope: project
---

# /refresh-architecture-doc

## Purpose

The architecture corpus has multiple authority surfaces (ADRs,
status.yaml, module-metadata.yaml, rule cards, recurring-families.yaml)
and multiple derived artefacts (release notes, root + L1 ARCHITECTURE.md,
contract-catalog.md, recurring-families.md, README.md, gate/README.md,
CLAUDE.md rule index, phase contracts' Active Rules tables). Refreshing
an authority surface WITHOUT re-rendering its downstream templates is
the single largest source of drift defects in the corpus (F-numeric-drift
recurs 14+ times; F-deleted-module-name-leakage, F-authority-surface-path-drift,
F-cross-authority-agreement, and F-l1-architecture-grounding-gap all
spring from the same "refresh first, render lag" mechanism).

This skill enforces the correct refresh order and surfaces the
template-render gate at the right point.

## Required workflow (9 stages)

Each stage runs an explicit gate command. Continue only if the gate is
green.

### Stage 0 — Pre-flight: current state is clean

```bash
bash gate/check_parallel.sh
```

If the gate is failing on the current tip, do NOT begin a refresh on top
of the failure. Fix the failing rules first.

### Stage 1 — ADRs land first

If this refresh introduces a new architectural decision, write the ADR
yaml under `docs/adr/NNNN-*.yaml` BEFORE editing any other surface.
Bring the relevant `principles/*.md`, `rules/*.md` cards alongside if
the ADR introduces a new rule.

```bash
ls docs/adr/$(printf '%04d' $NEXT)-*.yaml
```

### Stage 2 — Regenerate the architecture graph

```bash
python3 gate/build_architecture_graph.py
```

The graph is byte-identical idempotent under Rule G-1.b — if running
this twice in a row writes different bytes, that itself is a defect.

### Stage 3 — Refresh status.yaml baseline_metrics

Update `docs/governance/architecture-status.yaml` so the
`baseline_metrics` block reflects the new live values: rule count,
gate count, enforcer count, ADR count, family count, graph nodes/edges.
The `allowed_claim` paragraph mirror should match.

### Stage 4 — Refresh CLAUDE.md kernel + rule cards

If a rule kernel changed, edit BOTH the `CLAUDE.md` `#### Rule` paragraph
AND the matching `docs/governance/rules/rule-*.md` `kernel:` scalar.
Rule G-3.b requires byte-match. Same for any deferred-clause changes in
`docs/CLAUDE-deferred.md` (Rule G-3.d).

### Stage 5 — Refresh recurring-defect-families ledger

```bash
# if there is a /refresh-defect-archive companion skill, invoke it here
```

Edit `docs/governance/recurring-defect-families.yaml` (the SSOT). Add
new families or update existing rows for any surface that this refresh
touched. Per Rule G-9.b, a "no-op edit" (whitespace, trailing newline,
or bare `last_updated:` bump without family-state change) fails.

The companion `.md` is rendered from the `.yaml` (post-W4); pre-W4 it
is hand-authored and Rule G-9.c enforces parity. Post-W4 you do NOT
touch the `.md` directly.

### Stage 6 — Refresh phase-contract Active Rules tables

If this refresh added a new rule card, cite it in at least one phase
contract under `docs/governance/contracts/*.md`. Rule G-11 requires
every rule card to appear in ≥1 phase contract (orphan-rule check) and
every contract row to point at an existing card (ghost-rule check).

### Stage 7 — Re-render every templated + hybrid surface

```bash
python3 gate/lib/check_template_render_idempotency.py
```

This runs the Rule G-13.b byte-identical render check on every entry in
`docs/governance/templates/surface-classification.yaml`. If any entry
drifts, re-render it:

```bash
# Per-template render (example for release notes, post-W3):
python3 gate/lib/load_render_context.py release_note \
    --output gate/release-ci-evidence/<rc>-render-context.yaml \
    --seed gate/release-ci-evidence/<rc>-narrative-seed.yaml \
    --run-self-tests --include-maven-reports
python3 -m gate.lib.render_template \
    docs/governance/templates/release-note.md.j2 \
    --data gate/release-ci-evidence/<rc>-render-context.yaml \
    --output docs/logs/releases/<date>-l0-<rc>-<slug>.md
```

Today's `surface-classification.yaml` is empty (W0 state); this stage is
vacuously green until W3 lands the first template.

### Stage 8 — Update README + gate/README numeric claims

If the refresh changed any `baseline_metrics` value, update the prose
mirrors in `README.md` and `gate/README.md`. (Post-W8 these are also
rendered by the template engine; you'll only need to edit the `.md.j2`
sources.)

### Stage 9 — Full gate green

```bash
bash gate/check_parallel.sh
```

All 140+ rules MUST PASS. No `--allow-drift` override exists for Rule
G-13; the only resolution is re-render.

## Composes with

- `/formal-release-transaction` — release-note authoring delegates to
  this skill for stages 0–7, then runs its own evidence-bundle
  validation as stage 8.
- `/commit-mode` — system-commit phase contract exit criteria require
  this skill's stages 0–9 all PASS before commit.
- `/refresh-defect-archive` — companion that handles stage 5
  (recurring-defect-families ledger refresh).

## Does NOT

- Does NOT auto-commit. You stage and commit; the skill orchestrates
  the refresh.
- Does NOT bypass any gate. Each stage is gated; failures must be
  resolved at the stage, not skipped.

## See also

- ADR-0119 — single-source rendering authority + 11-wave roadmap.
- `docs/governance/rules/rule-G-13.md` — render-coherence rule card.
- `docs/governance/templates/surface-classification.yaml` — template registry.
- `docs/governance/templates/README.md` — author guide for templates.
- `gate/lib/render_template.py` — render engine.
- `gate/lib/load_render_context.py` — context loader (plugin-dispatched).
- `gate/lib/check_template_render_idempotency.py` — Rule G-13.b gate driver.
