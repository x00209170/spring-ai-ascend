---
level: L1
view: governance
status: draft
---

# A2D Intake

## 目的

保存从原始输入进入 A2D 的结构化入口记录。该目录用于比 `task.md` 更细地追踪多条输入、澄清和进入判断。

## 适用读者

业务负责人、架构负责人、AI agent。

## 维护规则

- 每个输入使用 `<id>.md` 建档。
- Intake 记录不得替代 version intent；进入版本工作流后必须回链到 `version-intents/<version>.md`。
- 记录必须说明用户、目标、约束、进入判断和澄清问题。
- 原始输入不完整时保持 `draft`，不得写成 accepted 设计。

## 最小结构

```markdown
# <id> A2D Intake

## 输入摘要
## 用户和目标
## 约束
## 进入判断
## 澄清问题
```
