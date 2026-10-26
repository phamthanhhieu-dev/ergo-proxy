package com.ergoproxy.platform.service;

import java.util.List;
import java.util.Map;

public interface AnimeSourceService {
    AnimeProvider resolve(String source);

    List<Map<String, String>> sources();

    tools.jackson.databind.JsonNode search(String query, String source) throws Exception;

    tools.jackson.databind.JsonNode episodes(String slug, String source) throws Exception;
}
