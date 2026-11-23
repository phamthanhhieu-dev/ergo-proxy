package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;

import java.util.List;

public interface VideoRouter {
    String getSource();

    List<?> search(SearchRequestDTO searchRequestDTO);
}
