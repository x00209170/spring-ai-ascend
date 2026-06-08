package com.huawei.ascend.agentsdk.factory;

import static org.assertj.core.api.Assertions.assertThat;

import com.huawei.ascend.runtime.engine.adapters.openjiuwen.OpenJiuwenAgentRuntimeHandler;
import com.huawei.ascend.runtime.engine.handler.AgentExecutionContext;
import com.huawei.ascend.runtime.engine.model.EngineExecutionScope;
import com.huawei.ascend.runtime.engine.model.EngineInput;
import com.huawei.ascend.runtime.engine.spi.AgentRuntimeHandler;
import com.huawei.ascend.runtime.schema.Message;
import com.openjiuwen.core.foundation.tool.Tool;
import com.openjiuwen.core.runner.Runner;
import com.openjiuwen.core.singleagent.ReActAgent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentHandlerFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    void exposesYamlToReactAgentThenReactAgentToHandlerSteps() throws Exception {
        Path yaml = exampleYaml("react");

        ReActAgent agent = AgentHandlerFactory.toReactAgent(yaml);
        AgentRuntimeHandler handler = AgentHandlerFactory.toHandler("sdk-example-agent", agent);

        assertThat(agent).isInstanceOf(ReActAgent.class);
        assertThat(handler).isInstanceOf(OpenJiuwenAgentRuntimeHandler.class);
        assertThat(handler.agentId()).isEqualTo("sdk-example-agent");
        assertRegisteredTools();
    }

    @Test
    void exposesYamlToDeepAgentThenDeepAgentToHandlerSteps() throws Exception {
        Path yaml = exampleYaml("deepagent");

        Object agent = AgentHandlerFactory.toDeepAgent(yaml);
        AgentRuntimeHandler handler = AgentHandlerFactory.toHandler("sdk-example-agent", agent);

        assertThat(agent).isNotNull();
        assertThat(handler).isInstanceOf(OpenJiuwenAgentRuntimeHandler.class);
        assertThat(handler.agentId()).isEqualTo("sdk-example-agent");
    }

    @Test
    void fromYamlComposesReactAgentCreationAndHandlerWrapping() throws Exception {
        Path yaml = exampleYaml("react");

        AgentRuntimeHandler handler = AgentHandlerFactory.fromYaml(yaml);

        assertThat(handler).isInstanceOf(OpenJiuwenAgentRuntimeHandler.class);
        assertThat(handler.agentId()).isEqualTo("sdk-example-agent");
        assertRegisteredTools();
    }

    @Test
    void fromYamlComposesDeepAgentCreationAndHandlerWrapping() throws Exception {
        Path yaml = exampleYaml("deepagent");

        AgentRuntimeHandler handler = AgentHandlerFactory.fromYaml(yaml);

        assertThat(handler).isInstanceOf(OpenJiuwenAgentRuntimeHandler.class);
        assertThat(handler.agentId()).isEqualTo("sdk-example-agent");
    }

    private Path exampleYaml(String agentType) throws Exception {
        Path common = Files.createDirectories(tempDir.resolve("skills").resolve("common"));
        Files.writeString(common.resolve("SKILL.md"), "# Common\n");
        Path order = Files.createDirectories(tempDir.resolve("skills").resolve("order"));
        Files.writeString(order.resolve("SKILL.md"), "# Order\n");
        Path yaml = tempDir.resolve("agent.yaml");
        Files.writeString(yaml, """
                schema: ascend-agent/v1
                name: sdk-example-agent
                description: SDK example agent
                framework:
                  type: openjiuwen
                  agent: %s
                  options:
                    executeMode: sdk-proof
                model:
                  provider: openai-compatible
                  name: deepseek-chat
                  baseUrl: https://api.deepseek.com
                  apiKey: sk-test
                prompt:
                  system: prove tools and skills
                skills:
                  sources:
                    - ./skills/common
                    - ./skills/order
                tools:
                  - name: firstTool
                    description: firstTool description
                    inputSchema:
                      type: object
                    ref:
                      type: file
                      class: %s
                      method: first
                  - name: secondTool
                    description: secondTool description
                    inputSchema:
                      type: object
                    ref:
                      type: file
                      class: %s
                      method: second
                """.formatted(agentType, TestTools.class.getName(), TestTools.class.getName()));

        return yaml;
    }

    private void assertRegisteredTools() {
        Object first = Runner.resourceMgr().getTool("firstTool");
        Object second = Runner.resourceMgr().getTool("secondTool");
        assertThat(first).isInstanceOf(Tool.class);
        assertThat(second).isInstanceOf(Tool.class);
    }

    public static final class TestTools {
        private TestTools() {
        }

        public static Map<String, Object> first(Map<String, Object> inputs) {
            return Map.of("proof", "first", "inputs", inputs);
        }

        public static Map<String, Object> second(Map<String, Object> inputs) {
            return Map.of("proof", "second", "inputs", inputs);
        }
    }

    private static AgentExecutionContext context() {
        EngineExecutionScope scope =
                new EngineExecutionScope("tenant", "user", "session", "task", "sdk-example-agent");
        EngineInput input = new EngineInput("text", List.of(Message.user("prove it")), Map.of());
        return new AgentExecutionContext(scope, input);
    }
}

