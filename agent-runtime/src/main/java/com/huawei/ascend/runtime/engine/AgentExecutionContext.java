package com.huawei.ascend.runtime.engine;

import com.huawei.ascend.runtime.common.RuntimeIdentity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.a2aproject.sdk.spec.Message;
import org.springframework.util.Assert;

public class AgentExecutionContext {

    public static final String AGENT_STATE_KEY_VARIABLE = "agentStateKey";
    public static final String STATE_KEY_VARIABLE = "stateKey";

    private RuntimeIdentity scope;
    private String inputType;
    private List<Message> messages;
    private Map<String, Object> variables;
    private String agentStateKey;
    private Map<String, Object> agentState;

    public AgentExecutionContext() {}

    public AgentExecutionContext(RuntimeIdentity scope, String inputType,
                                  List<Message> messages, Map<String, Object> variables) {
        this(scope, inputType, messages, variables,
                resolveAgentStateKey(scope, variables), null);
    }

    public AgentExecutionContext(RuntimeIdentity scope, String inputType,
                                  List<Message> messages, Map<String, Object> variables,
                                  String agentStateKey, Map<String, Object> agentState) {
        this.scope = scope;
        this.inputType = inputType != null ? inputType : "USER_MESSAGE";
        this.messages = messages != null ? List.copyOf(messages) : List.of();
        this.variables = variables != null ? Map.copyOf(variables) : Map.of();
        Assert.hasText(agentStateKey, "agentStateKey must not be blank");
        this.agentStateKey = agentStateKey;
        setAgentState(agentState);
    }

    public RuntimeIdentity getScope() { return scope; }
    public void setScope(RuntimeIdentity scope) { this.scope = scope; }
    public String getInputType() { return inputType; }
    public List<Message> getMessages() { return messages; }
    public Map<String, Object> getVariables() { return variables; }
    public String getAgentStateKey() { return agentStateKey; }

    public void setAgentStateKey(String key) {
        Assert.hasText(key, "agentStateKey must not be blank");
        this.agentStateKey = key;
    }

    public Optional<Map<String, Object>> getAgentState() { return Optional.ofNullable(agentState); }

    public void setAgentState(Map<String, Object> state) {
        this.agentState = state == null ? null : Map.copyOf(state);
    }

    public Map<String, Object> replaceAgentState(Map<String, Object> values) {
        Map<String, Object> next = Map.copyOf(values);
        this.agentState = next;
        return next;
    }

    private static String resolveAgentStateKey(RuntimeIdentity scope, Map<String, Object> variables) {
        Object explicit = variables != null ? variables.get(AGENT_STATE_KEY_VARIABLE) : null;
        if (!(explicit instanceof String text) || text.isBlank())
            explicit = variables != null ? variables.get(STATE_KEY_VARIABLE) : null;
        if (explicit instanceof String text && !text.isBlank()) return text;
        if (scope == null) throw new IllegalArgumentException("agentStateKey must be provided");
        return scope.taskId();
    }
}
