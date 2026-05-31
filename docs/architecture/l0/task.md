# AI Agent 平台架构文档体系重构任务说明

## 1. 背景

当前 AI Agent 平台设计过程中已经积累了大量系统约束、模块设计、接口设想和实现细节。但这些内容存在明显混杂：

* 有些内容是系统目标；
* 有些内容是能力要求；
* 有些内容是架构原则；
* 有些内容是模块划分；
* 有些内容是具体实现方案；
* 有些内容是 API、幂等、状态机、存储、重试等工程细节；
* 有些内容只是尚未决策的想法或局部建议。

这种混杂导致人类难以完整理解系统，也导致 AI 生成后续设计或代码时缺少稳定依据。因此，需要对当前设计文档进行系统性重构。

本次任务不是简单润色文档，也不是继续添加更多设计内容，而是要建立一套可用于大型系统协同开发的架构文档体系，使设计内容能够支撑后续模块并行开发、接口对接、harness 生成、测试验证和变更治理。

本次重构应采用 **A2D-H：Architecture-to-Delivery Harness** 思路。

核心链路是：

```text
Principle
  ↓
Capability
  ↓
Ownership
  ↓
Contract
  ↓
Scenario
  ↓
Executable Spec
  ↓
Harness
  ↓
Implementation
  ↓
Verification
  ↓
Governance
```

换句话说，架构文档不能只用于“阅读理解”，还要能够逐步转化为：

* 模块责任边界；
* 状态归属；
* 跨模块交互契约；
* 端到端场景；
* 可执行契约；
* 模块 harness；
* 契约测试；
* 场景测试；
* 失败注入测试；
* 变更治理规则。

## 2. 总体目标

请基于当前已有设计文档，完成一次架构文档体系重构。目标是把原来混杂的设计内容整理成一套分层、可追踪、可验证、可支撑开发的架构工件集合。

重构后的文档体系应满足以下目标：

1. 人类可以快速理解系统目标、架构原则、能力地图和模块边界；
2. 每个模块的职责、非职责、状态归属和外部依赖清晰；
3. 跨模块协作不依赖口头约定，而是通过正式 ICD / Contract 约束；
4. 核心端到端流程通过 Scenario Spec 描述，并带有可验证断言；
5. 架构原则能够转化为 architecture invariants；
6. 模块开发可以基于 harness-first 模式推进；
7. 每个设计项都能找到验证方式；
8. 后续变更可以通过 ADR / Change Request 管理；
9. AI 可以基于这些结构化文档继续生成模块 harness、测试用例和实现代码。

## 3. 核心要求

### 3.1 不要继续堆叠约束

不要把所有内容都继续放进一个“系统约束列表”。需要将现有内容重新分类为以下类型：

```text
Goal：系统目标
Capability：系统能力
Architecture Principle：架构原则
Quality Attribute：质量属性
Architecture Decision / ADR：架构决策
Module Responsibility：模块职责
State Ownership：状态归属
Interaction Contract / ICD：交互契约
Scenario Spec：端到端场景
Executable Contract：可执行契约
Architecture Invariant：架构不变量
Implementation Note：实现建议
Open Issue：待决问题
```

如果某条原始约束无法明确归类，应标记为 `Unclear`，不要强行编入正式设计。

### 3.2 严格区分 L0 / L1 / L2 / L3

文档必须分层。禁止把高层架构原则和低层实现细节混在一起。

建议分层如下：

```text
L0: Architecture Overview
系统目标、架构原则、能力地图、模块边界、状态归属、平台/业务归属、演进方向。

L1: Mechanism Design
关键机制设计，例如 Run 生命周期、Agent 执行模型、上下文装配、工具治理、Suspend/Resume、Checkpoint、Agent Swarm、权限、观测、审计等。

L2: Contract & Interface Design
模块交互契约、API 语义、事件契约、错误语义、幂等语义、重试语义、状态机、版本策略。

L3: Implementation Detail
具体代码结构、数据库表、Redis key、MQ topic、SDK 方法、配置项、具体框架适配。
```

L0 文档中不得出现以下内容：

* 具体 API 字段；
* 具体数据库表结构；
* Redis key 设计；
* MQ topic 细节；
* SDK 方法签名；
* retry 次数；
* TTL 数值；
* 某个框架的内部实现细节。

