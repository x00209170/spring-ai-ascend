package com.huawei.ascend.agentsdk.adapter.deepagent;

import com.huawei.ascend.agentsdk.adapter.OpenJiuwenAgentSpecMapper;
import com.huawei.ascend.agentsdk.adapter.OpenJiuwenSkillMapper;
import com.huawei.ascend.agentsdk.adapter.OpenJiuwenToolMapper;
import com.huawei.ascend.agentsdk.adapter.react.OpenJiuwenRuntimeProof;
import com.huawei.ascend.agentsdk.spec.AgentSpec;
import com.huawei.ascend.agentsdk.spec.tool.ResolvedTool;
import com.huawei.ascend.agentsdk.spec.tool.ToolResolver;
import com.huawei.ascend.agentsdk.spec.tool.ToolSpec;
import com.huawei.ascend.agentsdk.spec.tool.UnsupportedToolRefException;
import com.openjiuwen.core.foundation.tool.Tool;
import com.openjiuwen.harness.deep_agent.DeepAgent;
import com.openjiuwen.harness.rails.SkillUseRail;
import com.openjiuwen.harness.schema.config.DeepAgentConfig;
import com.openjiuwen.harness.workspace.Workspace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class OpenJiuwenDeepAgentBuilder {
    private static final Map<DeepAgent, RuntimeMetadata> RUNTIME_METADATA =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final List<ToolResolver> toolResolvers;
    private final OpenJiuwenToolMapper toolMapper = new OpenJiuwenToolMapper();
    private final OpenJiuwenSkillMapper skillMapper = new OpenJiuwenSkillMapper();
    private final OpenJiuwenAgentSpecMapper specMapper = new OpenJiuwenAgentSpecMapper();

    public OpenJiuwenDeepAgentBuilder(List<ToolResolver> toolResolvers) {
        this.toolResolvers = List.copyOf(toolResolvers);
    }

    public OpenJiuwenDeepAgentHandlerAdapter build(AgentSpec spec) {
        return toHandler(spec.name(), buildAgent(spec));
    }

    public DeepAgent buildAgent(AgentSpec spec) {
        OpenJiuwenDeepAgentOptions options = OpenJiuwenDeepAgentOptions.from(spec.frameworkOptions());
        List<Tool> tools = spec.toolSpecs().stream()
                .map(this::resolve)
                .map(toolMapper::toTool)
                .toList();
        List<Object> configTools = new ArrayList<>(tools);
        List<String> skillDirs = skillMapper.toSkillDirectories(spec.skillSpecs());
        List<Object> rails = List.of(new SkillUseRail(skillDirs, "all"));
        DeepAgentConfig config = DeepAgentConfig.builder()
                .systemPrompt(spec.promptSpec().system())
                .maxIterations(options.maxIterations())
                .tools(configTools)
                .skillDirectories(skillDirs)
                .rails(rails)
                .model(Map.of("model", spec.modelSpec().name()))
                .backend(Map.of(
                        "provider", spec.modelSpec().provider(),
                        "apiKey", spec.modelSpec().apiKey(),
                        "baseUrl", spec.modelSpec().baseUrl(),
                        "verifySsl", spec.modelSpec().sslVerify()))
                .build();
        DeepAgent deepAgent = new DeepAgent(
                specMapper.card(spec),
                config,
                Workspace.builder().rootPath("./").language("cn").build());
        RUNTIME_METADATA.put(deepAgent, new RuntimeMetadata(
                "sdk-proof".equalsIgnoreCase(options.executeMode()),
                new OpenJiuwenRuntimeProof(spec, tools)));
        return deepAgent;
    }

    public static OpenJiuwenDeepAgentHandlerAdapter toHandler(String agentId, DeepAgent agent) {
        RuntimeMetadata metadata = RUNTIME_METADATA.get(agent);
        return new OpenJiuwenDeepAgentHandlerAdapter(
                agentId,
                agent,
                metadata != null && metadata.proofMode(),
                metadata == null ? null : metadata.proof());
    }

    private ResolvedTool resolve(ToolSpec spec) {
        return toolResolvers.stream()
                .filter(resolver -> resolver.supports(spec.ref().scheme()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedToolRefException(
                        "Unsupported tool ref scheme: " + spec.ref().scheme()))
                .resolve(spec);
    }

    private record RuntimeMetadata(boolean proofMode, OpenJiuwenRuntimeProof proof) {
    }
}

