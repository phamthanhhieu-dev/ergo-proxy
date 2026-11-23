package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.common.configuration.properties.ApplicationProperties;
import com.ergoproxy.platform.common.configuration.properties.CacheProperties;
import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.common.exception.LogicException;
import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.VideoDTO;
import com.ergoproxy.platform.service.cache.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final List<VideoRouter> contracts;
    private final CacheProperties cacheProperties;
    private final CacheService cacheService;

    private VideoRouter getSource(String source) {
        return contracts.stream()
            .filter(contract -> contract.getSource().equals(source))
            .findFirst()
            .orElseThrow(() -> new LogicException("GSV_00002", "video.source.unsupported"));
    }

    @Override
    public ResponseEntity<?> search(SearchRequestDTO searchRequestDTO) {
        var response = getSource(searchRequestDTO.getSource()).search(searchRequestDTO);
        if (response.getBody() instanceof Page<?> page) {
            for (Object item : page.getContent()) {
                if (item instanceof VideoDTO videoDTO) {
                    cacheService.hashSet(Const.Source.REDIS, cacheProperties.getVideoCacheKey(), UUID.randomUUID().toString(), videoDTO, cacheProperties.getVideoCacheTtl());
                }
            }
        }
        return response;
    }

    @Override
    public ResponseEntity<?> getDetail(String source, String query) {
        var response = getSource(source).getDetail(query);
        return response;
    }
}
