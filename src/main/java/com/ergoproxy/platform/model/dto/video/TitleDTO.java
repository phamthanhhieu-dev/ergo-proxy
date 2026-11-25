package com.ergoproxy.platform.model.dto.video;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TitleDTO {
    private String english;
    @JsonProperty("native")
    private String nativeTitle;
    private String romaji;
}
