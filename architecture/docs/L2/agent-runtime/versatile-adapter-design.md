# Versatile Adapter 设计

> 文档归档：`architecture/docs/L2/agent-runtime/versatile-adapter-design.md`
> 目标模块：`agent-runtime/src/main/java/com/huawei/ascend/runtime/engine/versatile/`
> 最后更新：2026-06-13

---

## 1. 概述

Versatile Adapter 是 agent-runtime 引擎层的内置适配器，职责是将 A2A（Agent-to-Agent）JSON-RPC 请求转换为 REST 请求，转发到远端 Versatile 工作流服务；将远端返回的 SSE（Server-Sent Events）流式响应转换回 agent-runtime 标准的 `AgentExecutionResult`，再经由 A2A SDK 返回给调用方。

**核心理念**：Versatile Adapter 是一个**协议转换代理**——前端是 A2A JSON-RPC，后端是 Versatile REST/SSE，adapter 在这两者之间做双向转换。

### 1.1 设计原则

1. **模块闭环**：所有代码在 `engine/versatile/` 目录下，不穿越模块边界
2. **遵从 SPI 契约**：实现 `AgentRuntimeHandler` + `AgentCardProvider`，无缝接入 agent-runtime
3. **配置驱动**：远端连接参数通过 `application.yml` 注入，support YAML 预配置 + A2A 结构化 metadata 透传
4. **可扩展**：消息适配器和流适配器均可被子类化覆盖
5. **text = body 统一规则**：A2A message text 承载 body（`{"inputs":{...}}`），metadata 承载 headers/query 参数。两轮（LLM tool call 和 remote continuation）统一。

---

## 2. 模块结构

```
agent-runtime/src/main/java/com/huawei/ascend/runtime/engine/versatile/
├── VersatileAgentRuntimeHandler.java   // 主 Handler，实现 AgentRuntimeHandler + AgentCardProvider
├── VersatileMessageAdapter.java        // 输入转换：AgentExecutionContext → VersatileHttpRequest
├── VersatileStreamAdapter.java         // 输出转换：SSE 行 → Stream<AgentExecutionResult>
├── VersatileClient.java                // HTTP 客户端 (JDK HttpClient)，SSE 流式读取
├── VersatileHttpRequest.java           // REST 请求值对象 (method, url, headers, body)
└── VersatileProperties.java            // 配置属性类
```

### 2.1 类协作

```
A2aAgentExecutor
  │
  ▼
VersatileAgentRuntimeHandler.execute(context)
  │
  ├─ VersatileMessageAdapter.toRequest(context) → VersatileHttpRequest
  │     └─ URL:   模板替换 {conversation_id} + structured query params
  │     └─ Headers: 三级优先级 (YAML < flat metadata < structured versatile.headers)
  │     └─ Body:   {"inputs":{...}} — 优先从 text JSON 解析，回退到 context variables
  │
  ├─ VersatileClient.stream(request) → Stream<String> (lazy SSE lines)
  │
  └─ VersatileStreamAdapter.adapt(rawStream) → Stream<AgentExecutionResult>
        └─ 已知事件:  message → OUTPUT, workflow_finished → COMPLETED, exception → FAILED
        └─ 未知事件:  match/get extraction rules → 缓存 → End 后 COMPLETED(LLM)
                     无 extraction → passthrough OUTPUT(USER)
        └─ connection_closed 无 End → INTERRUPTED(USER)
```

---

## 3. 输入路径：A2A Request → Versatile REST

### 3.1 数据流

