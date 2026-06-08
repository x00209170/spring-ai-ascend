package com.huawei.ascend.agentsdk.factory;

import com.huawei.ascend.runtime.engine.spi.AgentRuntimeHandler;
import com.openjiuwen.core.singleagent.ReActAgent;
import com.openjiuwen.harness.deep_agent.DeepAgent;
import java.nio.file.Path;

public final class AgentHandlerFactory {
    private AgentHandlerFactory() {
    }

    public static AgentRuntimeHandler fromYaml(Path yamlPath) {
        return builder().fromYaml(yamlPath);
    }

    public static ReActAgent toReactAgent(Path yamlPath) {
        return builder().toReactAgent(yamlPath);
    }

    public static DeepAgent toDeepAgent(Path yamlPath) {
        return builder().toDeepAgent(yamlPath);
    }

    public static AgentRuntimeHandler toHandler(String name, Object agent) {
        return builder().toHandler(name, agent);
    }

    public static AgentHandlerFactoryBuilder builder() {
        return new AgentHandlerFactoryBuilder();
    }
}

