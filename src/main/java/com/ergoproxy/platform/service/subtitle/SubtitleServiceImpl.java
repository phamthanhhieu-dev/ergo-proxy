package com.ergoproxy.platform.service.subtitle;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SubtitleServiceImpl implements SubtitleService {

    private final Map<String, SubtitleRouter> subtitleSources;

    public SubtitleServiceImpl(List<SubtitleRouter> contracts) {
        this.subtitleSources = contracts.stream()
            .collect(Collectors
                .toMap(SubtitleRouter::getSource, Function.identity()));
    }

    private SubtitleRouter getSource(String source) {
        SubtitleRouter contract = subtitleSources.get(source);
        return contract;
    }

    @Override
    public List<?> search(String source, String query, String language, int season, int episode, String imdb, boolean movie, int page) {
        StringBuilder searchQuery = new StringBuilder()
            .append("query=").append(query)
            .append("&language=").append(language)
            .append("&season=").append(season)
            .append("&episode=").append(episode)
            .append("&imdb=").append(imdb)
            .append("&movie=").append(movie)
            .append("&page=").append(page);
        List<?> response = getSource(source).search(searchQuery.toString());
        return response;
    }
}
