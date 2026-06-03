package com.huawei.ascend.service.bootstrap;

import com.huawei.ascend.service.access.config.AccessLayerConfiguration;
import com.huawei.ascend.service.access.egress.EgressQueueRegistry;
import com.huawei.ascend.service.access.core.AccessSubmissionService;
import com.huawei.ascend.service.access.model.ReplyContext;
import com.huawei.ascend.service.access.protocol.a2a.A2aAcceptedResponse;
import com.huawei.ascend.service.access.protocol.a2a.A2aAccessService;
import com.huawei.ascend.service.access.protocol.a2a.A2aEnvelope;
import com.huawei.ascend.service.access.protocol.a2a.A2aOutput;
import com.huawei.ascend.service.access.protocol.a2a.A2aOutputHandle;
import com.huawei.ascend.service.access.protocol.a2a.A2aOutputRegistry;
import com.huawei.ascend.service.engine.config.EngineAutoConfiguration;
import com.huawei.ascend.service.engine.handler.AgentExecutionContext;
import com.huawei.ascend.service.engine.model.InterruptType;
import com.huawei.ascend.service.engine.spi.AgentExecutionResult;
import com.huawei.ascend.service.engine.spi.AgentHandler;
import com.huawei.ascend.service.engine.spi.AgentResultAdapter;
import com.huawei.ascend.service.queue.config.QueueAutoConfiguration;
import com.huawei.ascend.service.schema.AgentRequest;
import com.huawei.ascend.service.schema.Message;
import com.huawei.ascend.service.session.api.SessionManager;
import com.huawei.ascend.service.session.config.SessionManageConfiguration;
import com.huawei.ascend.service.taskcontrol.TaskControlService;
import com.huawei.ascend.service.taskcontrol.TaskState;
import com.huawei.ascend.service.taskcontrol.config.TaskControlAutoConfiguration;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test from the access layer's perspective: an A2A request enters,
 * flows access → task-centric-control → engine → fake agent, and the reply
 * comes back out through the access layer's A2A output channel.
 *
 * <p>This is the test the human review asked for — it exercises the whole
 * five-layer stack as a single wired runtime (not one module in isolation),
 * proving the glue closes the loop. The agent framework is faked with a small
 * echo {@link AgentHandler} so no external runtime is needed.
 */
@SpringBootTest(classes = AgentServiceEndToEndIT.TestRuntime.class)
class AgentServiceEndToEndIT {

    private static final String TENANT = "tenant-e2e";
    private static final String AGENT = "echo-agent";
    private static final String FAILING_AGENT = "boom-agent";
    private static final String INTERRUPTING_AGENT = "interrupt-agent";
    private static final String RESULT_FAILING_AGENT = "failed-result-agent";

    @Autowired
    private A2aAccessService a2aAccessService;

    @Autowired
    private A2aOutputRegistry outputRegistry;

    @Autowired
    private EgressQueueRegistry egressQueueRegistry;

    @Autowired
    private AccessSubmissionService submissionService;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private TaskControlService taskControlService;

    @Test
    void a2aRequestRunsThroughTheStackAndRepliesBack() {
        A2aEnvelope envelope = envelope("session-1", "hello world");

        A2aAcceptedResponse accepted = a2aAccessService.send(envelope);

        assertThat(accepted.accepted()).isTrue();
        assertThat(accepted.taskId()).isNotBlank();
        assertThat(accepted.tenantId()).isEqualTo(TENANT);

        // task-control dispatch and egress delivery run on their own threads, so
        // poll briefly for the reply to surface on the A2A output channel.
        A2aOutputHandle handle = new A2aOutputHandle(TENANT, "session-1", accepted.taskId());
        List<A2aOutput> outputs = awaitOutputs(handle);

        assertThat(outputs).isNotEmpty();
        assertThat(outputs).anyMatch(o -> "Message".equals(o.kind()));
        assertThat(outputs.get(outputs.size() - 1).terminal()).isTrue();
        assertThat(outputs).anyMatch(o -> String.valueOf(o.body()).contains("hello world"));
        assertThat(sessionManager.get(TENANT, "session-1")).hasValueSatisfying(session ->
                assertThat(session.currentUserInput()).anyMatch(message -> "hello world".equals(message.text())));

        // The reply channel must be torn down after the terminal frame — no leak.
        awaitEgressCleanup("session-1");
        assertThat(egressQueueRegistry.find(TENANT, "session-1")).isEmpty();
    }

