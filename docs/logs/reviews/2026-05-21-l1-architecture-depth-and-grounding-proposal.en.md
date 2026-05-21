---
level: L0
view: development
affects_level: L0, L1
affects_view: development, logical
status: proposed
---

# Architecture Review Proposal: L1 Architecture Depth & Grounding

> **Date:** 2026-05-21
> **Author:** LucioIT (Core Architect) & 急急 (Agent)
> **Target Wave:** W0/W1 (Immediate Enforcement)
> **Related Rules:** Rule G-1 (Layered 4+1 Discipline), Rule R-D (SPI + DFX + TCK Co-Design)

## 1. Executive Summary

This proposal addresses the risk of "hollow" L1 architecture documents (`agent-*/ARCHITECTURE.md`). Currently, Rule G-1 mandates the 4+1 structure but does not enforce the *depth* or *grounding* of the content. To ensure L1 documents effectively constrain subsequent L2 designs and code implementation, we propose adding a new sub-clause to Rule G-1 (or a standalone rule) requiring strict code-mapping and SPI enumeration.

## 2. Proposed Rule Addition: Rule G-1.c — L1 Architecture Depth & Grounding

**Every L1 architecture artefact (`agent-*/ARCHITECTURE.md`) MUST satisfy the following depth and grounding constraints:**

1. **Development View Code-Mapping:** The `Development View` section MUST explicitly declare the target directory tree (at least to the package level). Every major logical component mentioned in the `Logical View` MUST be mapped to a specific code path in this tree.
2. **SPI Interface Appendix:** The document MUST contain a dedicated section or appendix enumerating the full formal API/SPI design for the module. Every interface listed MUST correspond to an entry in the module's `module-metadata.yaml#spi_packages` and the global `docs/contracts/contract-catalog.md`.
3. **L2 Constraint Linkage:** For any complex subsystem delegated to an L2 design, the L1 document MUST define the "Boundary Contracts" (inputs, outputs, and performance/DFX expectations) that the subsequent L2 design MUST adhere to.

## 3. Proposed Automated Gate Constraints (Implementation Guided by Core Architecture Team)

To maintain the "Code-as-Contract" standard, the following gate rules (bash scripts) are recommended for implementation by the platform architecture team:

### A. Tree-Parser Enforcer
- **Logic:** Parse the `Development View` section of each `agent-*/ARCHITECTURE.md` to extract Markdown code blocks representing directory trees.
- **Assertion:** Cross-check the extracted paths against the actual filesystem (`src/main/java/...`). Fail the gate if a documented package does not exist or if a major production package is undocumented (excluding `impl` internals).

### B. SPI-Appendix-Scanner
- **Logic:** Extract all `Interface Name` declarations from the "SPI Interface Appendix" of the L1 document.
- **Assertion:** Perform a 3-way cross-validation: the names in the L1 appendix MUST exist in `docs/contracts/contract-catalog.md`, MUST fall under `module-metadata.yaml#spi_packages`, and MUST exist as actual `public interface` `.java` files.

## 4. Next Steps
- Review and approve the proposed rule text.
- LucioIT (Core Architect) to draft the formal ADR and guide the implementation of the corresponding `gate/rules/*.sh` scripts.
- Apply this standard to all upcoming L1 domain expansions.