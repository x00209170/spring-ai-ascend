---
level: L1
view: governance
status: draft
---

# Version Intents

## 目的

保存 H0 输入的版本目标、范围、非目标、约束、风险预算和发布门槛，作为 AI-1 需求归一化与 AI-2 影响分析的入口。

## 适用读者

业务负责人、架构负责人、模块负责人、AI agent。

## 维护规则

- 每个版本使用 `<version>.md` 建档。
- 文件必须说明目标、非目标、允许变更范围、禁止边界、风险预算和发布门槛。
- 需求分类表、冲突清单、澄清问题和架构影响矩阵可以作为附录写入同一文件。
- 已进入版本架构边界的内容必须回链到对应 `architecture-envelopes/<version>.md`。

## 最小模板

```yaml
version_goal:
functional_requirements: []
non_functional_requirements: []
must_keep: []
allowed_change_scope: []
risk_budget: []
release_bar: []
open_questions: []
```
