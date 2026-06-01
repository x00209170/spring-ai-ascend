package com.huawei.ascend.service.taskflow.queue;

/**
 * Static Internal Event Queue (IEQ) factory for the current in-memory backend.
 *
 * <p>This is intentionally not an SPI. Provider extensibility belongs in later
 * backend implementations; this wave only freezes the construction surface used
 * by session/access code.
 */
public final class QueueFactory {

    private QueueFactory() {
    }

    public static <T> TaskQueue<T> inMemoryQueue(String queueId) {
        return new InMemoryTaskQueue<>(queueId);
    }
}
