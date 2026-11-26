package com.ergoproxy.platform.model.dto.video;

import lombok.Data;

import java.util.List;

@Data
public class VideoDTO {
    private Long anilistId;
    private String UUID;
    private String source;
    private TitleDTO title;
    private CoverImageDTO coverImage;
    private String format;
    private String status;
    private List<String> genres;
    private String season;
    private Integer seasonYear;
    private Integer episodes;
    private String duration;
    private Integer subbed;
    private Integer dubbed;
    private Integer averageScore;
    private Long popularity;
    private String rating;
}
