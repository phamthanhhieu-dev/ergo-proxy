package com.ergoproxy.platform.dto.download;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DownloadVideoTargetResponseDTO {

    private UUID jobId;
    private String videoId;
    private String name;
    private int episode;
}
