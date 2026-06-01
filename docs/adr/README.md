# Architecture Decision Records

> Owner: architecture | Format: MADR 4.0 | Last refreshed: 2026-05-13

This directory contains Architecture Decision Records (ADRs) for spring-ai-ascend.
Each ADR documents a significant architectural decision with its context,
options considered, decision, and consequences.

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [0001](0001-java-21-spring-boot-runtime.md) | Java 21 + Spring Boot 4.0.5 as the runtime baseline | accepted |
| [0002](0002-spring-ai-llm-gateway.md) | Spring AI 2.0.0-M5 as the LLM gateway, not LangChain4j | accepted |
| [0004](0004-postgres-primary-data-store.md) | PostgreSQL 16 with RLS + pgvector, not separate vector DB | accepted |
| [0005](0005-tenant-isolation-guc-set-local.md) | Row-level security with SET LOCAL transaction-scoped GUC, not per-connection reset | accepted |
| [0006](0006-posture-model-dev-research-prod.md) | ActionGuard 5-stage chain (cycle-9 truth-cut), not 11-stage | accepted |
| [0010](0010-spring-security-oauth2.md) | Keycloak (OSS) as default IdP, but customer can BYO | accepted |
| [0011](0011-flyway-schema-migration.md) | Spring Cloud Gateway as ingress, not Kong / Traefik | accepted |
| [0014](0014-contract-spine-versioning-policy.md) | 3-posture model (dev/research/prod), not 5 or 2 | accepted |
| [0015](0015-layered-architecture-capability-model.md) | Defer multi-framework dispatch (Python sidecar, LangChain4j) to W4+ | accepted |
| [0016](0016-a2a-federation-strategic-deferral.md) | A2A federation strategic deferral: AgentCard + AgentRegistry reserved post-W4 | accepted |
| [0020](0020-runlifecycle-spi-and-runstatus-formal-dfa.md) | RunLifecycle SPI separation + RunStatus formal DFA + transition audit | accepted |
| [0024](0024-suspension-write-atomicity.md) | Suspension write atomicity: Checkpointer + RunRepository transactional contract | accepted |
| [0025](0025-checkpoint-ownership-boundary.md) | Checkpoint ownership boundary: executor resume cursors vs orchestrator Run row | accepted |
| [0027](0027-idempotency-scope-w0-header-validation.md) | Idempotency scope at W0: header validation only, dedup deferred to W1 | accepted |
| [0029](0029-cognition-action-separation.md) | Cognition-Action separation principle: cognitive reasoning isolated from action execution | accepted |
| [0032](0032-scope-based-run-hierarchy-and-planner-contract-minimal.md) | Scope-based run hierarchy (RunScope STEP_LOCAL/SWARM) + planner contract minimal (PlanState/RunPlanRef) | accepted |
| [0033](0033-logical-identity-equivalence-and-deployment-locus-vocabulary.md) | Logical Identity Equivalence: S-Cloud/S-Edge/C-Device deployment-locus vocabulary | accepted |
| [0035](0035-posture-enforcement-single-construction-path.md) | Posture enforcement single-construction-path: AppPostureGate + posture-model.md as canonical ledger | accepted |
| [0038](0038-skill-spi-resource-tier-classification.md) | Skill SPI resource tier classification: 4 enforceability tiers (hard/sandbox/advisory/hints) | accepted |
| [0039](0039-payload-migration-adapter-strategy.md) | Payload migration adapter strategy: Object → Payload → CausalPayloadEnvelope + adapter wrapper | accepted |
| [0040](0040-w1-http-contract-reconciliation.md) | W1 HTTP contract reconciliation: X-Tenant-Id + JWT cross-check, PENDING initial status, POST /cancel | accepted |
| [0049](0049-c-s-dynamic-hydration-protocol.md) | C/S Dynamic Hydration Protocol and Cursor Handoff (§4 #47): TaskCursor + BusinessRuleSubset + SkillPoolLimit request; HydrationRequest envelope; SyncStateResponse / SubStreamFrame / YieldResponse handoff modes; ResumeEnvelope; degradation-authority red line (ComputeCompensation vs BusinessDegradationRequest; GoalMutationProhibition); closes whitepaper-alignment P0-2 + P1-2 | accepted |
| [0051](0051-memory-knowledge-ownership-boundary.md) | Memory and Knowledge Ownership Boundary (§4 #49): C-side owned (business ontology) vs S-side owned (run trajectory) vs delegated; BusinessFactEvent + OntologyUpdateCandidate emission path; PlaceholderPreservationPolicy + SymbolicReturnEnvelope as first-class ship-blocking rule; ADR-0034 M3/M4/M5 split into platform-derived vs business-owned; closes whitepaper-alignment P0-5 | accepted |
| [0052](0052-skill-topology-scheduler-and-capability-bidding.md) | Skill Topology Scheduler and Capability Bidding (§4 #50): two-axis tenant×global arbitration; SkillResourceMatrix extended; CapabilityRegistry with tenant-scoped pre-authorization; BidRequest/BidResponse; PermissionEnvelope (subsumption-bounded, short-lived, signed); SkillSaturationYield releases LLM thread; closes whitepaper-alignment P1-1 | accepted |
| [0053](0053-cohesive-agent-swarm-execution.md) | Cohesive Agent Swarm Execution — Workflow Authority Invariant + SpawnEnvelope (§4 #51): SwarmRun / ParentRunRef / SwarmJoinPolicy aliases for existing types; SpawnEnvelope as consolidated 15-dimension parent→child propagation contract (1 W0 shipped, 5 partial, 7 W2 design gaps, 1 W1 deferred, 1 deferred); CrossWorkflowHandoff as explicit escape hatch; 5-boundary authority-transfer taxonomy; Envelope pattern codified; closes capability-labels reviewer P0 + class-based hidden defects | accepted |

## Process

New ADRs are proposed by opening a PR that adds a new file to this directory.
The file must use the MADR 4.0 template (see any existing ADR for reference).
ADR numbers are sequential; never reuse a number.
Superseded ADRs remain in the directory with Status: superseded, linking to the successor.

## References

- `ARCHITECTURE.md` sec-2 (OSS matrix)
- `docs/cross-cutting/contract-evolution-policy.md`
