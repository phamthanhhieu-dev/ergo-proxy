package com.ergoproxy.platform.model.dto.playback;

import java.util.UUID;

public record PlaybackFileDTO(UUID id, String name, String extension, long size) {
}
