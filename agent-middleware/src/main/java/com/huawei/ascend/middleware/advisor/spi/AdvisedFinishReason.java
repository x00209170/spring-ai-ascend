package com.huawei.ascend.middleware.advisor.spi;

import java.util.Locale;
import java.util.Objects;

/**
 * Same-package finish-reason vocabulary for advised responses.
 *
 * <p>Authority: ADR-0132.
 */
public enum AdvisedFinishReason {
    STOP("stop"),
    LENGTH("length"),
    TOOL_CALLS("tool_calls"),
    CONTENT_FILTER("content_filter"),
    OTHER("other");

    private final String wireValue;

    AdvisedFinishReason(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AdvisedFinishReason fromWireValue(String value) {
        Objects.requireNonNull(value, "value");
        String normalised = value.trim().toLowerCase(Locale.ROOT);
        for (AdvisedFinishReason reason : values()) {
            if (reason.wireValue.equals(normalised)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("unknown advised finish reason: " + value);
    }
}
