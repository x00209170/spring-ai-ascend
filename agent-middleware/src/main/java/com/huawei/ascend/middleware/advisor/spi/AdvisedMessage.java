package com.huawei.ascend.middleware.advisor.spi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Provider-neutral message carrier visible to advisors.
 *
 * <p>Authority: ADR-0132.
 */
public record AdvisedMessage(
        AdvisedMessageRole role,
        String content,
        List<AdvisedToolCall> toolCalls,
        Optional<String> toolCallId) {

    public AdvisedMessage {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(toolCalls, "toolCalls");
        Objects.requireNonNull(toolCallId, "toolCallId");
        toolCalls = List.copyOf(toolCalls);
        if (role != AdvisedMessageRole.ASSISTANT && !toolCalls.isEmpty()) {
            throw new IllegalArgumentException("toolCalls are only valid for assistant messages");
        }
        if (role == AdvisedMessageRole.TOOL && toolCallId.filter(s -> !s.isBlank()).isEmpty()) {
            throw new IllegalArgumentException("tool messages require a non-blank toolCallId");
        }
        if (role != AdvisedMessageRole.TOOL && toolCallId.isPresent()) {
            throw new IllegalArgumentException("toolCallId is only valid for tool messages");
        }
    }

    public static AdvisedMessage system(String content) {
        return new AdvisedMessage(AdvisedMessageRole.SYSTEM, content, List.of(), Optional.empty());
    }

    public static AdvisedMessage user(String content) {
        return new AdvisedMessage(AdvisedMessageRole.USER, content, List.of(), Optional.empty());
    }

    public static AdvisedMessage assistant(String content, List<AdvisedToolCall> toolCalls) {
        return new AdvisedMessage(AdvisedMessageRole.ASSISTANT, content, toolCalls, Optional.empty());
    }

    public static AdvisedMessage toolResult(String toolCallId, String content) {
        return new AdvisedMessage(AdvisedMessageRole.TOOL, content, List.of(), Optional.of(toolCallId));
    }
}
