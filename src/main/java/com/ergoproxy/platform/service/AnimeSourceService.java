package com.ergoproxy.platform.service;

import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public interface AnimeSourceService {
    Map<String, String> embed(com.ergoproxy.platform.model.dto.original.OriginalDownloadRequestDTO request, String source) throws Exception;

    AnimeProvider resolve(String source);

    List<Map<String, String>> sources();

    JsonNode search(String query, String source) throws Exception;

    JsonNode episodes(String slug, String source) throws Exception;
}
