package com.huawei.ascend.runtime.engine.a2a;

import com.huawei.ascend.runtime.engine.AgentExecutionContext;
import com.huawei.ascend.runtime.engine.EngineInput;
import com.huawei.ascend.runtime.common.Message;
import com.huawei.ascend.runtime.engine.spi.AgentExecutionResult;
import com.huawei.ascend.runtime.engine.spi.AgentRuntimeHandler;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges A2A SDK {@link AgentExecutor} to our {@link AgentRuntimeHandler} SPI.
 * Each framework adapter (openjiuwen, agentscope, etc.) supplies a handler;
 * this class handles the A2A protocol concerns — context mapping, result routing,
 * agent-card discovery — so framework adapters focus only on agent execution.
 */
public final class A2aAgentExecutor implements AgentExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2aAgentExecutor.class);

    private final AgentRuntimeHandler handler;

    public A2aAgentExecutor(AgentRuntimeHandler handler) {
        this.handler = handler;
    }

    @Override
    public void execute(RequestContext ctx, AgentEmitter emitter) throws A2AError {
        LOGGER.info("a2a execute start taskId={} agentId={}", ctx.getTaskId(), handler.agentId());
        emitter.startWork();

        try (Stream<?> raw = handler.execute(toExecutionContext(ctx));
             Stream<AgentExecutionResult> results = handler.resultAdapter().adapt(raw)) {
            results.forEach(result -> route(result, emitter));
        } catch (Exception e) {
            LOGGER.error("a2a execute failed taskId={}", ctx.getTaskId(), e);
            emitter.fail();
        }
    }

    @Override
    public void cancel(RequestContext ctx, AgentEmitter emitter) {
        emitter.cancel();
    }

    private void route(AgentExecutionResult result, AgentEmitter emitter) {
        switch (result.type()) {
            case OUTPUT -> {
                if (result.output() != null && result.output().getContent() != null) {
                    emitter.sendMessage(result.output().getContent());
                }
            }
            case COMPLETED -> emitter.complete();
            case FAILED -> {
                String code = result.errorCode() == null ? "RUNTIME_ERROR" : result.errorCode();
                String msg = result.errorMessage() == null ? code : result.errorMessage();
                emitter.fail();
            }
            case INTERRUPTED -> {
                String prompt = result.prompt() == null ? "" : result.prompt();
                emitter.sendMessage(prompt);
                emitter.requiresInput();
            }
        }
    }

    private AgentExecutionContext toExecutionContext(RequestContext ctx) {
        String text = extractText(ctx);
        List<Message> messages = List.of(Message.user(text));
        return new AgentExecutionContext(
                new com.huawei.ascend.runtime.common.RuntimeIdentity(
                        ctx.getTenant() != null ? ctx.getTenant() : "default",
                        metadata(ctx, "userId", "system"),
                        metadata(ctx, "agentId", handler.agentId()),
                        ctx.getContextId() != null ? ctx.getContextId() : ctx.getTaskId(),
                        ctx.getTaskId()),
                new EngineInput("USER_MESSAGE", messages, Map.of()));
    }

    private String extractText(RequestContext ctx) {
        if (ctx.getMessage() == null || ctx.getMessage().parts() == null) return "";
        return ctx.getMessage().parts().stream()
                .filter(TextPart.class::isInstance)
                .map(TextPart.class::cast)
                .map(TextPart::text)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private static String metadata(RequestContext ctx, String key, String fallback) {
        Map<String, Object> md = ctx.getMetadata();
        Object value = md == null ? null : md.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }
}
