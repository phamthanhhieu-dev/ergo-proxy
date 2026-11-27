package com.ergoproxy.platform.model.dto.subtitle.opensubtitles;

import com.ergoproxy.platform.model.dto.subtitle.SubtitleDTO;
import lombok.Data;

import java.util.List;

@Data
public class OpenSubtitlesSearchResponseDTO {
    private Integer totalPages;
    private Integer totalCount;
    private Integer perPage;
    private Integer page;
    private List<SubtitleDTO> data;
}
