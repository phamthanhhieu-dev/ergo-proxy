package com.ergoproxy.platform.dto.playback;

import java.util.List;

public record SubtitleLibraryEntryDTO(String series, String episode, List<PlaybackFileDTO> subtitles, List<PlaybackFileDTO> fonts) {
}
