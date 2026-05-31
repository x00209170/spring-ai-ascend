---
level: L1
view: governance
status: draft
---

# Architecture Envelopes

## 目的

保存 H1 确认后的版本架构边界，定义 AI 可以自动推进的范围、禁止触碰的边界、兼容性策略、验证要求和升级条件。

## 适用读者

架构负责人、模块负责人、评审者、AI agent。

## 维护规则

- 每个版本使用 `<version>.md` 建档。
- 边界文件必须引用对应 `version-intents/<version>.md`。
- H1 确认后状态可以从 `draft` 提升为 `accepted`。
- 触发升级条件后，AI 必须停止当前自动推进并提交裁决项。

## 最小模板

```yaml
accepted_goals: []
accepted_non_goals: []
allowed_module_changes: []
forbidden_module_changes: []
allowed_contract_changes: []
forbidden_contract_changes: []
state_ownership_policy: []
compatibility_policy: []
verification_required: []
escalation_conditions: []
```
