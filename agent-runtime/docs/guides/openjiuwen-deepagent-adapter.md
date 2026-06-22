# OpenJiuwen DeepAgent 适配器

在 `agent-runtime` 中托管 OpenJiuwen `DeepAgent`，并通过 A2A endpoint 对外提供服务。

## 1. 概览

OpenJiuwen DeepAgent 适配器把 OpenJiuwen harness 层的 `DeepAgent` 接入 `agent-runtime` 的统一执行模型。它与 ReAct adapter 的核心区别是：ReAct adapter 执行 `BaseAgent`，DeepAgent adapter 执行 harness wrapper 本身。

最小代码形态：

```java
public final class MyDeepAgentHandler extends OpenJiuwenDeepAgentRuntimeHandler {
    public MyDeepAgentHandler() {
        super("my-deep-agent");
    }

    @Override
    protected DeepAgent createOpenJiuwenDeepAgent(AgentExecutionContext context) {
        return HarnessFactory.createDeepAgent(card, config, workspace);
    }
}
```

适用场景：

- 需要 DeepAgent task loop、workspace、skillDirectories、MCP 或 harness tools。
- 希望通过 A2A endpoint 托管 DeepAgent。
- 希望 DeepAgent 调用其他 A2A agent 作为远端工具。
- 希望保持 ReAct adapter 的 `BaseAgent` 扩展点不变。

不适用场景：

- 只需要普通 OpenJiuwen ReActAgent：使用 `openjiuwen-adapter.md`。
- 只需要从 YAML 构造本地 DeepAgent 对象：使用 `agent-sdk-openjiuwen-yaml.md`。
- 需要显式 DAG 和人工确认恢复：使用 `openjiuwen-workflow-adapter.md`。

## 2. 快速开始

### 第一步：继承 Handler

```java
import com.huawei.ascend.runtime.engine.AgentExecutionContext;
import com.huawei.ascend.runtime.engine.openjiuwen.OpenJiuwenDeepAgentRuntimeHandler;
import com.openjiuwen.harness.deep_agent.DeepAgent;

public final class MyDeepAgentHandler extends OpenJiuwenDeepAgentRuntimeHandler {
    public MyDeepAgentHandler() {
        super("my-deep-agent");
    }

    @Override
    protected DeepAgent createOpenJiuwenDeepAgent(AgentExecutionContext context) {
        // 第二步在这里创建 DeepAgent
    }
}
```

### 第二步：实现 createOpenJiuwenDeepAgent()

```java
import com.openjiuwen.core.singleagent.schema.AgentCard;
import com.openjiuwen.harness.factory.HarnessFactory;
import com.openjiuwen.harness.schema.config.DeepAgentConfig;
import com.openjiuwen.harness.workspace.Workspace;
import java.util.Map;

@Override
protected DeepAgent createOpenJiuwenDeepAgent(AgentExecutionContext context) {
    AgentCard card = AgentCard.builder()
            .id("my-deep-agent")
            .name("my-deep-agent")
            .description("DeepAgent served by agent-runtime.")
            .build();

    DeepAgentConfig config = DeepAgentConfig.builder()
            .systemPrompt("Reply concisely.")
            .maxIterations(3)
            .enableTaskLoop(true)
            .language("en")
            .workspacePath("./target/deepagent-workspace")
            .model(Map.of("model", "gpt-5.4-mini", "temperature", 0.0, "max_tokens", 64))
            .backend(Map.of(
                    "provider", "openai",
                    "api_key", System.getenv("LLM_API_KEY"),
                    "api_base", "http://localhost:4000/v1",
                    "verify_ssl", false))
            .build();

    Workspace workspace = Workspace.builder()
            .rootPath("./target/deepagent-workspace")
            .language("en")
            .build();

    return HarnessFactory.createDeepAgent(card, config, workspace);
}
```

### 第三步：注册 Spring Bean 和 A2A Card

```java
@Bean
OpenJiuwenDeepAgentRuntimeHandler deepAgentHandler() {
    return new MyDeepAgentHandler();
}

@Bean
org.a2aproject.sdk.spec.AgentCard deepAgentCard() {
    return AgentCards.create("my-deep-agent", "DeepAgent served by agent-runtime.");
}
```

启动后，runtime 会通过 A2A endpoint 接收请求，并把请求交给 `OpenJiuwenDeepAgentRuntimeHandler`。

