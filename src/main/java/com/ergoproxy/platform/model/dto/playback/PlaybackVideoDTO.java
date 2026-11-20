package com.ergoproxy.platform.model.dto.playback;

import java.util.List;

public record PlaybackVideoDTO(PlaybackFileDTO video, String series, String episode, List<PlaybackFileDTO> subtitles, List<PlaybackFileDTO> fonts) {
}
