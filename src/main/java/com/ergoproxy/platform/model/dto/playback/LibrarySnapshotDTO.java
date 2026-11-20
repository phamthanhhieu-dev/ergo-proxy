package com.ergoproxy.platform.model.dto.playback;

import java.util.List;

public record LibrarySnapshotDTO(String name, List<PlaybackVideoDTO> videos) {
}
