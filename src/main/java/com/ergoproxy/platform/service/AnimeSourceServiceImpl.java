package com.ergoproxy.platform.service;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.exception.LogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnimeSourceServiceImpl implements AnimeSourceService {

    private final List<AnimeProvider> providers;

    @Override
    public Map<String, String> embed(com.ergoproxy.platform.dto.original.OriginalDownloadRequestDTO request, String source) throws Exception {
        AnimeProvider provider = resolve(source);
        String url = provider.embedUrl(request);
        Map<String, String> response = new LinkedHashMap<>();
        response.put(Const.Common.EMBED_URL, url);
        return response;
    }

    @Override
    public AnimeProvider resolve(String source) {
        for (AnimeProvider provider : providers) {
            String providerId = provider.id();
            if (providerId.equals(source)) {
                return provider;
            }
        }
        throw new LogicException("RAS_00001", "anime.source.invalid", source);
    }

    @Override
    public List<Map<String, String>> sources() {
        List<Map<String, String>> response = new ArrayList<>();
        for (AnimeProvider provider : providers) {
            Map<String, String> source = new LinkedHashMap<>();
            source.put(Const.Common.ID, provider.id());
            source.put(Const.Common.NAME, provider.displayName());
            response.add(source);
        }
        return response;
    }

    @Override
    public tools.jackson.databind.JsonNode search(String query, String source) throws Exception {
        AnimeProvider provider = resolve(source);
        tools.jackson.databind.JsonNode response = provider.search(query);
        return response;
    }

    @Override
    public tools.jackson.databind.JsonNode episodes(String slug, String source) throws Exception {
        AnimeProvider provider = resolve(source);
        tools.jackson.databind.JsonNode response = provider.episodes(slug);
        return response;
    }
}
