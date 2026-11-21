package com.ergoproxy.platform.repository;

import com.ergoproxy.platform.common.config.configmapping.ReanimeCfg;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeSearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeSearchResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;

@Repository
@RequiredArgsConstructor
public class ReanimeRepository {

    private final RestClient reanimeRestClient;
    private final ReanimeCfg reanimeCfg;

    public ReanimeSearchResponseDTO search(ReanimeSearchRequestDTO request) {
        return reanimeRestClient.get()
            .uri(uriBuilder -> buildSearchUri(uriBuilder, request))
            .retrieve()
            .body(ReanimeSearchResponseDTO.class);
    }

    private URI buildSearchUri(UriBuilder builder, ReanimeSearchRequestDTO request) {
        builder.path(reanimeCfg.getReanimeApiPath() + "/search");

        if (request.getGenre() != null) {
            builder.queryParam("genre", request.getGenre());
        }
        if (request.getSeason() != null) {
            builder.queryParam("season", request.getSeason());
        }
        if (request.getFormat() != null) {
            builder.queryParam("format", request.getFormat());
        }
        if (request.getStatus() != null) {
            builder.queryParam("status", request.getStatus());
        }
        if (request.getCountry() != null) {
            builder.queryParam("country", request.getCountry());
        }
        if (request.getAudio() != null) {
            builder.queryParam("audio", request.getAudio());
        }
        if (request.getRating() != null) {
            builder.queryParam("rating", request.getRating());
        }
        if (request.getYear() != null) {
            builder.queryParam("year", request.getYear());
        }
        if (request.getSort() != null) {
            builder.queryParam("sort", request.getSort());
        }
        if (request.getTag() != null) {
            builder.queryParam("tag", request.getTag());
        }
        if (request.getCharacter() != null) {
            builder.queryParam("character", request.getCharacter());
        }
        if (request.getStaff() != null) {
            builder.queryParam("staff", request.getStaff());
        }
        if (request.getStudio() != null) {
            builder.queryParam("studio", request.getStudio());
        }
        if (request.getLimit() != null) {
            builder.queryParam("limit", request.getLimit());
        }
        if (request.getOffset() != null) {
            builder.queryParam("offset", request.getOffset());
        }

        return builder.build();
    }
}
