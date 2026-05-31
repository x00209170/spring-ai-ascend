---
title: "L0 Senior-Architect Review — Recommend Reopening rc41 Before v2.0.0 GA"
date: 2026-05-25
status: architecture-team-feedback
scope:
  - rc41 "L0 final release-ready" decision (commit d20d1e3)
  - L0 architectural contract surface (SPI shapes) vs implementations
  - Brand-vs-shipped alignment for `spring-ai-ascend`
reviewer_role: Senior Java microservices + agent-architecture reviewer (independent)
relates_to:
  - docs/logs/releases/2026-05-25-l0-rc41-final-release-readiness.en.md
  - docs/logs/reviews/2026-05-25-l0-release-readiness-root-cause-analysis.en.md
  - ADR-0117 (Ascend/Kunpeng strategic repositioning)
  - ARCHITECTURE.md §1.1 (Audience boundary)
---

# L0 Senior-Architect Review — Recommend Reopening rc41 Before v2.0.0 GA

## §0 — Reading order and relationship to the prior reviewer

This memo intentionally disagrees with the conclusion in
`docs/logs/reviews/2026-05-25-l0-release-readiness-root-cause-analysis.en.md`
that *"the service split, engine/SPI direction, memory/knowledge boundary,
dynamic-planning staging, skill-capacity model, S2C placement, and
agent-service expansion are broadly proportionate for L0"*. That review's
contribution — diagnosing the **transaction-ness** of the release process —
is correct and load-bearing. My disagreement is narrower: even if the release
transaction itself were perfect, the **scope** of what rc41 declares "L0
complete" is not the scope of an agent-platform L0. The two memos can coexist:
the prior one says "you cannot prove the release", this one says "even when
you can prove it, you should not call it agent-platform L0 yet".

The recommendation below is for the architecture team to act on or rebut. I
make no code changes and propose no mutations to `architecture-status.yaml`,
rc41 release-note frontmatter, or the rc41 evidence bundle.

## §1 — Executive summary

**Recommendation: reopen rc41. Do not retract code. Retract the
`formal_release: true` claim, the `v2.0.0` framing, and the "L0 final
release-ready" prose. Re-scope rc41's deliverable to its honest surface
("Runtime Kernel + Governance Skeleton GA") and open an rc43+ "L0 Agentic
Contract Surface" wave to land seven missing SPI shapes as design-only
contracts. Re-close L0 only after that surface lands.**

The shipped runtime kernel and the governance machinery are excellent. The
gap is not in what was built; it is in what was **not declared** at the
contract layer. rc41 ships the contract surface for a *generic job runtime*
(Run / Engine / Skill-capacity / S2C). It does **not** ship a contract
surface for an *agent platform* (Agent / Model / Tool / Memory / Knowledge
/ Planner / Spring AI boundary). Stamping the former as "L0 complete" for a
project named `spring-ai-ascend` is a category error, not a defect of
execution.

## §2 — What the team did exceptionally well

Setting expectations honestly: this team's work is well above industry
median, and several elements are genuinely novel.

1. **Governance machinery.** 140 active gate rules, 252 self-tests,
   14 catalogued recurring-defect families, 478-node architecture graph,
   the Rule G-13 single-source rendering policy (ADR-0119), and the
   scenario-loaded phase contracts under `docs/governance/contracts/`
   (ADR-0098 / Rule G-11) are platform-engineering work I have not seen
   elsewhere at this fidelity. The recurring-defect ledger as a first-class
   gateable artefact is the kind of meta-discipline most teams never reach.

2. **Run lifecycle correctness.** `Run` / `RunStatus` / `RunStateMachine`
   under `agent-service/src/main/java/com/huawei/ascend/service/runtime/runs/`
   with atomic-CAS status writes (ADR-0118 close-out of the
   F-nonatomic-run-status-write family) is a clean primitive that downstream
   work can build on without revisiting.

3. **Engine contract (P-M) end-to-end.** `engine-envelope.v1.yaml` (runtime
   enforced), `EngineRegistry` boot validation, `ExecutorAdapter` strict
   matching, and `s2c-callback.v1.yaml` are the strongest contract-first
   pattern in the corpus. Engine pluggability is a real seam, not a
   placeholder.

