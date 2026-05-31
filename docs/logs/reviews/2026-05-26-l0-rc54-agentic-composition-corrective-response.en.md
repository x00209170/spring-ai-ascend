---
review_kind: corrective-response
target_review: docs/logs/reviews/2026-05-26-l0-rc53-post-closure-agentic-composition-review.en.md
target_release: docs/logs/releases/2026-05-26-rc53-agent-service-l1-4plus1-rewrite-closure.en.md
affects_level: L0, L1
affects_view: development, logical, process, scenarios
affects_artefact: [README.md, agent-middleware/ARCHITECTURE.md, agent-middleware/src/main/java/com/huawei/ascend/middleware/advisor/spi, agent-middleware/src/main/java/com/huawei/ascend/middleware/advisor/adapter, agent-service/src/main/java/com/huawei/ascend/service/agent/spi, agent-execution-engine/src/main/java/com/huawei/ascend/engine/planner/spi, docs/adr/0126-planner-spi.yaml, docs/adr/0127-skill-spi-tool-unification.yaml, docs/adr/0128-agent-first-class-entity.yaml, docs/adr/0129-streaming-aware-model-gateway.yaml, docs/adr/0132-chat-advisor-spi.yaml, docs/contracts/agent-definition.v1.yaml, docs/contracts/chat-advisor.v1.yaml, docs/contracts/contract-catalog.md, docs/contracts/model-streaming.v1.yaml, docs/contracts/plan.v1.yaml, docs/contracts/planning-request.v1.yaml, docs/contracts/skill-definition.v1.yaml, docs/governance/architecture-status.yaml, docs/governance/enforcers.yaml, docs/quickstart.md, gate/lib/check_contract_spi_count_truth.py, gate/lib/check_release_note_current_truth.py]
verdict: close-after-formal-release-evidence
related_rules: [Rule D-1, Rule D-3, Rule D-9, Rule R-A, Rule R-D, Rule G-8, Rule G-9]
related_family: [F-agentic-contract-composition-gap, F-placeholder-leaks-into-active-corpus]
---

# rc54 Agentic-Composition Corrective Response

## Classification before fixes

The rc53 findings were classified before code edits and before the
repository-wide sweep:

| Class | rc53 findings | Defect family decision |
|---|---|---|
| Agent/advisor composition gap | P0-1 | Existing `F-agentic-contract-composition-gap`. The family already records the rc53 recurrence in YAML and rendered Markdown, so no new family was added. |
| Advisor envelope schema gap | P0-2 | Same composition family. The fix keeps SPI purity strict: no cross-SPI dependencies; use typed same-package advisor carriers plus an adapter outside `.spi`. |
| Streaming hook/advisor ordering ambiguity | P1-1 | Same composition family. Advisor and streaming contracts now share `advisor-model-hook-order/v1`. |
| Comment-only semantic invariants | P1-2 | Same composition family, narrowed to carrier contract truth. Constructor-owned planner and skill invariants are now executable. |
| Current-release placeholder leak | P2-1 | Existing `F-placeholder-leaks-into-active-corpus`; no new family was needed. |

No unrecorded problem type was found. The review cited two already-recorded
families, and the rc53 family occurrence was already present in
`docs/governance/recurring-defect-families.yaml` plus its rendered Markdown
view before systemic code changes began.

## Repository-wide sweep

The same classes were scanned across the current repository, not only the
review line references:

- Agent/advisor binding claims: `chat-advisor.v1.yaml`, ADR-0132, quickstart,
  and Java `AgentDefinition` were checked for drift. The latent gap was fixed
  by `AgentDefinition.advisorBindings` and same-package `AdvisorBinding`.
- Advisor raw-envelope payloads: `requestEnvelope` / `responseEnvelope` and
  quickstart-only stale accessors were searched. The active SPI now uses
  `AdvisedModelRequest` and `AdvisedModelResponse`; historical review text is
  left untouched as log evidence.
- Streaming order claims: `chat-advisor.v1.yaml`, `model-streaming.v1.yaml`,
  ADR-0129, ADR-0132, and Javadocs now point at the same
  `advisor-model-hook-order/v1` sequence.
- Planner and skill carrier invariants: `Plan`, `PlanStep`, `StepBudget`,
  `PlanningBudget`, and `SkillDefinition` now enforce constructor-owned
  semantic invariants that the YAML contracts previously stated only in
  comments.
- Current placeholder tokens: the rc53 closure note Wave 8 cell no longer uses
  `TBD`, and Rule 127 now rejects live placeholders in the latest release note
  or latest review response while allowing historical defect-family citations.

## Corrective decisions

- Add `List<AdvisorBinding>` to `AgentDefinition` and `Agent.advisorBindings()`
  so L0 declares how agents own advisors.
- Keep the SPI purity interpretation strict for the advisor fix: `.spi`
  packages do not import adjacent SPI carriers. The advisor SPI defines typed
  same-package carriers, and `AdvisedModelEnvelopeAdapter` performs translation
  outside the SPI package.
- Promote the streaming/advisor/hook order to a shared sequence id,
  `advisor-model-hook-order/v1`, so sync and streaming calls have one
  documented bracket around `BEFORE_LLM`, advisor chains, gateway calls, and
  `AFTER_LLM`.
- Extend existing Rule 129 rather than adding a new active gate count: it now
  checks contract catalog counts plus agent/advisor composition truth.
- Extend Rule 127 so publication guards catch current placeholder leaks, with
  explicit citation exceptions for the placeholder-family vocabulary.

## Validation status

- Red phase: focused Maven tests initially failed to compile against missing
  `AdvisorBinding`, `AdvisedModelRequest`, `AdvisedModelResponse`, and
  `AdvisedModelEnvelopeAdapter`.
- `./mvnw -pl agent-middleware,agent-execution-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AdvisorSpiCarrierImmutabilityTest,AdvisedModelEnvelopeAdapterTest,SkillDefinitionCarrierInvariantTest,PlannerSpiCarrierImmutabilityTest test` — PASS, 18 tests.
- WSL-native `/tmp` verification copy:
  `./mvnw -pl agent-service -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AgentSpiCarrierImmutabilityTest,SpiPurityGeneralizedArchTest test` — PASS, 11 tests.
- WSL-native `/tmp` verification copy: `./mvnw -Pquality verify` — PASS, 461 XML-counted Maven tests.
- `bash gate/test_architecture_sync_gate.sh` — PASS, 260/260 self-tests.
- `bash gate/build_architecture_graph.sh` — PASS, 606 nodes / 1112 edges.

Final closure is the rc54 formal release note plus evidence bundle generated
from the frozen corrective candidate commit.
