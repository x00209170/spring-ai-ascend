---
affects_level: L0
affects_view: process
proposal_status: review
authors: ["Codex Java Microservices and Agent Architecture Review"]
responds_to: docs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review-response.en.md
related_adrs:
  - ADR-0032
  - ADR-0052
  - ADR-0074
  - ADR-0078
  - ADR-0079
related_rules:
  - Rule 25
  - Rule 28
  - Rule 31
  - Rule 32
  - Rule 41
  - Rule 46
  - Rule 77
  - Rule 82
  - Rule 83
affects_artefact:
  - agent-service/ARCHITECTURE.md
  - gate/README.md
  - gate/check_architecture_sync.sh
  - gate/test_architecture_sync_gate.sh
  - docs/contracts/contract-catalog.md
  - docs/contracts/plan-projection.v1.yaml
  - docs/governance/architecture-status.yaml
  - docs/governance/enforcers.yaml
  - agent-service/module-metadata.yaml
  - docs/dfx/agent-service.yaml
---

# L0 rc5 Post-Response Architecture Review

## Executive decision

Do not publish a no-findings L0 completion release note yet.

The rc5 response materially improves the architecture corpus: the engine extraction is real in code, S2C now uses the checked `SuspendSignal.forClientCallback(...)` path, `PlanProjection` is correctly staged as a W2 scheduler-admission projection rather than the full W4 planner, and the memory/knowledge ownership boundary remains appropriately design-level for L0.

However, three active-corpus inconsistencies remain. They are not new runtime implementation defects, but they are release-blocking for an L0 architecture-truth claim because active architecture documents and prevention gates still contradict the shipped module graph and canonical baseline.

Root cause: the rc5 remediation updated the root entrypoints and new catalog surfaces, but several active module-level architecture sections and gate README count claims stayed outside the new prevention rules.

Strongest valid reading: L0 completeness does not require every W2+ component to be implemented. It does require every active claim to say exactly what is shipped, design-only, or deferred, and it requires the gate to catch the exact drift class it claims to prevent.

## Findings

### P0-1: `agent-service/ARCHITECTURE.md` still describes pre-ADR-0079 engine and S2C ownership as current

The rc5 response closes the earlier `agent-execution-engine` skeleton-status defect across README, root architecture, the engine module architecture, POM comments, and `architecture-status.yaml`. It misses the active `agent-service/ARCHITECTURE.md` file.

Evidence:

- `agent-service/ARCHITECTURE.md:41` says every Java path below is rooted at `agent-service/src/main/java/ascend/springai/service/{platform,runtime}/...`.
- `agent-service/ARCHITECTURE.md:300-313` still lists `EngineRegistry`, `EngineEnvelope`, and `ExecutorAdapter` under `agent-service/src/main/java/ascend/springai/service/runtime/engine/`, then says engine code is scheduled to move to `agent-execution-engine`.
- `agent-service/ARCHITECTURE.md:315-323` still lists `S2cCallbackEnvelope` and `S2cCallbackTransport` under `agent-service/src/main/java/ascend/springai/service/runtime/s2c/spi/`.
- `agent-service/ARCHITECTURE.md:492-494` still marks engine extraction to `agent-execution-engine` as out of scope and scheduled post-Phase-C.
- `agent-service/ARCHITECTURE.md:580-583` still carries the risk item "Engine code in transit".
- Actual code now has `EngineRegistry.java` and `EngineEnvelope.java` under `agent-execution-engine/src/main/java/ascend/springai/service/runtime/engine/`, `ExecutorAdapter.java` under `agent-execution-engine/src/main/java/ascend/springai/engine/spi/`, and S2C SPI records/transports under `agent-runtime-core/src/main/java/ascend/springai/service/runtime/s2c/spi/`.
- `README.md:30` and `README.md:38`, `ARCHITECTURE.md:94`, `agent-execution-engine/ARCHITECTURE.md:19-26`, and ADR-0079 all state the post-extraction model.

Impact:

This is the same family as the previous P0-2, but in a still-active module architecture file. A developer or agent starting from `agent-service/ARCHITECTURE.md` would be told to modify the wrong module and would believe ADR-0079 extraction has not happened.

Recommended remediation:

