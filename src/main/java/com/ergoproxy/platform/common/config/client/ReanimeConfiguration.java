package com.ergoproxy.platform.common.config.client;

import com.ergoproxy.platform.common.config.configmapping.ReanimeCfg;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ReanimeCfg.class)
public class ReanimeConfiguration {

    @Bean
    public RestClient reanimeRestClient(RestClient.Builder builder, ReanimeCfg properties) {
        return builder.baseUrl(properties.getReanimeApiUrl()).build();
    }
}
