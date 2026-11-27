package com.ergoproxy.platform.repository;

import com.ergoproxy.platform.common.configuration.client.ReanimeConfiguration;
import com.ergoproxy.platform.common.configuration.properties.ReanimeProperties;
import com.ergoproxy.platform.model.dto.host.reanime.ReanimeHostResponseDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeSearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeSearchResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.util.UriBuilder;

import java.net.URI;

@Repository
@RequiredArgsConstructor
public class ReanimeRepository {

    private final ReanimeConfiguration reanimeConfiguration;
    private final ReanimeProperties reanimeProperties;

    public ReanimeSearchResponseDTO search(ReanimeSearchRequestDTO request) {
        return reanimeConfiguration.get()
            .uri(uriBuilder -> buildSearchUri(uriBuilder, request))
            .retrieve()
            .body(ReanimeSearchResponseDTO.class);
    }

    private URI buildSearchUri(UriBuilder builder, ReanimeSearchRequestDTO request) {
        builder.path(reanimeProperties.getReanimeApiPath() + "/search");

        if (request.getQ() != null && !request.getQ().isBlank()) {
            builder.queryParam("q", request.getQ());
        }
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

    public ReanimeHostResponseDTO getEmbedUrl(Long anilistId, Integer episode) {
        return reanimeConfiguration.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/flix/{anilistId}/{episode}")
                .queryParam("mal", anilistId)
                .build(anilistId, episode))
            .retrieve()
            .body(ReanimeHostResponseDTO.class);
    }
}
