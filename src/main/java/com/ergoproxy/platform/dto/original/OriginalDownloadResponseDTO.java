package com.ergoproxy.platform.dto.original;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

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