这些应放入 L2 或 L3。

### 3.3 区分“问题”“决策”和“实现方案”

如果现有文档中出现类似内容：

```text
使用 Redis 实现幂等。
使用 Temporal 实现长任务。
使用 Netty 解决长连接问题。
所有 API 都要使用 Idempotency-Key。
```

请不要直接当成架构约束。需要改写为：

```text
Problem：为什么需要这个能力？
Required Property：系统必须满足什么性质？
Architecture Decision：当前选择什么设计方向？
Implementation Option / Note：某种可选实现方案是什么？
Verification：如何验证这个性质被满足？
```

例如：

```text
Problem:
客户端可能因为网络超时或代理中断重复提交创建 Run 请求。

Required Property:
平台必须避免重复创建同一个业务 Run。

Architecture Decision:
Run 创建入口需要提供请求级幂等语义。

Implementation Note:
可以通过 Idempotency-Key + request hash + 状态记录实现。

Verification:
相同业务请求重复提交 N 次，只创建一个 Run，并返回同一个 runId。
```

## 4. 需要产出的文档工件

请重构或生成以下核心文档工件。

### 4.1 Architecture Overview

目标：给人类快速理解系统。

应包括：

```text
1. 系统目标
2. 背景和问题域
3. 核心架构原则
4. 能力地图
5. L0 模块图
6. 核心控制流
7. 核心数据流
8. 核心状态流
9. 模块职责边界
10. 平台能力 / 业务能力归属
11. 关键质量属性
12. 关键 ADR 索引
13. 核心场景索引
14. 关键风险和待决问题
```

要求：

* 保持高层，不混入实现细节；
* 每个模块必须写清楚“负责什么”和“不负责什么”；
* 明确系统中最重要的控制权归属，例如 Run 生命周期、工具治理、上下文装配、状态写入、审计等。

### 4.2 Capability Map

目标：先定义系统能力，再映射到模块，避免模块划分导致能力重复建设。

应至少包括：

```text
1. Agent Execution
2. Workflow Orchestration
3. Context Management
4. Tool Governance
5. Enterprise Middleware Integration
6. Observability
7. Runtime Governance
8. Security / Policy / Audit
9. Agent Swarm / Multi-Agent Coordination
10. Configuration / DSL / Extensibility
```

每项能力需要标注：

```text
Capability ID
Capability Name
Description
Owning Module
Participating Modules
Current Stage
Target Stage
Platform / Business Ownership
Related Scenarios
Related ADRs
Verification Method
```

### 4.3 Module Responsibility Cards

目标：明确模块边界，防止职责漂移和重复建设。

每个核心模块都需要一张责任卡。

至少覆盖：

```text
Gateway
Workflow / Orchestrator
Agent Service
Context Engine
Tool Gateway
Enterprise Middleware Adapter
Observability / Trace Service
Runtime Governance
Configuration / DSL Layer
Agent Swarm Manager, if applicable
```

每张责任卡必须包括：

```text
Module Name
Purpose
Responsibilities
Non-Responsibilities
Owned State
Read-only State
Forbidden State Mutations
Provided Capabilities
Consumed Capabilities
Provided Contracts
Consumed Contracts
Participating Scenarios
Platform / Business Ownership
Evolution Direction
Open Issues
```

特别要求：

* `Non-Responsibilities` 必须写清楚；
* `Owned State` 必须和 State Ownership Matrix 一致；
* 如果一个模块不得直接访问某类资源，需要明确写入 `Forbidden`；
* 不允许通过模糊表述逃避边界，例如“参与管理”“协助处理”这类表达必须进一步拆清楚。

### 4.4 State Ownership Matrix

目标：明确状态归属，解决大型系统中最常见的混乱来源。

需要覆盖至少以下状态：

```text
Run State
Step State
Agent Local State
Workflow Checkpoint
Context Package
Context Version
Tool Call Record
Tool Execution State
Approval State
Audit Record
Trace / Span / Event
Business State
Tenant / Permission / Policy State
Configuration State
```

每类状态必须标注：

