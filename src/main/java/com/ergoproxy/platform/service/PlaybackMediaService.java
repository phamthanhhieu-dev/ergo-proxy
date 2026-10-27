package com.ergoproxy.platform.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface PlaybackMediaService {
    ResponseEntity<Resource> file(UUID id);
}
