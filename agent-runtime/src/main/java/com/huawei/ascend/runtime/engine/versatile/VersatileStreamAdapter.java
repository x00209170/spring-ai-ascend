package com.huawei.ascend.runtime.engine.versatile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ascend.runtime.engine.spi.AgentExecutionResult;
import com.huawei.ascend.runtime.engine.spi.StreamAdapter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts versatile SSE text lines → framework-neutral {@link AgentExecutionResult}.
 *
 * <h3>Event mapping</h3>
 * <table>
 *   <tr><th>SSE event</th><th>Condition</th><th>AgentExecutionResult</th><th>Target</th></tr>
 *   <tr><td>{@code message}</td><td>text / summary present</td><td>{@code output(text)}</td><td>USER</td></tr>
 *   <tr><td>{@code workflow_finished}</td><td>—</td><td>{@code completed(assembledContent)}</td><td>LLM</td></tr>
 *   <tr><td>{@code exception}</td><td>—</td><td>{@code failed(code, message)}</td><td>BOTH</td></tr>
 *   <tr><td>{@code end}</td><td>—</td><td>{@code completed(assembledContent)}</td><td>LLM</td></tr>
 *   <tr><td>{@code connection_closed}</td><td>no prior end node</td><td>{@code interrupted(prompt)}</td><td>USER</td></tr>
 *   <tr><td>{@code connection_closed}</td><td>end node already received</td><td>filtered</td><td>—</td></tr>
 *   <tr><td>any <em>unknown</em> event</td><td>data contains text</td><td>{@code output(rawLine)}</td><td>USER</td></tr>
 *   <tr><td>control events</td><td>workflow_started, node_started, node_finished</td><td>filtered</td><td>—</td></tr>
 * </table>
 *
 * <h3>Node-type caching</h3>
 * Every {@code message} event is cached keyed by its {@code node_type} field from the
 * SSE data payload. When a terminal event ({@code end}, {@code workflow_finished})
 * arrives the adapter assembles the final completion content from the cache:
 * <ul>
 *   <li>If {@code versatile.result-node-type} is configured, only cached entries
 *       matching that node type (case-insensitive) are merged.</li>
 *   <li>Otherwise all cached entries are merged in insertion order.</li>
 * </ul>
 *
 * <h3>Interruption detection</h3>
 * A {@code connection_closed} event injected by {@link VersatileClient} signals that
 * the HTTP response stream ended. When no {@code node_type=End} (case-insensitive)
 * was observed before the close, the adapter emits {@code INTERRUPTED} so the A2A
 * task transitions to {@code INPUT_REQUIRED} per the standard A2A protocol.
 *
 * <h3>Target routing</h3>
 * Intermediate output → {@code Target.USER} (transparent to end user).
 * Final completion → {@code Target.LLM} (fed back to the calling LLM as tool result).
 * Interruption → {@code Target.USER} (shown to end user as prompt).
 */
