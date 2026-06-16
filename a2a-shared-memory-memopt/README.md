# a2a-shared-memory-memopt

A **kit middleware** (source, not a container) that backs the
[`a2a-shared-memory`](../a2a-shared-memory) experience layer with the **MemOpt**
cognitive memory engine, so cross-run A2A experience gains *persistent, semantic*
memory instead of living only in process.

## What it is

`a2a-shared-memory` defines two memory layers:

| Layer | Lifetime | Default backend |
|---|---|---|
| **Blackboard** (run-scoped) | one collaboration run | in-memory (ephemeral by design) |
| **Experience** (cross-run) | spans runs | in-memory (lost on restart) |

This module replaces *only* the **experience** backend with MemOpt. It is a single
class — `MemOptExperienceStore` — that implements the kit's
[`ExperienceStore`](../a2a-shared-memory/src/main/java/com/huawei/ascend/a2a/memory/experience/ExperienceStore.java)
SPI over MemOpt's framework-neutral **HTTP facade**. The run-scoped blackboard is
intentionally left alone: it is ephemeral by design and does not need a durable
engine.

```
record(...)  ──▶  POST /v1/memory/save     (lesson + provenance metadata)
recall(...)  ──▶  POST /v1/memory/search   (signature query → distilled lessons)
reinforce(.) ──▶  POST /v1/memory/save     (re-confirm a recurring lesson)
```

## Why a kit, not a container

- **Source, debuggable.** Pure JDK `HttpClient` + Jackson. No MemOpt Java types,
  no gRPC, no container image — drop the jar in and read every line.
- **Zero engine coupling.** It speaks only the versioned `/v1` HTTP contract
  ([`MEMORY_HTTP_CONTRACT.md`](https://gitcode.com/KRider/doushuaigong)), never
  MemOpt internals. Either side may evolve independently.
- **OpenClaw-safe.** It touches *only* MemOpt's additive HTTP facade — never the
  engine's native NATS / OpenClaw paths. A MemOpt instance already serving a
  local OpenClaw is unaffected.

## Resilience (memory is a side service)

Memory must never drag down an agent's main path, so the store is defensive:

| Concern | Behavior |
|---|---|
| Slow / down engine | **fails open** — `recall` returns empty, `record` is skipped; no exception on the agent path |
| Repeated failures | a fail-open **circuit** trips after N consecutive errors and short-circuits for `circuitOpenMs` (no wasted round-trips), then probes again |
| Timeouts | per-request connect + read timeout (default 2s) |
| Strict callers | set `Options.failOpen=false` to surface errors instead |

```java
ExperienceStore experience = new MemOptExperienceStore("http://localhost:8077");
// or tuned:
ExperienceStore strict = new MemOptExperienceStore(
        "http://localhost:8077",
        new MemOptExperienceStore.Options(Duration.ofSeconds(2), /*failOpen*/ false, 5, 30_000L));
```

## Partitioning & provenance

Each `(tenant, capability-set + task-type)` keeps its own lessons. The kit derives
a stable MemOpt partition (sent as `user_id`):

```
a2a-exp::<tenantId>::<sorted-capabilities>|<taskType>
```

Capabilities are sorted, so set order never changes the key. Per-lesson provenance
(`sourceAgentId`, `reinforcement`, `kind=a2a-experience`) rides in the record
`metadata`, which MemOpt forwards as the ingest event's `scope`.

> **Honest note on recall provenance.** MemOpt is a *cognitive* engine: it distills
> ingested turns into facts. On recall, hit `metadata` is retriever-shaped
> (`paths`/`tags`/`source`), so per-lesson `sourceAgentId`/`reinforcement` are
> **best-effort** — they may not echo back. The lesson **text** is the durable
> payload; the kit degrades gracefully (null source, 0 reinforcement) when
> provenance is absent.

## Build & test

Orphan module (not in the root reactor). The SPI dependency must be installed first:

```bash
export JAVA_HOME=/path/to/jdk-21
./mvnw -o -f a2a-shared-memory/pom.xml -DskipTests install   # the ExperienceStore SPI
./mvnw -o -f a2a-shared-memory-memopt/pom.xml test           # this module (offline)
```

Tests stub MemOpt's facade with a JDK `HttpServer`, so they run fully offline — no
engine, no network — and verify the save/search mapping, lesson parsing, fail-open,
strict mode, and stable partitioning.
