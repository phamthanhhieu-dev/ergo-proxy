package com.ergoproxy.platform.common.config.configmapping;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ReanimeCfg {

    @Value("${reanime.api.url:https://reanime.cz}")
    private String reanimeApiUrl;

    @Value("${reanime.api.path:/api/v1}")
    private String reanimeApiPath;
}
