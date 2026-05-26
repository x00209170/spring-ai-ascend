---
review_kind: post-corrective-architecture-review
reviewer_role: "senior Java microservices + agent-platform architect (independent)"
target_release: docs/logs/releases/2026-05-26-l0-rc52-agentic-completeness-corrective.en.md
also_reviewed:
  - docs/logs/releases/2026-05-26-rc53-agent-service-l1-4plus1-rewrite-closure.en.md
target_commit_reviewed: f34a2ac10faed7f24326a64c414a2880a1137051
target_ci_run: https://github.com/chaosxingxc-orion/spring-ai-ascend/actions/runs/26428240402
verdict: do-not-close-rc
affects_level: L0, L1
affects_view: development, logical, process, scenarios
blocking_findings:
  - P0-1
  - P0-2
non_blocking_findings:
  - P1-1
  - P1-2
  - P2-1
related_adrs:
  - ADR-0126
  - ADR-0127
  - ADR-0128
  - ADR-0129
  - ADR-0132
  - ADR-0133
  - ADR-0134
  - ADR-0138
  - ADR-0139
related_rules:
  - Rule D-1
  - Rule D-3
  - Rule D-9
  - Rule R-A
  - Rule R-D
  - Rule G-1.1
  - Rule G-8
  - Rule G-9
---

# L0 rc53 Post-Closure Agentic Composition Review

## 1. Executive verdict

**Do not close the L0 RC yet.** The rc52 corrective wave fixed the
previously cited primitive-level defects: streaming response chunks now
have terminal semantics, `ConversationMemory` stores a
`ConversationWindow`, model finish reasons are a closed enum, retrieval
uses same-package carriers, and formal-release transaction validation is
materially stronger. The current `main` commit
`f34a2ac10faed7f24326a64c414a2880a1137051` also has a green GitHub CI
run.

The remaining problem is narrower and deeper: the new agentic contracts
still do not compose as a complete developer-facing contract. In
particular, the `ChatAdvisor` SPI exists, but there is no
`AgentDefinition` binding that lets an agent own its advisors at L0, and
the advisor request/response carriers are untyped `Map<String, Object>`
envelopes without a canonical schema. That means W2 implementers still
must invent local binding and payload conventions for one of the core
agentic extension surfaces.

Root cause: rc52 correctly enforced strict same-package SPI purity for
the new `agent-middleware` surfaces, but the follow-through stopped at
"no cross-SPI imports". It did not complete the second half of the
repair: replacing the removed typed model carriers with a typed
same-package advisor vocabulary, or relaxing purity through an explicit
contract-composition edge. The result is a technically pure SPI that is
not yet a sufficiently precise L0 contract.

## 2. Review scope and method

I reviewed the current repository state at `f34a2ac10...` with this
scope:

- Latest L0 formal corrective note: `docs/logs/releases/2026-05-26-l0-rc52-agentic-completeness-corrective.en.md`.
- Latest L1 closure note: `docs/logs/releases/2026-05-26-rc53-agent-service-l1-4plus1-rewrite-closure.en.md`.
- Agentic SPI contracts for model, streaming, structured output, prompt,
  advisor, memory, vector/retrieval, skill, planner, and agent
  definition.
- Java SPI surfaces under `agent-middleware`, `agent-service`, and
  `agent-execution-engine`.
- Authority surfaces: `CLAUDE.md`, root/module `ARCHITECTURE.md`,
  `docs/contracts/contract-catalog.md`,
  `docs/governance/architecture-status.yaml`, and the recurring-family
  ledger.

Verification observed:

```bash
/usr/local/bin/gh run list --branch main --limit 8 --json databaseId,headSha,status,conclusion,displayTitle,url,createdAt
python3 -m unittest gate.test_release_readiness_tools -v
bash gate/check_formal_release_transaction.sh --evidence gate/release-ci-evidence/2026-05-26-l0-rc52-agentic-completeness-corrective.evidence.yaml
./mvnw -Pquality verify
bash gate/check_architecture_sync.sh
```

Results:

- GitHub CI is green for `f34a2ac10...`, run `26428240402`.
- The formal-release transaction unit tests pass.
- The rc52 formal-release transaction scaffold check passes.
- On the `/mnt/d` mounted workspace, `./mvnw -Pquality verify`
  reproducibly fails inside SpotBugs while copying
  `config/spotbugs/exclude.xml` to `agent-client/target/exclude.xml`
  (`Operation not permitted`). A direct Java `Files.copy(...)` to the
  same target succeeds, and the same working tree verifies successfully
  from a WSL-native copy under `/tmp/spring-ai-ascend-verify`.
