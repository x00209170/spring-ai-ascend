# Single-Source Rendering Templates

This directory hosts the Jinja2 templates and per-template context schemas
that drive Rule G-13 (Single-Source Rendering Coherence; authority ADR-0119).

The principle: every dynamic claim in a derived architecture document lives
in exactly one of (a) an authority YAML, (b) a deterministically-generated
YAML, or (c) a rendered slot in a `*.md.j2` template here. The rendered
`.md` artefact MUST be byte-identical to `render(template, data)`; the gate
fails on any drift.

## Lifecycle and waves

- **W0 (rc42)** — this README, the `surface-classification.yaml` registry
  schema, and the architectural decision (ADR-0119) land. No templates
  yet.
- **W1** — generalised render engine + render-context loader +
  byte-identical idempotency gate (`gate/lib/render_template.py`,
  `gate/lib/load_render_context.py`,
  `gate/check_template_render_idempotency.sh`). Registry stays empty.
- **W2** — `/refresh-architecture-doc` orchestrator skill +
  phase-contract integration. Still no templates.
- **W3** — first template: `release-note.md.j2` +
  `release-note.context-schema.yaml`. Migrates release-note authoring
  onto the render pattern; historical rc1–rc41 release notes are
  grandfathered.
- **W4** — `recurring-defect-families.md.j2` (renders the human view of
  the recurring-families ledger from the `.yaml` SSOT). Subsumes Rule
  G-9.c yaml↔md parity by construction.
- **W5** — `contract-catalog.md.j2` (renders the SPI catalog from a Java
  source scan + `module-metadata.yaml#spi_packages`). Subsumes Rule
  G-8.b/e.
- **W6** — root `ARCHITECTURE.md` becomes a hybrid template; numeric +
  module-list cells rendered. Subsumes Rule G-2.d.
- **W7** — every `agent-*/ARCHITECTURE.md` becomes a hybrid template;
  Development View tree (Rule G-1.1.a) and SPI Appendix (Rule
  G-1.1.b) sections rendered. Subsumes those sub-clauses' helpers.
- **W8** — `README.md` + `gate/README.md` numeric blocks rendered.
  Subsumes Rule G-2.b.
- **W9** — `CLAUDE.md` rule-index table rendered from rule-card
  frontmatter; phase contracts' Active Rules tables rendered.
- **W10** — cleanup, subsumed-rule retirement, final closure ADR.

## Authoring guidance

- Templates live as `*.md.j2`.
- Each template has a sibling `<name>.context-schema.yaml` that declares
  the variable contract.
- The Python context loader (`gate/lib/load_render_context.py`, W1) reads
  authority surfaces and emits a render context yaml.
- The render engine (`gate/lib/render_template.py`, W1) is deterministic:
  sorted dict iteration, `LC_ALL=C` sort, fixed-precision floats,
  no `datetime.now()` / `uuid.uuid4()` / `random`, Jinja2 environment
  configured with `trim_blocks=True`, `lstrip_blocks=True`,
  `keep_trailing_newline=True`.
- Rendered outputs carry a `<!-- DO NOT EDIT — generated from <template> -->`
  banner as the first non-frontmatter line. Hand-edits to the rendered
  file are blocked at pre-commit (W9 hook) and at gate (Rule 126).

## Why this directory exists in W0 (before any template)

The `surface-classification.yaml` schema and the architectural decision
need to be a stable authority that W3..W9 each add rows to. Landing the
registry empty in W0 lets Rule 126 enforce existence of the registry
itself today — and each subsequent wave is a small additive change to
the registry, the loader, and one template.

## See also

- [`../rules/rule-G-13.md`](../rules/rule-G-13.md) — rule card.
- [`../../adr/0119-single-source-rendering.yaml`](../../adr/0119-single-source-rendering.yaml) — authoring ADR + 11-wave roadmap.
- [`../../../gate/build_architecture_graph.py`](../../../gate/build_architecture_graph.py) — proof-of-concept of deterministic render idempotency (the architecture-graph itself uses this pattern, Rule G-1.b enforces byte-identical regen).
