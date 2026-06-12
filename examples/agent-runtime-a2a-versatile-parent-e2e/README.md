# agent-runtime-a2a-versatile-parent-e2e 手工验证指南

## 概述

本示例演示**主 Agent（OpenJiuwen LLM）调用子 Agent（Versatile）**的 A2A 父-子代理场景，
包含结构化 metadata 透传、中断检测、输出缓存、target 路由等新能力。

## 架构

```
用户 → 主 Agent (OpenJiuwen ReAct LLM, profile=main, port=18080)
        │
        ├─ LLM 通过 a2a_remote_versatile_child 工具调用子 Agent
        │
        └─→ 子 Agent (Versatile REST SSE proxy, profile=versatile, port=18082)
              │
              ├─ 从 metadata.versatile 还原真实 Versatile REST 请求
              ├─ 中间 OUTPUT(target=USER) → 透传 artifact 给用户
              ├─ HTTP 断开, 无 End 节点 → INTERRUPTED → INPUT_REQUIRED
              └─ 收到 End 节点 → COMPLETED(target=LLM) → 注入 LLM 继续 ReAct
```

## 前置条件

1. JDK 21+
2. Maven 3.9+
3. 在项目根目录 (`spring-ai-ascend/`) 下执行所有命令
4. （场景 B 必需）LLM API Key —— 见下方说明

---

## 验证步骤

### 步骤 1：编译并安装 agent-runtime

```bash
mvn install -pl agent-runtime -am -DskipTests -q
```

预期输出：`BUILD SUCCESS`

---

### 步骤 2：运行 agent-runtime 全部单元测试（193 个）

```bash
mvn test -pl agent-runtime
```

预期输出：
```
Tests run: 193, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

### 步骤 3：运行新 example 模块测试

```bash
mvn test -f examples/agent-runtime-a2a-versatile-parent-e2e/pom.xml
```

#### 场景 A：无 LLM API Key（3 个跳过 + 1 个通过）

```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 3
BUILD SUCCESS
```

- `agentCardIsDiscoverable` ✅ — 验证 Versatile 子 Agent 的 A2A AgentCard 可发现
- 其余 3 个 LLM 测试 ⏭️ 跳过

#### 场景 B：有 LLM API Key（4 个全部通过）

需要先配置 LLM API Key，测试才能走真实的 OpenJiuwen ReAct LLM 调用远程子 Agent 的完整链路。

**配置环境变量：**

```bash
# 必须设置 —— LLM API Key（不设则 LLM 测试跳过）
export SAA_SAMPLE_LLM_API_KEY=sk-your-api-key

# 可选 —— LLM 提供商、代理地址、模型名（以下为默认值）
export SAA_SAMPLE_OPENJIUWEN_MODEL_PROVIDER=openai
export SAA_SAMPLE_OPENJIUWEN_API_BASE=http://localhost:4000/v1
export SAA_SAMPLE_LLM_MODEL=gpt-5.4-mini