4. **Skill capacity arbitration.** `ResilienceContract` +
   `YamlSkillCapacityRegistry` + `SuspendReason` deliver the two-axis
   (tenant × global) arbitration that ADR-0052 / ADR-0070 / ADR-0080 +
   ADR-0081 lay out. The `SuspendReason.SKILL_CAPACITY_EXCEEDED` /
   `S2C_TIMEOUT` / `S2C_RESPONSE_INVALID` taxonomy is a cleaner suspension
   model than most reactive-orchestration frameworks ship.

5. **Five-plane topology with ArchUnit enforcement.** Module boundaries
   (`agent-bus` / `agent-middleware` / `agent-execution-engine` /
   `agent-service` / `agent-evolve` / `agent-client` /
   `spring-ai-ascend-graphmemory-starter` / BoM) plus
   `deployment_plane: edge|compute_control|bus_state|sandbox|evolution|none`
   per Rule R-I — concrete, gateable, with no fudge room.

6. **Posture-aware defaults shipped across the board.** `dev` /
   `research` / `prod` fail-closed semantics in `AppPostureGate` and
   `PostureBootGuard` are not just a convention; they are runtime-enforced
   construction-time refusals. This is a discipline that pays back during
   the W2+ durable-backend wave.

This is the substrate. The disagreement below is about what the substrate
gets called.

## §3 — The L0 closure category error

### What L0 means

L0 in the team's own framing (`Rule G-1` / ADR-0068) is the *contract layer
that bounds what L1-L4 implementations must honor*. The 4+1 view discipline
puts L0 above L1 (module-internal architecture) and L2 (deep technical
design). L0 is where the platform declares **what kinds of things exist and
how they relate** — not how each is implemented.

### What an agent platform promises in 2026

The peer reference set for `spring-ai-ascend` is well-defined. Spring AI
ships `ChatClient` + `ToolCallback` + `VectorStore` + `Advisor` +
`ChatMemory`. LangChain4j ships `AiServices` + `ChatLanguageModel` +
`EmbeddingStore` + `ContentRetriever`. Microsoft Semantic Kernel ships
`Kernel` + `KernelFunction` + `IChatCompletionService` + `IMemoryStore`.
AutoGen ships `ConversableAgent` + `register_for_llm` + `register_for_execution`.
Across every credible peer, the L0-equivalent contract surface names six
primitives:

- **Agent** (an entity with identity, model binding, tool set, memory)
- **Model / Chat client** (the LLM provider boundary)
- **Tool / Function** (the dispatch primitive for agent-callable code)
- **Memory** (conversational, episodic, semantic — typed)
- **Knowledge / Vector store / Retriever** (RAG primitives)
- **Planner / Workflow** (multi-step reasoning structure)

The platform does not have to copy these names, but it must answer the
question they ask: *given this platform, what is the shape of an Agent? a
Model? a Tool? a Memory? a Vector store? a Planner?* L0 is exactly where
that answer is supposed to live.

### The conflation

rc41 frames "L0 complete" as "structural skeleton + governance complete".
For a *generic job-execution runtime* this would be correct. The shipped
contracts (Run / RunStatus / Orchestrator / EngineEnvelope / ExecutorAdapter
/ SkillCapacityRegistry / S2cCallbackEnvelope) are precisely the contracts
of a multi-tenant, hook-instrumented, suspend-resumable job runtime. There
is nothing wrong with that surface; there is something wrong with calling it
"an agent platform L0".

The team's own ARCHITECTURE.md §1.1 makes the conflation explicit and
costly: Audience B is named as *"external Spring developers integrating into
their own Spring Boot 4 + Java 21 applications"*. A v2.0.0 GA tag invites
Audience B to evaluate now. The shipped surface forces Audience B to either
(a) drop down to Spring AI directly — defeating the platform — or (b) build
their own Agent / Tool / Memory abstractions on top of `ExecutorAdapter` —
recreating the work the platform is supposed to have done. Either way, the
brand promise is not met.

## §4 — The seven L0-level contract-shape gaps