```text
State Name
Owner
Allowed Writers
Allowed Readers
Lifecycle
Persistence Requirement
Replay Requirement
Audit Requirement
Idempotency Requirement
Related Module
Related Scenario
Forbidden Writers
```

关键要求：

* 必须明确唯一 owner；
* 可以有多个 reader，但 writer 必须严格控制；
* 如果存在多 writer，需要解释为什么必须如此，并定义冲突解决机制；
* 对不可逆副作用状态，需要明确幂等和审计要求。

### 4.5 ADR：Architecture Decision Records

目标：沉淀关键架构决策，避免设计讨论反复摇摆。

请从当前文档中提取并重写重要 ADR。

优先包含以下方向：

```text
ADR: Workflow owns Run lifecycle
ADR: Agent Service executes steps but does not own global lifecycle
ADR: Context Engine owns context packaging and versioning
ADR: Tool Gateway owns tool authorization and audit
ADR: Agent Swarm must be governed under unified run tree
ADR: Enterprise middleware integration belongs to runtime/governance layer
ADR: Cross-module interactions must be contract-first
ADR: Harness-first development is required for core modules
ADR: L0 documents must not contain L2/L3 implementation details
```

每条 ADR 使用统一模板：

```text
ADR ID
Title
Status: Proposed / Accepted / Rejected / Superseded
Context
Problem
Decision
Alternatives Considered
Consequences
Impacted Modules
Related Principles
Related Contracts
Related Scenarios
Verification Method
Open Questions
```

要求：

* 不要只写结论，必须写取舍；
* 如果现有文档中存在冲突，要在 ADR 中显式说明冲突和最终选择；
* 每个重大控制权归属必须有 ADR 支撑。

### 4.6 ICD：Interface Control Documents / Interaction Contracts

目标：让模块并行开发时可以稳定对接。

至少需要覆盖以下关键模块交互：

```text
Gateway ↔ Workflow
Workflow ↔ Agent Service
Workflow ↔ Context Engine
Agent Service ↔ Context Engine
Agent Service ↔ Tool Gateway
Workflow ↔ Observability
Tool Gateway ↔ Enterprise Middleware
Workflow ↔ Runtime Governance
Context Engine ↔ Knowledge / Memory Store
Agent Swarm Manager ↔ Workflow, if applicable
```

每份 ICD 必须包括：

```text
ICD ID
Participating Modules
Interaction Purpose
Direction
Sync / Async Model
Request Semantics
Response Semantics
Event Semantics, if any
State Semantics
Error Semantics
Retry Responsibility
Timeout Semantics
Idempotency Semantics
Security / Permission Semantics
Audit Semantics
Observability Fields
Versioning Strategy
Backward Compatibility Rules
Contract Tests
Open Issues
```

特别要求：

* ICD 不只是 API 字段文档；
* 必须写清状态、错误、幂等、重试、超时、观测、权限语义；
* 每份 ICD 必须能进一步生成 contract test；
* 如果某个交互目前还没有定论，应标记为 Open Issue，而不是假装已经确定。

### 4.7 Machine-readable Contract

目标：把关键 ICD 转化为机器可读契约，以支持 AI 生成 mock、stub、contract test 和 harness。

对关键 ICD，至少生成 YAML 形式的机器可读契约草案。

示例结构：

```yaml
contract: Workflow-AgentService
operation: executeStep
provider: AgentService
consumer: Workflow
request:
  required:
    - runId
    - stepId
    - attemptId
    - agentSpec
    - contextPackageRef
    - traceContext
response:
  oneOf:
    - StepCompleted
    - StepFailed
    - StepSuspended
idempotency:
  key: runId + stepId + attemptId
retry:
  owner: Workflow
  retryable_errors:
    - AGENT_TIMEOUT
    - MODEL_TEMPORARY_UNAVAILABLE
  non_retryable_errors:
    - INVALID_AGENT_SPEC
    - PERMISSION_DENIED
timeout:
  owner: Workflow
observability:
  required_fields:
    - traceId
    - runId
    - stepId
    - attemptId
security:
  required_context:
    - tenantId
    - actorId
compatibility:
  additive_fields_allowed: true
  breaking_change_requires_new_version: true
contract_tests:
  - completed_response_is_valid
  - suspended_response_is_valid
  - failed_response_is_valid
  - duplicate_attempt_is_idempotent
```

