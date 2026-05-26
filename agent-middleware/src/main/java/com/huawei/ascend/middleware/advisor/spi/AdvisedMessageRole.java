package com.huawei.ascend.middleware.advisor.spi;

import java.util.Locale;
import java.util.Objects;

/**
 * Provider-neutral role vocabulary for advised model messages.
 *
 * <p>Authority: ADR-0132.
 */
public enum AdvisedMessageRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private final String wireValue;

    AdvisedMessageRole(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AdvisedMessageRole fromWireValue(String value) {
        Objects.requireNonNull(value, "value");
        String normalised = value.trim().toLowerCase(Locale.ROOT);
        for (AdvisedMessageRole role : values()) {
            if (role.wireValue.equals(normalised)) {
                return role;
            }
        }
        throw new IllegalArgumentException("unknown advised message role: " + value);
    }
}
