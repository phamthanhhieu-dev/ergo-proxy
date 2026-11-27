package com.ergoproxy.platform.model.dto.subtitle.opensubtitles;

import lombok.Data;

@Data
public class OpenSubtitlesSearchRequestDTO {
    private Integer id;
    private Integer imdbId;
    private Integer tmdbId;
    private String type;
    private String query;
    private String languages;
    private String moviehash;
    private Integer uploaderId;
    private String hearingImpaired;
    private String foreignPartsOnly;
    private String trustedSources;
    private String machineTranslated;
    private String aiTranslated;
    private String orderBy;
    private String orderDirection;
    private Integer parentFeatureId;
    private Integer parentImdbId;
    private Integer parentTmdbId;
    private Integer seasonNumber;
    private Integer episodeNumber;
    private Integer year;
    private String moviehashMatch;
    private Integer page;
}