要求：

* 不需要一次性覆盖所有接口；
* 优先覆盖核心链路；
* 字段名可以先保持草案级，但语义必须清晰；
* 后续可以由工程实现继续收敛。

### 4.8 Scenario Spec

目标：用端到端场景验证架构是否真的可组合。

至少需要定义以下核心场景：

```text
S1: Create Run
S2: Execute Agent Step
S3: Build Context Package
S4: Tool Call With Governance
S5: Suspend / Resume
S6: Cancel Running Run
S7: Crash Recovery
S8: Duplicate Request / Idempotency
S9: Agent Spawns Sub-Agent
S10: Permission Denied For Tool Call
S11: Human Approval Required
S12: Observability / Run Tree Reconstruction
```

每个 Scenario Spec 必须包含：

```text
Scenario ID
Scenario Name
Business Goal
Participating Modules
Preconditions
Normal Flow
Alternative Flows
Failure Flows
State Transitions
Events Emitted
Idempotency Requirements
Security / Permission Requirements
Observability Requirements
Assertions
Verification Method
Related ICDs
Related ADRs
Open Issues
```

特别要求：

* 场景不是流程描述就结束，必须有 Assertions；
* Assertions 应该能够转化为测试；
* 优先写清跨模块交互和状态变化；
* 如果一个场景无法验证某个架构原则，应补充场景或修改原则。

示例 Assertion：

```yaml
scenario: SuspendResume
assertions:
  - run_state_sequence:
      - Running
      - Suspended
      - Resuming
      - Running
      - Completed
  - checkpoint_persisted_before_suspended: true
  - no_duplicate_tool_call: true
  - trace_contains:
      - run.suspended
      - approval.received
      - run.resumed
      - run.completed
```

### 4.9 Architecture Invariants

目标：把架构原则变成可检查的不变量，支撑 harness 和代码审查。

至少包括：

```text
Workflow is the sole owner of Run lifecycle state
Agent Service must not mutate Run State directly
Tool calls must go through Tool Gateway
Context packages must be produced by Context Engine
Business state must not be owned by Agent runtime
All cross-module calls must propagate trace context
All irreversible side effects must have idempotency and audit records
Sub-agent creation must be visible under unified Run Tree
```

每条 invariant 使用如下模板：

```yaml
invariant: WorkflowOwnsRunState
rule:
  state: RunState
  only_writer: Workflow
forbidden:
  - AgentService writes RunState
  - Gateway marks Run as Completed
  - ToolGateway mutates Run lifecycle
verification:
  - static_dependency_scan
  - repository_access_mock_assertion
  - integration_state_transition_assertion
related_adr:
  - ADR-Workflow-Owns-Run-Lifecycle
related_scenarios:
  - S1-CreateRun
  - S5-SuspendResume
  - S6-CancelRun
```

要求：

* 每个核心架构原则至少有一条 invariant；
* 每条 invariant 必须有 verification；
* 如果 invariant 暂时无法自动验证，应说明人工审查方式。

### 4.10 Module Harness Spec

目标：使架构文档能够支撑模块开发，而不是只支撑讨论。

至少为以下模块生成 Harness Spec 草案：

```text
Workflow / Orchestrator
Agent Service
Context Engine
Tool Gateway
Observability
```

如果时间有限，优先生成：

```text
Workflow / Orchestrator
Agent Service
Tool Gateway
```

每份 Module Harness Spec 包括：

```text
Module Name
Module Boundary
Responsibilities
Non-Responsibilities
Owned State
Provided Contracts
Consumed Contracts
Upstream Mocks
Downstream Stubs
Scenario Fixtures
Contract Tests
State Machine Tests
Failure Injection Cases
Golden Trace Assertions
Architecture Invariants
Compatibility Tests
Local Runner Requirement
CI Gate Requirements
```

示例：Workflow Harness 应至少覆盖：

