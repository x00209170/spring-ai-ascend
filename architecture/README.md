---
level: L0
view: scenarios
status: active
authority: "ADR-0147 (Structurizr Workspace Authority)"
---

# Architecture Workspace

`architecture/workspace.dsl` is the **future** architecture authoring root of
the spring-ai-ascend platform. Wave 1 lands the tooling foundation in
**advisory** mode; the gate does not yet block on profile violations. The
gate flips to **blocking** in Wave 5 after Waves 2–4 have populated the
authored zone, the generated zone, and the compatibility projection.

Read the full migration plan at
`D:\.claude\plans\structurizr-d-chao-workspace-spring-ai-adaptive-hellman.md`.

## Layout

```
architecture/
  workspace.dsl                  # Single entry point (root + views; W1 skeleton, grows W2/W3)
  README.md                      # this file
  profile/                       # Spring AI Ascend Architecture Profile
    profile.yaml                 # tags + machine settings
    profile.schema.yaml          # JSON Schema for profile.yaml
    relationship-types.yaml      # saa.rel vocabulary (source/target tag × type)
    required-properties.yaml     # required saa.* properties per tag
  features/                      # AUTHORED ZONE — capabilities/features/function-points
    README.md
    (capabilities.dsl)           # W2
    (function-points.dsl)        # W2
    (verification.dsl)           # W2
  docs/                          # AUTHORED ZONE — L1 narrative descriptions
    README.md
    L1/                          # W2; one .md per module
  decisions/                     # AUTHORED ZONE — ADR markdown imports
    README.md
  generated/                     # GENERATED ZONE — never hand-edit
    README.md
    (modules.dsl)                # W3 — emitted from */module-metadata.yaml
    (spi-catalog.dsl)            # W3 — emitted from module-metadata + Java scan
    (enforcers.dsl)              # W3 — emitted from docs/governance/enforcers.yaml
    (principles.dsl)             # W3 — emitted from docs/governance/principle-coverage.yaml
    (rules.dsl)                  # W3 — emitted from CLAUDE.md + rule cards
    (adr-graph.dsl)              # W3 — emitted from docs/adr/*.yaml
  views/                         # View definitions
    L0-system-context.dsl
    L1-development.dsl
```

## DSL conventions (corrected from the 2026-05-27 expert proposal)

- The DSL keyword for custom elements is `element`, NOT `customElement` —
  empirically verified per ADR-0148 empirical-finding-1.
- `Model.getElements()` includes CustomElements in structurizr-dsl 6.2.1
  (CustomElement extends GroupableElement extends Element). The
  `ProfileValidator` walks `getElements()` exactly once. Walking both
  `getElements()` AND `getCustomElements()` double-counts.
- Identifier scope is the default `flat`. Top-level identifiers are globally
  unique; nested-container short identifiers can be referenced from
  model-block-level relationships.
- Multi-word tags are written as one quoted string (`"SAA Module"`); the
  tags list is comma-separated.
- Property keys may be dotted (`"saa.id" "value"`); quote both name and value
  for safety.
- Every profile-tagged `element` MUST satisfy the common + per-tag
  required-property schema defined under `profile/required-properties.yaml`.

## Authored vs. generated zone

| Zone | Path | Edit by | Drift prevented by |
|---|---|---|---|
| Authored | `architecture/workspace.dsl` root, `features/`, `docs/L1/`, `decisions/` | humans | profile validator (Wave 1) |
| Generated | `architecture/generated/*.dsl` | machine emitters in `tools/architecture-workspace/` | byte-identical regeneration gate (Wave 3) |

## Running the tools

Validate the workspace parses and conforms to the profile:

```bash
./mvnw -f tools/architecture-workspace/pom.xml test
./mvnw -f tools/architecture-workspace/pom.xml exec:java -Dexec.args="validate architecture/workspace.dsl"
```

Emit the normalized JSON view:

```bash
./mvnw -f tools/architecture-workspace/pom.xml exec:java \
  -Dexec.args="normalize architecture/workspace.dsl out/architecture/normalized-model.json"
```

Wave 1 advisory gate:

```bash
bash gate/check_architecture_workspace.sh
```

## Lifecycle

- W0 (2026-05-27 — `9611096`): spike + ADRs landed; spike artifacts under
  `architecture-spike/`.
- W1 (current): production tooling + profile YAMLs + workspace.dsl
  skeleton + advisory gate + impact matrix.
- W2: authored zone populated with capabilities/features/function-points.
- W3: 7 fragment emitters under `tools/architecture-workspace/src/main/java/.../fragment/`.
- W4: reverse projection → byte-identical `architecture-graph.yaml`.
- W5: gate advisory → blocking; Rule G-1.b amended; 14-day soak.
- W6: YAML authority sunset (60-day soak).
- W7: final cleanup; closure ADR + family registration.