- In that WSL-native copy, `./mvnw -Pquality verify` passes and
  `bash gate/check_architecture_sync.sh` passes.

## 3. What is now sound

The architecture team did close several important gaps from the prior
review:

- `ModelFinishReason` gives the tool-call loop a closed, portable
  termination vocabulary.
- Streaming model chunks now distinguish success, cancellation, and
  provider/runtime error behavior.
- `ConversationMemory extends MemoryStore<String, ConversationWindow>`
  matches ordered window semantics much better than the prior one-turn
  generic did.
- Retrieval no longer imports vector `Document` directly across the
  retrieval SPI boundary.
- The formal-release validator now rejects dirty evidence, candidate SHA
  mismatch, release/evidence path mismatch, and non-formal latest-release
  evidence in the tested paths.

I do not recommend reopening the entire L0 design. The residual is not
"missing every agent component"; it is the binding and schema precision
of the agentic composition layer.

## 4. P0 findings

### P0-1 - `ChatAdvisor` is not bound into `AgentDefinition`

**Evidence**

- `docs/contracts/chat-advisor.v1.yaml:12-17` says the advisor chain
  runs around `ModelGateway.invoke(...)` and that Audience B composes
  advisors at `AgentDefinition` time.
- `docs/contracts/chat-advisor.v1.yaml:58-61` repeats that W2 binding
  composes advisors at agent-definition time.
- `docs/contracts/agent-definition.v1.yaml:15-28` lists
  `modelBinding`, `toolBindings`, `memoryBindings`, and optional
  `plannerBinding`, but no advisor binding.
- `agent-service/src/main/java/com/huawei/ascend/service/agent/spi/AgentDefinition.java:32-43`
  has no advisor field.
- `docs/quickstart.md:221-225` confirms the gap by saying the W2 LLM
  gateway wave will add an optional `advisorBindings:
  List<ChatAdvisor>` accessor later.

**Why this blocks L0 closure**

The core Audience B promise is that application developers define agents
against platform SPI shapes, not implementation-local hooks or Spring AI
types. `ChatAdvisor` is intended to be the public model-call decoration
surface for PII redaction, retrieval augmentation, cost attribution,
caching, and policy checks. Without an `AgentDefinition` binding, the
SPI is a floating bean type: an advisor can exist, but no portable agent
contract says which advisors belong to which agent, how ordering is
resolved per agent, or how sync and streaming advisors are selected.

Deferring the binding to W2 is not just deferring implementation. It is
deferring the L0 contract shape. That reopens the exact
developer-ergonomics concern that rc51 and rc52 were meant to close.

**Required correction**

Choose and publish one L0 binding shape:

- Recommended: add a design-only `advisorBindings` field to
  `AgentDefinition` with a stable same-package carrier, for example
  `List<AdvisorBinding>` where each binding references `advisorName`,
  mode (`SYNC`, `STREAMING`, `BOTH`), and an optional per-agent ordering
  override or tie-breaker.
- Update ADR-0128, ADR-0132, `agent-definition.v1.yaml`,
  `chat-advisor.v1.yaml`, `AgentDefinition`, `Agent`, quickstart, module
  architecture appendix, and contract catalog in one wave.
- Add a gate or focused test that fails if `chat-advisor.v1.yaml` claims
  `AgentDefinition` composition while `agent-definition.v1.yaml` and
  `AgentDefinition.java` do not expose an advisor binding.

If the team intentionally wants advisor composition to remain outside
`AgentDefinition` until W2, then `chat-advisor.v1.yaml` and ADR-0132
must stop claiming agent-definition-time composition, and L0 should not
be marked final for the agentic developer-ergonomics layer.

### P0-2 - Advisor envelopes are schema-less maps after the strict-purity repair

**Evidence**

- `AdvisedRequest` is `(tenantId, Map<String, Object> requestEnvelope,
  Map<String, Object> advisorContext)` at
  `agent-middleware/src/main/java/com/huawei/ascend/middleware/advisor/spi/AdvisedRequest.java:25-28`.
- `AdvisedResponse` is `(tenantId, Map<String, Object> responseEnvelope,
  Map<String, Object> advisorContext)` at
  `agent-middleware/src/main/java/com/huawei/ascend/middleware/advisor/spi/AdvisedResponse.java:26-29`.
- `docs/contracts/chat-advisor.v1.yaml:35-44` declares
  `requestEnvelope` and `responseEnvelope` only as `map<string, any>`.
  It does not define required keys, value types, mutation rules, or the
  canonical mapping to `ModelInvocation` and `ModelResponse`.
