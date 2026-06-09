package com.huawei.ascend.runtime.engine;
import com.huawei.ascend.runtime.common.RuntimeIdentity;

import java.time.Instant;

/**
 * A command placed on the engine's internal command queue. Carries the intent
 * ({@code commandType}: EXECUTE / RESUME / CANCEL) plus the scope and input.
 * See engine model design §6.1.
 */
public class EngineCommandEvent {
    private String commandType;
    private RuntimeIdentity scope;
    private EngineInput input;
    private Instant createdAt;

    public EngineCommandEvent() {
    }

    public EngineCommandEvent(String commandType, RuntimeIdentity scope, EngineInput input, Instant createdAt) {
        this.commandType = commandType;
        this.scope = scope;
        this.input = input;
        this.createdAt = createdAt;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public RuntimeIdentity getScope() {
        return scope;
    }

    public void setScope(RuntimeIdentity scope) {
        this.scope = scope;
    }

    public EngineInput getInput() {
        return input;
    }

    public void setInput(EngineInput input) {
        this.input = input;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
