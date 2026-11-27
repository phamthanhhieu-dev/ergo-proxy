
package com.ergoproxy.platform.common.configuration.client;

import com.ergoproxy.platform.common.configuration.logging.HttpLoggingInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class OpenSubtitlesConfiguration {

    private final RestClient restClient;

    public OpenSubtitlesConfiguration(HttpLoggingInterceptor httpLoggingInterceptor) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofSeconds(45));

        this.restClient = RestClient.builder()
            .requestFactory(factory)
            .requestInterceptor(httpLoggingInterceptor)
            .build();
    }

    public RestClient.RequestHeadersUriSpec<?> get() {
        return restClient.get();
    }

    public RestClient.RequestBodyUriSpec post() {
        return restClient.post();
    }
}
