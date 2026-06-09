package com.huawei.ascend.runtime.boot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Flow;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.reactivestreams.FlowAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * A2A JSON-RPC controller — parallels {@code agentscope-runtime-java}'s
 * {@code A2aController + JSONRPCHandler} pattern: parse the JSON-RPC
 * envelope, dispatch to the SDK's {@link RequestHandler}, and convert
 * streaming results to SSE.
 */
@RestController
public class A2aController {

    private static final Logger LOG = LoggerFactory.getLogger(A2aController.class);

    private final ObjectProvider<RequestHandler> handlerProvider;
    private final ObjectMapper objectMapper;

    public A2aController(ObjectProvider<RequestHandler> handlerProvider, ObjectMapper objectMapper) {
        this.handlerProvider = handlerProvider;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = {"/a2a", "/a2a/"})
    public Object handle(@RequestBody String body) {
        RequestHandler handler = handlerProvider.getIfAvailable();
        if (handler == null) {
            return ResponseEntity.status(503)
                    .body(errorJson(null, "No agent executor available"));
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String method = method(root);
            Object id = id(root);
            LOG.info("[A2A] method={} id={}", method, id);
            if (method == null) return errorJson(id, "Missing method");
            return dispatch(handler, root, method, id);
        } catch (Exception e) {
            LOG.error("[A2A] dispatch error", e);
            return ResponseEntity.ok(errorJson(null, e.getMessage()));
        }
    }

    @GetMapping(value = {"/a2a", "/a2a/"}, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public org.springframework.http.ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"ok\"}");
    }

    private Object dispatch(RequestHandler handler, JsonNode root, String method, Object id) {
        JsonNode params = root.get("params");
        ServerCallContext sctx = serverCallContext();
        try {
            if (isStreamMethod(method)) {
                return handleStream(handler, params, id, method, sctx);
            }
            return handleNonStream(handler, params, id, method, sctx);

        } catch (A2AError e) {
            LOG.warn("[A2A] a2a error method={} code={}", method, e.getMessage());
            return ResponseEntity.ok(errorJson(id, e.getMessage()));
        } catch (IOException e) {
            LOG.error("[A2A] serialization error method={}", method, e);
            return ResponseEntity.ok(errorJson(id, e.getMessage()));
        }
    }

    // ── Streaming (SSE) ──

    private SseEmitter handleStream(RequestHandler handler, JsonNode params, Object id,
                                     String method, ServerCallContext sctx) throws IOException {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            Flow.Publisher<StreamingEventKind> publisher = switch (method) {
                case "message/stream", "SendStreamingMessage" -> {
                    MessageSendParams p = objectMapper.treeToValue(params, MessageSendParams.class);
                    LOG.info("[A2A] stream taskId={} contextId={}",
                            p.message() != null ? p.message().taskId() : null,
                            p.message() != null ? p.message().contextId() : null);
                    yield handler.onMessageSendStream(p, sctx);
                }
                case "tasks/resubscribe", "SubscribeToTask" -> {
                    TaskIdParams p = new TaskIdParams(params.get("id").asText(),
                            params.has("tenant") ? params.get("tenant").asText() : null);
                    LOG.info("[A2A] resubscribe taskId={}", p.id());
                    yield handler.onSubscribeToTask(p, sctx);
                }
                default -> throw new IllegalArgumentException("Unknown stream method: " + method);
            };

            Flux.from(FlowAdapters.toPublisher(publisher))
                    .doOnSubscribe(s -> LOG.info("[A2A] sse-connected method={} id={}", method, id))
                    .subscribe(
                            event -> emitSse(emitter, id, event),
                            error -> { emitSseError(emitter, id, error); emitter.complete(); },
                            () -> { LOG.info("[A2A] sse-complete method={} id={}", method, id); emitter.complete(); });

        } catch (Exception e) {
            LOG.error("[A2A] stream-setup-error method={}", method, e);
            emitSseError(emitter, id, e);
            emitter.complete();
        }
        return emitter;
    }

    // ── Non-streaming (JSON-RPC response) ──

    private ResponseEntity<String> handleNonStream(RequestHandler handler, JsonNode params,
                                                    Object id, String method, ServerCallContext sctx)
            throws A2AError, IOException {
        Object result = switch (method) {
            case "message/send", "SendMessage" -> {
                MessageSendParams p = objectMapper.treeToValue(params, MessageSendParams.class);
                LOG.info("[A2A] send taskId={}", p.message() != null ? p.message().taskId() : null);
                yield handler.onMessageSend(p, sctx);
            }
            case "tasks/get", "GetTask" -> {
                TaskQueryParams p = new TaskQueryParams(params.get("id").asText());
                LOG.info("[A2A] getTask taskId={}", p.id());
                yield handler.onGetTask(p, sctx);
            }
            case "tasks/cancel", "CancelTask" -> {
                CancelTaskParams p = new CancelTaskParams(params.get("id").asText());
                LOG.info("[A2A] cancelTask taskId={}", p.id());
                yield handler.onCancelTask(p, sctx);
            }
            default -> throw new IllegalArgumentException("Unknown method: " + method);
        };

        return ResponseEntity.ok(toJsonRpc(id, result));
    }

    // ── SSE helpers ──

    private void emitSse(SseEmitter emitter, Object id, StreamingEventKind event) {
        try {
            Map<String, Object> frame = new HashMap<>();
            frame.put("jsonrpc", "2.0"); frame.put("id", id);
            frame.put("result", event);
            emitter.send(ServerSentEvent.builder().event("jsonrpc")
                    .data(objectMapper.writeValueAsString(frame)).build());
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void emitSseError(SseEmitter emitter, Object id, Throwable error) {
        try {
            String message = error instanceof A2AError ? ((A2AError) error).getMessage() : error.getMessage();
            Map<String, Object> frame = new HashMap<>();
            frame.put("jsonrpc", "2.0"); frame.put("id", id);
            frame.put("result", Map.of("error", Map.of("code", -32603, "message",
                    message != null ? message : error.getClass().getSimpleName())));
            emitter.send(ServerSentEvent.builder().event("error")
                    .data(objectMapper.writeValueAsString(frame)).build());
        } catch (IOException ignored) {}
    }

    // ── JSON-RPC envelope ──

    private String toJsonRpc(Object id, Object result) throws IOException {
        Map<String, Object> frame = new HashMap<>();
        frame.put("jsonrpc", "2.0"); frame.put("id", id); frame.put("result", result);
        return objectMapper.writeValueAsString(frame);
    }

    private String errorJson(Object id, String message) {
        try {
            Map<String, Object> err = new HashMap<>(); err.put("code", -32603); err.put("message", message);
            Map<String, Object> frame = new HashMap<>();
            frame.put("jsonrpc", "2.0"); frame.put("id", id); frame.put("error", err);
            return objectMapper.writeValueAsString(frame);
        } catch (Exception e) {
            return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603}}";
        }
    }

    private static boolean isStreamMethod(String m) {
        return "message/stream".equals(m) || "SendStreamingMessage".equals(m)
                || "tasks/resubscribe".equals(m) || "SubscribeToTask".equals(m);
    }

    private static String method(JsonNode r) { JsonNode m = r.get("method"); return m == null ? null : m.asText(); }
    private static Object id(JsonNode r) { JsonNode i = r.get("id"); return i == null || i.isNull() ? null : i.isTextual() ? i.asText() : i.asInt(); }
    private static ServerCallContext serverCallContext() { return new ServerCallContext(null, new HashMap<>(), java.util.Set.of()); }
}