```
A2A Client (JSON-RPC)
  │
  │  POST /a2a
  │  { "jsonrpc": "2.0",
  │    "method": "SendStreamingMessage",
  │    "params": { "message": {
  │      "role": "ROLE_USER",
  │      "parts": [{ "text": "{\"inputs\":{\"query\":\"...\",\"intent\":\"订酒店\"}}" }],
  │      "contextId": "ctx-001",
  │      "metadata": {
  │        "userId": "test-user",
  │        "versatile": {
  │          "headers": { "x-language": "zh-cn" },
  │          "query":   { "type": "controller", "workspace_id": "10" }
  │        }
  │      }
  │    }}
  │  }
  │
  ▼
A2aAgentExecutor.toExecutionContext()
  │  构建 AgentExecutionContext:
  │    messages  = [RuntimeMessage.user("{\"inputs\":{...}}")]
  │    variables = Map.of("userId","test-user", "versatile", {headers:{},query:{...}})
  │
  ▼
VersatileAgentRuntimeHandler.execute(context)
  │
  ▼
VersatileMessageAdapter.toRequest(context)
  │  返回 VersatileHttpRequest:
  │    url:     http://host:port/v1/{project_id}/agents/{agent_id}
  │             /conversations/{conversation_id}?type=controller&workspace_id=10
  │    method:  POST
  │    headers: { content-type: application/json, stream: true,
  │               x-language: zh-cn }
  │    body:    {"inputs":{"query":"...","intent":"订酒店"}}
  │
  ▼
VersatileClient.stream(request) → Stream<String> (SSE lines)
```

### 3.2 URL 构建

URL 是一个完整模板，`{placeholder}` 在运行时替换：

| 占位符 | 来源 | 说明 |
|--------|------|------|
| `{conversation_id}` | `context.getScope().sessionId()` | 对应 A2A 的 `contextId` |
| 其他 `{...}` | `VersatileProperties.urlVariables` | YAML 配置（如 `project_id`, `agent_id`） |

配置示例：
```yaml
versatile:
  url: http://host:3001/v1/{project_id}/agents/{agent_id}/conversations/{conversation_id}
  url-variables:
    project_id: mock_project_id
    agent_id: fb723468-c8ca-424b-a95f-a3e74b37e090
  query-params:
    type: controller
    workspace_id: "10"
```

### 3.3 请求体构建（text = body 统一规则）

**核心规则**：`inputs` 始终从 A2A message text 解析，metadata 承载 headers/query 参数。

```java
// VersatileMessageAdapter.buildBody()
Map<String, Object> inputs = extractInputs(context);  // 从 text JSON 或 variables 提取
Map<String, Object> structuredInputs = structuredInputs(context); // versatile.inputs 补充
body.put("inputs", inputs);  // 最终 body = {"inputs": {...}}
```

**inputs 提取优先级**：
1. **text JSON** — 解析 `context.lastUserText()` 为 JSON，取 `inputs` 字段（主路径，两轮统一）
2. **context variables** — `vars.get("inputs")`（回退，向后兼容）

### 3.4 Header 构建（三级优先级）

| 优先级 | 来源 | 说明 |
|--------|------|------|
| 低 | `versatile.headers` (YAML) | 部署时预设的默认值 |
| 中 | A2A flat metadata (passthrough allowlist) | `metadata.{key}` 在白名单内即透传 |
| 高 | `metadata.versatile.headers` (structured) | 用户显式指定的 header，同样受白名单控制 |

### 3.5 Structured metadata（推荐方式）

用户可在 A2A metadata 中通过 `versatile` 键传递结构化参数：

```json
"metadata": {
  "versatile": {
    "headers": { "x-language": "zh-cn" },
    "query":   { "type": "controller" },
    "inputs":  { "wap_userName": "张三" }
  }
}
```

- `versatile.headers` → HTTP headers（最高优先级，白名单控制）
- `versatile.query` → URL 查询参数（覆盖 YAML `query-params`）
- `versatile.inputs` → 合并到 body `inputs`（补充 LLM 不知道的字段如 `wap_userName`）

---

## 4. 输出路径：Versatile SSE → AgentExecutionResult

### 4.1 SSE 事件映射

