package com.ergoproxy.platform.service;

import com.ergoproxy.platform.dto.playback.LibrarySnapshotDTO;
import com.ergoproxy.platform.entity.playback.LibraryFile;

import java.util.UUID;

public interface PlaybackLibraryService {
    LibrarySnapshotDTO library();

    java.util.List<com.ergoproxy.platform.dto.playback.SubtitleLibraryEntryDTO> scanSubtitleLibrary();

    LibraryFile find(UUID id);
}
