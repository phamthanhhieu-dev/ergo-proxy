package com.ergoproxy.platform.dto.original;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OriginalDownloadRequestDTO {

    @NotBlank(message = "{original.slug.is.null.or.empty}")
    @Pattern(regexp = "[a-zA-Z0-9-]{1,180}", message = "{original.slug.format.invalid}")
    private String slug;

    @Min(value = 1, message = "{original.episode.min.invalid}")
    @Max(value = 10000, message = "{original.episode.max.invalid}")
    private int episode;

    @NotNull(message = "{original.server.is.null.or.empty}")
    @Pattern(regexp = "HD-1|HD-2", message = "{original.server.format.invalid}")
    private String server;

    @NotNull(message = "{original.audio.is.null.or.empty}")
    @Pattern(regexp = "sub|dub", message = "{original.audio.format.invalid}")
    private String audio;

    @Size(max = 300, message = "{original.page.url.size.invalid}")
    private String pageUrl;
}