public class VersatileStreamAdapter implements StreamAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(VersatileStreamAdapter.class);

    static final String ERROR_CODE_PREFIX = "VERSATILE_";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Events that carry no user-visible content — always filtered. */
    private static final Set<String> CONTROL_EVENTS = Set.of(
            "workflow_started", "node_started", "node_finished");

    private final String resultNodeType;

    /** Backward-compatible no-arg constructor (resultNodeType = null → merge all). */
    public VersatileStreamAdapter() {
        this.resultNodeType = null;
    }

    public VersatileStreamAdapter(VersatileProperties properties) {
        this.resultNodeType = properties != null ? properties.getResultNodeType() : null;
    }

    @Override
    public Stream<AgentExecutionResult> adapt(Stream<?> rawResults) {
        final Map<String, List<String>> cache = new LinkedHashMap<>();
        final boolean[] hasEnd = {false};
        final boolean[] done = {false};
        final Iterator<?> it = rawResults.iterator();

        return Stream.generate(() -> {
            if (done[0]) return null;
            while (it.hasNext()) {
                Object raw = it.next();
                AgentExecutionResult result = mapLine(raw, cache, hasEnd);
                if (result != null) {
                    if (isTerminal(result.type())) {
                        done[0] = true;
                    }
                    return result;
                }
            }
            return null;
        }).takeWhile(Objects::nonNull);
    }

    private static boolean isTerminal(AgentExecutionResult.Type type) {
        return type == AgentExecutionResult.Type.COMPLETED
                || type == AgentExecutionResult.Type.FAILED
                || type == AgentExecutionResult.Type.INTERRUPTED;
    }

    // ── Per-line mapping ──

    private AgentExecutionResult mapLine(Object raw, Map<String, List<String>> cache, boolean[] hasEnd) {
        if (raw == null) return null;
        String line = String.valueOf(raw).trim();
        if (line.isEmpty()) return null;

        // Strip SSE "data:" prefix
        String jsonStr = line;
        if (line.startsWith("data:")) {
            jsonStr = line.substring(5).trim();
            if (jsonStr.isEmpty()) return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = (Map<String, Object>) MAPPER.readValue(jsonStr, Map.class);
            if (json == null) return null;

            String event = (String) json.getOrDefault("event", "");
            if (event.isEmpty()) return null;

            if (CONTROL_EVENTS.contains(event)) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) json.get("data");

            return switch (event) {
                case "message" -> handleMessage(data, cache, hasEnd);
                case "workflow_finished" -> handleWorkflowFinished(data, cache);
                case "exception" -> handleException(data);
                case "end" -> handleEnd(cache);
                case "connection_closed" -> handleConnectionClosed(hasEnd);
                default -> handleUnknownEvent(line, event, data);
            };
        } catch (Exception e) {
            LOG.warn("versatile sse parse skipped: {}",
                    line.length() > 120 ? line.substring(0, 120) + "..." : line);
            return null;
        }
    }

    // ── Known event handlers ──

    private AgentExecutionResult handleMessage(Map<String, Object> data,
            Map<String, List<String>> cache, boolean[] hasEnd) {
        if (data == null) return null;

        // Check node_type for End detection (case-insensitive)
        String nodeType = asString(data.get("node_type"));
        if (!nodeType.isEmpty() && "end".equalsIgnoreCase(nodeType)) {
            hasEnd[0] = true;
        }

        String text = extractMessageText(data);
        if (text.isEmpty()) return null;

        // Cache by node_type
        String cacheKey = nodeType.isEmpty() ? "__default__" : nodeType;
        cache.computeIfAbsent(cacheKey, k -> new ArrayList<>()).add(text);

        return AgentExecutionResult.output(text, AgentExecutionResult.Target.USER);
    }

    private AgentExecutionResult handleWorkflowFinished(Map<String, Object> data,
            Map<String, List<String>> cache) {
        String content = extractWorkflowContent(data);
        // Merge with cache — workflow_finished may carry additional outputs
        String finalContent = assembleFinalContent(cache, content);
        return AgentExecutionResult.completed(finalContent, AgentExecutionResult.Target.LLM);
    }

    private AgentExecutionResult handleEnd(Map<String, List<String>> cache) {
        String finalContent = assembleFinalContent(cache, null);
        return AgentExecutionResult.completed(finalContent, AgentExecutionResult.Target.LLM);
    }

    private AgentExecutionResult handleException(Map<String, Object> data) {
        String code = data != null ? asString(data.get("code")) : "";
        String message = data != null ? asString(data.get("message")) : "";
        if (code.isEmpty()) code = "UNKNOWN";
        return AgentExecutionResult.failed(ERROR_CODE_PREFIX + code, message);
    }

    private AgentExecutionResult handleConnectionClosed(boolean[] hasEnd) {
        if (hasEnd[0]) {
            // Normal close after End — nothing extra to emit
            LOG.debug("versatile connection_closed after End — stream complete");
            return null;
        }
        // Stream closed without End → interruption
        LOG.info("versatile connection_closed without End — emitting INTERRUPTED");
        return AgentExecutionResult.interrupted(
                "Versatile connection closed before completion. Send a follow-up message to resume.",
                AgentExecutionResult.Target.USER);
    }

    // ── Unknown event → raw passthrough ──

    private AgentExecutionResult handleUnknownEvent(String rawLine, String event, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            LOG.debug("versatile unknown event '{}' with empty data — filtering", event);
            return null;
        }
        LOG.info("versatile passthrough event '{}' chars={}", event, rawLine.length());
        return AgentExecutionResult.output(rawLine, AgentExecutionResult.Target.USER);
    }

    // ── Caching and final content assembly ──

    /**
     * Assembles the final completion content from the cache, optionally merged
     * with an additional content string from workflow_finished outputs.
     *
     * @param cache node_type → collected text lines
     * @param extraContent additional content from workflow_finished outputs (may be empty/null)
     */
    private String assembleFinalContent(Map<String, List<String>> cache, String extraContent) {
        List<String> selectedLines;

        if (resultNodeType != null && !resultNodeType.isBlank()) {
            // Filter cache to matching node_type (case-insensitive)
            selectedLines = new ArrayList<>();
            for (var entry : cache.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(resultNodeType)) {
                    selectedLines.addAll(entry.getValue());
                }
            }
        } else {
            // Merge all cached content in insertion order
            selectedLines = new ArrayList<>();
            for (List<String> lines : cache.values()) {
                selectedLines.addAll(lines);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String line : selectedLines) {
            if (!sb.isEmpty()) sb.append('\n');
            sb.append(line);
        }
        if (extraContent != null && !extraContent.isBlank()) {
            if (!sb.isEmpty()) sb.append('\n');
            sb.append(extraContent);
        }
        return sb.toString();
    }

    // ── Extraction helpers ──

    private static String extractMessageText(Map<String, Object> data) {
        Boolean isFinished = data.get("is_finished") instanceof Boolean b ? b : false;
        String summary = asString(data.get("summary"));
        String text = asString(data.get("text"));
        if (isFinished && !summary.isEmpty()) return summary;
        if (!text.isEmpty()) return text;
        if (!summary.isEmpty()) return summary;
        return "";
    }

    private static String extractWorkflowContent(Map<String, Object> data) {
        if (data == null) return "";
        @SuppressWarnings("unchecked")
        Map<String, Object> outputs = (Map<String, Object>) data.get("outputs");
        if (outputs != null) {
            String content = asString(outputs.get("responseContent"));
            if (!content.isEmpty()) return content;
        }
        return "";
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