| SSE `event` | 条件 | `AgentExecutionResult` | Target | 说明 |
|-------------|------|------------------------|--------|------|
| `message` | `text` 或 `summary` 非空 | `OUTPUT(text)` | USER | 流式文本块，缓存到 node_type |
| `message` | `node_type=End` | 标记 hasEnd | — | 不产出结果，仅记录 End 已到达 |
| `workflow_finished` | — | `COMPLETED(cache)` | LLM | 工作流正常结束 |
| `end` | hasEnd=true | `COMPLETED(cache/extracted)` | LLM | 正常结束 |
| `end` / `connection_closed` | hasEnd=false | `INTERRUPTED("")` | USER | HTTP 流关闭但无 End → 中断 |
| `exception` | — | `FAILED(code, msg)` | BOTH | 工作流异常 |
| `workflow_started` | — | 过滤 | — | 控制事件 |
| `node_started` / `node_finished` | — | 过滤 | — | 控制事件 |
| 任意未知事件 | 命中 `match`/`get` 规则 | 缓存 → End 后 COMPLETED(LLM) | — | 结果提取 |
| 任意未知事件 | 未命中规则 | `OUTPUT(rawLine)` | USER | 透传，用户可见 |

### 4.2 结果提取（result-extractions）

用户只需配置两个直观字段：

```yaml
versatile:
  result-extractions:
    - match: hotel_book_success   # SSE 行包含此关键字即触发
      get: ticket                 # JSON 树中深度搜索此 key，返回值
```

- `match` — 匹配 SSE 行中**任意位置**的关键字（不限于 event 名）
- `get` — **深度搜索** JSON 树（含嵌套 Map/List），找到第一个匹配 key 返回其值
- 提取的值缓存，直到 End 节点后作为 `COMPLETED(target=LLM)` 返回

### 4.3 中断检测

```
Versatile REST API 返回 SSE 流
  │
  ├─ ... hotels_info events ...
  ├─ ... message events ...
  ├─ HTTP 流关闭（连接断开）
  │
  ▼
VersatileClient 注入 connection_closed 事件
  │
  ▼
VersatileStreamAdapter:
  hasEnd=false → INTERRUPTED(target=USER)
  → A2A task 进入 INPUT_REQUIRED
  → 用户第二轮输入 → remote continuation → 恢复远端任务
```

End 节点通过 `message` 事件中 `node_type=End`（大小写不敏感）判定。

### 4.4 Node-type 缓存

所有 `message` 事件按 `node_type` 缓存。End 到达时，缓存内容按插入顺序合并作为 `COMPLETED` 结果：

- 若配置 `result-node-type`：仅合并匹配的 node_type
- 若配置 `result-extractions` 且提取到数据：优先使用提取结果
- 否则：合并全部缓存

---

## 5. 配置模型（VersatileProperties）

### 5.1 完整配置

```yaml
versatile:
  # ── URL 模板 ──
  url: http://host:3001/v1/{project_id}/agents/{agent_id}/conversations/{conversation_id}
  url-variables:           # 模板占位符（{conversation_id} 自动解析为 sessionId）
    project_id: mock_project_id
    agent_id: fb723468-c8ca-424b-a95f-a3e74b37e090
  query-params:            # URL 查询参数（可被 structured versatile.query 覆盖）
    type: controller
    workspace_id: "10"

  # ── 超时 ──
  timeout: 30s

  # ── Header：YAML 预配置（低优先级）──
  headers:
    content-type: application/json
    stream: "true"

  # ── Header：A2A 透传白名单（中优先级）──
  passthrough-headers:
    - x-invoke-mode
    - x-language

  # ── 结果提取 ──
  result-node-type:        # 可选：仅合并该 node_type 的缓存
  result-extractions:      # match → get 规则列表
    - match: hotel_book_success
      get: ticket

  # ── Body 补充字段 ──
  input-metadata-keys:     # 从 A2A metadata 自动补充到 body inputs
    - intent
    - wap_userName
```

### 5.2 Java 配置类

