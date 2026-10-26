package com.ergoproxy.platform.dto.subtitle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleFileDTO {

    private byte[] bytes;
    private String fileName;
}
