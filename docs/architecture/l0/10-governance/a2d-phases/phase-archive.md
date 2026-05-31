---
phase: AI-9
trigger: H3 已确认残余风险和发布条件
human_review_density: light
---

# AI-9：版本归档

## 应该做什么

1. 汇总本版本变更：接受项、废弃项、未决项
2. 更新所有受影响文档的状态标记
3. 生成架构基线说明
4. 建立替代关系
5. 结转开放问题到后续版本
6. 更新 README 和 catalog

## 输入

| 来源 | 路径 |
|---|---|
| 版本意图 | `10-governance/version-intents/<version>.md` |
| 版本架构边界 | `10-governance/architecture-envelopes/<version>.md` |
| 评审包 | `10-governance/review-packets/<version>.md` |
| 交付视图 | `10-governance/delivery-projections/<version>.md` |
| 验证汇总 | 评审包的验证章节 |
| PR / commit | 代码仓库 |

## 输出

| 产出 | 归档位置 | 状态 |
|---|---|---|
| 架构基线说明 | `10-governance/baselines/<version>.md` | accepted |
| 文档状态更新 | 各 A2D 文档的 frontmatter | accepted |
| 替代关系记录 | 被替代文档的 frontmatter | accepted |
| README 更新 | `docs/architecture/README.md`、`docs/architecture/l0/README.md` | accepted |
| 结转开放问题 | 基线说明 + 模块 `open-issues.md` | accepted |

## 基线说明模板

```markdown
# <version> 架构基线

## 有效内容
本版本中状态为 accepted 的架构事实清单。

## 草稿内容
本版本中仍为 draft 的内容，不作为后续工作基线。

## 已替代内容
本版本中被替代的旧内容，链接到新内容。

## 结转开放问题
未在本版本解决的问题，说明 owner 和后续版本。
```

## 退出条件

- 当前 L0/L1/L2 哪些内容有效清楚
- 哪些内容是 draft、哪些已被替代清楚
- 后续 AI 和开发者不会把旧文档误读成当前基线

## 反模式

- 归档时不更新文档状态标记
- 结转问题没有 owner
- 基线说明只写"完成了什么"，不写"哪些仍然是 draft"
