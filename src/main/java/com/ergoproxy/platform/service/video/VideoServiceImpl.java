package com.ergoproxy.platform.service.video;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VideoServiceImpl implements VideoService {

    private final Map<String, VideoRouter> animeSources;

    public VideoServiceImpl(List<VideoRouter> contracts) {
        this.animeSources = contracts.stream()
            .collect(Collectors
                .toMap(VideoRouter::getSource, Function.identity()));
    }

    private VideoRouter getSource(String source) {
        VideoRouter contract = animeSources.get(source);
        return contract;
    }

    @Override
    public List<?> search(String source, String query) {
        var response = getSource(source).search(query);
        return response;
    }
}
