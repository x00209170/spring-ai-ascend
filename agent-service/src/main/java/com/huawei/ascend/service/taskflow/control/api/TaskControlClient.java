package com.huawei.ascend.service.taskflow.control.api;

import com.huawei.ascend.service.taskflow.control.TaskFailureCode;
import com.huawei.ascend.service.taskflow.control.TaskState;
import com.huawei.ascend.service.taskflow.control.WaitingReason;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

public interface TaskControlClient {

    CompletionStage<TaskResult> runTask(RunTaskCommand command);

    CompletionStage<TaskResult> markRunning(MarkTaskCommand command);

    CompletionStage<TaskResult> markWaiting(MarkTaskCommand command);

    CompletionStage<TaskResult> markSucceeded(MarkTaskCommand command);

    CompletionStage<TaskResult> markFailed(MarkTaskCommand command);

    CompletionStage<TaskResult> markCancelled(MarkTaskCommand command);

    enum TaskAction {
        RUN,
        RESUME_INPUT,
        CANCEL
    }

    record RunTaskCommand(
            String tenantId,
            String sessionId,
            String taskId,
            String agentId,
            TaskAction action,
            Object input,
            String reason,
            String idempotencyKey,
            Map<String, Object> metadata) {

        public RunTaskCommand {
            tenantId = requireNonBlank(tenantId, "tenantId");
            sessionId = requireNonBlank(sessionId, "sessionId");
            action = Objects.requireNonNull(action, "action");
            if (action == TaskAction.CANCEL) {
                taskId = requireNonBlank(taskId, "taskId");
            } else {
                Objects.requireNonNull(input, "input");
            }
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    record MarkTaskCommand(
            String tenantId,
            String sessionId,
            String taskId,
            long expectedRevision,
            WaitingReason waitingReason,
            TaskFailureCode failureCode,
            Object detail,
            Map<String, Object> metadata) {

        public MarkTaskCommand {
            tenantId = requireNonBlank(tenantId, "tenantId");
            sessionId = requireNonBlank(sessionId, "sessionId");
            taskId = requireNonBlank(taskId, "taskId");
            if (expectedRevision < 1L) {
                throw new IllegalArgumentException("expectedRevision must be positive");
            }
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    record TaskResult(
            String tenantId,
            String sessionId,
            String taskId,
            TaskState state,
            long revision,
            boolean accepted,
            String message) {

        public TaskResult {
            tenantId = requireNonBlank(tenantId, "tenantId");
            sessionId = requireNonBlank(sessionId, "sessionId");
            taskId = requireNonBlank(taskId, "taskId");
            Objects.requireNonNull(state, "state");
            if (revision < 1L) {
                throw new IllegalArgumentException("revision must be positive");
            }
        }
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
