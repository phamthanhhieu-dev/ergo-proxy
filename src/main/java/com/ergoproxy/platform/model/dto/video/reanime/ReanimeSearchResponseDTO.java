package com.ergoproxy.platform.model.dto.video.reanime;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReanimeSearchResponseDTO {

    private Integer limit;
    private Integer offset;
    @JsonProperty("processing_ms")
    private Integer processingMs;
    private String query;
    private List<ReanimeDTO> results;
    private Integer total;
}
