package com.ergoproxy.platform.common.configuration.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Configuration
public class CacheProperties {

    //general cache
    @Value("${video.cache.ttl:24h}")
    private Duration videoCacheTtl;

    @Value("${video.cache.key:videos:data}")
    private String videoCacheKey;

    //redis
    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private Integer port;

    @Value("${spring.data.redis.username:}")
    private String username;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private Integer database;

    @Value("${spring.data.redis.timeout:5s}")
    private Duration timeout;

    @Value("${spring.data.redis.connect-timeout:5s}")
    private Duration connectTimeout;

    @Value("${spring.data.redis.client-name:ergo-proxy}")
    private String clientName;
}
