/**
 * Engine SPI surfaces.
 *
 * <p>Contains two SPI families:
 * <ul>
 *   <li><b>StatelessEngine SPI</b> — pure-function compute surface for the
 *       Execution Engine Adapter component (5-component decomposition of
 *       agent-service). The {@link com.huawei.ascend.service.engine.spi.StatelessEngine}
 *       interface is the contract; {@link com.huawei.ascend.service.engine.spi.AgentInvokeRequest}
 *       and {@link com.huawei.ascend.service.engine.spi.StateDelta} are the carrier records.
 *       Wire contract: {@code docs/contracts/agent-invoke-request.v1.yaml} (status:
 *       design_only; reference impl in rc24).</li>
 *   <li><b>EngineDispatchSpi</b> — async dispatch entry point for task-centric-control
 *       to enqueue Agent execution requests. The {@link com.huawei.ascend.service.engine.spi.EngineDispatchSpi}
 *       interface is the contract; {@link com.huawei.ascend.service.engine.spi.EnqueueEngineExecutionRequest},
 *       {@link com.huawei.ascend.service.engine.spi.EnqueueEngineResumeRequest},
 *       {@link com.huawei.ascend.service.engine.spi.EnqueueEngineCancelRequest}, and
 *       {@link com.huawei.ascend.service.engine.spi.EnqueueEngineStatus} are the carrier types.
 *       Design authority: {@code docs/architecture/l1/2026-05-30-l1--agent-service-engine-model-design.md}.</li>
 * </ul>
 *
 * <p>SPI purity (Rule R-D): imports only {@code java.*}, own siblings, and
 * {@code com.huawei.ascend.service.engine.model} (execution scope and input carriers).
 * Concrete adapters land in {@code com.huawei.ascend.service.engine.adapter}.
 */
package com.huawei.ascend.service.engine.spi;
