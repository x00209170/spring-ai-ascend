package com.huawei.ascend.middleware.advisor.spi;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Typed provider-neutral request payload visible to advisors.
 *
 * <p>Authority: ADR-0132.
 */
public record AdvisedModelRequest(
        String modelId,
        List<AdvisedMessage> messages,
        List<String> tools,
        Map<String, Object> parameters,
        Map<String, Object> hookContext) {

    public AdvisedModelRequest {
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(messages, "messages");
        Objects.requireNonNull(tools, "tools");
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(hookContext, "hookContext");
        messages = List.copyOf(messages);
        tools = List.copyOf(tools);
        parameters = Map.copyOf(parameters);
        hookContext = Map.copyOf(hookContext);
        if (modelId.isBlank()) {
            throw new IllegalArgumentException("modelId must be non-blank");
        }
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must be non-empty");
        }
        for (String tool : tools) {
            if (tool == null || tool.isBlank()) {
                throw new IllegalArgumentException("tools must contain only non-blank skill keys");
            }
        }
    }
}
