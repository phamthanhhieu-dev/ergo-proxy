package com.ergoproxy.platform.entity.playback;

import java.nio.file.Path;
import java.util.UUID;

public record LibraryFile(UUID id, Path path, String extension, long size) {}