```java
@ConfigurationProperties(prefix = "versatile")
public class VersatileProperties {
    // URL
    private String url;
    private Map<String, String> urlVariables;
    private Map<String, String> queryParams;
    private Duration timeout;

    // Headers
    private Map<String, String> headers;
    private List<String> passthroughHeaders;

    // Result extraction
    private String resultNodeType;
    private List<ResultExtraction> resultExtractions;  // match + get

    // Body
    private List<String> inputMetadataKeys;
}
```

---

## 6. 对外呈现（Agent Card）

`VersatileAgentRuntimeHandler` 实现 `AgentCardProvider`，内建 Skill 描述供 LLM 理解如何调用：

```json
{
  "name": "versatile-child",
  "description": "...",
  "capabilities": { "streaming": true },
  "skills": [{
    "id": "versatile-workflow-proxy",
    "name": "Versatile workflow proxy",
    "description": "Call this tool to invoke a remote workflow via the Versatile engine. Pass a JSON object whose keys and values are the business parameters required by the target workflow. Every entry becomes an input field of the workflow request.",
    "tags": ["versatile", "sse", "streaming", "workflow", "interruption"]
  }],
  "supportedInterfaces": [{ "transport": "jsonrpc", "url": "/a2a" }]
}
```

外部 A2A 客户端通过 `GET /.well-known/agent-card.json` 获取卡信息，然后通过标准 JSON-RPC `POST /a2a` 调用。

---

## 7. 用户可见示例

### 7.1 第一轮：LLM 调用 Versatile 子 Agent

A2A Client → Main Agent (LLM) → Remote Tool Call → Versatile Child Agent:

```
用户: "帮我预订北京的酒店，3月30日入住，4月3日退房"
  ↓
LLM 按 skill 生成 tool call: {"inputs":{"query":"{...}","intent":"订酒店"}}
  ↓
A2A 层转发到 Child → VersatileMessageAdapter 解析 text JSON → REST body
  ↓
Versatile REST: POST .../conversations/{id}?type=controller&workspace_id=10
  Body: {"inputs":{"query":"{...}","intent":"订酒店"}}
  ↓
SSE 响应:
  data:{"event":"hotels_info","data":{"hotels":[...]}}
  → HTTP 流关闭（无 End）
  ↓
Child: INTERRUPTED → Parent: INPUT_REQUIRED
```

### 7.2 第二轮：Remote Continuation 直接路由到 Child

```
用户: {"inputs":{"query":"{\"hotel_name\":\"希尔顿花园酒店\"}","intent":"LATEST"}}
  ↓
Parent 识别 remote continuation → 直接路由到 Child（不经 LLM）
  ↓
Child 解析 text JSON → REST body
  ↓
Versatile REST: POST .../conversations/{same_id}?...
  Body: {"inputs":{"query":"{\"hotel_name\":\"...\"}","intent":"LATEST"}}
  ↓
SSE 响应:
  data:{"event":"hotel_book_success","data":{"ticket":{...}}}
  data:{"event":"message","data":{"node_type":"End","is_finished":true}}
  data:{"event":"end","data":{}}
  ↓
Child: extraction match='hotel_book_success' get='ticket' → COMPLETED(ticket, LLM)
  ↓
Parent: re-enter handler with tool result → LLM summarizes → COMPLETED
```

### 7.3 等价的直接 Versatile REST 调用

```bash
# Round 1 — query hotels
curl -X POST "http://host:3001/v1/{project_id}/agents/{agent_id}/conversations/{id}?type=controller&workspace_id=10" \
  -H "Content-Type: application/json" -H "stream: true" \
  -d '{"inputs":{"query":"{\"person_name\":\"李四\",\"checkin_date\":\"2026-03-30\",\"arrival_city\":\"北京\"}","intent":"订酒店"}}'

# Round 2 — confirm booking (same conversation_id)
curl -X POST "http://host:3001/v1/{project_id}/agents/{agent_id}/conversations/{same_id}?type=controller&workspace_id=10" \
  -H "Content-Type: application/json" -H "stream: true" \
  -d '{"inputs":{"query":"{\"hotel_name\":\"希尔顿花园酒店\"}","intent":"LATEST","wap_userName":"张三"}}'
```