# 可选 —— SSL 验证开关（默认为 true，自建代理通常需要关掉）
export SAA_SAMPLE_OPENJIUWEN_SSL_VERIFY=false
```

> **环境变量与配置文件的对应关系：**
>
> 这些变量在 `application-main.yaml` 中被引用：
> ```yaml
> sample.versatile-parent.llm.model-provider: ${SAA_SAMPLE_OPENJIUWEN_MODEL_PROVIDER:openai}
> sample.versatile-parent.llm.api-key:      ${SAA_SAMPLE_LLM_API_KEY:}
> sample.versatile-parent.llm.api-base:     ${SAA_SAMPLE_OPENJIUWEN_API_BASE:}
> sample.versatile-parent.llm.model-name:   ${SAA_SAMPLE_LLM_MODEL:deepseek-chat}
> sample.versatile-parent.llm.ssl-verify:   ${SAA_SAMPLE_OPENJIUWEN_SSL_VERIFY:true}
> ```
> `SAA_SAMPLE_LLM_API_KEY` 为空时，`assumeTrue` 会跳过所有 LLM 测试。

---

### 步骤 4：手工启动双进程验证（核心验证）

两个 profile 已预设固定端口。

#### 终端 1 — 启动 Versatile 子 Agent（端口 18082）

```bash
cd examples/agent-runtime-a2a-versatile-parent-e2e
mvn spring-boot:run -Dspring-boot.run.profiles=versatile
```

#### 终端 2 — 启动主 Agent（OpenJiuwen LLM，端口 18080）

```bash
cd examples/agent-runtime-a2a-versatile-parent-e2e
mvn spring-boot:run -Dspring-boot.run.profiles=main
```

---

### 步骤 5：发送订酒店请求（真实 Versatile 场景）

**场景**：用户订北京酒店，3/30 入住、4/3 退房，用户名叫李四。
主 Agent 的 LLM 将自然语言请求转换为结构化 JSON，调用 Versatile 子 Agent，
子 Agent 从 `metadata.versatile` 还原出真实 Versatile REST 请求。

对应的**真实 Versatile REST 调用**（作为对照）：
```bash
curl -X POST "http://7.213.200.213:3001/v1/mock_project_id/agents/fb723468-c8ca-424b-a95f-a3e74b37e090/conversations/{id}?type=controller&workspace_id=10" \
  -H "Content-Type: application/json" -H "stream: true" \
  -d '{"inputs":{"query":"{\"person_name\":\"李四\",\"checkin_date\":\"2026-03-30\",\"checkout_date\":\"2026-04-03\",\"arrival_city\":\"北京\"}","intent":"订酒店","wap_userName":"张三"}}'
```

#### 5.1 验证子 Agent Card（含 skills）

```bash
curl -s http://localhost:18082/.well-known/agent-card.json | python3 -m json.tool
```

Skills 内置在 `VersatileAgentRuntimeHandler` 中，所有 Versatile 示例通用：
```json
"skills": [{
    "id": "versatile-workflow-proxy",
    "name": "Versatile workflow proxy",
    "description": "Proxies A2A requests to a remote versatile REST API with SSE streaming. To invoke this agent, pass a JSON object...",
    "tags": ["versatile", "sse", "streaming", "workflow", "interruption"]
}]
```

#### 5.2 验证主 Agent Card

```bash
curl -s http://localhost:18080/.well-known/agent-card.json | python3 -m json.tool
```

#### 5.3 发送 A2A 请求（结构化 metadata）

`metadata.versatile` 分为 `headers` / `query` / `inputs` 三层，
与目标 Versatile REST 调用的结构一一对应：

```bash
SESSION_ID="ctx-hotel-$(date +%s)"

curl -s -X POST http://localhost:18080/a2a \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "method": "SendStreamingMessage",
    "id": "1",
    "params": {
      "message": {
        "role": "ROLE_USER",
        "messageId": "msg-001",
        "contextId": "'"$SESSION_ID"'",
        "metadata": {
          "userId": "test-user",
          "agentId": "main-parent",
          "sessionId": "'"$SESSION_ID"'",
          "versatile": {
            "headers": {},
            "query": {
              "type": "controller",
              "workspace_id": "10"
            },
            "inputs": {
              "intent": "订酒店",
              "wap_userName": "张三",
              "person_name": "李四",
              "checkin_date": "2026-03-30",
              "checkout_date": "2026-04-03",
              "arrival_city": "北京"
            }
          }
        },
        "parts": [{"text": "请帮我预订一家北京的酒店，3月30日入住，4月3日退房，我叫李四。"}]
      }
    }
  }' --no-buffer
```

**子 Agent 最终发出的 Versatile REST 请求：**
```
POST /v1/{project_id}/agents/{agent_id}/conversations/{id}?type=controller&workspace_id=10
Content-Type: application/json
stream: true

