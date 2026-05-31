---
level: L1
view: governance
status: draft
---

# 多层更新协议

## 目的

定义当一个变更同时影响 L0、L1、L2 多层架构文档时，应该以什么顺序工作、什么规则保持一致性、谁在层间边界审批。

变更分级（Level 0–3）和版本级工作流（H0–H3）已分别由 [change-governance.md](change-governance.md) 和 [a2d-working-model.md](a2d-working-model.md) 定义。本文补充的是**多层联动的工作编排**。

## 适用读者

架构负责人、模块负责人、AI agent。

## 核心规则

### 1. 先定影响层，再动手

任何变更在开始执行前，必须先回答：**这个变更影响了哪些层？**

| 变更示例 | 影响层 | 判断依据 |
|---|---|---|
| 新增模块内部 helper 方法 | L2 only | Level 0，不改变外部语义 |
| 给现有 ICD 加 optional 字段 | L2 + L1 | Level 1，ICD 属 L2，模块设计包属 L1 |
| 改变状态 owner | L0 + L1 + L2 | Level 3，涉及架构原则、模块设计、契约三方 |
| 新增业务场景 | L0 + L1 | Level 2，场景定义属 L0，模块承接属 L1 |
| 实现中发现 L0 原则与实际矛盾 | L2 → L1 → L0 | 自底向上发现，需要逐层裁决 |

判断方法：查看 `change-governance.md` 的变更分级表，Level ≥ 2 的变更通常跨层。

### 2. 自顶向下更新 + 影响传导

**当多层都需要更新时，永远从最高层开始，逐层向下。** 上层完成变更后，**必须主动分析下游影响并产出调整需求**，作为下层的输入。下层不是自己去猜上层改了什么，而是收到上层的传导需求后再工作。

```text
影响 L0 → 改 L0 → 产出 L1 调整需求 → 改 L1 → 产出 L2 调整需求 → 改 L2
影响 L1 → 改 L1 → 产出 L2 调整需求 → 改 L2
影响 L2 → 只改 L2
```

原因：上层定义约束，下层继承约束。如果先改下层，可能在改上层时发现下层方向错误，导致返工。而如果上层只改自己不传导影响，下层会遗漏调整或方向出错。

具体工作顺序：

```text
Step 1: 更新最高层文档，确认约束和方向
Step 2: 分析本次变更对下层的具体影响，产出层间调整需求
Step 3: 将调整需求传导给下层，下层基于需求执行变更
Step 4: 下层变更完成后，检查是否引入了对更下层的新影响；如有，重复 Step 2–3
Step 5: 全部层级更新完成后，执行跨层一致性检查
```

### 2.1 层间调整需求

**每完成一层的变更后，必须产出一份结构化的层间调整需求**，作为下层的变更输入。

```yaml
# 层间调整需求模板
source_layer: L0          # 本层
source_change: "新增 Run 状态 SUSPENDED，归属 owner 为 agent-service"
target_layer: L1          # 需要调整的下层
affected_modules:         # 受影响的模块设计包
  - module: agent-service
    impact: "需要更新 Run 生命周期组件，新增 SUSPENDED/RESUMING 状态转换"
  - module: agent-bus
    impact: "需要更新 federation envelope，支持 SUSPENDED 状态的跨服务传播"
affected_contracts:       # 受影响的契约（如果已知）
  - ICD-Workflow-AgentService: "状态转换表需要新增 SUSPENDED 相关行"
constraints:              # 下层变更必须遵守的约束
  - "SUSPENDED 状态的 writer 仍然是 agent-service，不得新增其他 writer"
  - "新增状态不得破坏已有 Running→Completed/Failed 的转换路径"
open_questions: []        # 上层不确定、需要下层确认的问题
```

层间调整需求的核心字段：

| 字段 | 含义 |
|---|---|
| `source_change` | 上层做了什么变更，用一句话描述 |
| `affected_modules` | 受影响的模块列表，每个模块一句话说明影响 |
| `affected_contracts` | 受影响的契约列表（L0 可能不确定，可以留空由 L1 补充） |
| `constraints` | 下层变更必须遵守的硬约束，从上层变更推导而来 |
| `open_questions` | 上层不确定的点，需要下层在变更时确认或反馈 |

### 2.2 下层收到调整需求后做什么

1. **确认理解**：确认调整需求中的 `source_change` 和 `constraints` 是否清晰。有疑问立即向上反馈。
2. **细化影响**：基于调整需求，在本层内详细分析受影响的文档、组件、契约。上层给的是方向，下层负责落到具体文档。
3. **执行变更**：在本层内执行变更，遵守 `constraints`。
4. **传导更下层**：如果本层变更对更下层有影响，按同样模板产出新的调整需求。
5. **反馈结果**：变更完成后，向上层报告调整完成情况，特别是 `open_questions` 的答复和任何新发现的约束。

### 2.3 传导的场景示例

**场景：L0 新增能力"Agent Sandbox 资源隔离"**

```text
L0 变更：能力地图新增 CAP-13 "Sandbox Resource Isolation"

→ 产出 L1 调整需求：
  affected_modules:
    - agent-execution-engine: "需要设计 Sandbox 执行器组件"
    - agent-middleware: "需要设计资源配额检查点"
  constraints:
    - "sandbox 执行必须受 R-L 策略约束"
    - "资源超限不得直接失败，必须通过 suspend 机制"

→ L1 agent-execution-engine 收到需求：
  细化为组件设计 → 产出 L2 调整需求：
    affected_contracts:
      - "需要新增 EngineEnvelope 的 executor_type=SANDBOX"
      - "需要新增 SuspendSignal.forResourceLimit(...)"
    constraints:
      - "sandbox executor 必须通过 EngineRegistry 匹配"

→ L2 收到需求：更新 ICD、契约 schema、harness spec
```

