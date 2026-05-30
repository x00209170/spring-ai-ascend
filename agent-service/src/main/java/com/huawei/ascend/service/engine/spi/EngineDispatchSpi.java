package com.huawei.ascend.service.engine.spi;

/**
 * Engine dispatch SPI.
 *
 * <p>The sole external entry point for task-centric-control to call the engine.
 * Only responsible for async enqueuing. Does not directly execute Agents.
 * Does not directly return real execution status. Real execution status is
 * written back through TaskControlClient.
 *
 * <p>Design authority: {@code docs/architecture/l1/2026-05-30-l1--agent-service-engine-model-design.md}.
 */
public interface EngineDispatchSpi {

    /**
     * Enqueue an Agent execution request.
     *
     * @param request execution request containing scope and input.
     * @return enqueue status (SUCCESS if accepted, FAILED if not accepted).
     */
    EnqueueEngineStatus enqueueExecution(EnqueueEngineExecutionRequest request);

    /**
     * Enqueue a resume request for an interrupted Agent execution.
     *
     * @param request resume request containing scope and input.
     * @return enqueue status (SUCCESS if accepted, FAILED if not accepted).
     */
    EnqueueEngineStatus enqueueResume(EnqueueEngineResumeRequest request);

    /**
     * Enqueue a cancel request for an Agent execution.
     *
     * @param request cancel request containing scope.
     * @return enqueue status (SUCCESS if accepted, FAILED if not accepted).
     */
    EnqueueEngineStatus enqueueCancel(EnqueueEngineCancelRequest request);
}