## 3. 工作原理

`OpenJiuwenDeepAgentRuntimeHandler` 每次执行创建一个 `DeepAgent`，并直接调用 `deepAgent.stream(input, null, List.of(StreamMode.OUTPUT))`。它不会用 `Runner.runAgentStreaming(...)` 包装 DeepAgent。

完整执行链路：

```text
A2A 请求
  -> A2aAgentExecutor
  -> AgentExecutionContext
  -> OpenJiuwenDeepAgentRuntimeHandler.execute(context)
  -> createOpenJiuwenDeepAgent(context)
  -> installRails(deepAgent, context)
  -> installDeepAgentRails(deepAgent, context)
  -> installRuntimeTools(deepAgent, context)
  -> 安装 trajectory rail
  -> OpenJiuwenMessageAdapter 转换输入
  -> 注入 conversation_id
  -> deepAgent.stream(input, null, List.of(StreamMode.OUTPUT))
  -> OpenJiuwenStreamAdapter 映射 OutputSchema
  -> AgentExecutionResult stream
  -> A2A SSE artifact/status events
```

关键设计：

- DeepAgent 实例每次请求重新创建，不跨请求缓存。
- `conversation_id` 使用 `AgentExecutionContext.getAgentStateKey()`，用于保持 OpenJiuwen 会话标识稳定。
- `openJiuwenRails(context)` 返回的 rail 安装在 `deepAgent.getAgent()` 的内部 ReAct callback bus；`openJiuwenDeepAgentRails(context)` 返回的 rail 安装在 DeepAgent harness rail 列表。
- 运行时远端 A2A、MCP、SkillHub 安装发生在 rail 初始化之后；rail 初始化期间注册的同名 tool 优先。
- trajectory rail 安装在 `deepAgent.getAgent()`，因为模型和工具 callback 来自内部 ReAct agent。
- stream 完成、关闭或异常时，runtime 会清理 `runningAgents` 中的 task 记录。

## 4. 核心接口

```java
public abstract class OpenJiuwenDeepAgentRuntimeHandler
        extends AbstractOpenJiuwenRuntimeSupport {

    protected abstract DeepAgent createOpenJiuwenDeepAgent(AgentExecutionContext context);
}
```

| 方法 | 用途 | 约束 |
|---|---|---|
| `createOpenJiuwenDeepAgent` | 构造 DeepAgent | 每次 `execute()` 调用都会执行，必须返回非 null |
| `openJiuwenRails` | 安装内部 ReAct callback rail | 用于需要 `beforeInvoke` / `beforeModelCall` / `afterInvoke` 等 ReAct callback 的 rail |
| `openJiuwenDeepAgentRails` | 安装 DeepAgent task-level rail | 用于 `TaskIterationRail` 等 DeepAgent harness rail；不注册到内部 ReAct callback bus |
| `memoryRuntimeRail` | 创建 runtime `MemoryProvider` memory rail | 运行在每个内部 ReAct round，不是 DeepAgent task-level memory |
| `openJiuwenExternalMemoryRail` | 创建 OpenJiuwen 原生 external memory rail | 从 `openJiuwenRails` 返回；不要和 `memoryRuntimeRail` 同时安装 |
| `installRuntimeTools` | 安装 runtime 远端 A2A、MCP、SkillHub 工具/skills | 基类已实现，子类通常不覆盖 |
| `cancel` | 取消正在运行的 task | 调用 `DeepAgent.requestAbort()` |
| `resultAdapter` | 映射 OpenJiuwen 输出 | 基类复用 `OpenJiuwenStreamAdapter` |

与 ReAct handler 的区别：

| 维度 | ReAct handler | DeepAgent handler |
|---|---|---|
| 扩展方法 | `createOpenJiuwenAgent(context)` | `createOpenJiuwenDeepAgent(context)` |
| 原生对象 | `BaseAgent` / `ReActAgent` | `DeepAgent` harness |
| 执行入口 | `Runner.runAgentStreaming(...)` | `deepAgent.stream(...)` |
| rail 扩展 | `openJiuwenRails(context)` | `openJiuwenRails(context)` + `openJiuwenDeepAgentRails(context)` |
| runtime tool 注册 | `Runner.resourceMgr()` + ability manager | `deepAgent.registerHarnessTool(...)` |
| 取消 | 关闭 stream，按 OpenJiuwen 行为停止 | `requestAbort()` + 关闭 stream |

