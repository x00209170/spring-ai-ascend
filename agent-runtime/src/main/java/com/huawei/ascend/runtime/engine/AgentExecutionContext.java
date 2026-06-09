package com.huawei.ascend.runtime.engine;

import com.huawei.ascend.runtime.common.Guards;
import com.huawei.ascend.runtime.common.RuntimeIdentity;
import java.util.Map;
import java.util.Optional;

/**
 * The context handed to an {@code AgentRuntimeHandler} for a single execution:
 * the scope, input, and optional framework-neutral Agent state owned by the
 * runtime.
 */
public class AgentExecutionContext {
    public static final String AGENT_STATE_KEY_VARIABLE = "agentStateKey";
    public static final String STATE_KEY_VARIABLE = "stateKey";

    private RuntimeIdentity scope;
    private EngineInput input;
    private String agentStateKey;
    private Map<String, Object> agentState;

    public AgentExecutionContext() {
    }

    public AgentExecutionContext(RuntimeIdentity scope, EngineInput input) {
        this(scope, input, resolveAgentStateKey(scope, input), null);
    }

    public AgentExecutionContext(
            RuntimeIdentity scope, EngineInput input, String agentStateKey, Map<String, Object> agentState) {
        this.scope = scope;
        this.input = input;
        this.agentStateKey = Guards.requireNonBlank(agentStateKey, "agentStateKey");
        setAgentState(agentState);
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

    public String getAgentStateKey() {
        return agentStateKey;
    }

    public void setAgentStateKey(String agentStateKey) {
        this.agentStateKey = Guards.requireNonBlank(agentStateKey, "agentStateKey");
    }

    public Optional<Map<String, Object>> getAgentState() {
        return Optional.ofNullable(agentState);
    }

    public void setAgentState(Map<String, Object> agentState) {
        this.agentState = agentState == null ? null : Map.copyOf(agentState);
    }

    public Map<String, Object> replaceAgentState(Map<String, Object> values) {
        Map<String, Object> next = Map.copyOf(values);
        this.agentState = next;
        return next;
    }

    private static String resolveAgentStateKey(RuntimeIdentity scope, EngineInput input) {
        Object explicit = input == null ? null : input.variables().get(AGENT_STATE_KEY_VARIABLE);
        if (!(explicit instanceof String text) || text.isBlank()) {
            explicit = input == null ? null : input.variables().get(STATE_KEY_VARIABLE);
        }
        if (explicit instanceof String text && !text.isBlank()) {
            return text;
        }
        if (scope == null) {
            throw new IllegalArgumentException("agentStateKey must be provided when scope is null");
        }
        return scope.taskId();
    }
}
