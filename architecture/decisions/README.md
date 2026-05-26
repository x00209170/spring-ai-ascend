# architecture/decisions/

**AUTHORED ZONE.** ADR markdown imports.

Structurizr DSL `!adrs decisions` (added in Wave 5) imports every ADR in
this directory into the workspace.

ADRs are authored under `docs/adr/*.{md,yaml}` (the existing corpus authority).
Wave 5 adds a build-time copy step that mirrors active ADRs into this
directory under stable filenames so that the Structurizr DSL can import them
without requiring `docs/adr/` path resolution.

W1 ships this README only; the copy mechanism lands in Wave 4 alongside
the L1 rendering rewire.