## 5. 能力详述

### 远端 A2A 工具

在应用配置中加入远端 agent URL：

```yaml
agent-runtime:
  remote-agents:
    - url: http://localhost:8082
```

当远端 agent card 暴露 skills 时，runtime 会把这些 skills 安装为 DeepAgent harness tools。tool call 会被内部 ReAct agent 上的 `OpenJiuwenRemoteAgentInterruptRail` 拦截，并转发到 runtime A2A 远端调用链路。

远端工具调用链路：

```text
DeepAgent 内部 ReActAgent
  -> LLM 选择远端 tool
  -> OpenJiuwenRemoteAgentInterruptRail 拦截
  -> AgentExecutionResult.interrupted(remoteInvocation)
  -> A2A remote invocation orchestrator
  -> 远端 Agent /a2a
  -> 远端结果返回
  -> DeepAgent 继续 task loop
```

如果占位 tool 被直接执行，返回值是 `REMOTE_AGENT_TOOL_NOT_INTERRUPTED`。这表示 interrupt rail 没有拦截该 tool call。

### Runtime MCP Tools

当应用配置了 runtime MCP provider 时，`McpAutoConfiguration` 会把同一个 `OpenJiuwenMcpToolInstaller` 注入 ReAct handler 和 DeepAgent handler。DeepAgent 路径会把 provider 发现到的每个 MCP tool 包装成 OpenJiuwen `Tool`，并通过 `deepAgent.registerHarnessTool(...)` 注册到 harness。

与 ReAct 路径的差异：

| ReAct | DeepAgent |
|---|---|
| `Runner.resourceMgr().addTool(..., true)`，必要时补充 ability manager | `deepAgent.registerHarnessTool(...)`，由 DeepAgent 原生逻辑加入 resource manager 和 ability manager |

MCP tool 发现失败时 installer 只记录 warn 并跳过，不阻断 DeepAgent 执行。

### Runtime SkillHub Skills

当存在 `SkillHubProvider` 时，`OpenJiuwenSkillHubAutoConfiguration` 会把 `OpenJiuwenSkillHubInstaller` 注入 DeepAgent handler。installer 读取 skill definition metadata 中的 `openjiuwen.skill.path` 或 `openjiuwen.skill.paths`，并调用内部 ReAct agent 的 `registerSkill(...)`。

DeepAgent SkillHub 有一个前置条件：`createOpenJiuwenDeepAgent(context)` 或应用工厂必须先配置内部 ReAct agent 的 SkillUtil。若 `agent.getAgent().getSkillUtil() == null`，installer 会记录 warn 并跳过安装；installer 不会在运行时重建或重新 `configure(...)` DeepAgent。

### Runtime Rails

DeepAgent handler 暴露两个 rail 扩展点：

```java
@Override
protected List<AgentRail> openJiuwenRails(AgentExecutionContext context) {
    return List.of(/* inner ReAct callback rails */);
}

@Override
protected List<DeepAgentRail> openJiuwenDeepAgentRails(AgentExecutionContext context) {
    return List.of(/* task-loop rails */);
}
```

使用规则：

| 扩展点 | 安装位置 | 适合能力 |
|---|---|---|
| `openJiuwenRails` | `deepAgent.getAgent().registerRail(...)` | 需要内部 ReAct callback 的 rail，例如 prompt/memory/interrupt 类能力 |
| `openJiuwenDeepAgentRails` | DeepAgent `registeredRails` | `TaskIterationRail`、task-loop guardrail、task-level 观测类能力 |

如果 `openJiuwenRails` 返回的 rail 同时是 `DeepAgentRail`，handler 会先调用 `init(deepAgent)` 并加入 DeepAgent registered rails。纯 `DeepAgentRail` 不会注册到内部 ReAct callback bus；`ExternalMemoryRail` 是已知例外，它既需要 DeepAgent 初始化，也需要内部 ReAct callback。

