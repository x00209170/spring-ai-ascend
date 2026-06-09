package com.huawei.ascend.runtime.engine;
import com.huawei.ascend.runtime.common.RuntimeIdentity;


/**
 * Outbound port to the access layer. The engine streams output and terminal
 * signals back to the caller-facing channel through this client.
 */
public interface AccessLayerClient {

    void appendOutput(RuntimeIdentity scope, EngineEvent event);

    void completeOutput(RuntimeIdentity scope, EngineEvent event);

    void failOutput(RuntimeIdentity scope, EngineEvent event);

    void requestUserInput(RuntimeIdentity scope, EngineEvent event);
}
