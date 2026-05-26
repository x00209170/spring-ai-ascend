// Spring AI Ascend Architecture Workspace
//
// Authority: ADR-0147 (Structurizr Workspace Authority); plan at
// D:\.claude\plans\structurizr-d-chao-workspace-spring-ai-adaptive-hellman.md.
//
// W1 SKELETON STATE. Wave 2 fills features/. Wave 3 emits generated/. Wave 4
// rewires the L1 doc rendering. Wave 5 declares this file the architecture
// authoring root (gate flips advisory → blocking).
//
// DSL conventions (per architecture/README.md):
//   * `element` is the keyword for custom elements (NOT `customElement`).
//   * `Model.getElements()` includes CustomElements; validator walks it once.
//   * Identifier scope is flat (top-level globally unique).
//   * Profile-tagged elements MUST satisfy required-properties.yaml.

workspace "Spring AI Ascend" "Architecture authority workspace (W1 advisory)" {

    model {
        springAiAscend = softwareSystem "Spring AI Ascend" "Agent runtime platform" "SAA Module" {
            properties {
                "saa.id" "SYS-SPRING-AI-ASCEND"
                "saa.kind" "software_system"
                "saa.level" "L0"
                "saa.view" "logical"
                "saa.status" "shipped"
                "saa.owner" "architecture"
            }

            agentClient = container "agent-client" "Client SDK and edge-plane API owner" "Java 21 / Maven" "SAA Module" {
                properties {
                    "saa.id" "MOD-AGENT-CLIENT"
                    "saa.kind" "module"
                    "saa.level" "L1"
                    "saa.view" "development"
                    "saa.status" "shipped"
                    "saa.owner" "agent-client"
                }
            }

            agentBus = container "agent-bus" "Bus & State Hub plane — ingress + s2c + three-track channel isolation" "Java 21 / Maven" "SAA Module" {
                properties {
                    "saa.id" "MOD-AGENT-BUS"
                    "saa.kind" "module"
                    "saa.level" "L1"
                    "saa.view" "development"
                    "saa.status" "shipped"
                    "saa.owner" "agent-bus"
                }
            }

            agentService = container "agent-service" "Northbound facade, runtime API, tenant/idempotency, run orchestration" "Java 21 / Spring Boot" "SAA Module" {
                properties {
                    "saa.id" "MOD-AGENT-SERVICE"
                    "saa.kind" "module"
                    "saa.level" "L1"
                    "saa.view" "development"
                    "saa.status" "shipped"
                    "saa.owner" "agent-service"
                }
            }

            agentExecutionEngine = container "agent-execution-engine" "Engine adapter + orchestration SPIs (EngineRegistry, EngineEnvelope)" "Java 21 / Maven" "SAA Module" {
                properties {
                    "saa.id" "MOD-AGENT-EXECUTION-ENGINE"
                    "saa.kind" "module"
                    "saa.level" "L1"
                    "saa.view" "development"
                    "saa.status" "shipped"
                    "saa.owner" "agent-execution-engine"
                }
            }

            agentMiddleware = container "agent-middleware" "RuntimeMiddleware SPI + hook dispatch" "Java 21 / Maven" "SAA Module" {
                properties {
                    "saa.id" "MOD-AGENT-MIDDLEWARE"
                    "saa.kind" "module"
                    "saa.level" "L1"
                    "saa.view" "development"
                    "saa.status" "shipped"
                    "saa.owner" "agent-middleware"
                }
            }

            agentEvolve = container "agent-evolve" "Evolution-plane boundary (skeleton; W3+)" "Java 21 / Maven" "SAA Module" {
                properties {
                    "saa.id" "MOD-AGENT-EVOLVE"
                    "saa.kind" "module"
                    "saa.level" "L1"
                    "saa.view" "development"
                    "saa.status" "shipped"
                    "saa.owner" "agent-evolve"
                }
            }

            graphMemoryStarter = container "spring-ai-ascend-graphmemory-starter" "GraphMemory Spring Boot starter" "Java 21 / Spring Boot starter" "SAA Module" {
                properties {
                    "saa.id" "MOD-GRAPHMEMORY-STARTER"
                    "saa.kind" "module"
                    "saa.level" "L1"
                    "saa.view" "development"
                    "saa.status" "shipped"
                    "saa.owner" "spring-ai-ascend-graphmemory-starter"
                }
            }
        }

        // Wave 2 will land:
        //   !include features/capabilities.dsl
        //   !include features/function-points.dsl
        //   !include features/verification.dsl
        //
        // Wave 3 will land:
        //   !include generated/modules.dsl
        //   !include generated/spi-catalog.dsl
        //   !include generated/enforcers.dsl
        //   !include generated/principles.dsl
        //   !include generated/rules.dsl
        //   !include generated/adr-graph.dsl
    }

    views {
        !include views/L0-system-context.dsl
        !include views/L1-development.dsl
    }
}
