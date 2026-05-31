---
level: L1
view: scenarios
status: draft
---

# Scenario Index

## 目的

定义本目录的场景层次：业务活动级核心场景用于串起整体模块，technical sub-scenarios 用于验证单个机制。

## 适用读者

架构师、模块负责人、AI agent、harness 生成器、评审者。

## 维护规则

- 核心场景必须是 Business Activity Scenario，能够串起多个架构模块。
- `technical/` 下的 S1-S6 是机制验证场景，不能替代业务活动级核心场景。
- 新增核心场景必须说明直接第一用户、开发态体验、运行态体验、模块协作流、特性覆盖、模块边界适配性和缺口检查。

## 场景层次

```text
Business Activity Scenario
  -> Direct user profile
  -> Development-time journey
  -> Runtime / operations journey
  -> Key feature coverage
  -> Architecture module collaboration flow
  -> State / contract / observability assertions
  -> Technical sub-scenarios
  -> Harness fixtures
  -> Verification Matrix
```

## 业务活动级核心场景

| ID | 场景 | 目的 | 覆盖 technical sub-scenarios |
|---|---|---|---|
| BA-001 | [Agent Handles Business Request](BA-001-agent-handles-business-request.md) | 用户提交业务请求，Agent 完成上下文装配、模型推理、工具调用和回复。 | S1, S2, S3, S4 |
| BA-002 | [Human Approval Tool Call](BA-002-human-approval-tool-call.md) | 工具调用需要人工确认，平台 suspend，收到确认后 resume。 | S1, S2, S4, S5 |
| BA-003 | [Multi-Agent Delegation](BA-003-multi-agent-delegation.md) | Agent 派生子任务或跨 service / 跨部门协作，统一 Task tree、trace、LLM 成本和审计。 | S1, S2, S5, S6 |

## 技术子场景

| ID | 场景 | 用途 |
|---|---|---|
| S1 | [Create Task / Invocation](technical/S1-create-run.md) | 入口、幂等、Task 初始状态和 client invocation reference。 |
| S2 | [Execute Agent Step](technical/S2-execute-agent-step.md) | engine dispatch、agent step、terminal transition。 |
| S3 | [Build Context Package](technical/S3-build-context-package.md) | Session / Memory / Retrieval 上下文装配。 |
| S4 | [Tool Call With Governance](technical/S4-tool-call-with-governance.md) | tool / skill 权限、capacity、审计和幂等。 |
| S5 | [Suspend / Resume](technical/S5-suspend-resume.md) | 长等待、client callback、resume 和资源释放。 |
| S6 | [Child Task / Federation](technical/S6-child-run-federation.md) | child Task、跨 service / 跨部门 A2A、federation envelope、join 和 Task tree 聚合。 |
