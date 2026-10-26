package com.ergoproxy.platform.config.client;

import com.ergoproxy.platform.config.logging.HttpLoggingInterceptor;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenSubtitleHttpClient {

    private final RestClient restClient;

    public OpenSubtitleHttpClient(HttpLoggingInterceptor httpLoggingInterceptor) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofSeconds(45));
        RestClient client = RestClient.builder().requestFactory(factory).requestInterceptor(httpLoggingInterceptor).build();
        this.restClient = client;
    }

    public RestClient.RequestHeadersUriSpec<?> get() {
        RestClient.RequestHeadersUriSpec<?> response = restClient.get();
        return response;
    }

    public RestClient.RequestBodyUriSpec post() {
        RestClient.RequestBodyUriSpec response = restClient.post();
        return response;
    }
}
