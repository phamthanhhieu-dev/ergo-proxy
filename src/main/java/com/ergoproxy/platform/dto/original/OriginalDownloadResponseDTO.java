package com.ergoproxy.platform.dto.original;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OriginalDownloadResponseDTO {

    private UUID id;
    private String filename;
    private String state;
    private double percent;
    private String message;
}