```text
Mocks:
- Gateway createRun / cancelRun / resumeRun

Stubs:
- Agent Service returns StepCompleted
- Agent Service returns StepFailed
- Agent Service returns StepSuspended
- Agent Service timeout
- Agent Service duplicate callback

State Machine Tests:
- Created -> Running
- Running -> Suspended
- Suspended -> Resuming
- Resuming -> Running
- Running -> Completed
- Running -> Failed
- Running -> Cancelling -> Cancelled

Invalid Transitions:
- Completed -> Running must be rejected
- Cancelled -> Resuming must be rejected

Failure Injection:
- checkpoint persisted then process crashes
- duplicate approval event
- out-of-order step result
- dependency timeout

Golden Trace Assertions:
- run.created
- step.dispatched
- step.started
- step.completed / step.suspended / step.failed
- run.completed / run.suspended / run.failed
```

### 4.11 Verification Matrix

目标：每个设计项都必须有验证方式。

生成统一验证矩阵，至少包含：

```text
Design Item ID
Design Item Type
Description
Related Module
Related Scenario
Verification Type
Test / Review Name
Owner
Status
```

Verification Type 可以包括：

```text
Contract Test
Scenario Test
State Machine Test
Failure Injection Test
Compatibility Test
Golden Trace Test
Static Architecture Check
Manual Architecture Review
Security Review
Performance Test
```

要求：

* 没有验证方式的设计项需要标记为 `Unverified`；
* 关键 ADR、ICD、Scenario、Invariant 都必须进入 Verification Matrix；
* Verification Matrix 应能指导后续 CI gate 和 harness 生成。

### 4.12 Change Governance

目标：避免后续设计变更再次造成混乱。

需要定义变更分级规则：

```text
Level 0: 模块内部实现变更
Level 1: 向后兼容的接口变更
Level 2: 跨模块语义变更
Level 3: 架构原则、状态归属或控制权变更
```

每级变更需要说明：

```text
Examples
Required Reviewers
Required Document Updates
Required Tests
Required ADR / CR
Compatibility Requirements
```

例如：

```text
如果 Run 状态机发生变化，至少需要：
- 更新 State Ownership Matrix
- 更新 Workflow ADR
- 更新 Gateway ↔ Workflow ICD
- 更新 Workflow Harness Spec
- 更新相关 Scenario Spec
- 更新 Verification Matrix
- 通过状态机测试和场景测试
```

## 5. 推荐文档目录结构

请尽量将文档整理成清晰目录结构。建议如下：

```text
docs/architecture/
  00-overview/
    architecture-overview.md
    glossary.md
    system-principles.md

  01-capabilities/
    capability-map.md

  04-modules/
    gateway.md
    workflow-orchestrator.md
    agent-service.md
    context-engine.md
    tool-gateway.md
    enterprise-middleware.md
    observability.md
    runtime-governance.md

  06-state/
    state-ownership-matrix.md
    run-state-machine.md
    step-state-machine.md

  03-adrs/
    ADR-001-workflow-owns-run-lifecycle.md
    ADR-002-agent-service-executes-steps.md
    ADR-003-tool-gateway-owns-tool-governance.md
    ADR-004-context-engine-owns-context-package.md

  05-contracts/
    human-readable/
      ICD-gateway-workflow.md
      ICD-workflow-agent-service.md
      ICD-agent-service-context-engine.md
      ICD-agent-service-tool-gateway.md
    machine-readable/
      workflow-agent-service.yaml
      gateway-workflow.yaml
      agent-service-tool-gateway.yaml

  02-scenarios/
    S1-create-run.md
    S2-execute-agent-step.md
    S3-build-context-package.md
    S4-tool-call-with-governance.md
    S5-suspend-resume.md
    S6-cancel-run.md
    S7-crash-recovery.md

  07-invariants/
    architecture-invariants.md

  08-harness/
    workflow-harness-spec.md
    agent-service-harness-spec.md
    context-engine-harness-spec.md
    tool-gateway-harness-spec.md
    observability-harness-spec.md

  09-verification/
    verification-matrix.md
    test-strategy.md

  10-governance/
    change-governance.md
    architecture-review-process.md
```

不要求完全照搬目录，但必须保证文档分层清晰，并且能从高层原则追踪到模块、契约、场景、harness 和验证。

## 6. 处理现有文档的具体步骤

请按以下步骤执行。

### Step 1: 扫描现有文档

