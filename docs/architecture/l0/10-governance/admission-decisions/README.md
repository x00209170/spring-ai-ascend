---
level: L1
view: governance
status: draft
---

# Admission Decisions

## 目的

保存 AI-4 准入判定结果，说明候选对象是 Architecture Module、Runtime Component、Capability、Contract、State、Build Artifact、Packaging Artifact、Implementation Constraint、Open Issue 还是 Reject。

## 适用读者

架构负责人、模块负责人、评审者、AI agent。

## 维护规则

- 每个判定使用 `<id>.md` 建档，或在版本工作包中批量登记后回链。
- 进入 Overview 或模块边界的对象必须有准入依据。
- Build Artifact、Packaging Artifact、Implementation Constraint 不得被提升为 L0 架构模块。
- 被拒绝或下沉的对象必须说明后续位置。

## 最小模板

```yaml
candidate:
classification:
rationale:
accepted_location:
rejected_or_deferred_location:
related_version_intent:
related_architecture_envelope:
```
