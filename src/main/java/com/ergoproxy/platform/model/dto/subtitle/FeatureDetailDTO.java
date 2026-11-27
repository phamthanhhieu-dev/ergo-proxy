package com.ergoproxy.platform.model.dto.subtitle;

import lombok.Data;

@Data
public class FeatureDetailDTO {
    private Long featureId;
    private String featureType;
    private Integer year;
    private String title;
    private String movieName;
    private Long imdbId;
    private Long tmdbId;
}
