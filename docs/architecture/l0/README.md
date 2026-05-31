---
level: L0
view: governance
status: draft
---

# L0 架构树

## 目的

本目录是 `docs/architecture/` 下的架构树根节点。L0 管理系统级架构事实；L1 服务架构和 L2 专题设计必须挂在本目录下，形成可追踪的父子关系。

```text
l0/
  00-overview/              # 系统概览、术语、原则
  01-capabilities/          # 系统能力
  04-modules/               # L0 模块职责和交付设计包
  06-state/                 # 系统级状态归属
  05-contracts/             # 跨服务 / 跨模块契约草案
  02-scenarios/             # 系统级业务场景和技术子场景
  07-invariants/            # 系统级架构不变量
  08-harness/               # 系统级或跨模块 harness
  09-verification/          # 系统级验证矩阵
  10-governance/            # A2D、评审、变更和目录治理
  l1/<service>/             # 服务级 L1 4+1 架构
  l1/<service>/l2/<topic>/  # 服务内 L2 专题设计
```

## 层级规则

- L0 可以定义服务边界、跨服务契约、系统状态归属、系统不变量和全局验证要求。
- L1 服务架构必须细化 L0，但不能改写 L0；每个 L1 服务目录必须说明它继承的 L0 边界。
- L2 专题设计必须挂在父级 L1 服务目录的 `l2/<topic>/` 下，细化该服务的某个视图、边界合同或关键机制。
- `04-modules/<module>/` 保留为交付设计包和开发切片入口，不作为 L1 4+1 架构的权威位置。

## 入口

| 类型 | 位置 |
|---|---|
| L0 文档地图 | [../README.md](../README.md) |
| L0 目录 catalog | [10-governance/document-artifact-catalog.md](10-governance/document-artifact-catalog.md) |
| A2D 工作模型 | [10-governance/a2d-working-model.md](10-governance/a2d-working-model.md) |
| `agent-service` L1 4+1 | [l1/agent-service/README.md](l1/agent-service/README.md) |