**场景：L2 实现中发现 L1 设计有问题**

```text
L2 发现：当前 ICD 要求 SuspendSignal 携带 sandboxId，但 agent-service 没有这个概念

→ L2 不自行决定，向上报告：
  source_layer: L2
  发现: "SuspendSignal 需要 sandboxId，但 agent-service 状态模型中没有 sandbox 概念"
  建议: "在 agent-service 的 Run context 中新增 sandboxRef 字段，或者改 SuspendSignal 不依赖 sandboxId"
  需要_L1_决策: true

→ L1 裁决：选择方案 A（新增 sandboxRef）
  → 更新 agent-service 组件设计
  → 产出新的 L2 调整需求（明确 sandboxRef 的生命周期）
```

### 3. 自底向上裁决

**当下层发现与上层冲突时，停止向下工作，向上裁决。**

| 场景 | 行为 |
|---|---|
| L2 实现发现 L1 设计不可行 | 停止实现，在 L1 目录的 `10-governance/open-issues.md` 中记录发现，等待 L1 设计调整 |
| L1 设计发现 L0 原则需要调整 | 停止 L1 设计，升级到架构负责人裁决，走 Level 3 变更流程 |
| L2 实现发现 L0 原则与实际矛盾 | 停止实现，逐层上报（L2 → L1 → L0），由架构负责人决定是否修改原则 |

裁决完成前，不继续下层工作。裁决结果可能：
- 修改上层（原则或设计让步于实际约束）
- 修改下层（实现找到新方案适应现有约束）
- 记录为 Open Issue，当前版本不解决

### 4. 层间一致性检查

每完成一轮多层更新后，检查以下一致性：

| 检查项 | 方法 |
|---|---|
| L1 模块设计是否违反 L0 原则 | L1 文档引用的 ADR、不变量是否仍在 L0 有效 |
| L2 契约是否违反 L1 模块边界 | L2 ICD 的参与模块、状态 owner 是否与 L1 一致 |
| 跨层引用是否完整 | L0 场景→L1 模块承接→L2 契约的追踪链是否不断裂 |
| 状态归属是否一致 | `06-state/` 矩阵、L1 模块 `03-state/`、L2 契约中的状态描述是否一致 |
| 不变量是否覆盖 | L0 不变量是否在 L1/L2 有对应的验证方式 |

如果发现不一致，按自顶向下原则从最高不一致层开始修正。

### 5. 层间审批边界

不同层的变更由不同角色审批。跨层变更需要**逐层审批**，不能跳层。

| 层 | 审批角色 | 审批内容 |
|---|---|---|
| L0 | 架构负责人 | 原则、场景、不变量、能力地图、模块边界的变更 |
| L1 | 模块负责人 + 架构负责人确认 | 模块设计包、组件分解、状态归属的变更 |
| L2 | 模块负责人 + 契约 owner | ICD、契约 schema、harness spec 的变更 |

跨层变更的审批顺序：

```text
L0 审批通过 → L1 变更就绪 → L1 审批通过 → L2 变更就绪 → L2 审批通过
```

如果某层审批未通过，回到该层修改，不影响已通过的上层。

### 6. AI agent 执行约束

AI agent 在执行多层更新时必须遵守：

1. **不得跳层更新**：不能只改 L2 而不改 L1，如果 L2 变更隐含了 L1 影响。
2. **不得同时开多层草稿**：必须先完成上层，再开下层。防止并行修改导致冲突。
3. **发现冲突时必须停下**：AI 在任何层发现与上层矛盾时，必须报告并等待裁决，不能自行决定修改上层。
4. **记录跨层影响**：AI 在修改任一层时，必须在变更说明中列出其他受影响的层。
5. **完成后主动做一致性检查**：AI 完成多层更新后，必须执行第 4 节的一致性检查并报告结果。

## 多层更新场景速查

| 场景 | 影响层 | 工作顺序 | 审批 |
|---|---|---|---|
| 新增能力 | L0 → L1 → L2 | 能力图 → 模块承接 → 契约 | 架构负责人 → 模块负责人 |
| 改变状态 owner | L0 → L1 → L2 | 状态矩阵 → 模块设计 → ICD + harness | 架构负责人 → 模块负责人 → 契约 owner |
| 新增技术场景 | L0 → L1 → L2 | 场景定义 → 模块承接 → harness | 架构负责人 → 模块负责人 |
| 修改契约语义 | L2 →（可能 L1） | ICD → 检查是否影响 L1 → 如影响则向上 | 契约 owner →（模块负责人） |
| 实现发现设计缺陷 | L2 → L1 →（可能 L0） | 停止实现 → 记录 open issue → 逐层上报 | 从发现层逐层向上裁决 |
| 新增模块 | L0 → L1 → L2 | 模块边界 → 模块设计包 → 契约 | 架构负责人 → 模块负责人 |
| 删除模块 | L0 → L1 → L2 | 标记废弃 → 迁移计划 → 契约清理 | 架构负责人 → 所有受影响模块负责人 |

## 与其他文档的关系

| 文件 | 关系 |
|---|---|
| [change-governance.md](change-governance.md) | 定义单变更分级；本文定义跨层工作顺序 |
| [a2d-working-model.md](a2d-working-model.md) | 定义版本级 H0→H3 流程；本文定义层间更新编排 |
| [a2d-human-checkpoints.md](a2d-human-checkpoints.md) | 定义人类在 H0–H3 的介入点；本文定义多层更新中人类的审批边界 |
| [document-artifact-catalog.md](document-artifact-catalog.md) | 定义目录职责；本文定义跨目录更新顺序 |

## 维护规则

- 新增跨层更新场景时，必须补充到场景速查表。
- 审批角色变更时，更新层间审批边界表。
