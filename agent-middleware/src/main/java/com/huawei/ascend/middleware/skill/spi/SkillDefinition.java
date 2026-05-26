package com.huawei.ascend.middleware.skill.spi;

import java.util.Objects;
import java.util.Optional;

/**
 * Declarative skill metadata customers attach via {@code @Bean} +
 * {@code @ConfigurationProperties}.
 *
 * <p>Authority: ADR-0127. Mirrored at the wire boundary by
 * {@code docs/contracts/skill-definition.v1.yaml}.
 *
 * @param skillKey      stable id; {@code (tenantId, skillKey)}
 *                      indexes {@link SkillRegistry} and
 *                      capacity arbitration.
 * @param kind          discriminator (TOOL / BUILTIN / ...).
 * @param displayName   human-readable name.
 * @param description   one-line purpose summary for LLM tool-choice
 *                      and for human operators.
 * @param inputSchema   JSON-schema string describing accepted
 *                      arguments; never null, may be {@code "{}"}.
 * @param outputSchema  JSON-schema string describing the result;
 *                      never null.
 * @param capacityKey   capacity arbitration key; defaults to
 *                      {@link #skillKey()} when omitted.
 */
public record SkillDefinition(
        String skillKey,
        SkillKind kind,
        String displayName,
        String description,
        String inputSchema,
        String outputSchema,
        String capacityKey) {

    public SkillDefinition {
        Objects.requireNonNull(skillKey, "skillKey");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(inputSchema, "inputSchema");
        Objects.requireNonNull(outputSchema, "outputSchema");
        if (skillKey.isBlank()) {
            throw new IllegalArgumentException("skillKey must be non-blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must be non-blank");
        }
    }

    /** {@code capacityKey} default = {@link #skillKey()}. */
    public String effectiveCapacityKey() {
        return Optional.ofNullable(capacityKey).filter(s -> !s.isBlank()).orElse(skillKey);
    }
}
