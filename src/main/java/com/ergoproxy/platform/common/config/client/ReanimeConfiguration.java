package com.ergoproxy.platform.common.config.client;

import com.ergoproxy.platform.common.config.configmapping.ReanimeCfg;
import com.ergoproxy.platform.common.config.logging.HttpLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ReanimeConfiguration {

    private final RestClient restClient;

    public ReanimeConfiguration(ReanimeCfg reanimeCfg, HttpLoggingInterceptor httpLoggingInterceptor) {
        RestClient client = RestClient.builder()
            .baseUrl(reanimeCfg.getReanimeApiUrl())
            .requestInterceptor(httpLoggingInterceptor)
            .build();
        this.restClient = client;
    }

    public RestClient.RequestHeadersUriSpec<?> get() {
        return restClient.get();
    }
}
