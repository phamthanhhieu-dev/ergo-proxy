package com.ergoproxy.platform.dto.bundle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeSubtitleRequestDTO {

    @Min(value = 1, message = "{bundle.subtitle.episode.min.invalid}")
    @Max(value = 10000, message = "{bundle.subtitle.episode.max.invalid}")
    private int episode;

    @Min(value = 1, message = "{bundle.subtitle.file.id.min.invalid}")
    private long fileId;
}
