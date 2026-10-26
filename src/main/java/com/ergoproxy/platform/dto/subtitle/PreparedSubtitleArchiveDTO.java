package com.ergoproxy.platform.dto.subtitle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreparedSubtitleArchiveDTO {

    private SubtitleArchiveRequestDTO archive;
    private SubtitleFileDTO first;
    private String name;
}