---

## 8. 错误处理

| 错误场景 | 行为 | A2A 结果 |
|----------|------|----------|
| HTTP 连接超时 | `VersatileClient` 捕获 `HttpTimeoutException` → `FAILED("VERSATILE_TIMEOUT", msg)` | FAILED |
| HTTP 4xx/5xx | `VersatileClient` 读取 error body → `FAILED("VERSATILE_HTTP_{code}", body)` | FAILED |
| SSE 行 JSON 解析失败 | 跳过该行，记录 WARN 日志 | 该行丢弃 |
| 流中收到 `exception` 事件 | `FAILED(code, message)` | FAILED |
| 流关闭无 End 节点 | `INTERRUPTED("", USER)` | INPUT_REQUIRED |
| `result-extractions` 匹配但路径不存在 | 跳过提取，回退到 cache | 正常 COMPLETED |

---

## 9. 文件清单

| 文件 | 说明 |
|------|------|
| `VersatileProperties.java` | `@ConfigurationProperties`，URL 模板、headers、result-extractions |
| `VersatileClient.java` | JDK `HttpClient`，SSE 流式读取，返回 `Stream<String>` |
| `VersatileHttpRequest.java` | 请求值对象（method, url, headers, body） |
| `VersatileMessageAdapter.java` | 输入转换：text JSON → body inputs，三级 header，URL 模板 |
| `VersatileStreamAdapter.java` | 输出转换：SSE → `AgentExecutionResult`，match/get 提取，中断检测 |
| `VersatileAgentRuntimeHandler.java` | 实现 `AgentRuntimeHandler` + `AgentCardProvider` |

---

## 10. 自动工具注入机制

Versatile 子 Agent 被主 Agent 发现后，会自动注册为 OpenJiuwen LLM 可调用的工具。

### 10.1 链路概览

```
Versatile 子 Agent 启动
  │
  ├─ VersatileAgentRuntimeHandler.agentCard()
  │     └─ 构建 AgentCard:
  │          name: "versatile-child"
  │          capabilities: { streaming: true }
  │          skills: [{
  │            id: "versatile-workflow-proxy",
  │            name: "Versatile workflow proxy",
  │            description: "Call this tool to invoke a remote workflow via the
  │              Versatile engine. Pass a JSON object whose keys and values are
  │              the business parameters required by the target workflow. Every
  │              entry becomes an input field of the workflow request.
  │              Example: {\"field_name_1\": \"value_1\", ...}"
  │          }]
  │
  ▼
A2A SDK 暴露 AgentCard 在 /.well-known/agent-card.json
  │
  ▼
主 Agent 启动
  │
  ├─ 配置: agent-runtime.remote-agents[0].url=http://localhost:18082
  │
  ├─ RemoteAgentCardCache 周期性拉取子 Agent card
  │     │
  │     ├─ AgentCard.skills[0].description → 工具描述
  │     ├─ RemoteAgentToolSpec:
  │     │     toolName = "versatile-child" (即 card.name)
  │     │     remoteAgentId = "versatile-child"
  │     │     输入 schema = 开放的 JSON 对象（任意 key-value）
  │     │     描述 = skill.description 原文
  │     │
  │     └─ LOG: "remote agent tool description assembled name=versatile-child
  │              skillsCount=1"
  │
  ├─ OpenJiuwenRemoteToolInstaller.install(agent, context)
  │     │
  │     ├─ 注册到 agent.getAbilityManager()
  │     │     工具 id/name: "versatile-child"
  │     │     工具描述: skill.description
  │     │     工具 schema: {type: object, properties: {}} （开放 schema）
  │     │
  │     └─ LOG: "installed 1 remote A2A tool(s) into openjiuwen agent=main-parent
  │              tool name=versatile-child remoteAgentId=versatile-child"
  │
  └─ OpenJiuwenRemoteAgentInterruptRail 注册
        │  拦截 toolName ∈ {versatile-child} 的调用
        │  创建 InterruptRequest → AgentExecutionResult.interrupted(remoteInvocation)
        │  A2A 层调用子 Agent
        └─ 子 Agent 返回后 resume LLM，注入 toolResult
```

