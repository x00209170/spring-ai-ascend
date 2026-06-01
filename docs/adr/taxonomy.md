# ADR Level/Module Taxonomy

This directory provides a practical **ADR grading (L0/L1/L2)** and **module ownership mapping** for all current ADR files.

## Grouped matrix

| Level | Module | ADR count | ADR IDs |
|---|---|---:|---|
| L0 | governance-core | 12 | 0006, 0025, 0033, 0035, 0038, 0039, 0059, 0064, 0065, 0066, 0067, 0068 |
| L1 | agent-service-runtime | 4 | 0001, 0020, 0027, 0032 |
| L1 | cross-cutting-domain | 9 | 0015, 0016, 0024, 0029, 0040, 0049, 0051, 0052, 0053 |
| L1 | data-persistence | 2 | 0004, 0011 |
| L1 | module-contracts | 1 | 0014 |
| L1 | security-isolation | 2 | 0005, 0010 |
| L1 | spring-ai-integration | 1 | 0002 |
| L2 | agent-service-runtime | 4 | 0071, 0135, 0136, 0137 |
| L2 | cross-cutting-domain | 11 | 0069, 0075, 0081, 0089, 0101, 0102, 0103, 0117, 0120, 0138, 0139 |
| L2 | data-persistence | 1 | 0077 |
| L2 | spring-ai-integration | 1 | 0134 |

## Full inventory

| ADR | Level | View | Module | File |
|---|---|---|---|---|
| 0001 | L1 | development | agent-service-runtime | `docs/adr/locked/0001-java-21-spring-boot-runtime.md` |
| 0002 | L1 | logical | spring-ai-integration | `docs/adr/locked/0002-spring-ai-llm-gateway.md` |
| 0004 | L1 | physical | data-persistence | `docs/adr/locked/0004-postgres-primary-data-store.md` |
| 0005 | L1 | physical | security-isolation | `docs/adr/locked/0005-tenant-isolation-guc-set-local.md` |
| 0006 | L0 | process | governance-core | `docs/adr/locked/0006-posture-model-dev-research-prod.md` |
| 0010 | L1 | process | security-isolation | `docs/adr/locked/0010-spring-security-oauth2.md` |
| 0011 | L1 | development | data-persistence | `docs/adr/locked/0011-flyway-schema-migration.md` |
| 0014 | L1 | logical | module-contracts | `docs/adr/locked/0014-contract-spine-versioning-policy.md` |
| 0015 | L1 | logical | cross-cutting-domain | `docs/adr/locked/0015-layered-architecture-capability-model.md` |
| 0016 | L1 | process | cross-cutting-domain | `docs/adr/0016-a2a-federation-strategic-deferral.md` |
| 0020 | L1 | process | agent-service-runtime | `docs/adr/locked/0020-runlifecycle-spi-and-runstatus-formal-dfa.md` |
| 0024 | L1 | logical | cross-cutting-domain | `docs/adr/0024-suspension-write-atomicity.md` |
| 0025 | L0 | scenarios | governance-core | `docs/adr/0025-checkpoint-ownership-boundary.md` |
| 0027 | L1 | logical | agent-service-runtime | `docs/adr/locked/0027-idempotency-scope-w0-header-validation.md` |
| 0029 | L1 | logical | cross-cutting-domain | `docs/adr/0029-cognition-action-separation.md` |
| 0032 | L1 | logical | agent-service-runtime | `docs/adr/0032-scope-based-run-hierarchy-and-planner-contract-minimal.md` |
| 0033 | L0 | process | governance-core | `docs/adr/0033-logical-identity-equivalence-and-deployment-locus-vocabulary.md` |
| 0035 | L0 | scenarios | governance-core | `docs/adr/0035-posture-enforcement-single-construction-path.md` |
| 0038 | L0 | scenarios | governance-core | `docs/adr/0038-skill-spi-resource-tier-classification.md` |
| 0039 | L0 | scenarios | governance-core | `docs/adr/0039-payload-migration-adapter-strategy.md` |
| 0040 | L1 | process | cross-cutting-domain | `docs/adr/0040-w1-http-contract-reconciliation.md` |
| 0049 | L1 | logical | cross-cutting-domain | `docs/adr/0049-c-s-dynamic-hydration-protocol.md` |
| 0051 | L1 | logical | cross-cutting-domain | `docs/adr/0051-memory-knowledge-ownership-boundary.md` |
| 0052 | L1 | process | cross-cutting-domain | `docs/adr/0052-skill-topology-scheduler-and-capability-bidding.md` |
| 0053 | L1 | process | cross-cutting-domain | `docs/adr/0053-cohesive-agent-swarm-execution.md` |
| 0059 | L0 | scenarios | governance-core | `docs/adr/0059-code-as-contract-architectural-enforcement.md` |
| 0064 | L0 | scenarios | governance-core | `docs/adr/0064-layer-0-governing-principles.md` |
| 0065 | L0 | scenarios | governance-core | `docs/adr/0065-competitive-baselines.md` |
| 0066 | L0 | development | governance-core | `docs/adr/0066-independent-module-evolution.md` |
| 0067 | L0 | development | governance-core | `docs/adr/0067-spi-dfx-tck-codesign.md` |
| 0068 | L0 | scenarios | governance-core | `docs/adr/0068-layered-4plus1-and-architecture-graph.yaml` |
| 0069 | L2 | logical | cross-cutting-domain | `docs/adr/0069-l0-ironclad-rules.yaml` |
| 0071 | L2 | logical | agent-service-runtime | `docs/adr/0071-engine-contract-structural-wave.yaml` |
| 0075 | L2 | logical | cross-cutting-domain | `docs/adr/0075-evolution-scope-boundary.yaml` |
| 0077 | L2 | logical | data-persistence | `docs/adr/0077-schema-first-domain-contracts.yaml` |
| 0081 | L2 | logical | cross-cutting-domain | `docs/adr/0081-resilience-contract-dual-surface-reconciliation.yaml` |
| 0089 | L2 | logical | cross-cutting-domain | `docs/adr/0089-edge-plane-ingress-gateway-mandate.yaml` |
| 0101 | L2 | logical | cross-cutting-domain | `docs/adr/0101-rc22-polymorphic-deployment-topology.yaml` |
| 0102 | L2 | logical | cross-cutting-domain | `docs/adr/0102-rc22-evolution-plane-online-offline-duality.yaml` |
| 0103 | L2 | logical | cross-cutting-domain | `docs/adr/0103-rc22-agent-middleware-naming-and-capability-services-distribution.yaml` |
| 0117 | L2 | logical | cross-cutting-domain | `docs/adr/0117-rc37-ascend-kunpeng-strategic-repositioning.yaml` |
| 0120 | L2 | logical | cross-cutting-domain | `docs/adr/0120-brand-audience-b-alignment.yaml` |
| 0134 | L2 | logical | spring-ai-integration | `docs/adr/0134-tool-call-iteration-loop.yaml` |
| 0135 | L2 | logical | agent-service-runtime | `docs/adr/0135-agent-session-as-run-projection.yaml` |
| 0136 | L2 | logical | agent-service-runtime | `docs/adr/0136-vocabulary-reconciliation-pr71-task-vs-run.yaml` |
| 0137 | L2 | logical | agent-service-runtime | `docs/adr/0137-suspendsignal-canonical-interruptsignal-glossary.yaml` |
| 0138 | L2 | logical | cross-cutting-domain | `docs/adr/0138-agent-service-five-layer-l1-ratification.yaml` |
| 0139 | L2 | logical | cross-cutting-domain | `docs/adr/0139-fast-slow-path-narrowed-semantics.yaml` |
