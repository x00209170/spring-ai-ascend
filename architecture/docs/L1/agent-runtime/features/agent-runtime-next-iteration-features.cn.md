---
level: L1
view: features
module: agent-runtime
status: planning
updated: 2026-06-14
authority: "v0.1.0 release checklist ⬜ items consolidation + third_party/fin-code analysis"
covers: [异构兼容增强, 中间件补齐, 通讯模型补全, 轨迹生产化, 运维独立部署, 多Agent编排, 记忆增强, 意图管道, 知识检索]
---

# agent-runtime — 下一迭代特性清单（v0.2.0 候选）

> 本文档汇总 v0.1.0 发布特性清单中所有 ⬜ 项，作为下一迭代的特性规划。
> 特性按优先级排列：P0 = 生产基线必备，P1 = 重要增强，P2 = 可延后。

---

## 1. P0 — 生产基线

### 1.1 非 Spring Boot 部署
- 当前仅 `LocalA2aRuntimeHost`（Spring Boot）可用，`RuntimeApp` API 丢失 A2A 协议端点、Task 管理、远程编排
- 目标：提供非 Spring Boot 的 `RuntimeHost` 实现，使 `RuntimeApp.create(handler).run(host)` 完整可用
- 影响：解耦 Spring Boot 依赖，支持嵌入式、轻量化部署

### 1.2 AgentScope Checkpoint 适配
- 当前 AgentScope Adapter 无状态持久化，重启后会话丢失
- 目标：AgentScope Handler 支持 checkpoint/save/restore
- 影响：AgentScope Agent 可支持中断恢复和长时运行

### 1.3 会话持久化（MySQL 后端）

- 当前 A2A Task 生命周期由 InMemoryTaskStore 管理，重启后丢失，Agent session 仅 OpenJiuwen Checkpoint 支持持久化
- 目标：提供 MySQL 持久化会话存储，支持超时回收、上下文窗口滑动、父子消息线程
- 影响：生产级会话管理，重启不丢状态。参考：fin-code SessionManagerService（323 行完整实现，MyBatis Plus 映射，30min 超时，10 轮默认上下文窗口）

### 1.4 AgentScope 记忆适配
- 当前仅 OpenJiuwen 已接入 `MemoryProvider`，AgentScope 无记忆能力
- 目标：AgentScope Handler 接入 `MemoryProvider` SPI，支持跨会话记忆检索与保存
- 影响：AgentScope Agent 具备与 OpenJiuwen 同等的记忆能力

---

## 2. P1 — 重要增强

### 2.1 轨迹生产化

- **OpenTelemetry 导出（OTLP）**：代码已有 `OtelSpanSink`，需补 example 验证和文档
- **轨迹数据对调用方可见**：`trajectory.northbound=true` 能力代码已有，需补 example 验证
- **首 Token 延迟（TTFT）观测**：`MODEL_CALL_FIRST_TOKEN` 枚举已存在，需 Adapter 实际发射
- **采样率控制**：`TrajectorySettings` 预留了 `sampleRate`，需实现采样门控逻辑
- **LLM 推理过程（REASONING）记录**：reasoning 内容当前嵌入 `MODEL_CALL_END`，需独立事件
- **大载荷外置存储**：`payload_ref://` 机制需实现
- **自定义脱敏逻辑注入**：Redactor SPI 需定义和实现

### 2.2 OpenJiuwen Workflow 适配

- 当前仅支持 Core Agent（ReActAgent），不支持 Workflow
- 目标：`OpenJiuwenAgentRuntimeHandler` 支持 Workflow Agent 的创建和执行
- 影响：OpenJiuwen 用户可使用多步 Workflow 编排

### 2.3 Redis 分布式 Checkpoint 预置适配

- 当前 Checkpoint 仅 InMemory / SQLite，多实例无法共享状态
- 目标：预置 Redis Checkpoint 适配器，开箱即用
- 影响：支持多实例部署的会话状态共享

### 2.4 Push Notification / Webhook

- 当前 A2A SDK 的 PushNotificationConfigStore 已装配但推送未激活
- 目标：Task 完成后主动回调 Webhook，替代轮询模式
- 影响：完整异步通讯模式，减少客户端轮询开销

### 2.5 记忆增强

- **记忆与提示词协同**：当前仅在 ReAct 轮次开始前一次性注入，不支持 Agent 推理中途按需检索
- **记忆工具**：Agent 无法在对话过程中主动调用记忆读写
- **双记忆架构（STM + LTM）**：短期记忆（轮次内上下文）+ 长期记忆（跨会话持久化），参考 fin-code MemoryService + MemoryContext 的双层设计（STM 用 ConcurrentHashMap，LTM 用 MemoryManager，检索时合并）
- **记忆压缩**：LLM 驱动的 STM→LTM 摘要压缩，将旧消息自动生成摘要与新消息拼合，控制上下文窗口。参考 fin-code MemoryCompressionService（DashScope qwen-max 驱动，可配置 token 比率）
- 目标：支持中途检索 + 提供内置记忆 Tool + 双记忆架构 + 自动压缩

### 2.6 PDCA 多 Agent 编排

