package com.ergoproxy.platform.common.config.configmapping;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ApplicationProperties {

    @Value("${server.address:0.0.0.0}")
    private String serverAddress;

    @Value("${server.port:8080}")
    private Integer serverPort;

    @Value("${spring.mvc.async.request-timeout:21600000}")
    private Long asyncRequestTimeout;

    @Value("${spring.application.name:ergo-proxy}")
    private String applicationName;

    @Value("${spring.messages.basename:message/message}")
    private String messagesBasename;

    @Value("${spring.messages.encoding:UTF-8}")
    private String messagesEncoding;

    @Value("${time.zone:Asia/Ho_Chi_Minh}")
    private String timeZone;

    @Value("${spring.banner.location:classpath:log/banner.txt}")
    private String bannerLocation;
}
