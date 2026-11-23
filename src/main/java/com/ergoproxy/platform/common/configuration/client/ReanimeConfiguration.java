package com.ergoproxy.platform.common.configuration.client;

import com.ergoproxy.platform.common.configuration.properties.ReanimeProperties;
import com.ergoproxy.platform.common.configuration.logging.HttpLoggingInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ReanimeConfiguration {

    private final RestClient restClient;

    public ReanimeConfiguration(ReanimeProperties reanimeProperties, HttpLoggingInterceptor httpLoggingInterceptor) {
        RestClient client = RestClient.builder()
            .baseUrl(reanimeProperties.getReanimeApiUrl())
            .requestInterceptor(httpLoggingInterceptor)
            .build();
        this.restClient = client;
    }

    public RestClient.RequestHeadersUriSpec<?> get() {
        return restClient.get();
    }
}
