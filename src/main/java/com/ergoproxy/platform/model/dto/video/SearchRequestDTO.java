package com.ergoproxy.platform.model.dto.video;

import com.ergoproxy.platform.model.dto.PaginationDTO;
import lombok.Data;

@Data
public class SearchRequestDTO extends PaginationDTO {
    private String title;
    private String source;
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
}
