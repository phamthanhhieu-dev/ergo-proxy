package com.ergoproxy.platform.dto.download;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DownloadFileResponseDTO {

    private String id;
    private String name;
    private long size;
}
