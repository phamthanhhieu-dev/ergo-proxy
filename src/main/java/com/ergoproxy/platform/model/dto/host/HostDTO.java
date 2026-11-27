package com.ergoproxy.platform.model.dto.host;

import lombok.Data;

@Data
public class HostDTO {
    private String id;
    private String name;
    private String embed;
    private String type;
    private String server;
    private Boolean continueWatching;
    private String source;
    private Integer episode;
}
