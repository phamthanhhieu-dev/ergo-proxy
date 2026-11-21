package com.ergoproxy.platform.model.dto.video;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoverImageDTO {

    private String color;

    @JsonProperty("extra_large")
    private String extraLarge;

    private String large;
    private String medium;
}
