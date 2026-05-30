package com.huawei.ascend.service.engine.spi;

/**
 * Enqueue status returned by {@link EngineDispatchSpi}.
 *
 * <p>Indicates whether the engine accepted the enqueue request.
 * Does not represent the real execution status; real execution status
 * is written back through TaskControlClient.
 */
public enum EnqueueEngineStatus {
    /**
     * Engine has accepted the enqueue request.
     */
    SUCCESS,

    /**
     * Engine did not accept the enqueue request.
     */
    FAILED
}
