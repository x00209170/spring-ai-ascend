package com.huawei.ascend.examples.deepresearch.mcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MCP tool that GETs a URL and returns the response body as text. The tool
 * caps the response at {@link #MAX_BODY_BYTES} to keep the JSON-RPC payload
 * sized for an LLM context, and surfaces all errors (network failure, non-2xx
 * response, malformed URL) through the MCP {@code isError} channel rather than
 * the JSON-RPC error envelope — that's the contract the agent-runtime adapter
 * maps to a tool-level failure the agent can recover from.
 *
 * <p>SSRF hardening: DNS is resolved once, all addresses are validated, and
 * the TCP connection is opened directly to a validated IP — eliminating the
 * TOCTOU DNS rebinding window that exists when an HTTP client re-resolves.</p>
 */
@Component
public class FetchUrlTool {
    private static final Logger LOG = LoggerFactory.getLogger(FetchUrlTool.class);
    private static final int MAX_BODY_BYTES = 16_384;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    public String name() {
        return "fetch_url";
    }

    public Map<String, Object> describe() {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", name());
        tool.put("description", "Fetch a URL and return the response body as text (truncated to "
                + MAX_BODY_BYTES + " bytes).");
        tool.put("inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                        "url", Map.of("type", "string", "description", "Absolute URL to fetch via HTTP GET.")),
                "required", List.of("url")));
        return tool;
    }

    public Map<String, Object> call(Map<String, Object> arguments) {
        String url = arguments == null ? "" : String.valueOf(arguments.getOrDefault("url", ""));
        if (url.isBlank()) {
            return errorResult("Missing required argument: url");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException error) {
            return errorResult("Malformed url: " + error.getMessage());
        }
        if (uri.getScheme() == null || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
            return errorResult("Only http and https schemes are supported");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return errorResult("URL must contain a host");
        }

        int port = uri.getPort();
        if (port == -1) {
            port = uri.getScheme().equals("https") ? 443 : 80;
        }

        // Resolve and validate ALL IP addresses once (prevents DNS rebinding TOCTOU)
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (Exception e) {
            LOG.warn("DNS resolution failed for host={} errorClass={} message={}",
                    host, e.getClass().getSimpleName(), e.getMessage());
            return errorResult("Unable to resolve host: " + host);
        }
        for (InetAddress addr : addresses) {
            if (addr.isLoopbackAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress()) {
                return errorResult("URL resolves to a blocked address: " + addr.getHostAddress());
            }
            byte[] octets = addr.getAddress();
            if (octets != null && octets.length == 4
                    && octets[0] == (byte) 169 && octets[1] == (byte) 254) {
                return errorResult("URL resolves to a blocked address: " + addr.getHostAddress());
            }
        }

        // Connect directly to the FIRST validated IP — no re-resolution
        InetAddress target = addresses[0];
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        String query = uri.getRawQuery();
        if (query != null) {
            path = path + "?" + query;
        }

        long started = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.setSoTimeout((int) READ_TIMEOUT.toMillis());
            socket.connect(new InetSocketAddress(target, port), (int) CONNECT_TIMEOUT.toMillis());

            // Build and send the HTTP request
            StringBuilder request = new StringBuilder();
            request.append("GET ").append(path).append(" HTTP/1.1\r\n");
            request.append("Host: ").append(host);
            if (port != 80 && port != 443) {
                request.append(":").append(port);
            }
            request.append("\r\n");
            request.append("User-Agent: FetchUrlTool/1.0\r\n");
            request.append("Accept: text/html, text/plain, */*\r\n");
            request.append("Connection: close\r\n");
            request.append("\r\n");

            OutputStream out = socket.getOutputStream();
            out.write(request.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Read response
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String statusLine = reader.readLine();
            if (statusLine == null) {
                return errorResult("Empty response from " + host);
            }
            int statusCode = parseStatusCode(statusLine);
            if (statusCode < 0) {
                return errorResult("Malformed HTTP response from " + host + ": " + statusLine);
            }

            // Skip headers
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // consume headers
            }

            // Read body up to MAX_BODY_BYTES + 1 to detect truncation
            StringBuilder body = new StringBuilder();
            int ch;
            int totalBytes = 0;
            while ((ch = reader.read()) != -1) {
                if (totalBytes < MAX_BODY_BYTES) {
                    body.append((char) ch);
                }
                totalBytes++;
            }
            String truncated = body.toString();

            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            LOG.info("fetch_url done url={} status={} bytes={} latencyMs={}",
                    url, statusCode, totalBytes, elapsedMs);

            if (statusCode < 200 || statusCode >= 300) {
                return errorResult("HTTP " + statusCode + " from " + url);
            }
            return successResult(truncated, statusCode, totalBytes, elapsedMs);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return errorResult("Interrupted while fetching " + url);
        } catch (Exception error) {
            LOG.warn("fetch_url failed url={} errorClass={} message={}",
                    url, error.getClass().getSimpleName(), error.getMessage());
            return errorResult(error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private static int parseStatusCode(String statusLine) {
        // "HTTP/1.1 200 OK" -> 200
        String[] parts = statusLine.split(" ", 3);
        if (parts.length < 2) {
            return -1;
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Map<String, Object> successResult(String text, int status, int totalBytes, long latencyMs) {
        return Map.of(
                "content", List.of(Map.of("type", "text", "text", text)),
                "structuredContent", Map.of(
                        "status", status,
                        "totalBytes", totalBytes,
                        "latencyMs", latencyMs),
                "isError", false);
    }

    private static Map<String, Object> errorResult(String message) {
        return Map.of(
                "content", List.of(Map.of("type", "text", "text", message)),
                "isError", true);
    }
}
