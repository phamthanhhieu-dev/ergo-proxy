package com.ergoproxy.platform.dto.subtitle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleCandidateResponseDTO {

    private long fileId;
    private String fileName;
    private String title;
    private String release;
    private String language;
    private int season;
    private int episode;
    private int downloads;
    private boolean hearingImpaired;
}
