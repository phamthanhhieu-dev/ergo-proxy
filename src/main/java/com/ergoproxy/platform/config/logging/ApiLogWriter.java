package com.ergoproxy.platform.config.logging;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.util.Messages;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@RequiredArgsConstructor
public class ApiLogWriter {

    @Value("${app.logging.api.max-body-size:16384}")
    private int maxBodySize;

    @Value("${app.logging.api.enabled:true}")
    private boolean enabled;

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
    private static final Pattern URL = Pattern.compile("https?://[^\\s\"<>]+", Pattern.CASE_INSENSITIVE);
    private final ObjectMapper objectMapper;

    public int limit() {
        int response = Math.max(1, Math.min(maxBodySize, 1024 * 1024));
        return response;
    }

    public boolean enabled() {
        boolean response = enabled;
        return response;
    }

    public void write(URI uri, String method, String request, String response, long started) {
        if (!enabled) {
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
            String response = Messages.get("api.log.body.omitted");
            return response;
        }
        if (truncated || bytes.length > limit()) {
            String response = Messages.get("api.log.body.too.large");
            return response;
        }
        if (bytes.length == 0) {
            String response = "{}";
            return response;
        }
        try {
            String response = objectMapper.writeValueAsString(mask(objectMapper.readTree(bytes)));
            return response;
        } catch (Exception ignored) {
            String response = Messages.get("api.log.body.json.invalid");
            return response;
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
                } catch (Exception ignored) {}
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
}
