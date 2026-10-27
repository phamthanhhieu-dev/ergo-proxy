package com.ergoproxy.platform.config.logging;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.exception.LogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HttpLoggingInterceptor implements ClientHttpRequestInterceptor {

    private final ApiLogWriter apiLogWriter;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (!apiLogWriter.enabled()) {
            ClientHttpResponse result = execution.execute(request, body);
            return result;
        }
        long started = System.nanoTime();
        Map<String, String[]> parameters = new LinkedHashMap<>();
        MultiValueMap<String, String> query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        for (Map.Entry<String, List<String>> entry : query.entrySet()) {
            String[] values = entry.getValue().toArray(String[]::new);
            parameters.put(entry.getKey(), values);
        }
        String requestLog =
            body.length == 0 ? apiLogWriter.parameters(parameters) : apiLogWriter.body(body, String.valueOf(request.getHeaders().getContentType()), false);
        ClientHttpResponse response;
        try {
            response = execution.execute(request, body);
        } catch (IOException | RuntimeException exception) {
            apiLogWriter.write(request.getURI(), request.getMethod().name(), requestLog, exception.getClass().getSimpleName(), started);
            if (exception instanceof LogicException existingLogicException) {
                throw existingLogicException;
            }
            throw new LogicException(exception, "IHLI_00001", "request.external.call.failed");
        }
        try {
            String contentType = String.valueOf(response.getHeaders().getContentType());
            boolean json = contentType.contains(Const.Common.JSON);
            InputStream original = json ? response.getBody() : null;
            byte[] prefix = json ? original.readNBytes(apiLogWriter.limit() + 1) : new byte[0];
            apiLogWriter.write(
                request.getURI(),
                request.getMethod().name(),
                requestLog,
                response.getStatusCode().value() + " " + apiLogWriter.body(prefix, contentType, false),
                started
            );
            if (!json) {
                return response;
            }
            // Replay the bounded prefix before the untouched remaining body.
            InputStream replay = new SequenceInputStream(new ByteArrayInputStream(prefix), original);
            ClientHttpResponse returnValue = new ClientHttpResponse() {
                @Override
                public HttpStatusCode getStatusCode() throws IOException {
                    HttpStatusCode result = response.getStatusCode();
                    return result;
                }

                @Override
                public String getStatusText() throws IOException {
                    String result = response.getStatusText();
                    return result;
                }

                @Override
                public HttpHeaders getHeaders() {
                    HttpHeaders result = response.getHeaders();
                    return result;
                }

                @Override
                public InputStream getBody() {
                    return replay;
                }

                @Override
                public void close() {
                    response.close();
                }
            };
            return returnValue;
        } catch (IOException | RuntimeException exception) {
            response.close();
            apiLogWriter.write(request.getURI(), request.getMethod().name(), requestLog, exception.getClass().getSimpleName(), started);
            if (exception instanceof LogicException existingLogicException) {
                throw existingLogicException;
            }
            throw new LogicException(exception, "IHLI_00002", "request.external.call.failed");
        }
    }
}
