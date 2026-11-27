package com.ergoproxy.platform.model.dto.subtitle.opensubtitles;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OpenSubtitlesSeasonDTO {
    @JsonProperty("season_number")
    private Integer seasonNumber;
    @JsonProperty("episodes")
    private List<OpenSubtitlesEpisodeDTO> episodes;
}
