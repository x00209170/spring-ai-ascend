package com.huawei.ascend.middleware.advisor.spi;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Typed provider-neutral response payload visible to advisors.
 *
 * <p>Authority: ADR-0132.
 */
public record AdvisedModelResponse(
        String content,
        List<AdvisedToolCall> toolCalls,
        AdvisedFinishReason finishReason,
        Optional<AdvisedUsage> usage,
        Map<String, Object> metadata) {

    public AdvisedModelResponse {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(toolCalls, "toolCalls");
        Objects.requireNonNull(finishReason, "finishReason");
        Objects.requireNonNull(usage, "usage");
        Objects.requireNonNull(metadata, "metadata");
        toolCalls = List.copyOf(toolCalls);
        metadata = Map.copyOf(metadata);
    }
}
