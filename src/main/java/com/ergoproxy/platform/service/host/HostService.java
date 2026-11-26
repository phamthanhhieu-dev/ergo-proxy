package com.ergoproxy.platform.service.host;

import com.ergoproxy.platform.model.dto.host.GetEmbedUrlRequestDTO;
import org.springframework.http.ResponseEntity;

public interface HostService {

    ResponseEntity<?> getEmbedUrl(GetEmbedUrlRequestDTO requestDTO);
}
