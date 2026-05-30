/**
 * Engine dispatch SPI surface.
 *
 * <p>Contains the {@link com.huawei.ascend.service.engine.spi.EngineDispatchSpi}
 * async dispatch entry point for task-centric-control to enqueue Agent
 * execution requests. The interface is the contract;
 * {@link com.huawei.ascend.service.engine.spi.EnqueueEngineExecutionRequest},
 * {@link com.huawei.ascend.service.engine.spi.EnqueueEngineResumeRequest},
 * {@link com.huawei.ascend.service.engine.spi.EnqueueEngineCancelRequest}, and
 * {@link com.huawei.ascend.service.engine.spi.EnqueueEngineStatus} are the carrier types.
 *
 * <p>Design authority:
 * {@code docs/architecture/l1/2026-05-30-l1--agent-service-engine-model-design.md}.
 *
 * <p>SPI purity (Rule R-D): imports only {@code java.*}, own siblings, and
 * {@code com.huawei.ascend.service.engine.model} (execution scope and input carriers).
 * Concrete adapters land in {@code com.huawei.ascend.service.engine.adapter}.
 */
package com.huawei.ascend.service.engine.spi;
