package com.huawei.ascend.runtime.engine.openjiuwen;

import com.huawei.ascend.runtime.engine.AgentExecutionContext;
import com.huawei.ascend.runtime.engine.spi.MemoryProvider;
import com.huawei.ascend.runtime.engine.spi.TrajectoryEmitter;
import com.huawei.ascend.runtime.engine.spi.TrajectoryEvent.Kind;
import com.openjiuwen.core.session.stream.StreamMode;
import com.openjiuwen.core.singleagent.rail.AgentRail;
import com.openjiuwen.harness.deep_agent.DeepAgent;
import com.openjiuwen.harness.rails.DeepAgentRail;
import com.openjiuwen.harness.rails.ExternalMemoryRail;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for OpenJiuwen DeepAgent runtime handlers.
 *
 * <p>DeepAgent owns its own session lifecycle in {@code stream(...)}, so this
 * handler calls the harness entrypoint directly instead of wrapping it with
 * OpenJiuwen {@code Runner.runAgentStreaming(...)}.
 *
 * <p>SkillHub installation uses the DeepAgent inner {@code ReActAgent}'s
 * existing skill runtime. Subclasses or factories that need SkillHub support
 * must configure that skill runtime while creating the DeepAgent.
 */
public abstract class OpenJiuwenDeepAgentRuntimeHandler extends AbstractOpenJiuwenRuntimeSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenJiuwenDeepAgentRuntimeHandler.class);

    private final ConcurrentMap<String, DeepAgent> runningAgents = new ConcurrentHashMap<>();
    private OpenJiuwenRemoteToolInstaller runtimeToolInstaller;
    private OpenJiuwenMcpToolInstaller mcpToolInstaller;
    private OpenJiuwenSkillHubInstaller skillHubInstaller;

    protected OpenJiuwenDeepAgentRuntimeHandler(String agentId) {
        super(agentId);
    }

    protected OpenJiuwenDeepAgentRuntimeHandler(String agentId, OpenJiuwenMessageAdapter messageConverter) {
        super(agentId, messageConverter);
    }

    OpenJiuwenDeepAgentRuntimeHandler(String agentId, OpenJiuwenMessageAdapter messageConverter,
            OpenJiuwenStreamAdapter resultMapper) {
        super(agentId, messageConverter, resultMapper);
    }

    @Override
    protected Set<Kind> supportedKinds() {
        return EnumSet.of(
                Kind.RUN_START, Kind.RUN_END,
                Kind.MODEL_CALL_START, Kind.MODEL_CALL_END,
                Kind.TOOL_CALL_START, Kind.TOOL_CALL_END,
                Kind.ERROR);
    }

    @Override
    protected final Stream<?> doExecute(AgentExecutionContext context, TrajectoryEmitter trajectory) {
        String taskId = context.getScope().taskId();
        DeepAgent agent = null;
        boolean registered = false;
        try {
            LOGGER.info("openjiuwen deepagent execute start tenantId={} sessionId={} taskId={} agentId={}",
                    context.getScope().tenantId(),
                    context.getScope().sessionId(),
                    taskId,
                    context.getScope().agentId());
            agent = Objects.requireNonNull(createOpenJiuwenDeepAgent(context), "openJiuwen deepAgent");
            runningAgents.put(taskId, agent);
            registered = true;

            installRails(agent, context);
            installDeepAgentRails(agent, context);
            installRuntimeTools(agent, context);
            if (trajectory != TrajectoryEmitter.NOOP) {
                agent.getAgent().registerRail(new OpenJiuwenTrajectoryRail(trajectory));
            }

            Map<String, Object> input = requireMap(toOpenJiuwenInput(context));
            Iterator<Object> iterator = runOpenJiuwenDeepAgentStreaming(
                    agent, input, openJiuwenConversationId(context), openJiuwenStreamModes(context));
            DeepAgent registeredAgent = agent;
            return flattenIterator(cleaningIterator(iterator, () -> runningAgents.remove(taskId, registeredAgent)))
                    .onClose(() -> runningAgents.remove(taskId, registeredAgent));
        } catch (RuntimeException error) {
            if (registered) {
                runningAgents.remove(taskId, agent);
            }
            return failedResult(context, trajectory, error);
        }
    }

    protected abstract DeepAgent createOpenJiuwenDeepAgent(AgentExecutionContext context);

    /**
     * Inner {@code ReActAgent} callback rails installed before DeepAgent streaming.
     *
     * <p>Rails returned here are registered on the inner callback bus. When a rail
     * is also a {@link DeepAgentRail}, it is initialized with the owning
     * {@link DeepAgent} before registration. Use this hook for rails that need
     * ReAct callback events such as {@link ExternalMemoryRail}.
     * Other {@link DeepAgentRail} subclasses are not registered on the inner
     * callback bus unless they are registered directly by the subclass.
     * Pure DeepAgent rails that do not need ReAct callbacks should prefer
     * {@link #openJiuwenDeepAgentRails(AgentExecutionContext)}.
     *
     * <p>Rails installed by this hook run before runtime remote, MCP, and SkillHub
     * tool installers, so tools registered by rail initialization take precedence
     * when the underlying OpenJiuwen harness skips later duplicate tool names.
     *
     * <p>Do not also place the same rail instance in {@code DeepAgentConfig.rails};
     * the native DeepAgent initialization path installs configured rails
     * separately. Duplicate detection is based on the same rail instance.
     */
    protected List<AgentRail> openJiuwenRails(AgentExecutionContext context) {
        return List.of();
    }

    /**
     * DeepAgent-level rails installed on the harness instance before streaming.
     *
     * <p>Rails returned here are initialized with the owning {@link DeepAgent}
     * and added to the DeepAgent registered rail list. They are not registered on
     * the inner ReActAgent callback bus. Use this hook for task-loop lifecycle
     * rails that are dispatched from the DeepAgent registered rail list. Use
     * {@link #openJiuwenRails(AgentExecutionContext)} for rails that need inner
     * ReActAgent callback events.
     *
     * <p>Native completion-policy wiring is still owned by
     * {@code DeepAgentConfig.rails}; a completion rail returned only from this
     * hook is not promoted into DeepAgent's private completion-policy field.
     * Avoid returning the same rail instance here and in {@code DeepAgentConfig.rails}.
     * Duplicate detection is based on the same rail instance.
     */
    protected List<DeepAgentRail> openJiuwenDeepAgentRails(AgentExecutionContext context) {
        return List.of();
    }

    protected void installRuntimeTools(DeepAgent agent, AgentExecutionContext context) {
        if (runtimeToolInstaller != null) {
            runtimeToolInstaller.install(agent, context);
        }
        if (mcpToolInstaller != null) {
            mcpToolInstaller.install(agent, context);
        }
        if (skillHubInstaller != null) {
            skillHubInstaller.install(agent, context);
        }
    }

    public final void setRuntimeToolInstaller(OpenJiuwenRemoteToolInstaller runtimeToolInstaller) {
        this.runtimeToolInstaller = runtimeToolInstaller;
    }

    public final void setMcpToolInstaller(OpenJiuwenMcpToolInstaller mcpToolInstaller) {
        this.mcpToolInstaller = mcpToolInstaller;
    }

    public final void setSkillHubInstaller(OpenJiuwenSkillHubInstaller skillHubInstaller) {
        this.skillHubInstaller = skillHubInstaller;
    }

    /**
     * Builds a memory rail for DeepAgent's inner {@code ReActAgent}.
     *
     * <p>The rail runs on each inner ReAct callback round, so search/save semantics
     * are tied to the ReAct prompt cycle rather than the outer DeepAgent task
     * lifecycle. A future DeepAgent-level rail should be used when memory must
     * align with task planning or task completion boundaries.
     *
     * <p>Do not install this rail together with
     * {@link #openJiuwenExternalMemoryRail(AgentExecutionContext, MemoryProvider)}
     * for the same execution. Both rails mutate the inner ReAct prompt state and
     * persist turns from the same callback cycle.
     */
    protected final OpenJiuwenAgentRuntimeHandler.MemoryRuntimeRail memoryRuntimeRail(
            AgentExecutionContext context,
            MemoryProvider memoryProvider) {
        return new OpenJiuwenAgentRuntimeHandler.MemoryRuntimeRail(
                context, memoryProvider, new OpenJiuwenMemoryMessageAdapter());
    }

    /**
     * Creates an OpenJiuwen-native external memory rail backed by the runtime
     * neutral {@link MemoryProvider}.
     *
     * <p>Return this rail from {@link #openJiuwenRails(AgentExecutionContext)}
     * so it is installed on the inner ReActAgent callback bus. During that
     * installation, this handler initializes it with the owning DeepAgent so the
     * harness rail can register memory tools and prompt sections on the
     * DeepAgent surface. Those memory tools are registered before runtime remote,
     * MCP, and SkillHub tools, so they take precedence for duplicate tool names.
     *
     * <p>Do not install this rail together with
     * {@link #memoryRuntimeRail(AgentExecutionContext, MemoryProvider)} for the
     * same execution. Both rails mutate the inner ReAct prompt state and persist
     * turns from the same callback cycle.
     */
    protected final AgentRail openJiuwenExternalMemoryRail(
            AgentExecutionContext context,
            MemoryProvider memoryProvider) {
        return new ExternalMemoryRail(
                new OpenJiuwenExternalMemoryProviderAdapter(context, memoryProvider),
                context.getScope().userId(),
                context.getAgentStateKey(),
                context.getScope().sessionId());
    }

    protected Iterator<Object> runOpenJiuwenDeepAgentStreaming(
            DeepAgent agent,
            Map<String, Object> input,
            String conversationId,
            List<StreamMode> streamModes) {
        input.putIfAbsent("conversation_id", conversationId);
        return agent.stream(input, null, streamModes);
    }

    protected List<StreamMode> openJiuwenStreamModes(AgentExecutionContext context) {
        return List.of(StreamMode.OUTPUT);
    }

    @Override
    public void cancel(String taskId) {
        DeepAgent agent = runningAgents.get(taskId);
        if (agent != null) {
            agent.requestAbort();
        }
    }

    int runningAgentCount() {
        return runningAgents.size();
    }

    private void installRails(DeepAgent agent, AgentExecutionContext context) {
        for (AgentRail rail : openJiuwenRails(context)) {
            if (rail != null) {
                if (rail instanceof DeepAgentRail deepAgentRail) {
                    registerDeepAgentRail(agent, deepAgentRail);
                }
                if (requiresInnerCallbackRegistration(rail)) {
                    agent.getAgent().registerRail(rail);
                }
            }
        }
    }

    private void installDeepAgentRails(DeepAgent agent, AgentExecutionContext context) {
        for (DeepAgentRail rail : openJiuwenDeepAgentRails(context)) {
            if (rail != null) {
                registerDeepAgentRail(agent, rail);
            }
        }
    }

    private static void registerDeepAgentRail(DeepAgent agent, DeepAgentRail rail) {
        if (containsSameInstance(agent.getRegisteredRails(), rail)) {
            return;
        }
        if (agent.getConfig().getRails() != null && containsSameInstance(agent.getConfig().getRails(), rail)) {
            return;
        }
        rail.init(agent);
        agent.getRegisteredRails().add(rail);
    }

    private static boolean containsSameInstance(List<?> rails, Object rail) {
        for (Object candidate : rails) {
            if (candidate == rail) {
                return true;
            }
        }
        return false;
    }

    private static boolean requiresInnerCallbackRegistration(AgentRail rail) {
        // ExternalMemoryRail is a DeepAgentRail that also needs inner ReAct callbacks.
        return !(rail instanceof DeepAgentRail) || rail instanceof ExternalMemoryRail;
    }

    private static Map<String, Object> requireMap(Object rawInput) {
        if (rawInput instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, value) -> normalized.put(String.valueOf(key), value));
            return normalized;
        }
        throw new IllegalArgumentException("OpenJiuwen DeepAgent input must be a Map<String,Object>");
    }

    private static Iterator<Object> cleaningIterator(Iterator<Object> delegate, Runnable cleanup) {
        Iterator<Object> safeDelegate = delegate != null ? delegate : List.of((Object) null).iterator();
        AtomicBoolean cleaned = new AtomicBoolean();
        Runnable cleanOnce = () -> {
            if (cleaned.compareAndSet(false, true)) {
                cleanup.run();
            }
        };
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                try {
                    boolean hasNext = safeDelegate.hasNext();
                    if (!hasNext) {
                        cleanOnce.run();
                    }
                    return hasNext;
                } catch (RuntimeException error) {
                    cleanOnce.run();
                    throw error;
                }
            }

            @Override
            public Object next() {
                try {
                    Object next = safeDelegate.next();
                    if (!safeDelegate.hasNext()) {
                        cleanOnce.run();
                    }
                    return next;
                } catch (RuntimeException error) {
                    cleanOnce.run();
                    throw error;
                }
            }
        };
    }
}
