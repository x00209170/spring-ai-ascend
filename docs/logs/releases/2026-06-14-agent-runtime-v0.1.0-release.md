---
formal_release: true
release_candidate_branch: release/v0.1.0
status: formal-release-candidate
---

# agent-runtime v0.1.0 Release Notes

> Release date: 2026-06-14
> Version: v0.1.0
> Artifact: `agent-runtime-0.1.0.jar`

---

## 一、v0.1.0 发布特性

本次发布为 agent-runtime 首个功能版本。agent-runtime 是框架中立的 Agent 托管运行时 SDK，
提供统一的 `AgentRuntimeHandler` SPI 接入异构 Agent 框架，通过 Google A2A 协议对外暴露
Agent 端点，记录执行轨迹，并支持远程 A2A Agent 编排调用。

特性清单详见：`architecture/docs/L1/agent-runtime/features/agent-runtime-release-features.cn.md`

### 1. 异构 Agent 框架兼容

通过统一的 Adapter 抽象接入不同类型的 Agent 实现，上层调用方无需感知底层 Agent 框架差异。

**1.1 OpenJiuwen 适配器**

- 进程内直接调用 `openjiuwen-agent-core-java`，低延迟同进程执行
- Rails 注入机制，支持三种扩展点：
  - 轨迹追踪 Rail — 自动捕获模型调用/工具调用的全链路事件
  - 远程工具中断 Rail — 将远程 A2A Agent 调用拦截为可恢复的中断点
  - 记忆注入 Rail — 在每次调用前后自动检索和保存长期记忆
- Agent 执行状态持久化（Checkpoint），支持 InMemory / SQLite / 自定义后端
- 当前限制：仅支持 Core Agent（ReActAgent），不支持 Workflow；cancel 仅阻止结果消费，不中断 LLM 调用

**1.2 AgentScope 适配器**

- 进程内直接调用 AgentScope Agent
- 三种运行模式：本地 Agent、Harness Agent、远程 SSE 客户端
- AgentScope 错误码到标准 ErrorCategory 的自动映射
- 当前限制：不支持 Checkpoint、不支持 Memory、仅支持 Core Agent

**1.3 Versatile REST 代理适配器**

- 协议转换代理：前端 A2A JSON-RPC，后端 Versatile REST/SSE
- URL 模板机制 + 两级 Header 透传 + 结果提取规则引擎（match → deep-find key）
- 中断检测与自动工具注入

**1.4 Adapter 抽象层**

- 统一的 `AgentRuntimeHandler` SPI：`execute()`、`cancel()`、`resultAdapter()`
- `AgentExecutionResult`：OUTPUT / COMPLETED / FAILED / INTERRUPTED 四种统一结果
- `AbstractAgentRuntimeHandler` 基类：内置轨迹生命周期管理

### 2. 中间件解耦 — Memory & State

**2.1 Memory 服务**

- 框架无关的 `MemoryProvider` SPI（search / save），按 userId / sessionId 隔离
- 预置 OpenJiuwen 记忆集成：调用前自动检索 + 调用后自动写回，可替换为自定义实现
- 当前限制：仅在 ReAct 轮次开始前一次性注入，不支持中途检索

**2.2 State 持久化**

- OpenJiuwen Checkpoint：InMemory / SQLite，通过 `CheckpointerFactory` 全局配置
- 当前限制：仅 OpenJiuwen 支持，无 Redis 分布式预置适配

### 3. S2C 通讯模型 + A2A 协议标准

**3.1 三种通讯模式**

- 阻塞请求-响应：`SendMessage` — A2A 层收集 Handler Stream 后一次性返回 JSON
- 流式：`SendStreamingMessage` — SSE 推送，终端状态正确关闭，`SubscribeToTask` 断线重连
- 异步：`GetTask` / `CancelTask` / `ListTasks`，完整 Task 生命周期（SUBMITTED → WORKING → terminal）

**3.2 A2A Methods 全覆盖**

- `SendMessage` / `SendStreamingMessage` / `GetTask` / `CancelTask` / `ListTasks` / `SubscribeToTask`
- Agent Card 端点：`/.well-known/agent-card.json` + `/.well-known/agent.json`
- 单一 `POST /a2a` 入口，JSON-RPC `method` 字段驱动分发

**3.3 Agent Card YAML 配置**

- YAML 驱动的 AgentCard 自动生成，支持 skills / capabilities 声明
- `AgentCardProvider` SPI 编程覆盖；配置优先级：Bean > Provider > YAML > auto

### 4. 轨迹可观测性

