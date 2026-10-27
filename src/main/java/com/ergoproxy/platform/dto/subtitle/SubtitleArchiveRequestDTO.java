package com.ergoproxy.platform.dto.subtitle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleArchiveRequestDTO {

    private String name;
    private List<SubtitleArchiveItemRequestDTO> files;
    private List<Integer> missing;
}
