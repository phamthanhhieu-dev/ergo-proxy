package com.ergoproxy.platform.model.dto.host;

import lombok.Data;
import org.springframework.web.bind.annotation.PathVariable;

@Data
public class GetEmbedUrlRequestDTO {
    private String source;
    private Long anilistId;
    private Integer episode;
    private String movieName;
}
