package com.ergoproxy.platform.dto.bundle;

import jakarta.validation.Valid;
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
public class EpisodeBundleRequestDTO {

    @NotBlank(message = "{bundle.slug.is.null.or.empty}")
    @Pattern(regexp = "[a-zA-Z0-9-]{1,180}", message = "{bundle.slug.format.invalid}")
    private String slug;

    @NotBlank(message = "{bundle.title.is.null.or.empty}")
    @Size(max = 200, message = "{bundle.title.size.invalid}")
    private String title;

    @NotNull(message = "{bundle.episodes.is.null.or.empty}")
    @Size(min = 1, max = 2000, message = "{bundle.episodes.size.invalid}")
    private List<@NotNull(message = "{bundle.episodes.element.is.null.or.empty}") @Min(value = 1, message = "{bundle.episodes.element.min.invalid}") @Max(
        value = 10000,
        message = "{bundle.episodes.element.max.invalid}"
    ) Integer> episodes;

    @NotNull(message = "{bundle.server.is.null.or.empty}")
    @Pattern(regexp = "HD-1|HD-2", message = "{bundle.server.format.invalid}")
    private String server;

    @NotNull(message = "{bundle.audio.is.null.or.empty}")
    @Pattern(regexp = "sub|dub", message = "{bundle.audio.format.invalid}")
    private String audio;

    @NotNull(message = "{bundle.language.is.null.or.empty}")
    @Pattern(regexp = "vi|en|ja", message = "{bundle.language.format.invalid}")
    private String language;

    @Min(value = 1, message = "{bundle.season.min.invalid}")
    @Max(value = 1000, message = "{bundle.season.max.invalid}")
    private int season;

    @Pattern(regexp = "(?:tt)?[0-9]{1,12}|", message = "{bundle.imdb.format.invalid}")
    private String imdb;

    private boolean movie;

    @Size(max = 300, message = "{bundle.page.url.size.invalid}")
    private String pageUrl;

    @Size(max = 2000, message = "{bundle.subtitles.size.invalid}")
    private List<@NotNull(message = "{bundle.subtitles.element.is.null.or.empty}") @Valid EpisodeSubtitleRequestDTO> subtitles;
}
