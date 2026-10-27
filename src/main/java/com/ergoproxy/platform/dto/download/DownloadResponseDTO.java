package com.ergoproxy.platform.dto.download;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DownloadResponseDTO {

    private UUID id;
    private String status;
    private int completed;
    private int total;
    private int currentEpisode;
    private String error;
    private List<DownloadFileResponseDTO> files;
    private DownloadProgressResponseDTO progress;
}
