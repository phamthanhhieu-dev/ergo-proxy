package com.ergoproxy.platform.dto.playback;

import java.util.List;

public record LibrarySnapshotDTO(String name, List<PlaybackVideoDTO> videos) {
}
