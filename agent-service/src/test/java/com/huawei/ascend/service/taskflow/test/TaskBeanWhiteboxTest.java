package com.huawei.ascend.service.taskflow.test;

import com.huawei.ascend.service.taskflow.control.Task;
import com.huawei.ascend.service.taskflow.control.TaskState;
import com.huawei.ascend.service.taskflow.control.WaitingReason;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskBeanWhiteboxTest {

    @Test
    void javaBeanSettersExposeTaskState() {
        Instant createdAt = Instant.parse("2026-06-01T00:00:00Z");
        Task task = new Task();

        task.setTenantId("tenant");
        task.setSessionId("session");
        task.setTaskId("task");
        task.setAgentId("agent");
        task.setState(TaskState.CREATED);
        task.setRevision(1L);
        task.setCreatedAt(createdAt);
        task.setUpdatedAt(createdAt);

        assertThat(task.getTenantId()).isEqualTo("tenant");
        assertThat(task.getSessionId()).isEqualTo("session");
        assertThat(task.getTaskId()).isEqualTo("task");
        assertThat(task.getAgentId()).isEqualTo("agent");
        assertThat(task.getState()).isEqualTo(TaskState.CREATED);
        assertThat(task.getRevision()).isEqualTo(1L);
        assertThat(task.getCreatedAt()).isEqualTo(createdAt);
        assertThat(task.getUpdatedAt()).isEqualTo(createdAt);
    }

    @Test
    void transitionMutatesStateAndIncrementsRevision() {
        Instant createdAt = Instant.parse("2026-06-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-01T00:01:00Z");
        Task task = Task.created("tenant", "session", "agent", createdAt);

        task.transitionTo(TaskState.WAITING, WaitingReason.USER_INPUT, null, "need-city", updatedAt);

        assertThat(task.getState()).isEqualTo(TaskState.WAITING);
        assertThat(task.getWaitingReason()).isEqualTo(WaitingReason.USER_INPUT);
        assertThat(task.getFailureCode()).isNull();
        assertThat(task.getDetail()).isEqualTo("need-city");
        assertThat(task.getRevision()).isEqualTo(2L);
        assertThat(task.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(task.terminal()).isFalse();
    }

    @Test
    void rejectsBlankIdentifiersAndInvalidRevision() {
        Task task = new Task();

        assertThatThrownBy(() -> task.setTenantId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
        assertThatThrownBy(() -> task.setRevision(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revision");
    }
}