1. Update `agent-service/ARCHITECTURE.md` so its path convention applies only to code still owned by `agent-service`.
2. Rewrite the engine section to say that `agent-service` consumes `agent-execution-engine`; only reference adapters remain in `service.runtime.orchestration.inmemory`.
3. Rewrite the S2C section to say that S2C SPI types live in `agent-runtime-core`, while `InMemoryS2cCallbackTransport` remains in `agent-service`.
4. Delete the out-of-scope and risk statements that describe engine extraction as future work.
5. Add a prevention gate, or strengthen Rule 25 / Rule 81, so active module architecture path claims either resolve to real paths or are explicitly marked historical/deferred. Include a negative self-test reproducing the stale `agent-service/src/main/.../runtime/engine` claim.

### P1-1: Rule 82 does not enforce the baseline single-source invariant it claims to enforce

The canonical baseline says rc5 has 68 active gate checks and 129 gate executable test cases. `gate/README.md` still states 64 and 121 in active operational prose.

Evidence:

- `docs/governance/architecture-status.yaml:89-96` defines `baseline_metrics.active_gate_checks: 68` and `baseline_metrics.gate_executable_test_cases: 129`.
- `docs/governance/architecture-status.yaml:90-91` says raw numeric claims in README and gate README fail Rule 82.
- `gate/README.md:3` says "64 active gate rules" and "121 self-tests".
- `gate/README.md:18-20` repeats `64 active gate rules` and `121 self-tests` in the canonical command block.
- `gate/README.md:51` says `check_architecture_sync.sh` is the canonical L0 release gate with 64 active rules.
- `gate/README.md:53` and `gate/README.md:68` repeat 121 self-tests / `Tests passed: 121/121`.
- `CLAUDE.md:405` and `gate/check_architecture_sync.sh:3733-3762` only require the substring `architecture_sync_gate.baseline_metrics` to appear in README and gate README. They do not reject stale adjacent counts.
- Verification confirms the gap: `bash gate/test_architecture_sync_gate.sh` returns 129/129, `bash gate/check_architecture_sync.sh` passes, and `bash gate/check_parallel.sh` passes while the stale `gate/README.md` counts remain present.

Impact:

This reopens the same defect family that rc5 claims to close. The gate proves that an entrypoint points at the canonical block, but it does not prove that the entrypoint avoids or matches active numeric claims.

Recommended remediation:

1. Either remove all active raw baseline counts from `gate/README.md`, or update every raw count there to match `architecture_sync_gate.baseline_metrics`.
2. Strengthen Rule 82 so README and gate README fail when they contain active unqualified count phrases that disagree with `baseline_metrics`.
3. Add a negative self-test where the file contains the required pointer plus stale `64 active gate rules` / `121 self-tests`; it must fail.
4. Update the rc5 response or follow-up release note so it no longer claims gate README was count-free or baseline-consistent unless that is true.

### P1-2: `ResilienceContract` is simultaneously treated as a shipped SPI and excluded from SPI governance

The corpus currently has two incompatible classifications for `ResilienceContract`.

Evidence:

- `docs/contracts/contract-catalog.md:22-31` lists `ResilienceContract` in "Active SPI interfaces (11 total)" with package `ascend.springai.service.runtime.resilience`.
- `docs/contracts/contract-catalog.md:43` counts `agent-service` as having two SPI interfaces: `GraphMemoryRepository` and `ResilienceContract`.
- `docs/governance/architecture-status.yaml:324-330` calls `ResilienceContract` an SPI and lists its implementation/tests.
- `agent-service/ARCHITECTURE.md:284-290` calls `ResilienceContract` an operation-routing SPI.
- `agent-service/src/main/java/ascend/springai/service/runtime/resilience/ResilienceContract.java:1` declares package `ascend.springai.service.runtime.resilience`, which has no `.spi` token.
- `agent-service/module-metadata.yaml:13-14` declares only `ascend.springai.service.runtime.memory.spi` under `spi_packages`.
- Rule 77 therefore passes vacuously: `ResilienceContract` is called a shipped SPI in the catalog, but it is not in a declared SPI package and is not governed by the `.spi` package convention.
- The rc5 response flags this as a future follow-up only "if a future wave promotes ResilienceContract to a published SPI", but the current catalog already promotes it by listing it as an active shipped SPI.

Impact:

