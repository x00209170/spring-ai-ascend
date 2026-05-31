---
level: L0
view: scenarios
affects_level: L0
affects_view: logical, physical
status: proposed
---

# Architecture Review Proposal: Polymorphic Deployment, Online Evolution, and Middleware Nomenclature

> **Date:** 2026-05-21
> **Author:** 正秋总 (Chief Architect) & 急急 (Agent)
> **Target Wave:** W2/W3
> **Related ADRs:** ADR-0033 (Deployment Locus), ADR-0048 (Microservice Commitment), ADR-0073 (Hook SPI), ADR-0075 (Evolution Scope)

## 1. Executive Summary

This proposal addresses critical misalignments between the current code-repository documentation and the architectural consensus regarding the definition of "Agent Middleware". It also formalizes the "Polymorphic Deployment" topology (Platform-Centric vs. Business-Centric) and introduces the "Online/Offline" duality for the Evolution Plane, establishing the blueprint for the target Agentic Cloud architecture.

## 2. L0 Nomenclature Correction: The `agent-middleware` Misalignment

### Issue
The current `agent-middleware/ARCHITECTURE.md` (and ADR-0073) defines `agent-middleware` strictly as an in-process `Runtime Hook SPI` (e.g., intercepting `PRE_LLM_CALL`). This represents a severe semantic hallucination/deviation by previous implementers. 

### Consensus Definition
In our architectural consensus, **"Agent Middleware"** refers to **Cloudified Agentic Capability Services**, specifically:
- Memory Systems (Episodic, Semantic)
- Skill Registry & Topology Centers
- Sandbox Execution Services
- Knowledge Index & Search Platforms

These are analogous to traditional microservice middlewares (Redis, MQ) but elevated for agents. They provide platform-side capabilities that business applications can consume. The current "Hook SPI" is merely an interception mechanism, not the middleware itself.

### Proposed Action
1. **Rename/Re-scope:** Re-designate the current Hook SPI implementation (ADR-0073) to something like `agent-runtime-hooks` or absorb it into `agent-execution-engine`.
2. **Elevate `agent-middleware`:** Redefine the `agent-middleware` module/plane to exclusively host the Cloudified Agentic Services (Memory, Skills, Sandbox, Knowledge).
3. **Degradation Design:** Formalize local fallback strategies for Business-Centric deployments (e.g., local memory caching, local process-level sandbox degradation) when disconnected from the platform middleware.

## 3. Polymorphic Deployment Modes

The architecture MUST natively support two distinct deployment loci for the `Service` and `Execution Engine` components.

### Mode A: Platform-Centric Mode (SaaS / S-Cloud)
- **Topology:** `agent-client` resides on the business side. All other components (`service`, `engine`, `bus`, `middleware`, `evolve`) are hosted on the platform/cloud.
- **Characteristics:** Absolute governance sovereignty. Best for light-weight clients and centralized control. Fits the current Rule R-I.1 (IngressGateway mandate).

### Mode B: Business-Centric Mode (Federated / S-Edge)
- **Topology:** `client`, `service`, and `execution-engine` are deployed on the business department's servers or client devices. `bus` and `middleware` (Capability Services) remain on the platform.
- **Design Extensions Required:**
  - **Local Direct-Connect:** C/S interaction is local (in-process/IPC), bypassing the network for zero-latency execution loops.
  - **Middleware Proxies:** Business-side deployments require a local proxy/cache of the platform middleware to survive network partitions.
  - **Federation Bus:** The platform `agent-bus` evolves from a simple gateway to a Federation Hub, managing cross-network capability RPCs and state synchronization.

## 4. Evolution Plane: Online vs. Offline Modality

The Evolution Plane (`agent-evolve`) must support both Offline (T+1) and Online (Dual-Track Real-time) modalities, adapting to the polymorphic deployment modes.

### 4.1 Modality Definitions
- **Offline Mode:** Telemetry and traces are collected, stored, and analyzed asynchronously (e.g., nightly batch jobs). Model weights, prompts, and routing rules are updated via explicit version releases.
- **Online Mode (Dual-Track):** Employs a Fast Track (System 1) for rapid user-facing execution, and a Slow Track (System 2 / LLM-as-a-Judge) for real-time trajectory critique. Optimizations are injected back into the active agent's short/long-term memory dynamically.

### 4.2 Matrix Adaptation

| Evolution Modality | Platform-Centric Deployment | Business-Centric Deployment |
| :--- | :--- | :--- |
| **Offline (T+1)** | **Fully Centralized:** Data is captured locally on the platform. Batch processing and optimizer run in the same datacenter. | **Edge Collect / Cloud Optimize:** Business side emits scrubbed/PII-filtered trace logs to the platform. Platform optimizes and pushes static rule/model updates. |
| **Online (Dual-Track)** | **In-Cluster Async:** Fast and Slow tracks run in the same cluster. Cache/Memory updates are executed via local Redis/DB. | **Heaven-Earth Coordination:** Fast track runs on the Business Edge for zero-latency. Cloud acts as a Cognitive Coprocessor, running the Slow track. Cloud pushes real-time `ReflectionEnvelope` updates via the S2C Bus to hot-patch local Edge strategies. |

## 5. Next Steps
1. Revise ADR-0073 and `agent-middleware/ARCHITECTURE.md` to reflect the Nomenclature Correction.
2. Introduce a new ADR formally defining the Polymorphic Deployment topologies and the `ReflectionEnvelope` S2C contract for Online Evolution.
