package com.huawei.ascend.service.taskflow.test;

import com.huawei.ascend.service.taskflow.control.TaskState;
import com.huawei.ascend.service.taskflow.control.WaitingReason;
import com.huawei.ascend.service.taskflow.control.api.TaskControlClient;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskControlClientApiWhiteboxTest {

    @Test
    void runTaskCommandCopiesMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("traceId", "trace-1");

        TaskControlClient.RunTaskCommand command = new TaskControlClient.RunTaskCommand(
                "tenant",
                "session",
                null,
                "agent",
                TaskControlClient.TaskAction.RUN,
                "hello",
                null,
                "idem",
                metadata);
        metadata.put("traceId", "mutated");

        assertThat(command.metadata()).containsEntry("traceId", "trace-1");
        assertThatThrownBy(() -> command.metadata().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void cancelActionRequiresTaskId() {
        assertThatThrownBy(() -> new TaskControlClient.RunTaskCommand(
                "tenant",
                "session",
                null,
                "agent",
                TaskControlClient.TaskAction.CANCEL,
                null,
                "user cancelled",
                "idem",
                Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("taskId");
    }

    @Test
    void markTaskCommandValidatesExpectedRevision() {
        assertThatThrownBy(() -> new TaskControlClient.MarkTaskCommand(
                "tenant",
                "session",
                "task",
                0L,
                WaitingReason.USER_INPUT,
                null,
                null,
                Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedRevision");
    }

    @Test
    void taskResultPinsRequiredIdentityAndState() {
        TaskControlClient.TaskResult result = new TaskControlClient.TaskResult(
                "tenant",
                "session",
                "task",
                TaskState.RUNNING,
                2L,
                true,
                "accepted");

        assertThat(result.accepted()).isTrue();
        assertThat(result.state()).isEqualTo(TaskState.RUNNING);
        assertThat(result.revision()).isEqualTo(2L);
    }
}
