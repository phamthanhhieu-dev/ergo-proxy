package com.ergoproxy.platform.common.config.configmapping;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class LogProperties {

    @Value("${logging.config:classpath:log/logback-spring.xml}")
    private String config;

    @Value("${app.logging.api.enabled:true}")
    private Boolean apiEnabled;

    @Value("${app.logging.api.max-body-size:16384}")
    private Integer apiMaxBodySize;

    @Value("${logging.level.com.ergoproxy:INFO}")
    private String level;
}
