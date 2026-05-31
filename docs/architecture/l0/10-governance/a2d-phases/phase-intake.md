---
phase: AI-1
trigger: 人类注入版本意图，或出现新的需求输入
human_review_density: heavy
---

# AI-1：需求归一化与分流

## 应该做什么

1. 读取人类提供的版本意图（可能是完整的 YAML 模板，也可能是一段自然语言描述）
2. 补充阅读项目上下文：`CLAUDE.md`、`architecture-status.yaml`、当前架构基线
3. 把原始输入整理成统一需求池，逐项分类
4. 检查冲突、重复、缺口
5. 生成澄清问题（如果输入不足以继续）
6. 进入 AI-2 架构影响分析

## 输入

| 来源 | 路径 |
|---|---|
| 版本意图 | `10-governance/version-intents/<version>.md`（如不存在则从人类对话中提取） |
| 治理规则 | `CLAUDE.md` |
| 架构状态 | `docs/governance/architecture-status.yaml` |
| 现有架构基线 | `00-overview/architecture-overview.md`、`01-capabilities/capability-map.md` |
| 模块边界 | `04-modules/module-responsibility-cards.md` |
| 状态归属 | `06-state/state-ownership-matrix.md` |

## 输出

| 产出 | 归档位置 | 状态 |
|---|---|---|
| 结构化版本意图 | `10-governance/version-intents/<version>.md` | draft |
| 需求分类表 | 版本意图文件的附录 | draft |
| 冲突清单 | 版本意图文件的附录 | draft |
| 澄清问题 | 版本意图文件的附录 | draft |

## 需求分类集合

```text
functional_requirement     # 功能需求
non_functional_requirement # 非功能需求：性能、可用性、安全、隔离等
governance_constraint      # 治理约束：不改变规则本身，但影响实现方式
bug_regression             # 缺陷或回归
open_question              # 不确定事项
out_of_scope               # 明确不属于本版本
```

## 检查清单

- 是否有互相冲突的需求
- 是否有重复需求
- 是否有缺少验收口径的需求
- 是否有影响已有架构事实但未说明的需求
- 是否有属于本版本但人类未纳入的需求（对照 capability-map 和 scenario）

## 何时停下问人

- 需求存在根本冲突，AI 无法自行裁决
- 输入不足以判断是功能需求还是非功能需求
- 发现人类可能遗漏的重要约束或风险

## 反模式

- 把模糊需求直接当成明确需求处理
- 跳过冲突检查，留给后续阶段发现
- 要求人类先写完整设计才继续
