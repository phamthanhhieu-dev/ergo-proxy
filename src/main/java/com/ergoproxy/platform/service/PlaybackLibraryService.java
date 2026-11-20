package com.ergoproxy.platform.service;

import com.ergoproxy.platform.model.dto.playback.LibrarySnapshotDTO;
import com.ergoproxy.platform.model.entity.playback.LibraryFile;

import java.util.UUID;

public interface PlaybackLibraryService {
    LibrarySnapshotDTO library();

    java.util.List<com.ergoproxy.platform.model.dto.playback.SubtitleLibraryEntryDTO> scanSubtitleLibrary();

    LibraryFile find(UUID id);
}