Each gap is presented with: (1) what is missing, (2) what exists today,
(3) why it is L0 (contract), not W1+ (implementation), (4) a concrete
contract-shape proposal at design-only depth.

### 4.1 — `Agent` first-class entity/SPI

**Missing.** No Java type, no SPI interface, no contract YAML naming what
an Agent IS.

**Today.** `Run` is a job execution instance; `RunContext` is per-run
state; `ExecutorDefinition` is engine config. None of these IS the Agent.
The closest carrier is `agent-service/src/main/java/com/huawei/ascend/service/runtime/runs/Run.java`,
which is a persisted record of *one execution*, not the description of
*the thing that executes*.

**Why L0.** An external integrator cannot answer "what is an Agent on this
platform?" by reading the contract catalog. Every Audience B integrator
will invent their own Agent class with different fields. ADR-0094's
`F-cross-authority-agreement` family predicts the rest: four mutually
inconsistent Agent definitions will appear by W3.

**Proposal.** ADR-0120 (next free) declaring
`com.huawei.ascend.agent.spi.Agent` SPI with `agentId` / `modelBinding`
(ref to ModelGateway by id) / `toolBindings` (Set\<ToolRef\>) /
`memoryBindings` (Map\<MemoryCategory, MemoryStoreRef\>) / `systemPrompt`
/ `safetyPolicy` accessors; companion `AgentDefinition` value record;
`agent-definition.v1.yaml` schema at `status: design_only`. No
implementation required at L0; the *shape* is the deliverable.

### 4.2 — `ModelGateway` / LLM provider SPI shape

**Missing.** No SPI naming the LLM-call boundary, no request/response
envelope schema, no hook-binding contract.

**Today.** `pom.xml:53,86-92` pins `spring-ai-bom 2.0.0-M5` but a Grep
across all production Java files (`agent-*/src/main/java/**/*.java`)
returns zero imports of `org.springframework.ai.*`, `com.openai.*`, or
`com.anthropic.*`. The only mention is
`agent-service/src/test/java/com/huawei/ascend/service/runtime/architecture/LlmGatewayHookChainOnlyTest.java`,
which is an ArchUnit guard that is vacuously green (`allowEmptyShould(true)`)
because the `com.huawei.ascend.service.runtime.llm/` package does not
exist. `HookPoint` (ADR-0073) names `BEFORE_LLM` / `AFTER_LLM` hooks but
defines no LLM envelope for them to wrap.

