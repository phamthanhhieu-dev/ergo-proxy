package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import org.springframework.http.ResponseEntity;

public interface VideoService {

    ResponseEntity<?> search(SearchRequestDTO searchRequestDTO);

    ResponseEntity<?> getDetailByUUID(String uuid);
}