- 框架中立事件模型：8 种 Kind 类型，Adapter 自动记录，runtime 统一 stamping
- 已覆盖事件：RUN / MODEL_CALL / TOOL_CALL / ERROR / PROGRESS（按 Adapter 不同）
- 敏感信息掩码：可配置正则 + 截断阈值，同时应用于轨迹事件和日志
- 父-子 Agent 链路追踪：parentTaskId / parentTraceId 传递

### 5. 远程 Agent 编排（A2A 南向/出站）

- YAML 配置远程端点（`agent-runtime.remote-agents`），自动拉取 Card + 缓存 + 自适应刷新（10s/600s/指数退避）
- Card Skills → RemoteAgentToolSpec → 自动注入为本地 Agent Tool
- 中断-续接流水线：远程 INPUT_REQUIRED → 父 Task 挂起 → 用户输入 → 续写 → 结果回灌
- 远程进度投射 + metadata 转发 + 取消级联传播
- 关键约束：仅 skills 非空的 Card 才被注入；不支持嵌套远程调用

### 6. 运维就绪

- 生命周期管理：start → serve → stop → drain，优雅停机，就绪门控
- 健康检查：Handler 独立健康状态 + 远程 Agent 目录状态
- MDC 日志关联（contextId/taskId/tenantId/agentId）+ RuntimeErrorCode 错误分类
- `RuntimeApp.create(handler).run(host)` 嵌入式部署 API

### 7. 已知限制

- 无 MCP 协议支持
- 无 gRPC 传输（仅 HTTP + SSE）
- AgentScope 不支持 Checkpoint 和 Memory
- OpenJiuwen cancel 仅阻止结果消费，不中断 LLM 调用
- 仅 Spring Boot 部署路径可用
- OpenJiuwen 仅支持 Core Agent，不支持 Workflow

---

## 二、下一迭代计划（v0.2.0 候选）

详细规划见：`architecture/docs/L1/agent-runtime/features/agent-runtime-next-iteration-features.cn.md`

### agent-runtime 能力补齐

- OpenJiuwen Workflow 适配：支持多步骤 Workflow Agent 的创建和执行
- MCP (Model Context Protocol) 协议接入：新增 MCP Adapter，连接 Agent 到外部工具生态
- 完善日志轨迹记录：输出结构化轨迹日志，提供生产环境下的轨迹收集、存储和查询最佳实践
- 支持自研记忆服务：提供记忆服务的标准接入方式，支持短期会话记忆和长期记忆检索，按用户和会话隔离

### agent-sdk — YAML 配置驱动 Agent 生成

开发者通过 YAML 配置文件声明 Agent 的模型连接、系统提示词、工具和技能，SDK 自动构建可运行的 Agent 实例。

- 模型配置：YAML 中声明 LLM 连接信息（provider、apiKey、baseUrl、modelName）
- 提示词配置：YAML 中声明系统提示词，支持文件引用和环境变量注入
- 工具配置：YAML 中声明 Agent 可用工具，支持 HTTP 接口工具和本地 Java 方法工具
- 技能配置：YAML 中声明 Agent 技能目录，自动加载技能描述文件
- 与 runtime 集成：YAML 定义的 Agent 自动注册为 runtime 的 Handler，通过 A2A 端点对外暴露
- 启动校验：启动时校验 YAML 文件的 schema 正确性和工具可达性

### agent-service — 开箱即用的 Agent 平台服务

结合 runtime 的 A2A 协议能力和 SDK 的声明式 Agent 生成能力，提供可直接部署的 Agent 平台服务。

- 一键部署：一个 Spring Boot 应用启动即用，自动集成 A2A 协议端点和 Agent 管理
- YAML 驱动：通过 YAML 配置文件声明 Agent，无需编写 Java 代码
- Agent 管理 API：提供 Agent 列表、状态查询、启停控制的管理接口

---

## 三、致谢

感谢以下贡献者在本版本中的代码、示例和文档贡献：

**agent-runtime 模块**：Kevin-708090、Chao Xing、chaosxingxc-orion、yougq、x00209170、Euphoria Yan、yansuqing、Suqing Yan、Kevin Hu

**Examples 模块**：yougq、x00209170、chaosxingxc-orion、yansuqing、Euphoria Yan、xuefanfan-cmd、Kevin-708090、Chao Xing、Kevin Hu、nickylba、caikongerbanhzz-ui、Suqing Yan

**文档**：Chao Xing、chaosxingxc-orion、yougq、x00209170、yansuqing、Euphoria Yan、LucioIT、Kevin-708090
