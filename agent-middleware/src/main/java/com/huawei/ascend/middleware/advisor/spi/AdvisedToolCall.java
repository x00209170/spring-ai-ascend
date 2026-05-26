package com.huawei.ascend.middleware.advisor.spi;

import java.util.Objects;

/**
 * Same-package tool-call carrier used by advised request and response payloads.
 *
 * <p>Authority: ADR-0132.
 */
public record AdvisedToolCall(String callId, String skillKey, String arguments) {

    public AdvisedToolCall {
        Objects.requireNonNull(callId, "callId");
        Objects.requireNonNull(skillKey, "skillKey");
        Objects.requireNonNull(arguments, "arguments");
        if (callId.isBlank()) {
            throw new IllegalArgumentException("callId must be non-blank");
        }
        if (skillKey.isBlank()) {
            throw new IllegalArgumentException("skillKey must be non-blank");
        }
    }
}
