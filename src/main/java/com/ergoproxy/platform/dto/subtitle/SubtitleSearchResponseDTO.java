package com.ergoproxy.platform.dto.subtitle;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleSearchResponseDTO {

    private List<SubtitleCandidateResponseDTO> results;
    private int totalPages;
}
