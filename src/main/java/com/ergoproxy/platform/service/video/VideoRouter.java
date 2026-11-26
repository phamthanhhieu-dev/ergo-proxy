package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.VideoDTO;

import java.util.List;

public interface VideoRouter {
    String getSource();

    List<VideoDTO> search(SearchRequestDTO searchRequestDTO);
}