**Why L0.** Without a `ModelGateway` SPI, every W2 LLM integration will
invent its own request shape. The hook surface cannot bind to events it
has no envelope for. Provider-portability claims (Audience C: "FIPS,
multi-region, vertical-agnostic") are unsubstantiated until the model
boundary is named.

**Proposal.** ADR-0121 declaring
`com.huawei.ascend.model.spi.ModelGateway` with
`Mono<ModelResponse> invoke(ModelInvocation)` (Reactive per Rule R-G);
`model-invocation.v1.yaml` envelope schema (model id / messages / tools
/ parameters / hook context); explicit contract that `BEFORE_LLM` hooks
receive `ModelInvocation` and `AFTER_LLM` hooks receive `ModelResponse`.
The un-armed `LlmGatewayHookChainOnlyTest` becomes non-vacuous the
moment the SPI exists.

### 4.3 — `Tool` vs `Skill` semantic mapping

**Missing.** No ADR resolves whether a Tool IS a Skill, whether
`SkillCapacityRegistry` arbitrates tool calls, or how a customer
registers a function as either.

**Today.** ADR-0030 names `Skill SPI` with lifecycle
init/execute/suspend/teardown; ADR-0052 names `SkillResourceMatrix` and
`CapabilityRegistry`. Spring AI uses `ToolCallback`. No Java `Skill` or
`Tool` interface ships. `SkillCapacityRegistry` arbitrates capacity for
an abstract "skill" that does not exist as code.

**Why L0.** The Tool/Skill split (or unification) determines registry
shape, dispatch path, capacity model, and sandbox boundary. Resolving it
in W2 means the LLM gateway, the tool registry, and the sandbox each
implement against an undefined contract.

**Proposal.** ADR-0122 resolving one of three paths: (a) Tool IS a
Skill — single registry, Skill subsumes Spring AI ToolCallback via
adapter; (b) Tool is a Skill-Kind enum value — one registry,
kind-discriminated; (c) Tool and Skill are siblings with separate
registries — explicit reason required. If (a) or (b),
`com.huawei.ascend.skill.spi.Skill` lands as the unified SPI; if (c),
`com.huawei.ascend.tool.spi.Tool` + `ToolRegistry` lands alongside.

### 4.4 — Unified `MemoryStore` SPI

**Missing.** No unified Memory SPI; the M3 (semantic long-term) and M5
(knowledge index) categories declared by ADR-0034 have no Java surface
at all.

**Today.** ADR-0034 declares a 6-category taxonomy (M1-M6) but only
three unrelated SPIs ship: `TaskStateStore`
(`agent-service/src/main/java/com/huawei/ascend/service/task/spi/TaskStateStore.java`,
M2-ish), `GraphMemoryRepository`
(`agent-service/src/main/java/com/huawei/ascend/service/runtime/memory/spi/GraphMemoryRepository.java`,
M4 stub), `ContextProjector`
(`agent-service/src/main/java/com/huawei/ascend/service/session/spi/ContextProjector.java`,
session). The taxonomy's common `MemoryMetadata` envelope is
prose-only.

**Why L0.** ADR-0051's "Memory & Knowledge Ownership Boundary" places
C-side vs S-side ownership decisions at the platform contract. Without a
unified `MemoryStore` SPI, ownership cannot be enforced; the
`PlaceholderPreservationPolicy` and `BusinessFactEvent` named in
ADR-0051 have no Java shape to attach to.

**Proposal.** ADR-0123 declaring
`com.huawei.ascend.memory.spi.MemoryStore<K, V>` parameterized by
`MemoryCategory` enum (M1-M6); CQRS split into `MemoryReader` +
`MemoryWriter`; common `MemoryMetadata` Java record matching ADR-0034
prose; explicit `Agent.memoryBindings` binding multiple stores. Land
`SemanticMemoryStore` (M3) and `KnowledgeMemoryStore` (M5) SPI shapes
even without adapters.

### 4.5 — `VectorStore` / `Retriever` / `EmbeddingModel` SPI

**Missing.** No platform-level SPI for vector storage, retrieval, or
embedding. M5 in ADR-0034 is a placeholder name.

**Today.** Spring AI ships `VectorStore` / `DocumentRetriever` /
`EmbeddingModel`; the platform imports the BoM but re-exports nothing
and provides no extension seam. An external Spring developer wanting
RAG today must bypass `spring-ai-ascend` and use Spring AI directly.

**Why L0.** RAG is the most common agent use case in 2026. A platform
that cannot answer "where does my vector store plug in?" at L0 is not
yet an agent platform regardless of how many gate rules it ships.

**Proposal.** ADR-0124 resolving the integration boundary (see §4.6),
then either decorate Spring AI's `VectorStore` / `DocumentRetriever` /
`EmbeddingModel` with platform decorators (tenant scoping, hook
integration, `ResilienceContract` pass-through), or declare native
`com.huawei.ascend.vector.spi.VectorStore` /
`com.huawei.ascend.retrieval.spi.Retriever` /
`com.huawei.ascend.embedding.spi.EmbeddingModel` with Spring AI as one
adapter. `vector-store.v1.yaml` design-only contract for non-Spring-AI
providers (e.g., an Ascend-NPU-optimized vector engine when that lands).

### 4.6 — Spring AI integration boundary

**Missing.** No ADR declaring whether Spring AI is the canonical model /
tool / vector abstraction (with platform decorators) or one adapter among
many.

**Today.** The brand `spring-ai-ascend` + the `OssApiProbe`
(`agent-service/src/main/java/com/huawei/ascend/service/runtime/probe/OssApiProbe.java`)
classpath check + the `LlmGatewayHookChainOnlyTest` ArchUnit guard all
imply Spring AI is intended as the model abstraction. The actual L0
has zero production Spring AI imports. This is the **worst** option: brand
promise without contract delivery.

**Why L0.** The project name is part of the contract surface. External
readers of L0 docs reasonably expect either (a) Spring AI is the model
abstraction the platform decorates, or (b) the platform defines its own
abstraction and Spring AI is one provider. The current ambiguity is an
L0 defect even before any code is touched.

**Proposal.** ADR-0125 explicitly resolving the boundary. Recommended
choice: (a) — Spring AI is the canonical Model / Tool / Vector
abstraction, the platform decorates with `tenantId` scoping, hook
binding, `ResilienceContract` pass-through, and `TraceContext`
propagation. This matches the project name and avoids re-inventing
solved problems.

### 4.7 — `Planner` SPI shape

**Missing.** No `Planner` SPI; no input/output contract for plan
generation.

**Today.** `docs/contracts/plan-projection.v1.yaml` (design_only)
describes scheduler admission (step → required skills → budget envelope
→ memory scope), not planner output. ADR-0032 names
`PlanState` / `RunPlanRef` but declares no `Planner` interface. The W4
implementer designs from scratch with no L0 boundary.

**Why L0.** Dynamic planning is named in the team's own competitive
narrative as a differentiating capability. Deferring the SPI shape to
W4 leaves three years of inter-team coordination with no contract to
reference.

**Proposal.** ADR-0126 declaring
`com.huawei.ascend.planner.spi.Planner` with
`PlanningResult plan(PlanningRequest)`; output `Plan` carries a DAG
(steps / dependencies / branch points / loop annotations);
`planning-request.v1.yaml` and `plan.v1.yaml` design-only schemas;
explicit relationship to `plan-projection.v1.yaml` (planner OUTPUT →
projection INPUT).

## §5 — Why each gap is L0-blocking (rebuttal of "W1+ deferral is fine")

Five arguments against the "deferred to W2/W3/W4 is sufficient" framing.

**Argument 1 — Contract-shape-first is the team's own discipline.** The
team's pattern with `engine-envelope.v1.yaml` (contract runtime-enforced
before any non-reference adapter ships) and `ingress-envelope.v1.yaml`
(contract design-only with Java records pre-staged) proves the team knows
how to land a shape ahead of implementation. Suspending that discipline
for exactly the seven primitives that define the platform's category is
inconsistent.

