package com.ergoproxy.platform.dto.download;

import jakarta.validation.constraints.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DownloadRequestDTO {

    @NotBlank(message = "{download.slug.is.null.or.empty}")
    @Pattern(regexp = "[a-zA-Z0-9-]{1,180}", message = "{download.slug.format.invalid}")
    private String slug;

    private boolean all;

    @Size(max = 2000, message = "{download.episodes.size.invalid}")
    private List<@Min(value = 1, message = "{download.episodes.element.min.invalid}") @Max(
        value = 10000,
        message = "{download.episodes.element.max.invalid}"
    ) Integer> episodes;

    @Min(value = 360, message = "{download.quality.min.invalid}")
    @Max(value = 2160, message = "{download.quality.max.invalid}")
    private int quality;

    @NotNull(message = "{download.audio.is.null.or.empty}")
    @Pattern(regexp = "sub|dub|dual", message = "{download.audio.format.invalid}")
    private String audio;
}
