package com.ergoproxy.platform.model.dto.subtitle.opensubtitles;

import lombok.Data;

@Data
public class OpenSubtitlesFeatureDTO {
    private String id;
    private String type;
    private OpenSubtitlesAttributeDTO attributes;
}
