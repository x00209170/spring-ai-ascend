package com.huawei.ascend.service.taskflow.test;

import com.huawei.ascend.service.taskflow.queue.InMemoryTaskQueue;
import com.huawei.ascend.service.taskflow.queue.QueueFactory;
import com.huawei.ascend.service.taskflow.queue.TaskQueue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryTaskQueueWhiteboxTest {

    @Test
    void factoryCreatesJdkBackedFifoQueue() {
        TaskQueue<Object> queue = QueueFactory.inMemoryQueue("session-queue");

        queue.offer("first");
        queue.offer("second");

        assertThat(queue).isInstanceOf(InMemoryTaskQueue.class);
        assertThat(queue.queueId()).isEqualTo("session-queue");
        assertThat(queue.size()).isEqualTo(2);
        assertThat(queue.peek()).contains("first");
        assertThat(queue.poll()).contains("first");
        assertThat(queue.poll()).contains("second");
        assertThat(queue.poll()).isEmpty();
    }

    @Test
    void snapshotIsReadOnlyAndDoesNotDrainQueue() {
        TaskQueue<String> queue = QueueFactory.inMemoryQueue("snapshot-queue");
        queue.offer("one");

        assertThat(queue.snapshot()).containsExactly("one");
        assertThatThrownBy(() -> queue.snapshot().add("two"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(queue.size()).isEqualTo(1);
    }

    @Test
    void rejectsBlankQueueIdAndNullValues() {
        assertThatThrownBy(() -> QueueFactory.inMemoryQueue(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("queueId");

        TaskQueue<Object> queue = QueueFactory.inMemoryQueue("valid");
        assertThatThrownBy(() -> queue.offer(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("value");
    }
}