读取当前所有相关设计文档，提取：

```text
目标
约束
模块
接口
状态
场景
实现方案
待决问题
冲突点
重复内容
```

输出一份 `constraint-and-design-inventory.md`，记录原始内容的归类结果。

### Step 2: 分类和降噪

将提取内容分类为：

```text
Goal
Capability
Principle
Quality Attribute
ADR Candidate
Module Responsibility
State Ownership
ICD Candidate
Scenario Candidate
Implementation Note
Open Issue
Conflict
Duplicate
```

对于 Implementation Note，不要放入 L0。

对于 Conflict，不要自行掩盖，必须显式记录。

### Step 3: 生成 L0 / L1 架构主文档

生成或重构：

```text
architecture-overview.md
system-principles.md
capability-map.md
module responsibility cards
state-ownership-matrix.md
```

### Step 4: 生成 ADR

将关键决策写成 ADR。

所有涉及控制权和状态归属的重大设计必须有 ADR。

### Step 5: 生成 ICD 和 Machine-readable Contract

优先覆盖核心链路：

```text
Gateway ↔ Workflow
Workflow ↔ Agent Service
Agent Service ↔ Context Engine
Agent Service ↔ Tool Gateway
Workflow ↔ Observability
```

每份 ICD 必须有：

```text
human-readable document
machine-readable contract draft
contract test ideas
```

### Step 6: 生成 Scenario Spec

优先生成：

```text
S1 Create Run
S2 Execute Agent Step
S3 Build Context Package
S4 Tool Call With Governance
S5 Suspend / Resume
```

每个 scenario 必须有 assertions。

### Step 7: 生成 Architecture Invariants

从 L0 原则和 ADR 中提取 invariants。

每条 invariant 必须说明如何验证。

### Step 8: 生成 Module Harness Spec

优先生成：

```text
Workflow Harness Spec
Agent Service Harness Spec
Tool Gateway Harness Spec
```

如果资料足够，再生成 Context Engine 和 Observability。

### Step 9: 生成 Verification Matrix

把 ADR、ICD、Scenario、Invariant、Harness 要求全部纳入验证矩阵。

### Step 10: 生成 Change Governance

定义后续如何修改架构、契约、状态机和 harness。

## 7. 输出质量要求

### 7.1 可追踪性

每个重要设计项都应能追踪到：

```text
Related Principle
Related Capability
Related Module
Related ADR
Related ICD
Related Scenario
Related Verification
```

如果暂时无法追踪，应标记为 `Traceability Missing`。

### 7.2 可验证性

每个关键设计都必须有验证方式。

不接受只有口号的设计，例如：

```text
系统必须高可用。
系统必须可扩展。
系统必须安全。
系统必须可观测。
```

必须改写成可验证场景或质量属性，例如：

```text
当 Agent Service 在 step 执行中崩溃时，Workflow 可以基于 checkpoint 恢复任务，且不会重复执行已完成的不可逆工具调用。
```

### 7.3 不得过度实现化

L0/L1 不要落入实现细节。

例如：

* 不要在 L0 里写 Redis key；
* 不要在 L0 里写数据库表；
* 不要在 L0 里写 SDK 方法签名；
* 不要在 L0 里绑定某个实现框架，除非它已经是已接受 ADR。

### 7.4 冲突必须显式化

如果发现以下问题，必须记录：

```text
模块职责重叠
状态 owner 不唯一
调用方向不清晰
重试责任冲突
幂等责任冲突
权限判断重复
上下文归属不清晰
Agent 与 Workflow 控制权冲突
业务状态与运行状态混杂
实现方案被误写成架构约束
```

冲突记录模板：

```text
Conflict ID
Description
Involved Documents
Involved Modules
Conflicting Statements
Impact
Recommended Resolution
Required ADR
Status
```

### 7.5 面向 harness-first development

所有文档都应服务于后续 harness 生成。

因此，以下内容必须明确：

```text
模块边界
状态归属
交互契约
场景断言
错误语义
幂等语义
重试语义
观测字段
架构不变量
验证矩阵
```

## 8. 非目标

本次任务不要求：

