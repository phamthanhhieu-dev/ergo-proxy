package com.ergoproxy.platform.common.config.logging;

import com.ergoproxy.platform.common.config.configmapping.LogProperties;
import com.ergoproxy.platform.common.config.configmapping.OpenSubtitleProperties;
import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.util.Messages;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ApiLoggingWriter {

    private static final Set<String> SECRETS = Set.of(
        Const.Common.PASSWORD,
        "passwordhash",
        Const.Common.TOKEN,
        "accesstoken",
        "refreshtoken",
        "authorization",
        "apikey",
        "secret",
        "cookie",
        "configuredtoken",
        "credentialref",
        "privatekey",
        Const.Common.USERNAME
    );
    private static final Pattern URL = Pattern.compile(Const.Regex.DIAGNOSTIC_URL, Pattern.CASE_INSENSITIVE);
    private final ObjectMapper objectMapper;
    private final LogProperties logProperties;
    private static final Pattern SECRET_VALUE = Pattern.compile(
        "(?i)(\"(?:password|passwordhash|token|access[-_]?token|refresh[-_]?token|authorization|api[-_]?key|secret|cookie|configuredtoken|credentialref|privatekey|username)\"\\s*:\\s*)\"(?:\\\\.|[^\"\\\\])*(?:\"|$)"
    );

    public int limit() {
        int response = Math.max(1, Math.min(logProperties.getApiMaxBodySize(), 1024 * 1024));
        return response;
    }

    public boolean enabled() {
        boolean response = logProperties.getApiEnabled();
        return response;
    }

    public void write(URI uri, String method, String request, String response, long started) {
        if (!logProperties.getApiEnabled()) {
            return;
        }
        try {
            LoggerFactory.getLogger(Const.Common.API_LOGGER).atInfo()
                .log(
                    "{} {}\nRequest: {}\nResponse: {}\nDuration: {} ms",
                    method,
                    safeUrl(uri),
                    request,
                    response,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
                );
        } catch (RuntimeException ignored) {
            // Logging must never interrupt a request or a file download.
        }
    }

    public String body(byte[] bytes, String contentType, boolean truncated) {
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains(Const.Common.JSON)) {
            return Messages.get("api.log.body.omitted");
        }

        if (bytes.length == 0) {
            return "{}";
        }

        int length = Math.min(bytes.length, limit());
        boolean partial = truncated || bytes.length > limit();

        if (partial) {
            String response = new String(bytes, 0, length, java.nio.charset.StandardCharsets.UTF_8);
            response = SECRET_VALUE.matcher(response).replaceAll("$1\"***\"");
            response = response.replaceAll("(?i)bearer\\s+[^\\s,;]+", "Bearer ***");
            response = URL.matcher(response).replaceAll(match -> {
                try {
                    return Matcher.quoteReplacement(safeUrl(URI.create(match.group())));
                } catch (RuntimeException ignored) {
                    return "***";
                }
            });
            return response + " ... [TRUNCATED]";
        }

        try {
            return objectMapper.writeValueAsString(mask(objectMapper.readTree(bytes)));
        } catch (Exception ignored) {
            return Messages.get("api.log.body.json.invalid");
        }
    }

    public String parameters(Map<String, String[]> parameters) {
        try {
            String json = objectMapper.writeValueAsString(mask(objectMapper.valueToTree(parameters)));
            String response = json.length() > limit() ? Messages.get("api.log.body.too.large") : json;
            return response;
        } catch (Exception ignored) {
            String response = "{}";
            return response;
        }
    }

    private JsonNode mask(JsonNode node) {
        if (node instanceof ObjectNode object) {
            object.properties().forEach(entry -> {
                String field = entry.getKey().toLowerCase(Locale.ROOT).replaceAll("[-_]", Const.Common.EMPTY);
                object.set(entry.getKey(), SECRETS.contains(field) ? objectMapper.getNodeFactory().textNode("***") : mask(entry.getValue()));
            });
            return object;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                ((tools.jackson.databind.node.ArrayNode) node).set(i, mask(node.get(i)));
            }
            return node;
        }
        if (node.isTextual()) {
            String value = node.asText();
            // Native download forms carry a JSON payload inside a string.
            if (value.stripLeading().startsWith("{")) {
                try {
                    JsonNode response = mask(objectMapper.readTree(value));
                    return response;
                } catch (Exception ignored) {
                }
            }
            String safe = URL.matcher(value).replaceAll(match -> {
                try {
                    String response = Matcher.quoteReplacement(safeUrl(URI.create(match.group())));
                    return response;
                } catch (RuntimeException ignored) {
                    String response = "***";
                    return response;
                }
            });
            safe = safe.replaceAll("(?i)bearer\\s+[^\\s,;]+", "Bearer ***");
            JsonNode response = objectMapper.getNodeFactory().textNode(safe);
            return response;
        }
        return node;
    }

    private String safeUrl(URI uri) {
        String response = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() < 0 ? Const.Common.EMPTY : ":" + uri.getPort()) + uri.getRawPath();
        return response;
    }

    public void writeRequest(URI uri, String method, String request) {
        if (!logProperties.getApiEnabled()) {
            return;
        }
        try {
            LoggerFactory.getLogger(Const.Common.API_LOGGER).atInfo()
                .log("{} {}\nRequest: {}", method, safeUrl(uri), request);
        } catch (RuntimeException ignored) {
            // Logging must never interrupt a request.
        }
    }

    public void writeResponse(URI uri, int status, String response, long started) {
        if (!logProperties.getApiEnabled()) {
            return;
        }
        try {
            LoggerFactory.getLogger(Const.Common.API_LOGGER).atInfo()
                .log("{} {}\nResponse: {}\nDuration: {} ms",
                    status, safeUrl(uri), response,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        } catch (RuntimeException ignored) {
            // Logging must never interrupt a request.
        }
    }
}
