package com.huawei.ascend.service.agent.spi;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Per-agent advisor binding by stable advisor name.
 *
 * <p>Authority: ADR-0128 and ADR-0132. The binding intentionally uses
 * same-package values instead of importing middleware advisor SPI types:
 * registration resolves {@link #advisorName()} to concrete advisor beans.
 *
 * @param advisorName    stable advisor bean or registry name; non-blank.
 * @param mode           sync/streaming applicability for this agent.
 * @param orderOverride  optional per-agent ordering override.
 * @param metadata       free-form binding metadata.
 */
public record AdvisorBinding(
        String advisorName,
        Mode mode,
        Optional<Integer> orderOverride,
        Map<String, Object> metadata) {

    public AdvisorBinding {
        Objects.requireNonNull(advisorName, "advisorName");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(orderOverride, "orderOverride");
        Objects.requireNonNull(metadata, "metadata");
        if (advisorName.isBlank()) {
            throw new IllegalArgumentException("advisorName must be non-blank");
        }
        metadata = Map.copyOf(metadata);
    }

    public enum Mode {
        SYNC,
        STREAMING,
        BOTH
    }
}
