package com.huawei.ascend.runtime.control;

import com.huawei.ascend.runtime.engine.EngineEvent;
import com.huawei.ascend.runtime.common.RuntimeIdentity;

/**
 * Outbound port from control to access: control calls this after verifying task
 * state transitions, so output never leaks without authoritative approval.
 * Defined in the control package; the access layer implements it.
 */
public interface ControlOutputPort {

    void writeOutput(RuntimeIdentity id, EngineEvent event);

    void writeCompleted(RuntimeIdentity id, EngineEvent event);

    void writeFailed(RuntimeIdentity id, String errorCode, String errorMessage);

    void writeInputRequired(RuntimeIdentity id, EngineEvent event);
}
