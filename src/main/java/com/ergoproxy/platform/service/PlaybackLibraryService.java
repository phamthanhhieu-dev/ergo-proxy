package com.ergoproxy.platform.service;

import com.ergoproxy.platform.dto.playback.LibrarySnapshotDTO;
import com.ergoproxy.platform.entity.playback.LibraryFile;
import java.util.UUID;

public interface PlaybackLibraryService {
    LibrarySnapshotDTO library();
    LibraryFile find(UUID id);
}
