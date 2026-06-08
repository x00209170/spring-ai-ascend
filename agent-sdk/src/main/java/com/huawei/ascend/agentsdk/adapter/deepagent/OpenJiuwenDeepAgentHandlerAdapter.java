package com.huawei.ascend.agentsdk.adapter.deepagent;

import com.huawei.ascend.agentsdk.adapter.react.OpenJiuwenRuntimeProof;
import com.huawei.ascend.runtime.engine.openjiuwen.OpenJiuwenAgentRuntimeHandler;
import com.huawei.ascend.runtime.engine.handler.AgentExecutionContext;
import com.openjiuwen.harness.deep_agent.DeepAgent;
import java.util.stream.Stream;

public final class OpenJiuwenDeepAgentHandlerAdapter extends OpenJiuwenAgentRuntimeHandler {
    private final DeepAgent deepAgent;
    private final boolean proofMode;
    private final OpenJiuwenRuntimeProof proof;

    public OpenJiuwenDeepAgentHandlerAdapter(
            String agentId,
            DeepAgent deepAgent,
            boolean proofMode,
            OpenJiuwenRuntimeProof proof) {
        super(agentId);
        this.deepAgent = deepAgent;
        this.proofMode = proofMode;
        this.proof = proof;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<?> execute(AgentExecutionContext context) {
        try {
            Object input = toOpenJiuwenInput(context);
            if (proofMode) {
                return Stream.of(proof.run(input));
            }
            return Stream.of(deepAgent.run((java.util.Map<String, Object>) input));
        } catch (RuntimeException error) {
            return Stream.of(java.util.Map.of(
                    "result_type", "error",
                    "output", errorMessage(error)));
        } finally {
            safeRelease(context);
        }
    }
}