{"inputs":{
  "query":"请帮我预订一家北京的酒店...",
  "intent":"订酒店",
  "wap_userName":"张三",
  "person_name":"李四",
  "checkin_date":"2026-03-30",
  "checkout_date":"2026-04-03",
  "arrival_city":"北京"
}}
```

**透传链路：**
```
metadata.versatile.headers  → HTTP headers（受 passthroughHeaders allowlist 约束）
metadata.versatile.query    → URL query params（覆盖 config query-params）
metadata.versatile.inputs   → body.inputs（全部字段透传）
message.text                → body.inputs.query
```
未使用 `versatile` 键时自动回退到平铺 metadata 模式（向后兼容）。

#### 5.4 观察 SSE 流事件

1. **`TASK_STATE_SUBMITTED`** → **`TASK_STATE_WORKING`**
2. **`TaskArtifactUpdateEvent`** — 子 Agent 中间输出（target=USER，透传给用户）
3. **最终状态**：
   - 正常完成 → **`TASK_STATE_COMPLETED`**（收到 End 节点，缓存归纳为最终结果，target=LLM 注入父 LLM）
   - 中断 → **`TASK_STATE_INPUT_REQUIRED`**（断开未收到 End，需用相同 taskId 恢复）

#### 5.5 中断恢复（如果 5.4 返回 INPUT_REQUIRED）

用相同 `taskId` + `contextId` 恢复——runtime 识别为 remote continuation，**跳过 LLM 直接路由到子 Agent**：

```bash
SESSION_ID="ctx-hotel-..."   # 与 5.3 相同
TASK_ID="<5.4 SSE 事件中的 taskId>"

curl -s -X POST http://localhost:18080/a2a \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "method": "SendStreamingMessage",
    "id": "2",
    "params": {
      "message": {
        "role": "ROLE_USER",
        "messageId": "msg-002",
        "contextId": "'"$SESSION_ID"'",
        "taskId": "'"$TASK_ID"'",
        "metadata": {
          "userId": "test-user",
          "agentId": "main-parent",
          "sessionId": "'"$SESSION_ID"'"
        },
        "parts": [{"text": "继续执行"}]
      }
    }
  }' --no-buffer
```

---

### 步骤 6：确认远程工具注册成功（关键检查点）

在终端 2（主 Agent）的日志中依次检查：

**6.1 远程 Card 缓存初始化：**
```
remote agent card cache initialized configuredUrls=1 uniqueUrls=1
```

**6.2 远程 Card 解析成功：**
```
remote agent card resolved url=http://localhost:18082 agentId=versatile-child toolName=a2a_remote_versatile_child endpoint=http://localhost:18082/a2a
```

**6.3 远程工具已安装到 OpenJiuwen Handler：**
```
installed remote tool installer into openjiuwen handler agentId=main-parent
```

**6.4 Card 刷新完成：**
```
remote agent card refresh complete succeeded=1 failed=0 availableTools=1
```

**6.5 LLM 实际调用工具：**
```
remote tool invocation start taskId=... toolName=a2a_remote_versatile_child
```

**6.6 Versatile 结构化 metadata 应用：**
```
versatile structured headers applied: [...]
versatile structured inputs applied: [...]
versatile url with structured query params=[...]
```

---

## 关键验证点汇总

| # | 验证点 | 如何验证 |
|---|--------|---------|
| 1 | **Skills 通用** | 子 Agent Card 的 skills 由 runtime 内置，不随示例变更 |
| 2 | **结构化 metadata** | `versatile.headers/query/inputs` 映射到 REST 请求的对应位置 |
| 3 | **Target 路由** | 中间 OUTPUT 透传用户，最终 COMPLETED 注入 LLM |
| 4 | **node_type=End 检测** | Versatile 收到 End 节点后才发 COMPLETED |
| 5 | **中断检测** | HTTP 断开无 End → `INPUT_REQUIRED` |
| 6 | **中断恢复路由** | 同 taskId 恢复 → 直接到子 Agent，跳过 LLM |
| 7 | **Remote Agent 配置** | `output.default-target=USER` + `completion-target=LLM` 生效 |

---

## 常见问题

### Q: 启动报 "port already in use"
```bash
# 杀掉占用端口的进程，或临时覆盖端口
mvn spring-boot:run -Dspring-boot.run.profiles=versatile \
    -Dspring-boot.run.arguments="--server.port=18092"
```

### Q: 主 Agent 找不到子 Agent
```bash
curl -s http://localhost:18082/.well-known/agent-card.json | python3 -m json.tool | grep -E '"name"|"skills"'
```

### Q: LLM 调用子 Agent 工具但没响应
确认 LLM API Key 和代理地址正确：
```bash
curl -s http://localhost:4000/v1/models
```