```text
1. 直接实现业务代码；
2. 直接重构代码仓；
3. 完整实现所有 harness；
4. 完整定义所有 API 字段；
5. 绑定具体技术选型，除非已有明确 ADR；
6. 把所有细节一次性写完。
```

本次任务的重点是重构架构文档体系，使后续可以可靠地产生 harness 和实现任务。

## 9. 最重要的判断标准

重构完成后，请用以下问题自检：

```text
1. 一个新加入的架构师能否在 30 分钟内理解系统 L0 结构？
2. 一个模块负责人能否明确知道自己负责什么、不负责什么？
3. 每类核心状态是否有唯一 owner？
4. 每个核心跨模块交互是否有 ICD？
5. 每个核心场景是否有 assertions？
6. 每个核心架构原则是否能转化为 invariant？
7. 每个关键设计项是否有验证方式？
8. 是否能基于这些文档生成 Workflow / Agent Service / Tool Gateway 的 harness？
9. 是否能发现并记录当前设计中的冲突和待决问题？
10. 是否避免了把 L2/L3 实现细节污染到 L0？
```

如果这些问题不能回答“是”，说明文档体系还没有重构到位。

## 10. 最小优先级

如果无法一次性完成全部内容，请按以下优先级完成：

```text
P0:
- Architecture Overview
- System Principles
- Capability Map
- Module Responsibility Cards
- State Ownership Matrix

P1:
- ADR for core control ownership
- ICD for Gateway ↔ Workflow
- ICD for Workflow ↔ Agent Service
- ICD for Agent Service ↔ Tool Gateway
- ICD for Agent Service ↔ Context Engine

P2:
- Scenario Spec for Create Run
- Scenario Spec for Execute Agent Step
- Scenario Spec for Tool Call With Governance
- Scenario Spec for Suspend / Resume

P3:
- Architecture Invariants
- Workflow Harness Spec
- Agent Service Harness Spec
- Tool Gateway Harness Spec
- Verification Matrix

P4:
- Machine-readable contracts for additional interfaces
- Context Engine Harness Spec
- Observability Harness Spec
- Change Governance
```

优先级原则：

```text
先保证系统心智一致，
再保证模块边界清晰，
再保证跨模块契约稳定，
再保证场景可验证，
最后生成 harness 和测试体系。
```

## 11. 给 Codex 的执行要求

请在执行时遵守以下规则：

1. 不要只做文字润色，必须重构信息结构；
2. 不要把原文所有内容无差别保留，必须分类、降噪、提升或下沉；
3. 不要隐藏冲突，必须显式记录冲突和待决问题；
4. 不要在 L0 文档中加入 L2/L3 实现细节；
5. 不要发明已经不存在的确定性设计，如果缺少依据，应标记为 Open Issue；
6. 不要把实现方案误认为架构原则；
7. 输出文档必须能够支撑后续 harness-first development；
8. 尽量保持文档短而结构清晰，不要生成无法维护的超长文档；
9. 对每个文档给出清晰标题、目的、适用读者和维护规则；
10. 对每个关键设计项给出 related ADR / ICD / Scenario / Verification 的追踪关系。

## 12. 期望最终结果

最终结果应该是一套新的架构文档体系，而不是一份单体大文档。

理想结果是：

```text
docs/architecture 下形成清晰分层目录；
核心模块有责任卡；
核心状态有 ownership matrix；
核心决策有 ADR；
核心交互有 ICD；
核心场景有 Scenario Spec；
核心原则有 Invariants；
核心模块有 Harness Spec；
关键设计项进入 Verification Matrix；
后续变更有 Change Governance。
```

重写文档时不要保留优先。请先对每个现有项做准入判定：
Architecture Module / Runtime Component / Capability / Contract / State /
Build Artifact / Packaging Artifact / Implementation Constraint / Open Issue。

只有 Architecture Module 和必要 Runtime Component 可以进入 Architecture Overview 的模块边界。
Build Artifact、BoM、starter、dependency management、test fixture、demo、adapter scaffold
不得作为 L0 架构模块出现；如果重要，只能下沉到 implementation constraint、
build governance 或附录。

发现现有文档把构建单元误写成架构模块时，必须纠正，不要兼容保留。
输出前自检：这份 Overview 是否只帮助理解系统运行架构？