- 当前远程 Agent 编排仅支持单层 Tool 调用（LLM→远程 Agent→回灌），不支持结构化的多 Agent 协作流程
- 目标：支持 Plan-Do-Check-Act 多 Agent 协作周期：
  - **Plan Agent**：生成结构化 JSON 步骤计划
  - **Do Agent**：由 AgentPool 中的专业 Agent 执行每个步骤
  - **Check Agent**：审查结果（PASS/FAIL）
  - **Act Agent**：失败时生成修正计划，可配置最大循环次数
- 影响：从"单 Agent + 远程 Tool"升级为"多 Agent 协作编排"。参考：fin-code AgentProcessEngine（1302 行完整实现，SYNC/STREAMING_SIMPLE/STREAMING_AGUI 三种执行模式，JSON 多策略回退解析）

### 2.7 意图识别管道

- 当前 Agent 直接消费用户原始消息，无预处理管道
- 目标：提供可插拔的意图识别管道：NER 实体识别 → Query 改写（语义补全、术语标准化）→ 意图分类（LLM + 规则回退）→ Skill 映射
- 影响：提升 Agent 对模糊/简短用户输入的鲁棒性。参考：fin-code IntentEngine（4 阶段管道，`agent.intent.*` 配置开关独立控制每个阶段）

---

## 3. P2 — 可延后

### 3.1 MCP (Model Context Protocol) 协议接入

- A2A 的互补协议，连接 Agent → 工具生态
- 目标：新增 MCP Adapter，支持 MCP 工具的发现和调用
- 影响：Agent 可接入 MCP 生态的工具和服务

### 3.2 gRPC 传输协议

- A2A v0.3 已加入 gRPC 支持，当前仅 HTTP + SSE
- 目标：新增 gRPC 传输通道，与 HTTP/SSE 并存
- 影响：高性能场景的低延迟通信

### 3.3 Reactive 响应式接口

- OpenJiuwen Core 下个版本支持 Flux / Mono
- 目标：适配器支持 Reactive 流式接口，替代当前同步 `Runner.runAgent()` 调用
- 影响：OpenJiuwen 实现真正的流式执行和 cancel 中断

### 3.4 SDK / Client 库

- 当前调用方需自行集成 A2A Java SDK
- 目标：提供 agent-runtime 专用 Client 库，封装 Agent Card 解析、方法调用、SSE 消费
- 影响：降低调用方接入门槛

### 3.5 Skills / Capabilities 声明式定义的示例演示

- 当前无 example 演示如何声明 skills 并验证远程 Tool 注入
- 目标：新增独立 example 展示完整的 skills 声明 → 远程注入 → LLM 调用流程
- 影响：开发者可直观理解 skills 机制

### 3.6 知识检索 / RAG 集成

- 当前无内置知识库检索能力
- 目标：支持多知识库接入（每个知识库作为独立 Tool，携带名称和描述）。参考：fin-code KnowledgeRetrievalTool + BailianKnowledgeRegistry（多厂商抽象，每个知识库实例化为独立 AgentTool）
- 影响：Agent 可检索企业知识库，增强回答准确性

### 3.7 视觉 / 多模态 Agent

- 当前仅支持文本输入
- 目标：支持图片分析（base64 编码 + 多模态模型调用），产出结构化分析结果（表格/文档提取/分类）。参考：fin-code ImageAgent（qwen-vl-max 驱动，AGUI 流式事件，多图并行分析）
- 影响：Agent 可处理图片附件，扩展适用场景

### 3.8 流式协议扩展（AGUI / WebSocket）

- 当前仅支持 A2A SSE 流式推送
- 目标：支持 WebSocket 双向流式通信，产出 toolCallStart/Args/End/textMessageStart 等细粒度事件。参考：fin-code AgentProcessEngine STREAMING_AGUI 模式 + ImageAgent AGUI 事件
- 影响：支持更丰富的实时交互场景（逐步展示推理过程、工具调用进度）

---

## 迭代完成度预估

| 优先级 | 特性数 | 预估工作量 | 说明 |
|--------|--------|----------|------|
| P0 | 4 | 大 | 非 Spring Boot 部署 + 会话持久化 + AgentScope Checkpoint + AgentScope 记忆 |
| P1 | 15 | 大 | 轨迹 7 项 + Workflow + Redis + Webhook + Memory 增强（双架构+压缩） + PDCA 编排 + 意图管道 |
| P2 | 8 | 大 | MCP + gRPC + Reactive + SDK + Skills Example + RAG + 视觉 Agent + AGUI |
| **合计** | **27** | | |

### fin-code 参考来源

| fin-code 特性 | 纳入清单 | 成熟度 | 文件 |
|--------------|---------|--------|------|
| 会话持久化（MySQL） | P0 1.3 | 完整（323 行） | `SessionManagerService.java` |
| 双记忆架构（STM+LTM） | P1 2.5 | 完整（~450 行） | `MemoryService.java` + `MemoryContext.java` |
| 记忆压缩 | P1 2.5 | 完整（204 行） | `MemoryCompressionService.java` |
| PDCA 多 Agent 编排 | P1 2.6 | 完整（1302 行） | `AgentProcessEngine.java` |
| 意图识别管道 | P1 2.7 | 大部分完整（383 行） | `IntentEngine.java` |
| 知识检索 / RAG | P2 3.6 | 骨架 | `KnowledgeRetrievalTool.java` |
| 视觉 / 多模态 Agent | P2 3.7 | 大部分完整（213 行） | `ImageAgent.java` |
| AGUI 流式协议 | P2 3.8 | 部分 | `AgentProcessEngine.java` + `ImageAgent.java` |
