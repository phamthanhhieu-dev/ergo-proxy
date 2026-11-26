package com.ergoproxy.platform.model.dto.host.reanime;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReanimeHostDTO {
    @JsonProperty("$id")
    private String id;
    private String serverName;
    private String dataLink;
    private String dataType;
    @JsonProperty("continue")
    private Boolean continueWatching;
    private Boolean softsub;
    private Boolean hardsub;
}
