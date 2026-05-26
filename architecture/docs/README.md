# architecture/docs/

**AUTHORED ZONE.** Engineers edit these files directly.

Wave 2 of the Structurizr workspace authority migration populates
`architecture/docs/L1/<module>.md`: one Markdown narrative per L1 module
describing capabilities, scenarios, and key flows.

Wave 4 rewires the existing `docs/governance/templates/*.j2` Jinja2 templates
to ingest workspace-derived JSON for STRUCTURAL sections (modules, SPI
packages, capabilities) while continuing to read narrative sections from
files under `architecture/docs/L1/`.

Wave 5 declares these files canonical L1 narrative.
