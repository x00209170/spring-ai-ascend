# architecture/features/

**AUTHORED ZONE.** Engineers edit these files directly.

Wave 2 of the Structurizr workspace authority migration populates:

- `capabilities.dsl` — every L1 capability listed in
  `docs/governance/architecture-status.yaml#capabilities`.
- `function-points.dsl` — every L1 function point (run lifecycle, cancel,
  status polling, ingress, graph execution, S2C callback, GraphMemory, etc.).
- `verification.dsl` — SAA Test elements + `verifies` relationships for
  function points that have test coverage.

Each profile-tagged element MUST satisfy the schema in
`architecture/profile/required-properties.yaml`. New capabilities or function
points land via a single PR that updates BOTH the relevant `.dsl` file AND
the source ADR (`saa.sourceAdr`).

The DSL keyword for these declarations is `element`. Example:

```structurizr
capRunLifecycle = element "Run Lifecycle" "Capability" "Run lifecycle — create/cancel/status/replay" "SAA Capability" {
    properties {
        "saa.id" "CAP-RUN-LIFECYCLE"
        "saa.kind" "capability"
        "saa.level" "L1"
        "saa.view" "scenarios"
        "saa.status" "shipped"
        "saa.owner" "agent-service"
        "saa.sourceAdr" "ADR-0020"
    }
}
```
