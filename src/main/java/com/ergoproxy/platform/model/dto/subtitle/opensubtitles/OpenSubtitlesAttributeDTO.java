package com.ergoproxy.platform.model.dto.subtitle.opensubtitles;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OpenSubtitlesAttributeDTO {
    @JsonProperty("title")
    private String title;
    @JsonProperty("original_title")
    private String originalTitle;
    @JsonProperty("year")
    private Integer year;
    @JsonProperty("imdb_id")
    private Long imdbId;
    @JsonProperty("tmdb_id")
    private Long tmdbId;
    @JsonProperty("title_aka")
    private List<String> titleAka;
    @JsonProperty("feature_id")
    private Long featureId;
    @JsonProperty("feature_type")
    private String featureType;
    @JsonProperty("url")
    private String url;
    @JsonProperty("img_url")
    private String imgUrl;
    @JsonProperty("subtitles_count")
    private Integer subtitlesCount;
    @JsonProperty("subtitles_counts")
    private Map<String, Integer> subtitlesCounts;
    @JsonProperty("ai_subtitles_counts")
    private Map<String, Integer> aiSubtitlesCounts;
    @JsonProperty("seasons")
    private List<OpenSubtitlesSeasonDTO> seasons;
}
