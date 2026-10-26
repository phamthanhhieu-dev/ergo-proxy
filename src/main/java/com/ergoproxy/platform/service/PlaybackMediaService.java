package com.ergoproxy.platform.service;

import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface PlaybackMediaService {
    ResponseEntity<Resource> file(UUID id);
}
