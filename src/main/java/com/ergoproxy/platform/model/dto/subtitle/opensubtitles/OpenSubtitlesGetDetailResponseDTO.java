package com.ergoproxy.platform.model.dto.subtitle.opensubtitles;

import lombok.Data;

import java.util.List;

@Data
public class OpenSubtitlesGetDetailResponseDTO {
    private List<OpenSubtitlesFeatureDTO> data;
}
