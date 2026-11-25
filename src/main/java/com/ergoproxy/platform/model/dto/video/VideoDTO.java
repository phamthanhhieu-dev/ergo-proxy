package com.ergoproxy.platform.model.dto.video;

import com.ergoproxy.platform.model.dto.SourceDTO;
import lombok.Data;

import java.util.List;

@Data
public class VideoDTO {
    private String UUID;
    private SourceDTO source;
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
