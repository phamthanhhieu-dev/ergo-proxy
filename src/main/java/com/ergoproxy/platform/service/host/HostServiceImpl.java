package com.ergoproxy.platform.service.host;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HostServiceImpl implements HostService {

    private final Map<String, HostRouter> subtitleSources;

    public HostServiceImpl(List<HostRouter> contracts) {
        this.subtitleSources = contracts.stream()
            .collect(Collectors
                .toMap(HostRouter::getSource, Function.identity()));
    }

    private HostRouter getSource(String source) {
        HostRouter contract = subtitleSources.get(source);
        return contract;
    }

    @Override
    public String getEmbedUrl(String source, String test) {
        var response = getSource(source).getEmbedUrl(test);
        return "";
    }
}
