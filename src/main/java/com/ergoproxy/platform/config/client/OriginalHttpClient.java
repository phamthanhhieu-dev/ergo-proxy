package com.ergoproxy.platform.config.client;

import com.ergoproxy.platform.config.logging.ApiLogWriter;
import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.exception.LogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class OriginalHttpClient {

    private final ObjectMapper mapper;
    private final ApiLogWriter apiLogWriter;
    @Value("${flixcloud.api.url}")
    private String flixcloudApiUrl;

    public static void validate(URI uri) {
        String host = String.valueOf(uri.getHost()).toLowerCase(Locale.ROOT);
        if (
            !Const.Common.HTTPS.equalsIgnoreCase(uri.getScheme()) ||
                uri.getUserInfo() != null ||
                (uri.getPort() != -1 && uri.getPort() != 443) ||
                !(host.equals(Const.Common.REANIME_HOST) || host.equals(Const.Common.FLIXCLOUD_HOST) || fileHost(host))
        ) {
            throw new LogicException("VOHC_00001", "original.host.invalid", host);
        }
    }

    public static boolean fileHost(String host) {
        boolean response = host != null && (host.matches("fetch[0-9]+\\.flixcloud\\.cc") || host.equals("dl.spacedl.top"));
        return response;
    }

    public static void validateRedirect(URI current, URI next) throws IOException {
        validate(next);
        if (fileHost(current.getHost()) && !fileHost(next.getHost())) {
            throw new LogicException("VROHC_00001", "original.redirect.host.invalid", next.getHost());
        }
    }

    public Body open(URI initial) throws IOException {
        URI current = initial;
        for (int hop = 0; hop < 5; hop++) {
            validate(current);
            HttpURLConnection connection = (HttpURLConnection) current.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(90000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0" + " Safari/537.36");
            connection.setRequestProperty("Referer", flixcloudApiUrl + Const.Common.SLASH);
            connection.setRequestProperty("Accept", "*/*");
            long started = System.nanoTime();
            int status;
            try {
                status = connection.getResponseCode();
            } catch (IOException ex) {
                connection.disconnect();
                if (apiLogWriter != null) {
                    apiLogWriter.write(current, Const.Common.GET, "{}", ex.getClass().getSimpleName(), started);
                }

                throw new LogicException(ex, "OOHC_00001", "original.connection.failed", ex.getClass().getSimpleName());
            }

            if (apiLogWriter != null && status != 200) {
                apiLogWriter.write(current, Const.Common.GET, "{}", String.valueOf(status), started);
            }
            URI loggedUri = current;
            if (status >= 300 && status < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null) {
                    throw new LogicException("OOHC_00002", "original.redirect.location.is.null.or.empty");
                }
                URI next = current.resolve(location);
                validateRedirect(current, next);
                // Log hosts only; redirect URLs contain short-lived download credentials.

                current = next;
                continue;
            }
            if (status != 200) {
                connection.disconnect();
                throw new LogicException("OOHC_00003", "original.http.failed", status);
            }
            java.io.ByteArrayOutputStream prefix = new java.io.ByteArrayOutputStream();
            String responseType = connection.getContentType();
            InputStream input = new FilterInputStream(connection.getInputStream()) {
                private boolean closed;

                @Override
                public int read() throws IOException {
                    byte[] one = new byte[1];
                    int response = read(one, 0, 1) < 0 ? -1 : one[0] & 255;
                    return response;
                }

                @Override
                public int read(byte[] bytes, int offset, int length) throws IOException {
                    int count = in.read(bytes, offset, length);
                    if (count > 0 && apiLogWriter != null && responseType != null && responseType.contains(Const.Common.JSON)) {
                        int kept = Math.min(count, Math.max(0, apiLogWriter.limit() + 1 - prefix.size()));
                        prefix.write(bytes, offset, kept);
                    }
                    return count;
                }

                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        connection.disconnect();
                        if (!closed && apiLogWriter != null) {
                            closed = true;
                            apiLogWriter.write(
                                loggedUri,
                                Const.Common.GET,
                                "{}",
                                "200 " + apiLogWriter.body(prefix.toByteArray(), responseType, false),
                                started
                            );
                        }
                    }
                }
            };
            Body response = new Body(input, connection.getContentLengthLong(), connection.getContentType());
            return response;
        }
        throw new LogicException("OOHC_00004", "original.redirect.too.many");
    }

    public JsonNode json(URI uri) throws IOException {
        try (Body body = open(uri)) {
            byte[] bytes = body.stream().readNBytes(2 * 1024 * 1024 + 1);
            if (bytes.length > 2 * 1024 * 1024) {
                throw new LogicException("JOHC_00001", "original.metadata.too.large");
            }
            try {
                JsonNode response = mapper.readTree(bytes);
                return response;
            } catch (tools.jackson.core.JacksonException ex) {
                throw new LogicException(ex, "JOHC_00002", "original.metadata.json.invalid");
            }
        }
    }

    public record Body(InputStream stream, long length, String type) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            stream.close();
        }
    }
}