The interface is a live extension-like contract used by Rule 41 skill-capacity semantics, but it is outside the SPI package metadata, DFX package matching, SPI purity scans, and eventual TCK/binary-compat story. W2 tenant-aware changes could become an accidental public contract break while all SPI gates remain green.

Recommended remediation:

Choose one classification now:

- Preferred: treat it as SPI. Move the public surface (`ResilienceContract` plus the value types it exposes, such as `ResiliencePolicy`, `SkillResolution`, `SuspendReason`, and any public registry interface needed by the contract) into a `.spi` package, keep implementations in `runtime.resilience`, add the new package to `agent-service/module-metadata.yaml` and `docs/dfx/agent-service.yaml`, and update the catalog.
- Alternative: treat it as internal runtime contract. Remove it from the "Active SPI interfaces" table and SPI counts, stop calling it an SPI in `agent-service/ARCHITECTURE.md` and `architecture-status.yaml`, and document that a W2 tenant-aware SPI will be introduced later if needed.

Add a prevention check: every interface listed in the contract catalog's SPI table must reside under one of the owning module's declared `spi_packages`, unless the row explicitly marks it as internal/non-SPI and is excluded from the SPI count.

### P2-1: `plan-projection.v1.yaml` still carries an rc1 "no enforcer ships" comment after Rule 83 shipped

This is not a design blocker, but it is a small active-comment drift in the dynamic-planning contract.

Evidence:

- `docs/contracts/plan-projection.v1.yaml:3-4` says the contract is design-only at v2.0.0-rc1 and that no enforcer ships in the hotfix.
- rc5 now ships Rule 83 (`design_only_contract_registered_in_catalog`) and its self-tests. That is not runtime enforcement, but it is an architecture-corpus enforcer for this design-only contract.

Recommended remediation:

Update the comment to distinguish "no runtime self-validation / Java type" from "catalog-registration enforcement now exists via Rule 83".

## Agent-Architecture Assessment

No additional overdesign issue was found in the core agent-driven runtime model itself:

- Engine extraction via `agent-runtime-core` + `agent-execution-engine` is justified; it resolves the prior dependency cycle without forcing engine adapters back into the service module.
- S2C is better after the rc3/rc5 unification: a checked suspension variant is the right L0-level mechanism, and the non-blocking bridge remains explicitly deferred to the W2 async orchestrator.
- Dynamic planning is now at the right abstraction level for L0: `PlanProjection` is a scheduler-admission projection, not the full planner DAG. That keeps W2 capacity arbitration joinable without prematurely shipping the W4 planner.
- Skill capacity and memory/knowledge ownership are conceptually sound. The remaining weakness is not the skill/memory design; it is the `ResilienceContract` classification mismatch around whether that surface is a governed SPI.
- The main overdesign smell is in governance, not runtime: adding new rules with narrow substring checks can create a false sense of closure. Future rules should include negative fixtures that reproduce the actual escaped defect, not only the simplified missing-field variant.

## Verification Performed

- `bash gate/test_architecture_sync_gate.sh` -> `Tests passed: 129/129`
- `bash gate/check_architecture_sync.sh` -> `GATE: PASS`
- `bash gate/check_parallel.sh` -> `GATE: PASS`
- `python gate/build_architecture_graph.py` -> `323 nodes, 445 edges`, validation OK
- `./mvnw.cmd clean verify` -> `BUILD SUCCESS`

The verification results are intentionally included because the first two findings are gate-scope gaps: the current gates pass while active-corpus contradictions remain.

## Suggested rc6 Acceptance Criteria

1. `agent-service/ARCHITECTURE.md` no longer claims engine/S2C SPI ownership that moved to `agent-execution-engine` or `agent-runtime-core`.
2. `gate/README.md` either contains no active raw baseline counts or all active counts match `architecture_sync_gate.baseline_metrics`.
3. Rule 82 has a negative test for "pointer present but stale adjacent count".
4. `ResilienceContract` has a single classification: governed SPI or internal runtime contract, with catalog, metadata, DFX, and prose aligned.
5. `plan-projection.v1.yaml` distinguishes runtime enforcement from Rule 83 catalog-registration enforcement.
6. The architecture team reruns `bash gate/test_architecture_sync_gate.sh`, `bash gate/check_architecture_sync.sh`, `bash gate/check_parallel.sh`, `python gate/build_architecture_graph.py`, and `./mvnw.cmd clean verify`.