    private void awaitEgressCleanup(String sessionId) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline
                && egressQueueRegistry.find(TENANT, sessionId).isPresent()) {
            try {
                Thread.sleep(20L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Test
    void aThrowingAgentStillRepliesWithATerminalError() {
        A2aEnvelope envelope = envelope(FAILING_AGENT, "session-err", "trigger failure");

        A2aAcceptedResponse accepted = a2aAccessService.send(envelope);
        assertThat(accepted.accepted()).isTrue();

        A2aOutputHandle handle = new A2aOutputHandle(TENANT, "session-err", accepted.taskId());
        List<A2aOutput> outputs = awaitOutputs(handle);

        // A handler that throws must still yield a terminal reply — no hang, no leak.
        assertThat(outputs).isNotEmpty();
        assertThat(outputs.get(outputs.size() - 1).terminal()).isTrue();
        assertThat(outputs).anyMatch(o -> "error".equals(o.kind()));
        awaitEgressCleanup("session-err");
        assertThat(egressQueueRegistry.find(TENANT, "session-err")).isEmpty();
    }

    @Test
    void a2aInterruptedTaskCanBeResumedThroughAccessAndComplete() {
        A2aEnvelope firstTurn = envelope(INTERRUPTING_AGENT, "session-wait", "weather");

        A2aAcceptedResponse waitingAccepted = a2aAccessService.send(firstTurn);
        A2aOutputHandle handle = new A2aOutputHandle(TENANT, "session-wait", waitingAccepted.taskId());
        List<A2aOutput> waitingOutputs = awaitAtLeastOutputs(handle, 1);

        assertThat(waitingAccepted.accepted()).isTrue();
        assertThat(waitingOutputs).isNotEmpty();
        assertThat(waitingOutputs.get(waitingOutputs.size() - 1).terminal()).isFalse();
        assertThat(waitingOutputs).anyMatch(output -> String.valueOf(output.body()).contains("which city"));
        awaitTaskState("session-wait", waitingAccepted.taskId(), TaskState.WAITING);

        AgentRequest resumeRequest = new AgentRequest(
                TENANT,
                "user-1",
                INTERRUPTING_AGENT,
                "session-wait",
                List.of(Message.user("beijing")),
                UUID.randomUUID().toString(),
                Map.of("correlationId", "corr-resume"));
        A2aAcceptedResponse resumed = toA2a(submissionService.resume(
                resumeRequest,
                ReplyContext.a2a(true, "corr-resume", null, Map.of()))
                .toCompletableFuture().join());
        List<A2aOutput> resumedOutputs = awaitOutputs(handle);

        assertThat(resumed.accepted()).isTrue();
        assertThat(resumed.taskId()).isEqualTo(waitingAccepted.taskId());
        assertThat(resumedOutputs).anyMatch(output -> String.valueOf(output.body()).contains("answer: beijing"));
        assertThat(resumedOutputs.get(resumedOutputs.size() - 1).terminal()).isTrue();
        awaitTaskState("session-wait", waitingAccepted.taskId(), TaskState.COMPLETED);
        awaitEgressCleanup("session-wait");
        assertThat(egressQueueRegistry.find(TENANT, "session-wait")).isEmpty();
    }

    @Test
    void aReturnedFailureResultStillRepliesWithATerminalError() {
        A2aEnvelope envelope = envelope(RESULT_FAILING_AGENT, "session-failed-result", "return failure");

        A2aAcceptedResponse accepted = a2aAccessService.send(envelope);
        A2aOutputHandle handle = new A2aOutputHandle(TENANT, "session-failed-result", accepted.taskId());
        List<A2aOutput> outputs = awaitOutputs(handle);

        assertThat(accepted.accepted()).isTrue();
        assertThat(outputs).isNotEmpty();
        assertThat(outputs.get(outputs.size() - 1).terminal()).isTrue();
        assertThat(outputs).anyMatch(output -> "error".equals(output.kind()));
        assertThat(outputs).anyMatch(output -> String.valueOf(output.body()).contains("planned failure"));
        awaitTaskState("session-failed-result", accepted.taskId(), TaskState.FAILED);
    }

    @Test
    void a2aCancelRequestCancelsTaskThroughTaskControlAndEngine() {
        A2aEnvelope envelope = envelope(INTERRUPTING_AGENT, "session-cancel", "book ticket");

        A2aAcceptedResponse accepted = a2aAccessService.send(envelope);
        A2aOutputHandle handle = new A2aOutputHandle(TENANT, "session-cancel", accepted.taskId());
        List<A2aOutput> outputs = awaitAtLeastOutputs(handle, 1);
        awaitTaskState("session-cancel", accepted.taskId(), TaskState.WAITING);

        A2aAcceptedResponse cancelAccepted = a2aAccessService.cancel(cancelEnvelope(
                INTERRUPTING_AGENT,
                "session-cancel",
                accepted.taskId()));

        assertThat(outputs).anyMatch(output -> String.valueOf(output.body()).contains("which city"));
        assertThat(cancelAccepted.accepted()).isTrue();
        assertThat(cancelAccepted.taskId()).isEqualTo(accepted.taskId());
        awaitTaskState("session-cancel", accepted.taskId(), TaskState.CANCELLED);
    }

    private List<A2aOutput> awaitOutputs(A2aOutputHandle handle) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
        List<A2aOutput> outputs = outputRegistry.list(handle);
        while (System.nanoTime() < deadline
                && (outputs.isEmpty() || !outputs.get(outputs.size() - 1).terminal())) {
            try {
                Thread.sleep(20L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
            outputs = outputRegistry.list(handle);
        }
        return outputs;
    }

    private List<A2aOutput> awaitAtLeastOutputs(A2aOutputHandle handle, int count) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
        List<A2aOutput> outputs = outputRegistry.list(handle);
        while (System.nanoTime() < deadline && outputs.size() < count) {
            try {
                Thread.sleep(20L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
            outputs = outputRegistry.list(handle);
        }
        return outputs;
    }

    private void awaitTaskState(String sessionId, String taskId, TaskState state) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (taskControlService.findTask(TENANT, sessionId, taskId)
                    .filter(task -> task.getState() == state)
                    .isPresent()) {
                return;
            }
            try {
                Thread.sleep(20L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertThat(taskControlService.findTask(TENANT, sessionId, taskId))
                .as("task %s/%s should eventually reach %s", sessionId, taskId, state)
                .hasValueSatisfying(task -> assertThat(task.getState()).isEqualTo(state));
    }

    private static A2aAcceptedResponse toA2a(com.huawei.ascend.service.access.model.AccessAcceptedResponse accepted) {
        return new A2aAcceptedResponse(
                accepted.tenantId(),
                accepted.userId(),
                accepted.agentId(),
                accepted.sessionId(),
                accepted.taskId(),
                accepted.accepted(),
                accepted.message());
    }

    private static A2aEnvelope envelope(String sessionId, String text) {
        return envelope(AGENT, sessionId, text);
    }

    private static A2aEnvelope envelope(String agentId, String sessionId, String text) {
        A2aEnvelope.A2aContext context = new A2aEnvelope.A2aContext(
                TENANT, "user-1", agentId, sessionId, "ctx-1", UUID.randomUUID().toString(), "corr-1");
        A2aEnvelope.A2aMessage message = new A2aEnvelope.A2aMessage(text, List.of(), java.util.Map.of());
        return new A2aEnvelope(context, message, null);
    }

    private static A2aEnvelope cancelEnvelope(String agentId, String sessionId, String taskId) {
        A2aEnvelope.A2aContext context = new A2aEnvelope.A2aContext(
                TENANT, "user-1", agentId, sessionId, "ctx-cancel",
                UUID.randomUUID().toString(), "corr-cancel");
        A2aEnvelope.A2aMessage message = new A2aEnvelope.A2aMessage(
                null,
                List.of(),
                Map.of("taskId", taskId));
        return new A2aEnvelope(context, message, null);
    }

    /**
     * Minimal runtime: the five module configurations plus the bootstrap glue
     * and a fake echo agent. Deliberately avoids the full
     * {@code @SpringBootApplication} so the test does not pull in datasource,
     * Flyway and security auto-configuration it does not need.
     */
    @SpringBootConfiguration
    @Import({
            QueueAutoConfiguration.class,
            TaskControlAutoConfiguration.class,
            AgentServiceBootstrapConfiguration.class,
            AccessLayerConfiguration.class,
            SessionManageConfiguration.class,
            EngineAutoConfiguration.class
    })
    static class TestRuntime {

        @Bean
        AgentHandler echoAgentHandler() {
            return new EchoAgentHandler();
        }

        @Bean
        AgentHandler boomAgentHandler() {
            return new ThrowingAgentHandler();
        }

        @Bean
        AgentHandler interruptingAgentHandler() {
            return new InterruptingAgentHandler();
        }

        @Bean
        AgentHandler failedResultAgentHandler() {
            return new FailedResultAgentHandler();
        }
    }

    /** Fake agent framework: echoes the latest user text back as final output. */
    static final class EchoAgentHandler implements AgentHandler {

        @Override
        public String agentId() {
            return AGENT;
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public Stream<?> execute(AgentExecutionContext context) {
            String userText = context.getInput() == null || context.getInput().messages().isEmpty()
                    ? "" : context.getInput().messages().get(0).text();
            return Stream.of(
                    java.util.Map.of("result_type", "output", "output", "echo: " + userText),
                    java.util.Map.of("result_type", "answer", "output", "echo: " + userText));
        }

        @Override
        public AgentResultAdapter resultAdapter() {
            return AgentServiceEndToEndIT::adaptRawResults;
        }
    }

    /** Fake agent that throws, to prove a failure still yields a terminal reply. */
    static final class ThrowingAgentHandler implements AgentHandler {

        @Override
        public String agentId() {
            return FAILING_AGENT;
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public Stream<?> execute(AgentExecutionContext context) {
            throw new IllegalStateException("boom");
        }

        @Override
        public AgentResultAdapter resultAdapter() {
            return AgentServiceEndToEndIT::adaptRawResults;
        }
    }

    /** Fake agent that waits on first turn, then completes when access resumes it. */
    static final class InterruptingAgentHandler implements AgentHandler {

        @Override
        public String agentId() {
            return INTERRUPTING_AGENT;
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public Stream<?> execute(AgentExecutionContext context) {
            String inputType = context.getInput() == null ? "" : context.getInput().inputType();
            if ("RESUME_SIGNAL".equals(inputType)) {
                String userText = context.getInput().messages().isEmpty()
                        ? "" : context.getInput().messages().get(0).text();
                return Stream.of(AgentExecutionResult.completed("answer: " + userText));
            }
            return Stream.of(AgentExecutionResult.interrupted(InterruptType.HUMAN_INPUT, "which city?"));
        }

        @Override
        public AgentResultAdapter resultAdapter() {
            return rawResults -> rawResults.map(result -> (AgentExecutionResult) result);
        }
    }

    /** Fake agent that returns a failure result instead of throwing. */
    static final class FailedResultAgentHandler implements AgentHandler {

        @Override
        public String agentId() {
            return RESULT_FAILING_AGENT;
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public Stream<?> execute(AgentExecutionContext context) {
            return Stream.of(AgentExecutionResult.failed("PLANNED_FAILURE", "planned failure"));
        }

        @Override
        public AgentResultAdapter resultAdapter() {
            return rawResults -> rawResults.map(result -> (AgentExecutionResult) result);
        }
    }

    @SuppressWarnings("unchecked")
    private static Stream<AgentExecutionResult> adaptRawResults(Stream<?> rawResults) {
        return rawResults.map(rawResult -> {
            java.util.Map<String, Object> result = (java.util.Map<String, Object>) rawResult;
            String output = String.valueOf(result.get("output"));
            if ("answer".equals(result.get("result_type"))) {
                return AgentExecutionResult.completed(output);
            }
            return AgentExecutionResult.output(output);
        });
    }
}