### 10.2 AgentCard 的 skills 字段

`VersatileAgentRuntimeHandler.agentCard()` 内建了一个 Skill：

```java
private static final String SKILL_ID = "versatile-workflow-proxy";
private static final String SKILL_NAME = "Versatile workflow proxy";
private static final String SKILL_DESCRIPTION = """
        Call this tool to invoke a remote workflow via the Versatile engine.
        Pass a JSON object whose keys and values are the business parameters
        required by the target workflow. Every entry becomes an input field
        of the workflow request.

        Example:
        {"field_name_1": "value_1", "field_name_2": "value_2", ...}
        """;
```

这个 Skill 描述直接成为 LLM 看到的工具描述。LLM 根据此描述决定如何填充 JSON 参数。

### 10.3 工具生成规则

| 输入 | 来源 | 说明 |
|------|------|------|
| `toolName` | `AgentCard.name` | 作为 LLM function name（如 `versatile-child`） |
| `remoteAgentId` | `AgentCard.name`（去重后缀） | 路由到哪个 A2A 邻居 |
| `描述` | `AgentCard.skills[0].description` | LLM 看到的 function description |
| `输入 schema` | `{}
========
{}
;`
 | 开放 schema，接受任意 JSON 对象 |

**触发条件**：只有 `AgentCard.skills` 非空的远端 Agent 才会被注册为工具。没有 skills 的 Agent 无法被 LLM 调用。

---

## 11. 最佳实践：为主 Agent 添加 Skill 引导 LLM 填充参数

以 `examples/agent-runtime-a2a-versatile-parent-e2e` 为例。

### 11.1 架构

```
 skills/hotel-booking/SKILL.md        ← 业务 skill 文档（给 LLM 读）
 MainAgentConfiguration.java          ← 主 Agent 配置
   ├─ SYSTEM_PROMPT                   ← 告诉 LLM 调用工具 + 按 skill 填参数
   ├─ addSysOpTool(readFile)          ← 让 LLM 能读 SKILL.md
   ├─ agent.registerSkill("skills")   ← 注册 skill 目录
   └─ SkillUtil.getSkillPrompt()      ← 自动注入 skill 摘要到 system prompt
```

### 11.2 第一步：编写业务 SKILL.md

位置：`skills/hotel-booking/SKILL.md`

```markdown
---
name: versatile-request
description: 调用 Versatile 工作流工具时，按目标 REST API 的 inputs 结构直接输出完整 JSON。
---

# Versatile 请求体组装技能

调用 Versatile 工作流工具时，直接传入完整的请求体 JSON：

` ` `json
{"inputs":{"query":"...","intent":"..."}}
` ` `

## inputs 层字段

| 字段 | 说明 | 示例 |
|------|------|------|
| `query` | **JSON 字符串**，业务参数嵌套在 JSON 字符串内部 | `"{\"person_name\":\"李四\",...}"` |
| `intent` | 操作意图 | `"订酒店"` / `"LATEST"` |

### `query` 内可包含的字段

| 字段 | 说明 | 示例值 |
|------|------|--------|
| `person_name` | 入住人姓名 | `"李四"` |
| `checkin_date` | 入住日期 | `"2026-03-30"` |
| `checkout_date` | 退房日期 | `"2026-04-03"` |
| `arrival_city` | 目的地城市 | `"北京"` |
| `hotel_name` | 用户选择的酒店名称 | `"美居宾馆"` |

## 调用示例

用户消息："请帮我订一家北京的酒店，3月30日入住，4月3日退房，我叫李四"

` ` `json
{"inputs":{"query":"{\"person_name\":\"李四\",\"checkin_date\":\"2026-03-30\",\"checkout_date\":\"2026-04-03\",\"arrival_city\":\"北京\"}","intent":"订酒店"}}
` ` `
```