**Argument 2 — Rule R-A is vacuously satisfied today.** Rule R-A requires
extension via SPI + `@ConfigurationProperties` only. With no Agent / Tool
/ Memory / Vector / Planner SPIs, *there is nothing to extend* — the rule
cannot fail because no extension surface exists. Vacuous satisfaction is
not real compliance, and the gate cannot detect the difference.

**Argument 3 — Audience B credibility.** ARCHITECTURE.md §1.1 commits
the platform to serving Audience B at W2/W3 primary. A v2.0.0 GA tag
today invites Audience B to evaluate now. The shipped surface forces
Audience B to bypass the platform for the most common agent use cases.
That is a brand-vs-shipped delta a GA tag amplifies, not absorbs.

**Argument 4 — Reviewer surface.** L0 ADRs are how external reviewers —
OS-vendor partners, Audience C regulated-industry compliance teams,
open-source contributors — evaluate architectural soundness. Reviewing
rc41 today, an external architect cannot answer "how does this platform
represent an Agent?". That is an L0 surface defect, not a W1+
implementation gap.

**Argument 5 — Predictable future drift.** ADR-0094's
`F-cross-authority-agreement` family catalogs cross-authority drift as
recurrent across rc14–rc40. Deferring seven contract shapes to four
different W-waves means W2 (LLM gateway), W2 (tool registry), W2 (memory
adapters), and W4 (planner) each independently invent their input/output
shapes. The empirical base rate from the team's own ledger says these
shapes will not converge organically; a future rc-N will spend a wave
reconciling them. Landing the shapes at L0 prevents the drift before it
starts.

## §6 — Recommended remediation path (rc43+ "L0 Agentic Contract Surface" wave)

Concrete close-out plan, modeled on the team's existing
`docs/runbooks/multi-wave-release.md` (rc19 Wave 4):

- **rc43 Wave 1 — ADR landing.** Seven new ADRs (ADR-0120 through
  ADR-0126), one per gap, each contract-only with explicit
  "implementation: deferred to W_x". Each cites Rule R-A vacuous-
  satisfaction and Rule R-D SPI + DFX + TCK co-design as the closure
  basis. ~5 working days.

