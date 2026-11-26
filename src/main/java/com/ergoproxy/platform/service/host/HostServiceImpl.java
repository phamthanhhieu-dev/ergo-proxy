package com.ergoproxy.platform.service.host;

import com.ergoproxy.platform.common.exception.LogicException;
import com.ergoproxy.platform.model.dto.host.GetEmbedUrlRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            .orElseThrow(() -> new LogicException("GSH_00002", "host.source.unsupported", source));
    }

    @Override
    public ResponseEntity<?> getEmbedUrl(GetEmbedUrlRequestDTO requestDTO) {
        String source = requestDTO.getSource();
        var response = getSource(source).getEmbedUrl(requestDTO);
        return ResponseEntity.ok().body(response);
    }
}
