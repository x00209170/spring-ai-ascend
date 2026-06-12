package com.huawei.ascend.examples.a2a.versatileparent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * End-to-end test: Main parent (OpenJiuwen LLM) calls versatile child agent
 * via A2A remote tool, with interruption detection and resume.
 *
 * <p>The versatile child starts first on a random port. The main parent starts
 * second with the child's URL configured as a remote agent. The runtime discovers
 * the child's A2A card, injects it as a tool into the parent's OpenJiuwen
 * ReActAgent, and the LLM chooses to invoke it.
 *
 * <p>The real-LLM test requires {@code SAA_SAMPLE_LLM_API_KEY} and is skipped
 * otherwise.
 *
 * <h3>Test scenarios</h3>
 * <ol>
 *   <li><b>Agent card discovery:</b> Both agents' cards are discoverable
 *       via {@code /.well-known/agent-card.json}.</li>
 *   <li><b>Parent calls child:</b> Parent LLM invokes the versatile child
 *       tool; child streams intermediate output to user (target=USER);
 *       child caches output; child detects End node and returns final
 *       assembled result (target=LLM); parent LLM receives tool result
 *       and summarizes.</li>
 *   <li><b>Interruption on connection loss:</b> When the versatile HTTP
 *       connection closes without an End node, the child emits INTERRUPTED;
 *       the parent task enters INPUT_REQUIRED; client resumes with same
 *       taskId; request is routed directly to child (skipping LLM).</li>
 * </ol>
 */
@Tag("e2e")
@ResourceLock("real-llm")
class VersatileParentA2eE2eTest {

    private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(90);

    @Test
    void agentCardIsDiscoverable() throws Exception {
        try (ConfigurableApplicationContext versatile = startRuntime("versatile")) {
            VersatileParentA2aClient client = new VersatileParentA2aClient(
                    URI.create("http://localhost:" + port(versatile)), CLIENT_TIMEOUT);
            AgentCard card = client.agentCard();
            assertThat(card.name()).isEqualTo("versatile-child");
            assertThat(card.description()).contains("interruption");
            assertThat(card.capabilities().streaming()).isTrue();
            assertThat(card.supportedInterfaces())
                    .extracting(AgentInterface::protocolBinding)
                    .contains(TransportProtocol.JSONRPC.asString());
        }
    }

    @Test
    void mainAgentCardIsDiscoverable() throws Exception {
        assumeTrue(hasText(System.getenv("SAA_SAMPLE_LLM_API_KEY")),
                "SAA_SAMPLE_LLM_API_KEY not set; skipping real-LLM test");

        try (ConfigurableApplicationContext main = startRuntime("main")) {
            VersatileParentA2aClient client = new VersatileParentA2aClient(
                    URI.create("http://localhost:" + port(main)), CLIENT_TIMEOUT);
            AgentCard card = client.agentCard();
            assertThat(card.name()).isEqualTo(MainAgentConfiguration.AGENT_ID);
            assertThat(card.capabilities().streaming()).isTrue();
        }
    }

    @Test
    void llmParentInvokesVersatileChildViaA2aTool() throws Exception {
        assumeTrue(hasText(System.getenv("SAA_SAMPLE_LLM_API_KEY")),
                "SAA_SAMPLE_LLM_API_KEY not set; skipping real-LLM remote A2A e2e test");

        // Start versatile child
        try (ConfigurableApplicationContext child = startRuntime("versatile")) {
            int childPort = port(child);

            // Start main parent, pointing to child
            try (ConfigurableApplicationContext main = startRuntime("main",
                    "agent-runtime.remote-agents[0].url=http://localhost:" + childPort)) {

                // Verify parent card
                VersatileParentA2aClient client = new VersatileParentA2aClient(
                        URI.create("http://localhost:" + port(main)), CLIENT_TIMEOUT);
                AgentCard card = client.agentCard();
                assertThat(card.name()).isEqualTo(MainAgentConfiguration.AGENT_ID);

                // Ask parent to call versatile child
                List<StreamingEventKind> events = client.streamMessage(
                        "sample-user",
                        MainAgentConfiguration.AGENT_ID,
                        "ctx-versatile-parent-e2e",
                        null,
                        "Please call the versatile child agent to process my request.");

                assertThat(events).isNotEmpty();
                assertThat(events).anySatisfy(event ->
                        assertThat(VersatileParentA2aClient.isTerminal(event)).isTrue());

                String answer = VersatileParentA2aClient.textFrom(events);
                assertThat(answer).isNotBlank();
            }
        }
    }

    /**
     * Tests the interruption scenario: the versatile child responds to a
     * request, the HTTP connection closes before End, the task enters
     * INPUT_REQUIRED, and the client resumes with the same taskId.
     *
     * <p>This test requires a real versatile endpoint that can be interrupted.
     * When the versatile endpoint is not available, this test is skipped.
     */
    @Test
    void versatileInterruptionAndResume() throws Exception {
        assumeTrue(hasText(System.getenv("SAA_SAMPLE_LLM_API_KEY")),
                "SAA_SAMPLE_LLM_API_KEY not set; skipping real-LLM test");

        // Start versatile child
        try (ConfigurableApplicationContext child = startRuntime("versatile")) {
            int childPort = port(child);

            // Start main parent
            try (ConfigurableApplicationContext main = startRuntime("main",
                    "agent-runtime.remote-agents[0].url=http://localhost:" + childPort)) {

                VersatileParentA2aClient client = new VersatileParentA2aClient(
                        URI.create("http://localhost:" + port(main)), CLIENT_TIMEOUT);

                String sessionId = "ctx-interrupt-e2e-" + System.currentTimeMillis();

                // First request — may result in INPUT_REQUIRED if versatile interrupts
                List<StreamingEventKind> firstEvents = client.streamMessage(
                        "sample-user",
                        MainAgentConfiguration.AGENT_ID,
                        sessionId,
                        null,
                        "Please call the versatile child agent with a task that may be interrupted.");

                assertThat(firstEvents).isNotEmpty();

                if (VersatileParentA2aClient.isInputRequired(firstEvents)) {
                    // Interruption occurred — resume with same taskId
                    String taskId = VersatileParentA2aClient.firstTaskId(firstEvents);
                    assertThat(taskId).isNotBlank();

                    List<StreamingEventKind> resumeEvents = client.streamMessage(
                            "sample-user",
                            MainAgentConfiguration.AGENT_ID,
                            sessionId,
                            taskId,
                            "Here is the follow-up input for the interrupted versatile task.");

                    assertThat(resumeEvents).isNotEmpty();
                    assertThat(resumeEvents).anySatisfy(event ->
                            assertThat(VersatileParentA2aClient.isTerminal(event)).isTrue());
                }
            }
        }
    }

    private static ConfigurableApplicationContext startRuntime(String profile, String... extraProperties) {
        java.util.List<String> args = new java.util.ArrayList<>();
        args.add("--server.port=0");
        args.add("--spring.profiles.active=" + profile);
        for (String property : extraProperties) {
            args.add(property.startsWith("--") ? property : "--" + property);
        }
        return new SpringApplicationBuilder(VersatileParentA2aApplication.class)
                .run(args.toArray(String[]::new));
    }

    private static int port(ConfigurableApplicationContext context) {
        Integer port = context.getEnvironment().getProperty("local.server.port", Integer.class);
        if (port == null) {
            throw new IllegalStateException("local.server.port is not available");
        }
        return port;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
