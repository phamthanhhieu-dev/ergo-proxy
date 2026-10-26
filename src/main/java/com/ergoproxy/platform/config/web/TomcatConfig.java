package com.ergoproxy.platform.config.web;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TomcatConfig {

    @Bean
    public TomcatServletWebServerFactory servletWebServerFactory() {
        TomcatServletWebServerFactory response = new TomcatServletWebServerFactory();
        response.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
        return response;
    }
}