- **rc43 Wave 2 — SPI Java types land.** One package per gap under
  semantic-home modules (`agent-service` for Agent / Memory / Planner;
  new `agent-model` module for ModelGateway / Tool / Vector if scope
  warrants — or co-locate under existing modules and split later).
  Java SPI interfaces only, no implementations. `package-info.java`
  describes the contract. ~3 working days.

- **rc43 Wave 3 — Contract YAML + catalog refresh.** Seven new
  `docs/contracts/*.v1.yaml` files at `status: design_only`,
  `runtime_enforced: false`, each citing its ADR. Update
  `docs/contracts/contract-catalog.md`. Update `agent-service/ARCHITECTURE.md`
  §7 SPI appendix. ~2 working days.

- **rc43 Wave 4 — Gate rules + DFX coverage.** New gate rules guarding
  the catalog ↔ metadata ↔ DFX parity for the seven additions; Rule R-D
  sub-clauses .b/.c/.d/.e/.f/.g already cover most of this — verify and
  extend where needed. ~2 working days.

- **rc43 Wave 5 — Release transaction + lockstep.** New release note
  re-tagging v2.0.0 as `Runtime Kernel + Governance Skeleton GA`; new
  rc43 release note as `L0 Agentic Contract Surface`. Update README,
  `architecture-status.yaml#baseline_metrics`,
  `recurring-defect-families.yaml`. ~2-3 working days including
  evidence-bundle regeneration.

Estimated total: 12-15 working days for a focused team. The constraint
is review-cycle calendar time, not engineering.

## §7 — Re-close criteria (what "L0 truly complete" looks like)

A checklist the team can run against rc43+ to verify the gap is genuinely
closed:

- [ ] Seven new ADRs accepted, each citing its motivating L0 boundary.
- [ ] Seven Java SPI interfaces exist under `*.spi.*` packages, declared
      in respective `module-metadata.yaml#spi_packages`, with
      `package-info.java` describing the contract.
- [ ] Seven `docs/contracts/*.v1.yaml` files exist at
      `status: design_only`, registered in `contract-catalog.md`, cited
      by their ADR.
- [ ] DFX docs (`docs/dfx/<module>.yaml`) include the new SPI packages
      under `spi_packages:` (Rule R-D.e parity holds).
- [ ] `agent-service/ARCHITECTURE.md` §7 SPI Appendix lists the new
      interfaces (Rule G-1.1.b 4-way parity holds).
- [ ] One worked example in `docs/quickstart.md` shows registering a
      custom `Agent` with a `ModelGateway` and `Tool` — even if the
      bindings throw `UnsupportedOperationException` at W0 — proving
      Audience B has a shape to extend.
- [ ] CLAUDE.md kernel makes no terminal-verb claim about agentic
      capability shipping; the seven primitives stay clearly bounded as
      `design_only` contracts at L0 per Rule G-3.e.
- [ ] Rule R-A "Business/Platform Decoupling" is non-vacuously satisfied
      (there is a non-trivial extension surface to satisfy).
- [ ] Brand-vs-shipped alignment: the rc43 release note explicitly
      addresses how `spring-ai-ascend` integrates Spring AI (or explains
      the chosen alternative per §4.6).

## §8 — Closing note

The team's discipline and governance machinery are the strongest evidence
the gap **can** be closed quickly. This is a scoping issue, not a
competence issue. Closing it makes the v2.0.0 GA tag durable; deferring
it ships a credibility delta that will compound across W1-W4 and cost more
to reconcile later than it costs to land now.

The decision to act on or rebut this memo belongs to the architecture
team. If the team disagrees with the scope diagnosis — for example, by
arguing that `spring-ai-ascend` is intentionally only a runtime kernel
and the agentic primitives are out of scope by design — then the
remediation is different: re-scope ARCHITECTURE.md §1.1 to remove the
Audience B promise, rename the project to match the kernel-only scope,
and keep rc41 as-is. Either path is internally consistent. The current
path — keep the brand and the Audience B promise but ship neither the
primitives nor the integration — is not.
