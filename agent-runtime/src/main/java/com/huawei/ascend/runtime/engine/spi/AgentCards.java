package com.huawei.ascend.runtime.engine.spi;

import java.util.List;
import com.huawei.ascend.runtime.boot.AgentCardProperties;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentProvider;
import org.a2aproject.sdk.spec.TransportProtocol;

/**
 * Small factory for the default A2A card shape used by local runtime handlers.
 */
public final class AgentCards {

    private AgentCards() {
    }

    public static AgentCard create(String name, String description) {
        return create(name, description, "0.1.0", "/a2a");
    }

    public static AgentCard create(String name, String description, String version, String endpoint) {
        org.springframework.util.Assert.hasText(name, "name must not be blank");
        org.springframework.util.Assert.hasText(description, "description must not be blank");
        org.springframework.util.Assert.hasText(version, "version must not be blank");
        org.springframework.util.Assert.hasText(endpoint, "endpoint must not be blank");
        AgentCapabilities capabilities = AgentCapabilities.builder()
                .streaming(true)
                .pushNotifications(true)
                .extendedAgentCard(false)
                .build();
        return AgentCard.builder()
                .name(name)
                .description(description)
                .url(endpoint)
                .version(version)
                .provider(new AgentProvider("spring-ai-ascend", "http://localhost:8080"))
                .capabilities(capabilities)
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text", "artifact"))
                .skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), endpoint)))
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .build();
    }

    /**
     * Build an {@link AgentCard} from YAML-driven properties, using sensible
     * defaults for any unset fields. The caller is responsible for providing
     * a non-blank {@code name}.
     */
    public static AgentCard createFromProperties(String name, AgentCardProperties properties) {
        String description = blankToDefault(properties.getDescription(), "agent-runtime");
        String version = blankToDefault(properties.getVersion(), "0.1.0");
        String endpoint = blankToDefault(properties.getEndpoint(), "/a2a");
        String organization = blankToDefault(properties.getOrganization(), "spring-ai-ascend");
        String organizationUrl = blankToDefault(properties.getOrganizationUrl(), "http://localhost:8080");

        AgentCapabilities capabilities = AgentCapabilities.builder()
                .streaming(true)
                .pushNotifications(true)
                .extendedAgentCard(false)
                .build();
        return AgentCard.builder()
                .name(name)
                .description(description)
                .url(endpoint)
                .version(version)
                .provider(new AgentProvider(organization, organizationUrl))
                .capabilities(capabilities)
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text", "artifact"))
                .skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), endpoint)))
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .build();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
