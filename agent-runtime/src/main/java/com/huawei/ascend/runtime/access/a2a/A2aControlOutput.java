package com.huawei.ascend.runtime.access.a2a;

import com.huawei.ascend.runtime.control.ControlOutputPort;
import com.huawei.ascend.runtime.engine.EngineEvent;
import com.huawei.ascend.runtime.common.RuntimeIdentity;
import com.huawei.ascend.runtime.engine.EngineOutput;
import java.util.Map;
import java.util.Objects;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TextPart;

/**
 * Access-side implementation of {@link ControlOutputPort}: translates engine
 * events into A2A wire events and writes them to the {@link A2aOutputRegistry}.
 */
public final class A2aControlOutput implements ControlOutputPort {

    private final A2aOutputRegistry registry;

    public A2aControlOutput(A2aOutputRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void writeOutput(RuntimeIdentity id, EngineEvent event) {
        String text = text(event);
        registry.append(id, output(id, text, false,
                Map.of("runStatus", "in_progress", "protocol", "A2A")));
    }

    @Override
    public void writeCompleted(RuntimeIdentity id, EngineEvent event) {
        String text = text(event);
        registry.append(id, output(id, text, true,
                Map.of("runStatus", "completed", "protocol", "A2A", "notificationType", "LLM_RESULT")));
    }

    @Override
    public void writeFailed(RuntimeIdentity id, String errorCode, String errorMessage) {
        String text = "%s: %s".formatted(errorCode == null ? "UNKNOWN" : errorCode,
                errorMessage == null ? "" : errorMessage);
        registry.append(id, output(id, text, true,
                Map.of("runStatus", "failed", "protocol", "A2A", "notificationType", "ERROR")));
    }

    @Override
    public void writeInputRequired(RuntimeIdentity id, EngineEvent event) {
        String prompt = event == null ? null : event.prompt();
        registry.append(id, output(id, prompt == null ? "" : prompt, false,
                Map.of("runStatus", "input_required", "protocol", "A2A", "notificationType", "ACK",
                        "waitingReason", "USER_INPUT")));
    }

    // handle() removed — RuntimeIdentity is already the universal key type

    private A2aOutput output(RuntimeIdentity id, String text, boolean terminal,
                              Map<String, Object> metadata) {
        var message = org.a2aproject.sdk.spec.Message.builder()
                .role(org.a2aproject.sdk.spec.Message.Role.ROLE_AGENT)
                .parts(java.util.List.of(new TextPart(text)))
                .messageId(java.util.UUID.randomUUID().toString())
                .contextId(id.sessionId())
                .taskId(id.taskId())
                .metadata(metadata)
                .build();
        if (terminal) {
            // Terminal events use TaskStatusUpdateEvent so A2A clients recognize completion
            var sseEvent = new TaskStatusUpdateEvent(id.taskId(),
                    new TaskStatus(TaskState.TASK_STATE_COMPLETED, message, null),
                    id.sessionId(), metadata);
            return new A2aOutput("TaskStatus", id.taskId(), sseEvent, text, terminal, metadata);
        }
        return new A2aOutput("Message", id.taskId(), message, text, terminal, metadata);
    }

    private static String text(EngineEvent event) {
        EngineOutput output = event == null ? null : event.output();
        return output == null || output.getContent() == null ? "" : output.getContent();
    }
}
