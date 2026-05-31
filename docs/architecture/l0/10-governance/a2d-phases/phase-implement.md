---
phase: AI-7
trigger: H2 已确认架构包，允许进入自动实现
human_review_density: light
---

# AI-7：自动实现

## 应该做什么

在已确认的版本架构边界内，按照交付视图自动修改代码、测试、配置和文档。

1. 按开发切片顺序实现
2. 每个切片完成后运行测试
3. 持续检查实现是否偏离版本架构边界
4. 回写文档：如果实现改变了设计事实，更新对应 A2D 文档

## 输入

| 来源 | 路径 |
|---|---|
| 交付视图 | `10-governance/delivery-projections/<version>.md` |
| 版本架构边界 | `10-governance/architecture-envelopes/<version>.md` |
| 模块设计包 | `04-modules/<module>/` |
| Harness 设计 | `08-harness/` 或模块 `harness-design.md` |
| 验证矩阵 | `09-verification/verification-matrix.md` |
| 代码结构 | 仓库源码 |

## 输出

| 产出 | 归档位置 | 状态 |
|---|---|---|
| 代码变更 | 代码仓库 | reviewed |
| 测试变更 | 代码仓库 | reviewed |
| 文档回写 | 对应 A2D 文档 | draft |
| 实现证据 | PR 描述、CI 结果、验证矩阵 | reviewed |

## 约束

- 遵循 `CLAUDE.md` 所有工程规则
- 遵循 `change-governance.md` 的变更分级
- 不得生成脱离版本架构边界的代码
- 每个 commit 遵循 Rule D-3 预提交清单
- 不得绕过 gate、linter 或测试

## 何时停下问人

- 触发版本架构边界的升级条件
- 实现发现设计包有缺口或矛盾
- 测试无法通过且根因不在本切片范围内
- 发现需要改变版本架构边界的实际情况

## 反模式

- 触发升级条件后继续自动实现
- 只改代码不回写文档
- 跳过测试直接标记切片完成
- 把 Level 2/3 的实现藏在多个 Level 0 commit 里
