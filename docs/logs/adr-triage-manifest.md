# ADR Triage Manifest (Wave 1B)

> Per the Authority-Surface Cleanup migration plan (2026-05-19), all 85 ADRs are classified into `locked` / `active` / `archive`. This manifest is the canonical record of the triage decisions. File moves (per the table below) happen in the same commit as this manifest.
>
> **Classification criteria:**
> - **locked**: foundational decision baked into code/build (compile-time or test-frozen); no future evolution expected. Lives in `docs/adr/locked/`.
> - **active**: still-evolving design decision; lives in `docs/adr/`.
> - **archive**: rcN-closure narrative (absorbed into `docs/logs/governance-waves.md`), or explicitly superseded by a later ADR. Lives in `docs/logs/adr-amendment-narratives/`.

## Classification table

| ADR | Title | Classification | Reason |
|---|---|---|---|
| 0001 | java-21-spring-boot-runtime | **locked** | Java 21 + Spring Boot 4 pinned in pom.xml; compile-time enforcement |
| 0002 | spring-ai-llm-gateway | **locked** | Spring AI ChatClient is the LLM gateway abstraction; no other gateway choice surface |
| 0003 | temporal-durable-workflows | active | W3+ design; not yet implemented |
| 0004 | postgres-primary-data-store | **locked** | DB choice fixed; Flyway migrations target Postgres only |
| 0005 | tenant-isolation-guc-set-local | **locked** | Postgres `SET LOCAL app.tenant_id` + RLS enforced by Rule 40 / R-J |
| 0006 | posture-model-dev-research-prod | **locked** | Three-posture model in AppPostureGate; codified |
| 0007 | outbox-postgres-not-kafka | active | W1-W3 envelope choice; alternative may emerge |
| 0008 | resilience4j-circuit-breaker | active | Resilience contract dual-surface evolving (ADR-0081) |
| 0009 | micrometer-observability | active | Telemetry vertical still evolving (ADR-0061) |
| 0010 | spring-security-oauth2 | **locked** | JWT validation contract codified (ADR-0056) |
| 0011 | flyway-schema-migration | **locked** | Migration tool fixed; integration tests rely on it |
| 0012 | valkey-session-cache | active | W3+ design |
| 0013 | vault-secrets-management | active | W3+ design |
| 0014 | contract-spine-versioning-policy | **locked** | SemVer + contract-spine policy fixed |
| 0015 | layered-architecture-capability-model | **locked** | Layer 0/1/2 model is foundational to ADR-0068 (4+1) |
| 0016 | a2a-federation-strategic-deferral | active | Deferred but normative — federation gates designed |
| 0017 | dev-time-trace-replay | active | MCP-only replay surface; design evolves |
| 0018 | sandbox-executor-spi | active | Sandbox subsumption (R-L / Rule 42.b) still W2 |
| 0019 | suspend-signal-and-suspend-reason-taxonomy | active | Amended by ADR-0074 (rc3 unification) + ADR-0085 |
| 0020 | runlifecycle-spi-and-runstatus-formal-dfa | **locked** | RunStateMachine.java codified the DFA; sealed types |
| 0021 | layered-spi-taxonomy | active | Cross-references active ADRs 0044, 0080 |
| 0022 | payload-codec-spi | active | Deferred (Rule 22 deferred) but design active |
| 0023 | cross-boundary-context-propagation | active | TraceContext carriers; telemetry vertical evolving |
| 0024 | suspension-write-atomicity | active | Three-tier atomicity contract; W1-W2 evolving |
| 0025 | checkpoint-ownership-boundary | active | InMemoryCheckpointer; durable variant deferred |
| 0026 | module-dependency-direction-contracts-split | **archive** | Superseded by ADR-0055 (permit platform→runtime) |
| 0027 | idempotency-scope-w0-header-validation | **locked** | IdempotencyHeaderFilter + V2 SQL schema codified |
| 0028 | causal-payload-envelope-and-semantic-ontology | active | Payload envelope evolving |
| 0029 | cognition-action-separation | active | Design ADR; not yet operationalised |
| 0030 | skill-spi-lifecycle-resource-matrix | active | Extended by ADR-0070, ADR-0080, ADR-0081 |
| 0031 | three-track-channel-isolation | active | P-E principle (Rule 35 / R-E); design active |
| 0032 | scope-based-run-hierarchy-and-planner-contract-minimal | active | Plan-projection contract design-only |
| 0033 | logical-identity-equivalence-and-deployment-locus-vocabulary | active | Vocabulary fixed but extended by execution-locus binding |
| 0034 | memory-and-knowledge-taxonomy-at-l0 | active | Extended by ADR-0044, ADR-0082 |
| 0035 | posture-enforcement-single-construction-path | active | Single construction path codified but extended into wider posture rules |
| 0036 | contract-surface-truth-generalization | active | Generalisation of contract-truth; informs Rule 25 / G-2.a |
| 0037 | wave-authority-consolidation | active | Wave plan authority; informs Rule 35-related governance |
| 0038 | skill-spi-resource-tier-classification | active | Extends ADR-0030 |
| 0039 | payload-migration-adapter-strategy | active | Migration adapter strategy; not yet operationalised |
| 0040 | w1-http-contract-reconciliation | active | W1 HTTP contract (cursor flow + cancel re-auth) |
| 0041 | active-corpus-truth-sweep | active | Generalises Rule 94 / G-2.f corpus-truth checks |
| 0042 | test-evidence-enforcement-for-rule-25 | active | Test evidence policy for Rule 25 / G-2.a |
| 0043 | active-normative-doc-catalog-and-peripheral-drift-prevention | active | ACTIVE_NORMATIVE_DOCS catalogue authority |
| 0044 | spi-contract-precision-and-memory-metadata-reconciliation | active | Extends ADR-0030 + ADR-0034 |
| 0045 | shipped-row-evidence-path-existence-and-peripheral-wave-qualifier | active | Shipped-row evidence rules (Rule 19 deferred) |
| 0046 | release-note-shipped-surface-truth | active | Release note shipped-surface invariants (Rule 26 deferred) |
| 0047 | active-entrypoint-truth-and-system-boundary-prose-convention | active | Active-entrypoint truth (Rule 41 deferred surface) |
| 0048 | service-layer-microservice-architecture-commitment | active | Service Layer as long-running microservices |
| 0049 | c-s-dynamic-hydration-protocol | active | TaskCursor / BusinessRuleSubset / HydrationRequest |
| 0050 | workflow-intermediary-mailbox-rhythm-track | active | Workflow intermediary + mailbox + rhythm-track |
| 0051 | memory-knowledge-ownership-boundary | active | C-side ontology vs S-side trajectory |
| 0052 | skill-topology-scheduler-and-capability-bidding | active | Skill topology scheduler design |
| 0053 | cohesive-agent-swarm-execution | active | Cohesive swarm + SpawnEnvelope |
| 0054 | long-connection-containment-runtime-handles | active | LogicalCallHandle + ConnectionLease |
| 0055 | permit-platform-to-runtime-direction | active | Generalised platform→runtime dep direction (supersedes ADR-0026) |
| 0056 | jwt-validation-and-tenant-claim-cross-check | active | Full JWT validation contract (Rule 56) |
| 0057 | durable-idempotency-claim-replay | active | Durable idempotency + replay (Rule 57 deferred) |
| 0058 | posture-boot-guard | active | PostureBootGuard + @RequiredConfig (E21) |
| 0059 | code-as-contract-architectural-enforcement | active | Rule 28 / R-C.a authority |
| 0060 | phase-l-reviewer-remediation | active | Phase L reviewer P0/P1 closure |
| 0061 | telemetry-vertical-layer | active | Telemetry vertical (Rule 61 / G-3 surface) |
| 0062 | trace-run-session-identity | active | RunContext trace/span/session identity |
| 0063 | client-sdk-observability-contract | active | Client SDK observability contract |
| 0064 | layer-0-governing-principles | active | L0 P-A..P-D — authority for principle layer |
| 0065 | competitive-baselines | active | Four pillars baseline.yaml (Rule 30 / R-B) |
| 0066 | independent-module-evolution | active | Per-module module-metadata.yaml (Rule 31 / R-C) |
| 0067 | spi-dfx-tck-codesign | active | SPI + DFX + TCK co-design (Rule 32 / R-D) |
| 0068 | layered-4plus1-and-architecture-graph | active | Twin sources of truth (Rules 33+34 / G-1) |
| 0069 | l0-ironclad-rules | active | LucioIT L0 §6/§7 import (Rules 35-42 / R-E..R-L) |
| 0070 | cursor-flow-and-skill-capacity-runtime | active | Cursor flow + skill capacity runtime |
| 0071 | engine-contract-structural-wave | active | Engine contract umbrella (Rules 43-47 / R-M) |
| 0072 | engine-envelope-and-strict-matching | active | EngineRegistry + EngineEnvelope (R-M.a/.b) |
| 0073 | engine-hooks-and-runtime-middleware | active | RuntimeMiddleware + HookSurface (R-M.c) |
| 0074 | s2c-capability-callback | active | S2cCallbackTransport SPI (R-M.d) |
| 0075 | evolution-scope-boundary | active | EvolutionExport scope (R-M.e) |
| 0076 | r2-pilot-runtime-self-validation | active | EngineRegistry boot validation (R-M.a Phase 5 pilot) |
| 0077 | schema-first-domain-contracts | active | Schema-first contracts (Rule 48 / M-2.a) |
| 0078 | agent-service-consolidation | active | Phase C consolidation (agent-platform + agent-runtime → agent-service) |
| 0079 | engine-extraction-runtime-core | active | Engine extraction from agent-service to agent-runtime-core |
| 0080 | resilience-contract-spi-package-alignment | active | ResilienceContract .spi package move |
| 0081 | resilience-contract-dual-surface-reconciliation | active | ResilienceContract operation-policy + skill-capacity dual surface |
| 0082 | graphmemory-ownership-canonical-and-topology-truth | active | GraphMemoryRepository canonical owner |
| 0083 | rc9-corpus-truth-and-ci-acceptance | **archive** | rcN-closure narrative absorbed into governance-waves.md |
| 0084 | rc10-corpus-truth-and-prevention-widening | **archive** | rcN-closure narrative + retracted per ADR-0085 |
| 0085 | rc11-kernel-truth-and-shadow-corpus-precision | **archive** | rcN-closure narrative absorbed into governance-waves.md |

