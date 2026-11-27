package com.ergoproxy.platform.common.configuration.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class OpenSubtitlesProperties {
    @Value("${opensubtitles.api.url}")
    private String openSubtitlesApiUrl;
    @Value("${opensubtitles.api-key:}")
    private String apiKey;
    @Value("${opensubtitles.username:}")
    private String username;
    @Value("${opensubtitles.password:}")
    private String password;
    @Value("${opensubtitles.token:}")
    private String configuredToken;
}
