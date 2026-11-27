package com.ergoproxy.platform.repository;

import com.ergoproxy.platform.common.configuration.client.OpenSubtitlesConfiguration;
import com.ergoproxy.platform.common.configuration.properties.OpenSubtitlesProperties;
import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesGetDetailResponseDTO;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesSearchRequestDTO;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesSearchResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Repository
@RequiredArgsConstructor
public class OpenSubtitlesRepository {

    private final OpenSubtitlesConfiguration openSubtitlesConfiguration;
    private final OpenSubtitlesProperties openSubtitlesProperties;

    public OpenSubtitlesGetDetailResponseDTO getDetails(String query, Integer year, String type) {
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(openSubtitlesProperties.getOpenSubtitlesApiUrl())
            .path(Const.Path.API + Const.Version.V1 + "/features")
            .queryParam("query", query)
            .queryParam("type", type)
            .queryParam("full_search", true)
            .queryParam("query_match", "word");;

        if (year != null) {
            builder.queryParam("year", year);
        }

        URI uri = URI.create(builder.build()
            .encode()
            .toUriString()
            .replace("%20", "+"));

        return openSubtitlesConfiguration.get()
            .uri(uri)
            .header("Api-Key", openSubtitlesProperties.getApiKey())
            .header("User-Agent", "ErgoProxy v1.0")
            .header("Accept", "application/json")
            .retrieve()
            .body(OpenSubtitlesGetDetailResponseDTO.class);
    }

    public OpenSubtitlesSearchResponseDTO getSubtitles(OpenSubtitlesSearchRequestDTO request) {
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(openSubtitlesProperties.getOpenSubtitlesApiUrl())
            .path(Const.Path.API + Const.Version.V1 + "/subtitles");

        if (request.getAiTranslated() != null) {
            builder.queryParam("ai_translated", request.getAiTranslated());
        }
        if (request.getEpisodeNumber() != null) {
            builder.queryParam("episode_number", request.getEpisodeNumber());
        }
        if (request.getForeignPartsOnly() != null) {
            builder.queryParam("foreign_parts_only", request.getForeignPartsOnly());
        }
        if (request.getHearingImpaired() != null) {
            builder.queryParam("hearing_impaired", request.getHearingImpaired());
        }
        if (request.getId() != null) {
            builder.queryParam("id", request.getId());
        }
        if (request.getImdbId() != null) {
            if (request.getSeasonNumber() != null && request.getEpisodeNumber() != null) {
                builder.queryParam("parent_imdb_id", request.getImdbId());
                builder.queryParam("type", "episode");
            } else {
                builder.queryParam("imdb_id", request.getImdbId());
            }
        }
        if (request.getLanguages() != null && !request.getLanguages().isBlank()) {
            builder.queryParam("languages", request.getLanguages());
        }
        if (request.getMachineTranslated() != null) {
            builder.queryParam("machine_translated", request.getMachineTranslated());
        }
        if (request.getMoviehash() != null && !request.getMoviehash().isBlank()) {
            builder.queryParam("moviehash", request.getMoviehash());
        }
        if (request.getMoviehashMatch() != null) {
            builder.queryParam("moviehash_match", request.getMoviehashMatch());
        }
        if (request.getOrderBy() != null) {
            builder.queryParam("order_by", request.getOrderBy());
        }
        if (request.getOrderDirection() != null) {
            builder.queryParam("order_direction", request.getOrderDirection());
        }
        if (request.getPage() != null) {
            int page = request.getPage() + 1;
            if (page > 1) {
                builder.queryParam("page", page);
            }
        }
        if (request.getParentFeatureId() != null) {
            builder.queryParam("parent_feature_id", request.getParentFeatureId());
        }
        if (request.getParentImdbId() != null) {
            builder.queryParam("parent_imdb_id", request.getParentImdbId());
        }
        if (request.getParentTmdbId() != null) {
            builder.queryParam("parent_tmdb_id", request.getParentTmdbId());
        }
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            builder.queryParam("query", request.getQuery());
        }
        if (request.getSeasonNumber() != null) {
            builder.queryParam("season_number", request.getSeasonNumber());
        }
        if (request.getTmdbId() != null) {
            builder.queryParam("tmdb_id", request.getTmdbId());
        }
        if (request.getTrustedSources() != null) {
            builder.queryParam("trusted_sources", request.getTrustedSources());
        }
        if (request.getType() != null) {
            builder.queryParam("type", request.getType());
        }
        if (request.getUploaderId() != null) {
            builder.queryParam("uploader_id", request.getUploaderId());
        }
        if (request.getYear() != null) {
            builder.queryParam("year", request.getYear());
        }

        URI uri = URI.create(builder.build()
            .encode()
            .toUriString()
            .replace("%20", "+"));

        return openSubtitlesConfiguration.get()
            .uri(uri)
            .header("Api-Key", openSubtitlesProperties.getApiKey())
            .header("User-Agent", "ErgoProxy v1.0")
            .header("Accept", "application/json")
            .retrieve()
            .body(OpenSubtitlesSearchResponseDTO.class);
    }
}