`TaskCompletionRail` 有特殊限制：OpenJiuwen `DeepAgent` 只会从 `DeepAgentConfig.rails` 中把 completion rail 提升为私有 completion policy。只从 `openJiuwenDeepAgentRails(context)` 返回 `TaskCompletionRail` 会完成 init/registeredRails 安装，但不会成为 task completion evaluator。需要自定义 completion policy 时，应在 `createOpenJiuwenDeepAgent(context)` 构造 `DeepAgentConfig.rails`。

### Runtime MemoryProvider

DeepAgent handler 提供两个 memory helper：

| Helper | 安装方式 | 语义 |
|---|---|---|
| `memoryRuntimeRail(context, memoryProvider)` | 从 `openJiuwenRails(context)` 返回 | runtime 自实现 memory rail，在每个内部 ReAct round 搜索并注入 memory，afterInvoke 保存消息 |
| `openJiuwenExternalMemoryRail(context, memoryProvider)` | 从 `openJiuwenRails(context)` 返回 | OpenJiuwen 原生 `ExternalMemoryRail`，由 runtime neutral `MemoryProvider` 适配 |

二者都运行在内部 ReAct callback 周期，会操作 prompt 状态并保存同一轮消息。不要在同一次执行中同时安装二者。当前 helper 不是 DeepAgent task-level memory；如果记忆需要严格绑定 task planning 或 task completion 边界，需要后续新增专用 DeepAgent-level memory rail。

### 取消与轨迹

取消是协作式的。runtime 对当前运行的 `DeepAgent` 调用 `requestAbort()`；如果 OpenJiuwen 正在阻塞模型调用，会在下一次协作中止检查点停止。

trajectory 事件来自内部 ReAct agent 上的 OpenJiuwen callback：

- `MODEL_CALL_START` / `MODEL_CALL_END`
- `TOOL_CALL_START` / `TOOL_CALL_END`
- `ERROR`
- runtime `RUN_START` / `RUN_END`

### 输入与输出

输入转换由 `OpenJiuwenMessageAdapter` 负责。DeepAgent handler 要求转换后的输入是 `Map<String,Object>`。如果输入不是 map，runtime 返回 `OPENJIUWEN_RUN_ERROR`。

输出映射规则：

| OpenJiuwen raw item | runtime 输出 |
|---|---|
| `AgentExecutionResult` | 原样透传 |
| `OutputSchema` | 通过 `OpenJiuwenStreamAdapter` 转换 |
| `null` | `FAILED("OPENJIUWEN_ERROR")` |
| 其他对象 | 文本 output |

## 6. 完整示例

示例目录：

```text
examples/agent-runtime-a2a-openjiuwen-deepagent-e2e/
```

示例结构：

```text
examples/agent-runtime-a2a-openjiuwen-deepagent-e2e/
├── pom.xml
├── src/main/java/com/huawei/ascend/examples/a2a/
│   └── OpenJiuwenDeepAgentConfiguration.java
└── src/test/java/com/huawei/ascend/examples/a2a/
    └── OpenJiuwenDeepAgentA2aE2eTest.java
```

运行示例测试：

```bash
mvn -f examples/agent-runtime-a2a-openjiuwen-deepagent-e2e/pom.xml test
```

运行时请求示例：

```bash
curl -s -X POST http://localhost:8080/a2a \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "id": "r1",
    "method": "SendStreamingMessage",
    "params": {
      "message": {
        "role": "ROLE_USER",
        "messageId": "m1",
        "contextId": "c1",
        "metadata": {
          "userId": "u1",
          "agentId": "my-deep-agent",
          "sessionId": "s1"
        },
        "parts": [{"text": "请完成一个简短计划"}]
      }
    }
  }'
```

## 7. 配置参考

| 属性 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `sample.openjiuwen.model-provider` | String | `openai` | 模型 provider |
| `sample.openjiuwen.api-key` | String | `sk-local-placeholder` | 模型 API key |
| `sample.openjiuwen.api-base` | String | `http://localhost:4000/v1` | 模型 API base URL |
| `sample.openjiuwen.model-name` | String | `gpt-5.4-mini` | 模型名称 |
| `sample.openjiuwen.ssl-verify` | boolean | `false` | backend TLS 校验 |
| `sample.openjiuwen.workspace-path` | String | `./target/deepagent-workspace` | DeepAgent workspace |
| `sample.openjiuwen.checkpointer` | String | `in-memory` | OpenJiuwen 全局 checkpointer |
| `sample.openjiuwen.redis-url` | String | `redis://localhost:6379` | Redis checkpointer URL |
| `agent-runtime.remote-agents[].url` | URL | 空 | 远端 A2A agent card 地址 |
| runtime MCP provider 配置 | 视 provider 而定 | 空 | provider 发现到的 MCP tools 会安装为 DeepAgent harness tools |
| `SkillHubProvider` bean | Bean | 空 | provider 返回的 OpenJiuwen skill path 会注册到内部 ReAct skill runtime |

