---
level: L1
view: governance
status: draft
---

# Delivery Projections

## 目的

保存 AI-6 生成的交付视图，把已确认的架构产物转化为开发切片、实现任务草案、DoD、harness 计划和验证矩阵增量。

## 适用读者

模块负责人、开发者、AI agent、harness 生成器、测试负责人。

## 维护规则

- 每个版本使用 `<version>.md` 建档。
- 每个任务必须追溯到版本架构边界、A2D 产物和验证矩阵。
- 任务不得越过 `architecture-envelopes/<version>.md` 的允许范围。
- 进入自动实现前，DoD、验证方式和 harness 计划必须明确。

## 最小结构

```markdown
# <version> 交付视图

## 1. 开发切片
## 2. 实现任务草案
## 3. Harness 计划
## 4. 验证矩阵增量
```
