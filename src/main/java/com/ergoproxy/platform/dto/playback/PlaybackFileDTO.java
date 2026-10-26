package com.ergoproxy.platform.dto.playback;

import java.util.UUID;

public record PlaybackFileDTO(UUID id, String name, String extension, long size) {}
