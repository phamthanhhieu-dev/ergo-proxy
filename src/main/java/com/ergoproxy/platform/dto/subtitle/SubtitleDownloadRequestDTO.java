package com.ergoproxy.platform.dto.subtitle;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleDownloadRequestDTO {

    @Min(value = 1, message = "{subtitle.download.file.id.min.invalid}")
    private long fileId;
}
