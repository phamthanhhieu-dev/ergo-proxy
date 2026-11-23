package com.ergoproxy.platform.service.host;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HostServiceImpl implements HostService {

    private final List<HostRouter> contracts;

    private HostRouter getSource(String source) {
        return contracts.stream()
            .filter(contract -> contract.getSource().equals(source))
            .findFirst()
            .orElseThrow();
    }

    @Override
    public String getEmbedUrl(String source, String test) {
        var response = getSource(source).getEmbedUrl(test);
        return "";
    }
}
