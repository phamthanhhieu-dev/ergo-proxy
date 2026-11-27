package com.ergoproxy.platform.service.host;

import com.ergoproxy.platform.model.dto.host.GetEmbedUrlRequestDTO;
import com.ergoproxy.platform.model.dto.host.HostDTO;

import java.util.List;

public interface HostRouter {
    String getSource();

    List<HostDTO> getEmbedUrl(GetEmbedUrlRequestDTO requestDTO);
}
