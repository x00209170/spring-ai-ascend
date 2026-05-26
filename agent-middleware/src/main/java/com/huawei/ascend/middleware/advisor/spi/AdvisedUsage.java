package com.huawei.ascend.middleware.advisor.spi;

/**
 * Provider-neutral token usage surfaced to advisors.
 *
 * <p>Authority: ADR-0132.
 */
public record AdvisedUsage(int promptTokens, int completionTokens, int totalTokens) {
}
