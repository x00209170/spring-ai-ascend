package com.huawei.ascend.middleware.skill.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillDefinitionCarrierInvariantTest {

    @Test
    void skillDefinitionRejectsBlankRequiredTextFields() {
        assertThatThrownBy(() -> new SkillDefinition(
                " ",
                SkillKind.TOOL,
                "Search",
                "Finds documents",
                "{}",
                "{}",
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skillKey");
        assertThatThrownBy(() -> new SkillDefinition(
                "search",
                SkillKind.TOOL,
                " ",
                "Finds documents",
                "{}",
                "{}",
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("displayName");
    }

    @Test
    void skillDefinitionDefaultsCapacityKeyToSkillKeyWhenBlank() {
        SkillDefinition definition = new SkillDefinition(
                "search",
                SkillKind.TOOL,
                "Search",
                "Finds documents",
                "{}",
                "{}",
                " ");

        assertThat(definition.effectiveCapacityKey()).isEqualTo("search");
    }
}
