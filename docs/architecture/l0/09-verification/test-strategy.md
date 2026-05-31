---
level: L1
view: scenarios
status: draft
---

# Test Strategy

## 目的

定义从文档到 harness 再到 CI gate 的验证层次。

## 适用读者

测试负责人、模块负责人、AI agent。

## 维护规则

- 实现任务开始前，必须能指向至少一个 verification matrix 行。
- 单元测试、contract test、scenario test 和 architecture check 需要互补，不互相替代。
- 核心场景测试必须从 BA-* 业务活动场景出发，再下钻到 technical sub-scenario。

## 四层测试策略

| Layer | 目标 | 输入文档 | 产出 |
|---|---|---|---|
| Layer 1: Contract | 验证模块交互语义。 | ICD + machine-readable YAML | contract tests, mocks, stubs |
| Layer 2: Business Activity Scenario | 验证业务活动是否能串起模块、能力、状态、契约和观测证据。 | BA-* Scenario + Capability Map + Module Cards + State Matrix | end-to-end BA harness, golden trace assertions |
| Layer 3: Technical Sub-scenario | 验证跨模块机制是否成立。 | `technical/S*.md` + State Matrix | scenario tests, state-machine tests, failure injection |
| Layer 4: Architecture | 验证架构不变量和依赖方向。 | Invariants + Module Cards | static checks, review checklist |

## Harness 生成顺序

1. 读取 BA-* 业务活动场景，确定业务目标和参与模块。
2. 读取 Module Responsibility Card，确认职责和非职责。
3. 加载相关 technical sub-scenario assertions。
4. 加载相关 ICD 和 YAML。
5. 生成 upstream mocks 和 downstream stubs。
6. 生成 state-machine / contract / golden-trace tests。
7. 将测试名回填 Verification Matrix。

## 最小测试闭环

| Capability | 最小闭环 |
|---|---|
| Workflow | 状态机测试 + suspend/resume failure injection。 |
| Agent Service | create task idempotency + tenant isolation + TaskStateStore transition guard；historical RunRepository 只作兼容入口。 |
| Tool Gateway | permission denied no side effect + duplicate attempt no repeated side effect。 |
| Context Engine | tenant-scoped context package + forbidden memory category rejection。 |
| Observability | BA-001 / BA-002 golden trace sequence，technical S1 / S5 负责机制细节。 |
| Developer Experience / Operational Insight | BA-001 debug timeline + context/tool/model evidence + runtime metrics dimension assertion。 |
| Deployment Locus / Capability Placement | hosted service onboarding + local client tool handoff + platform middleware adapter + no C-Side business fact persistence。 |

## 测试诚实规则

- 不用 mock 掩盖 contract 缺失。
- 不用 happy path 场景证明 failure semantics。
- 不用 API 字段 snapshot 替代 ICD 语义测试。
- 不用 S1-S6 技术机制清单替代 BA-* 业务活动级核心场景。
- design_only contract 可以有 harness draft，但不能声明 runtime enforced。