**关键设计点**：
- `query` 字段必须是 **JSON 字符串**（而非普通文本），因为 Versatile REST API 的 `inputs.query` 接受 JSON 字符串
- `intent` 区分首轮（`"订酒店"` / `"NEW"`）和续轮（`"LATEST"`）
- 明确列出所有可用业务字段，LLM 按此提取

### 11.3 第二步：配置 System Prompt

```java
private static final String SYSTEM_PROMPT = """
        You are a helpful assistant. When the user asks you to perform a task
        that requires external workflow execution, extract the relevant business
        parameters from the user's message and call the available tool with a
        JSON object containing those parameters.

        Follow the hotel-booking skill for hotel reservation requests.

        After receiving a tool result, summarize it briefly for the user.
        """;
```

System Prompt 的职责：
1. 告诉 LLM **何时调用工具**（需要外部工作流执行时）
2. 告诉 LLM **按哪个 skill 操作**（Follow the hotel-booking skill）
3. 告诉 LLM **收到结果后做什么**（summarize briefly）

### 11.4 第三步：注册 Skill 目录

```java
// 注册 local SysOperation → 提供 readFile 工具
SysOperationCard sysOpCard = SysOperationCard.builder()
        .id(AGENT_ID).mode(OperationMode.LOCAL)
        .workConfig(LocalWorkConfig.builder().workDir(null).build())
        .build();
Runner.resourceMgr().addSysOperation(sysOpCard, null);

// 注入 readFile 工具（让 LLM 能读 SKILL.md）
addSysOpTool(agent, sysOpCard.getId(), "fs", "readFile");

// 注册 skill 目录
Path skillsDir = Path.of("skills");
agent.registerSkill(skillsDir.toString());
```

`SkillUtil.getSkillPrompt()` 自动生成 skill 摘要并注入到 LLM 的 system prompt：
```
To help you better complete tasks, the following skill knowledge is equipped:
0. Skill name: hotel-booking; Skill description: 调用 Versatile 工作流工具时...;
   Skill directory file path: skills/hotel-booking
You can use the readFile tool to read the corresponding SKILL.md file...
```

### 11.5 完整的 LLM 调用流程

```
1. System Prompt 告诉 LLM：有 skill hotel-booking，用 readFile 读完再操作

2. LLM 第一步：调用 readFile("skills/hotel-booking/SKILL.md")
   → 获取完整的 SKILL.md 内容

3. LLM 第二步：按 SKILL.md 指导，从用户消息提取参数，生成 tool call:
   versatile-child({
     "inputs": {
       "query": "{\"person_name\":\"李四\",\"checkin_date\":\"2026-03-30\",\"checkout_date\":\"2026-04-03\",\"arrival_city\":\"北京\"}",
       "intent": "订酒店"
     }
   })

4. A2A 层拦截 tool call → 调用 Versatile 子 Agent

5. 子 Agent 返回后，tool result 注入 LLM 上下文 → LLM 生成自然语言回答
```

### 11.6 开发 Checklist

| # | 文件 | 职责 |
|---|------|------|
| 1 | `skills/<name>/SKILL.md` | 业务参数模板 + 调用示例（LLM 按此填充 JSON） |
| 2 | System Prompt | 告诉 LLM 调用工具的时机、skill 名称、收到结果后的行为 |
| 3 | `addSysOpTool(readFile)` | 让 LLM 能读取 SKILL.md |
| 4 | `agent.registerSkill("skills")` | 注册 skill 目录，自动注入 skill 摘要到 prompt |
| 5 | `agent-runtime.remote-agents[0].url` | 指向子 Agent，自动发现 + 注入远程工具 |
| 6 | `versatile.result-extractions` | 定义业务事件 → LLM 结果的提取规则（可选） |
