package com.ergoproxy.platform.model.dto.video.reanime;

import com.ergoproxy.platform.model.dto.video.CoverImageDTO;
import com.ergoproxy.platform.model.dto.video.VideoTitleDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReanimeDTO {

    @JsonProperty("anime_id")
    private String animeId;

    @JsonProperty("anilist_id")
    private Long anilistId;

    private VideoTitleDTO title;
    private CoverImageDTO coverImage;
    private String format;
    private String status;
    private List<String> genres;
    private String season;

    @JsonProperty("season_year")
    private Integer seasonYear;

    private Integer episodes;
    private String duration;
    private Integer subbed;
    private Integer dubbed;

    @JsonProperty("average_score")
    private Integer averageScore;

    private Long popularity;
    private String rating;

    @JsonProperty("can_watch")
    private Boolean canWatch;

    @JsonProperty("can_request")
    private Boolean canRequest;
}
