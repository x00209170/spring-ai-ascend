---
level: L0
view: scenarios
status: active
authority: "ADR-0068 (Layered 4+1 + Architecture Graph); Wave 1B triage 2026-05-19"
---

# ADR Index

The current ADR corpus is organised in three partitions:

| Partition | Path | Count | Purpose |
|---|---|---|---|
| Active | `docs/adr/*.{md,yaml}` | ~70 | Currently-evolving design decisions; AI agents read these for current authority |
| Locked | `docs/adr/locked/*.md` | 11 | Foundational decisions baked into code/build (no further evolution expected) |
| Archive | `docs/logs/adr-amendment-narratives/*` | 4 | Superseded ADRs + rcN-closure narratives absorbed into `docs/logs/governance-waves.md` |

See [`docs/logs/adr-triage-manifest.md`](../logs/adr-triage-manifest.md) for the full triage decision record.

## Locked (foundational; no future evolution)

| ID | Title | Why locked |
|---|---|---|
| [0001](locked/0001-java-21-spring-boot-runtime.md) | Java 21 + Spring Boot 4 runtime | Pinned in `pom.xml`; compile-time enforcement |
| [0002](locked/0002-spring-ai-llm-gateway.md) | Spring AI ChatClient as LLM gateway | No alternative gateway surface |
| [0004](locked/0004-postgres-primary-data-store.md) | Postgres as primary data store | All Flyway migrations Postgres-only |
| [0005](locked/0005-tenant-isolation-guc-set-local.md) | Postgres GUC `SET LOCAL app.tenant_id` + RLS | Enforced by Rule R-J.a / R-J |
| [0006](locked/0006-posture-model-dev-research-prod.md) | Three-posture model | Codified in `AppPostureGate` |
| [0010](locked/0010-spring-security-oauth2.md) | Spring Security OAuth2 Resource Server | JWT validation codified (Rule 56) |
| [0011](locked/0011-flyway-schema-migration.md) | Flyway migration tool | Integration tests rely on it |
| [0014](locked/0014-contract-spine-versioning-policy.md) | Contract-spine SemVer policy | Fixed |
| [0015](locked/0015-layered-architecture-capability-model.md) | Layer 0/1/2 capability model | Foundation for ADR-0068 (4+1) |
| [0020](locked/0020-runlifecycle-spi-and-runstatus-formal-dfa.md) | RunLifecycle SPI + RunStatus DFA | Codified in `RunStateMachine.java` |
| [0027](locked/0027-idempotency-scope-w0-header-validation.md) | W0 idempotency header validation | `IdempotencyHeaderFilter` + V2 SQL schema codified |

## Archive (superseded or rcN-closure)

| ID | Reason | Where to look |
|---|---|---|
| [0026](../logs/adr-amendment-narratives/0026-module-dependency-direction-contracts-split.md) | Superseded by ADR-0055 (permit platform→runtime direction) | Active: ADR-0055 |
| [0083](../logs/adr-amendment-narratives/0083
| [0084](../logs/adr-amendment-narratives/0084
| [0085](../logs/adr-amendment-narratives/0085

## Active (current design evolution)

See [`ADR-CLASSIFICATION.md`](ADR-CLASSIFICATION.md) for the per-ADR Level × View classification (consumed by `gate/migrate_adrs_to_yaml.py` and `gate/build_architecture_graph.sh`).

Active ADRs cover:
- Runtime / SPI design (0019, 0021-0025, 0028-0030, 0044, 0070, 0072-0082)
- Cross-cutting governance (0033, 0035-0047, 0059, 0064-0068)
- Telemetry + observability (0009, 0061-0063)
- W3+ deferred design (0003, 0012, 0013, 0016, 0017, 0018, 0022, 0029, 0039, 0050-0054)
- Phase L/M reviewer remediation (0040, 0042-0049, 0058, 0060)
- L0 ironclad rules + engine contract (0069-0079)
- ResilienceContract + GraphMemoryRepository alignment (0080-0082)

## Policy

- New ADRs land in `docs/adr/*.{md,yaml}` as "active" by default.
- Promotion to `locked/` requires: decision baked into code/build with compile-time or integration-test enforcement; no future amendment expected; explicit ADR cross-reference in the next ADR that touches the same surface area.
- Demotion to `docs/logs/adr-amendment-narratives/` happens when an ADR is explicitly superseded by another OR when its content has been absorbed into a consolidated log narrative.
- Modifications to `locked/` ADRs require a new ADR superseding the locked one (and re-classification of the predecessor as archive in the same PR).
