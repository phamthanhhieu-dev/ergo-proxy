package com.ergoproxy.platform.model.dto.video.reanime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReanimeSearchRequestDTO {
    private String genre;
    private String season;
    private String format;
    private String status;
    private String country;
    private String audio;
    private String rating;
    private Integer year;
    private String sort;
    private String tag;
    private String character;
    private String staff;
    private String studio;
    private Integer limit;
    private Integer offset;
}
