package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.playback.LibrarySnapshotDTO;
import com.ergoproxy.platform.dto.playback.SubtitleLibraryEntryDTO;
import com.ergoproxy.platform.service.PlaybackLibraryService;
import com.ergoproxy.platform.service.PlaybackMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(Const.Path.PLAYBACK)
@RequiredArgsConstructor
public class PlaybackController {

    private final PlaybackLibraryService libraryService;
    private final PlaybackMediaService mediaService;

    @GetMapping("/library")
    public LibrarySnapshotDTO library() {
        LibrarySnapshotDTO response = libraryService.library();
        return response;
    }

    @GetMapping("/subtitles")
    public java.util.List<SubtitleLibraryEntryDTO> scanSubtitleLibrary() {
        java.util.List<SubtitleLibraryEntryDTO> response = libraryService.scanSubtitleLibrary();
        return response;
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> file(@PathVariable UUID id) {
        ResponseEntity<Resource> response = mediaService.file(id);
        return response;
    }
}
