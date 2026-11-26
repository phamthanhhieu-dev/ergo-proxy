package com.ergoproxy.platform.service.subtitle;

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
    public List<?> search(String source, String query, String language, int season, int episode, String imdb, boolean movie, int page) {
        String searchQuery = "query=" + query +
            "&language=" + language +
            "&season=" + season +
            "&episode=" + episode +
            "&imdb=" + imdb +
            "&movie=" + movie +
            "&page=" + page;
        List<?> response = getSource(source).search(searchQuery);
        return response;
    }
}
