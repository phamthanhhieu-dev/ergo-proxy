package com.ergoproxy.platform.service.subtitle;

import com.ergoproxy.platform.common.configuration.properties.OpenSubtitlesProperties;
import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.model.dto.subtitle.AttributeDTO;
import com.ergoproxy.platform.model.dto.subtitle.FeatureDetailDTO;
import com.ergoproxy.platform.model.dto.subtitle.SubtitleDTO;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesGetDetailResponseDTO;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesSearchRequestDTO;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesSearchResponseDTO;
import com.ergoproxy.platform.repository.OpenSubtitlesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenSubtitles implements SubtitleRouter {
    private final OpenSubtitlesProperties openSubtitlesProperties;
    private final OpenSubtitlesRepository openSubtitlesRepository;

    @Override
    public String getSource() {
        return Const.Source.OPEN_SUBTITLES;
    }

    @Override
    public List<SubtitleDTO> search(String movieName, String language, Integer seasonNumber, Integer episodeNumber, Integer year, String type,Integer page, Integer size) {
        OpenSubtitlesGetDetailResponseDTO details = openSubtitlesRepository.getDetails(movieName, year, type);
        Long imdbId = details == null || details.getData() == null ? null :
            details.getData().stream()
                .map(feature -> feature.getAttributes())
                .filter(attributes -> attributes != null && attributes.getImdbId() != null)
                .map(attributes -> attributes.getImdbId())
                .findFirst()
                .orElse(null);

        OpenSubtitlesSearchRequestDTO request = new OpenSubtitlesSearchRequestDTO();
        request.setQuery(imdbId == null ? movieName : null);
        request.setLanguages(language);
        request.setSeasonNumber(seasonNumber);
        request.setEpisodeNumber(episodeNumber);
        request.setYear(year);
        request.setPage(page);
        request.setImdbId(imdbId == null ? null : Math.toIntExact(imdbId));

        OpenSubtitlesSearchResponseDTO openSubtitlesSearchResponseDTO = openSubtitlesRepository.getSubtitles(request);

        return openSubtitlesSearchResponseDTO.getData();
    }
}