- `docs/quickstart.md:203-206` still teaches advisors to read
  `req.invocation().messages()` and `response.content()`, but those
  accessors do not exist on the rc52 `AdvisedRequest` /
  `AdvisedResponse` records.

**Why this blocks L0 closure**

The old defect was cross-SPI carrier mismatch. rc52 removed that
mismatch by replacing model carriers with same-package maps. That is
pure, but it is not a precise Java microservice contract. A PII
redaction advisor, retrieval advisor, cost advisor, and cache advisor
all need stable access to model id, messages, tools, parameters, content,
tool calls, usage, and finish reason. With the current map shape, two W2
implementations can both satisfy the L0 SPI while using incompatible key
names and value structures.

This is an overcorrection risk: strict same-package purity became more
important than type-safe developer ergonomics. The resulting API is
harder to use than the provider API it is meant to shield.

**Required correction**

Choose one of these approaches and make it explicit:

- Preferred if strict same-package purity remains mandatory: replace the
  raw envelope maps with typed same-package advisor carriers
  (`AdvisedModelRequest`, `AdvisedMessage`, `AdvisedToolCall`,
  `AdvisedModelResponse`, `AdvisedUsage`, etc.) and add adapter tests
  proving lossless translation to/from `ModelInvocation`,
  `ModelResponse`, and `ModelResponseChunk`.
- Preferred if type reuse is more important than same-package purity:
  create an explicit, narrow "contract composition edge" that allows
  `middleware.advisor.spi` to depend on `middleware.model.spi` carriers,
  and encode that exception in `SpiPurityGeneralizedArchTest`,
  `ARCHITECTURE.md`, and ADR-0132.
- Minimum viable fallback: keep the maps, but publish a versioned
  `advised-model-envelope/v1` schema with required keys, value shapes,
  constants, validators, and contract tests. This is weaker than typed
  carriers and should be treated as a compatibility compromise.

In all cases, update the quickstart so examples compile against the
current SPI and no longer reference non-existent accessors.

## 5. P1 findings

### P1-1 - Streaming advisor ordering relative to hook firing is underspecified

**Evidence**

- `docs/contracts/chat-advisor.v1.yaml:30-34` defines
  `StreamingChatAdvisor`.
- `docs/contracts/chat-advisor.v1.yaml:58-61` describes only
  `ChatAdvisor` around `ModelGateway.invoke(...)`; it does not say how
  `StreamingChatAdvisor` wraps `ModelGateway.stream(...)`.
- `docs/contracts/model-streaming.v1.yaml:12-15` says `BEFORE_LLM` fires
  once before stream open and `AFTER_LLM` fires once with the terminal
  `Complete.finalResponse()`.
- `docs/contracts/model-streaming.v1.yaml:57-68` gives hook payload and
  timing, but does not place `StreamingChatAdvisor` before, inside, or
  after those hook brackets.

**Why this matters**

The first W2 streaming implementation must decide whether telemetry,
policy, memory, and redaction advisors see pre-hook or post-hook
payloads, and whether `AFTER_LLM` observes the provider response or the
advisor-mutated final response. Different choices lead to different
cost attribution, policy, and audit behavior.

**Required correction**

Add one sequence rule to ADR-0129, ADR-0132, `chat-advisor.v1.yaml`, and
`model-streaming.v1.yaml`. For example:

1. Translate `ModelInvocation` into an advised request.
2. Fire `BEFORE_LLM` with the same canonical request payload.
3. Run ordered `StreamingChatAdvisor` chain.
4. Terminal chain step invokes `ModelGateway.stream(...)`.
5. Assemble terminal response.
6. Run outbound advisor transformations.
7. Fire `AFTER_LLM` with the final advised/model response chosen by the
   contract.

The exact order may differ, but it must be single-source and testable.

### P1-2 - Planner and skill contracts still mix invariants with comments only

**Evidence**

- `docs/contracts/plan.v1.yaml:18-25` declares a cycle-free dependency
  map for `Plan`.
- `Plan.java:31-46` null-checks and defensively copies the plan, but it
  does not validate blank ids, duplicate step ids, unknown dependency
  references, or cycles.
- `docs/contracts/planning-request.v1.yaml:24-28` declares budget fields,
  including `maxCostUnits`, but `StepBudget.java:11-14` only checks
  `maxWallClock` for null.
- `docs/contracts/skill-definition.v1.yaml:18-25` says `displayName` is
  non-blank, but `SkillDefinition.java:36-46` only enforces non-blank
  `skillKey`.

**Why this matters**

This is not as severe as the advisor composition blockers, because the
planner runtime is later-wave and the contracts are still
`design_only`. However, it shows a recurring enforcement pattern:
carrier tests mostly prove immutability, while semantic invariants remain
in comments. For dynamic planning and skills, those invariants are part
of the contract. If they are not enforced in carriers, the team should
explicitly state they are TCK/provider obligations and add focused TCK
scaffolding before W2/W4.

