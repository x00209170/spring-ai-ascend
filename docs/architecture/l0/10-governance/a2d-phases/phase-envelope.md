---
phase: AI-3
trigger: AI-2 架构影响分析完成，影响矩阵已形成
human_review_density: heavy
---

# AI-3：生成版本架构边界

## 应该做什么

1. 根据版本意图和架构影响矩阵，生成版本架构边界草案
2. 明确 AI 可以自动推进的范围和禁止触碰的边界
3. 定义升级条件
4. 提交给 H1 人类确认
5. 确认后进入 AI-4

## 输入

| 来源 | 路径 |
|---|---|
| 版本意图 | `10-governance/version-intents/<version>.md` |
| 架构影响矩阵 | 版本意图文件的附录 |
| 现有基线约束 | `CLAUDE.md`、`07-invariants/` |

## 输出

| 产出 | 归档位置 | 状态 |
|---|---|---|
| 版本架构边界 | `10-governance/architecture-envelopes/<version>.md` | draft → H1 确认后 accepted |

## 版本架构边界模板

```yaml
accepted_goals:
  - 本版本接受的目标
accepted_non_goals:
  - 明确不做或后续处理的目标
allowed_module_changes:
  - 可自动调整的模块职责和代码范围
forbidden_module_changes:
  - 不得自动改变的模块边界或依赖方向
allowed_contract_changes:
  - 可自动处理的增量契约变更
forbidden_contract_changes:
  - 破坏兼容的契约变化
state_ownership_policy:
  - 状态归属的保持或变更规则
compatibility_policy:
  - 兼容性、迁移、废弃要求
verification_required:
  - 必须生成或更新的验证证据
escalation_conditions:
  - AI 必须停下来请人类裁决的条件
```

## 默认升级条件

- 改变状态 owner、writer 或跨模块控制权
- 引入破坏兼容的契约变化
- 绕过已确认模块边界或依赖方向
- 需求与 `CLAUDE.md`、正式 ADR 或治理规则冲突
- 关键风险无法进入验证矩阵
- 实现结果偏离版本架构边界

## 何时停下问人

- 版本架构边界已生成，提交 H1 确认
- 0→1 场景下，人类需要先选择架构方向再确认边界

## 反模式

- 把边界设得过宽，把 Level 3 变更也放进自动推进范围
- 把边界设得过窄，连 Level 0 变更都需要人工确认
- 升级条件写得模糊，导致 AI 不知道什么时候该停下
