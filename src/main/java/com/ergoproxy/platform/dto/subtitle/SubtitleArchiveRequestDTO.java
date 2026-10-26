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
public class SubtitleArchiveRequestDTO {

    private String name;
    private List<SubtitleArchiveItemRequestDTO> files;
    private List<Integer> missing;
}
