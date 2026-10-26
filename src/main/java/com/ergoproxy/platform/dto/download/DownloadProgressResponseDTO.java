package com.ergoproxy.platform.dto.download;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DownloadProgressResponseDTO {

    private Double percent;
    private int segmentsDone;
    private int segmentsTotal;
    private String speed;
    private String eta;
    private String stage;
    private Instant updatedAt;
    private List<String> logs;
}