典型配置：

```yaml
sample:
  openjiuwen:
    model-provider: openai
    api-key: ${LLM_API_KEY}
    api-base: ${LLM_API_BASE:http://localhost:4000/v1}
    model-name: ${LLM_MODEL:gpt-5.4-mini}
    ssl-verify: false
    workspace-path: ./target/deepagent-workspace
    checkpointer: in-memory

agent-runtime:
  remote-agents:
    - url: http://localhost:8082
```

## 8. 限制与排障

| 场景 | 现象 | 处理 |
|---|---|---|
| 模型看不到远端 tool | DeepAgent 不调用远端 agent | 检查 `agent-runtime.remote-agents`、远端 Agent Card skills 和 installer 日志 |
| 出现 `REMOTE_AGENT_TOOL_NOT_INTERRUPTED` | 占位 tool 被直接执行 | 检查 interrupt rail 是否注册到 `deepAgent.getAgent()` |
| 没有模型/工具 trajectory | 只有 RUN_START/RUN_END | 检查 trajectory 开关和内部 ReAct rail 注册 |
| MCP tool 没进入 DeepAgent | 模型看不到 MCP tool | 检查 runtime MCP provider、installer 日志和 tool name 是否与 rail 注册工具重名 |
| SkillHub skills 没进入 DeepAgent | 日志提示 skill runtime 未配置 | 在 `createOpenJiuwenDeepAgent(context)` 或工厂中配置内部 ReAct agent 的 SkillUtil |
| memory 注入重复或 prompt 异常 | 同时安装了两种 memory rail | `memoryRuntimeRail` 与 `openJiuwenExternalMemoryRail` 二选一 |
| `TaskCompletionRail` 不影响停止条件 | rail 只从 `openJiuwenDeepAgentRails` 返回 | 把 completion rail 放入 `DeepAgentConfig.rails` |
| 取消不立即停止 | 模型调用仍在等待 | DeepAgent abort 是协作式，等待下一次 OpenJiuwen 检查点 |
| `OPENJIUWEN_RUN_ERROR` | handler 构建失败或输入不是 map | 查看异常 message 和 handler 日志 |
| workspace 文件异常 | DeepAgent 无法读写工作区 | 检查 `workspace-path` 权限和进程工作目录 |

限制：

| 限制 | 影响 | 替代 |
|---|---|---|
| 不解析 YAML | runtime handler 只执行 DeepAgent | 使用 `agent-sdk` 先从 YAML 构造 |
| SkillHub 不自动创建 skill runtime | 内部 ReAct SkillUtil 未配置时 SkillHub installer 会跳过 | 在 DeepAgent 工厂或子类中完成 SkillUtil 配置 |
| `TaskCompletionRail` 不能通过 runtime hook 提升为 completion policy | 只从 `openJiuwenDeepAgentRails` 返回时不会影响停止 evaluator | 在 `DeepAgentConfig.rails` 中配置 completion rail |
| memory helper 是 inner ReAct round 语义 | 不等价于 DeepAgent task-level memory | 需要 task-level memory 时实现专用 DeepAgent rail |
| 不缓存 DeepAgent 实例 | 每次请求有构建开销 | 构建开销通常小于模型调用；需要缓存时由应用评估状态隔离 |
| 取消是协作式 | 阻塞模型调用期间不会立即退出 | 缩短模型 timeout 或使用支持中断的模型客户端 |

## 9. 相关资源

- 设计文档：`architecture/docs/L2/agent-runtime/openjiuwen-deepagent-runtime-adapter-design.md`
- ReAct 适配器：`openjiuwen-adapter.md`
- Workflow 适配器：`openjiuwen-workflow-adapter.md`
- Agent SDK YAML：`agent-sdk-openjiuwen-yaml.md`
- 远端调用：`remote-invocation.md`
- 轨迹观测：`trajectory-observability.md`
