package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface VideoRouter {
    String getSource();

    ResponseEntity<?>  search(SearchRequestDTO searchRequestDTO);

    ResponseEntity<?> getDetail(String query);
}
