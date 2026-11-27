package com.ergoproxy.platform.model.dto.subtitle.opensubtitles;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OpenSubtitlesEpisodeDTO {
    @JsonProperty("episode_number")
    private Integer episodeNumber;
    @JsonProperty("title")
    private String title;
    @JsonProperty("feature_id")
    private Long featureId;
    @JsonProperty("feature_imdb_id")
    private Long featureImdbId;
    @JsonProperty("slug")
    private String slug;
}
