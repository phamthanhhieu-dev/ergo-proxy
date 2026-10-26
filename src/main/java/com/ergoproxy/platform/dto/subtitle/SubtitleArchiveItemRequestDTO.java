package com.ergoproxy.platform.dto.subtitle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleArchiveItemRequestDTO {

    private long fileId;
    private int episode;
}