**Required correction**

- Decide which invariants are constructor-level and which are TCK-level.
- Add focused tests for whichever level owns them.
- Update plan, planning, and skill contracts so "required field" does
  not imply runtime validation when the Java carrier intentionally
  leaves validation to implementers.

## 6. P2 finding

### P2-1 - The rc53 closure note still contains a live placeholder token

**Evidence**

- `docs/logs/releases/2026-05-26-rc53-agent-service-l1-4plus1-rewrite-closure.en.md:66`
  contains `TBD` in the Wave 8 line-count cell.
- The same release note registers
  `F-placeholder-leaks-into-active-corpus` at
  `docs/logs/releases/2026-05-26-rc53-agent-service-l1-4plus1-rewrite-closure.en.md:82-86`,
  explicitly naming `TBD` as part of the placeholder-leak class.

**Why this matters**

This is a small release-note hygiene issue, but it is a useful signal:
the closure workflow can register a defect family and still leave an
instance of that family in the same closure note. That means the family
is not yet guarded at the publication boundary.

**Required correction**

- Replace the Wave 8 `TBD` with a measured line count, `n/a`, or a
  clearly non-placeholder value such as `not measured`.
- Add a current-release placeholder guard that ignores historical
  citations but fails on placeholder tokens in the active release note
  body and current review response.

## 7. Over-design assessment

The main over-design risk is the strict interpretation of same-package
SPI purity for agentic surfaces. The intended principle is good: public
SPI should not leak Spring, provider, in-memory, or implementation
types. But treating every cross-SPI carrier reuse as forbidden pushed the
advisor API toward untyped maps and duplicate stream chunks.

That tradeoff is acceptable only if the replacement carriers are typed
and canonical. It is not acceptable if the replacement is a generic map
whose schema lives only in test fixtures or implementation convention.

The architecture team should either:

- relax purity with a documented contract-composition exception between
  adjacent middleware SPI packages, or
- keep strict purity and pay the full design cost of typed same-package
  carriers plus lossless adapter tests.

The current middle state is the weakest option: pure enough for ArchUnit,
but too vague for portable W2 implementation.

## 8. Corrective wave acceptance criteria

The next corrective wave should be considered complete only when all of
the following are true:

- `AgentDefinition` has an explicit advisor binding shape, or ADR-0132
  and `chat-advisor.v1.yaml` no longer claim agent-definition-time
  composition.
- Advisor request/response payloads are either typed carriers or backed
  by a versioned schema with constants, validators, and tests.
- Streaming advisor ordering relative to `BEFORE_LLM` and `AFTER_LLM`
  is single-source and reflected in ADR-0129, ADR-0132, and both
  contract YAMLs.
- Quickstart examples compile conceptually against the current Java SPI;
  no example references removed or never-existing accessors.
- Planner and skill invariant ownership is explicit: constructor,
  implementation, or TCK.
- The rc53 closure note no longer contains live placeholder tokens.
- WSL/Linux verification includes `./mvnw -Pquality verify` before
  `bash gate/check_architecture_sync.sh`, so `whitebox_quality_reports`
  is a real green signal.

## 9. Recommended gate additions

To avoid another 30+ RC cycle around the same class of issue, add small
composition gates rather than only more inventory counts:

- `advisor_binding_contract_truth`: if `chat-advisor.v1.yaml` mentions
  `AgentDefinition`, `agent-definition.v1.yaml` and
  `AgentDefinition.java` must expose an advisor binding field or the
  contract must explicitly mark the binding as deferred.
- `advisor_envelope_schema_truth`: reject raw `map<string, any>` advisor
  payloads unless a versioned envelope schema or typed carrier list is
  declared and tested.
- `quickstart_spi_accessor_truth`: grep or compile-smoke quickstart Java
  snippets for non-existent accessor names such as `req.invocation()`.
- `streaming_advisor_hook_order_truth`: require both streaming and
  advisor contracts to cite the same sequence id / ADR paragraph.
- `current_release_placeholder_guard`: block `TBD`, `TODO-template`, and
  anonymous placeholder tokens in the active release note, with a
  documented exception mechanism for historical citations.

## 10. Closure recommendation

Do not publish the final L0 closure release note yet. Publish an rc54
corrective response that closes the two P0 findings and either closes or
explicitly accepts the P1/P2 residuals with concrete follow-up gates.
After that, regenerate the quality reports, run the canonical gate, and
publish a clean formal L0 closure note from a frozen candidate commit.