## Summary

| Classification | Count | Files affected |
|---|---|---|
| **locked** | 12 | 0001, 0002, 0004, 0005, 0006, 0010, 0011, 0014, 0015, 0020, 0027 (and 0035 if elevated) |
| **active** | 69 | Default classification for ADRs that don't qualify as locked or archive |
| **archive** | 4 | 0026 (superseded), 0083, 0084 (retracted), 0085 (rcN-closure) |

Note: 0035 is currently listed as active because the "single construction path" rule is still being extended into wider posture rules. May elevate to locked after Wave 5 verification.

## Move operations

```bash
# Locked
git mv docs/adr/0001-java-21-spring-boot-runtime.md           docs/adr/locked/
git mv docs/adr/0002-spring-ai-llm-gateway.md                 docs/adr/locked/
git mv docs/adr/0004-postgres-primary-data-store.md           docs/adr/locked/
git mv docs/adr/0005-tenant-isolation-guc-set-local.md        docs/adr/locked/
git mv docs/adr/0006-posture-model-dev-research-prod.md       docs/adr/locked/
git mv docs/adr/0010-spring-security-oauth2.md                docs/adr/locked/
git mv docs/adr/0011-flyway-schema-migration.md               docs/adr/locked/
git mv docs/adr/0014-contract-spine-versioning-policy.md      docs/adr/locked/
git mv docs/adr/0015-layered-architecture-capability-model.md docs/adr/locked/
git mv docs/adr/0020-runlifecycle-spi-and-runstatus-formal-dfa.md docs/adr/locked/
git mv docs/adr/0027-idempotency-scope-w0-header-validation.md docs/adr/locked/

# Archive (rcN-closure absorbed into governance-waves.md)
git mv docs/adr/0026-module-dependency-direction-contracts-split.md docs/logs/adr-amendment-narratives/
git mv docs/adr/0083-rc9-corpus-truth-and-ci-acceptance.yaml         docs/logs/adr-amendment-narratives/
git mv docs/adr/0084-rc10-corpus-truth-and-prevention-widening.yaml  docs/logs/adr-amendment-narratives/
git mv docs/adr/0085-rc11-kernel-truth-and-shadow-corpus-precision.yaml docs/logs/adr-amendment-narratives/
```

After moves, update:
- `docs/governance/architecture-status.yaml` capability rows that cite locked/archived ADRs by path (Rule 25 / G-2.a evidence)
- `gate/check_architecture_sync.sh` ADR-path enforcement rules to accept the new `docs/adr/locked/` and `docs/logs/adr-amendment-narratives/` paths as authoritative
- `docs/adr/INDEX.md` (new) — current ADR catalogue by topic
- `docs/adr/archive/INDEX.md` (existing) — reconcile with new archive structure or merge into `docs/logs/adr-amendment-narratives/INDEX.md`
