package com.ergoproxy.platform.service.subtitle;

import com.ergoproxy.platform.common.constant.Language;
import com.ergoproxy.platform.common.exception.LogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubtitleServiceImpl implements SubtitleService {

    private final List<SubtitleRouter> contracts;

    private SubtitleRouter getSource(String source) {
        return contracts.stream()
            .filter(contract -> contract.getSource().equals(source))
            .findFirst()
            .orElseThrow(() -> new LogicException("GSS_00002", "subtitle.source.unsupported", source));
    }

    @Override
    public List<?> search(String source, String movieName, String language, Integer seasonNumber, Integer episodeNumber, Integer year, String type,Integer page, Integer size) {
        if (!Language.isValid(language)) {
            throw new LogicException("SS_00001", "language.invalid", language);
        }
        List<?> response = getSource(source).search(movieName, language, seasonNumber, episodeNumber, year, type, page, size);
        return response;
    }
}
