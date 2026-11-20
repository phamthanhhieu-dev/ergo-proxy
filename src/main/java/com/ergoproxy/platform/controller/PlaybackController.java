package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.model.dto.playback.LibrarySnapshotDTO;
import com.ergoproxy.platform.model.dto.playback.SubtitleLibraryEntryDTO;
import com.ergoproxy.platform.service.PlaybackLibraryService;
import com.ergoproxy.platform.service.PlaybackMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.PLAYBACK)
@RequiredArgsConstructor
public class PlaybackController {

    private final PlaybackLibraryService libraryService;
    private final PlaybackMediaService mediaService;

    @GetMapping(Const.Path.LIBRARY)
    public LibrarySnapshotDTO library() {
        var response = libraryService.library();
        return response;
    }

    @GetMapping(Const.Path.SUBTITLES)
    public List<SubtitleLibraryEntryDTO> scanSubtitleLibrary() {
        var response = libraryService.scanSubtitleLibrary();
        return response;
    }

    @GetMapping(Const.Path.FILES + "/{id}")
    public ResponseEntity<Resource> file(@PathVariable UUID id) {
        var response = mediaService.file(id);
        return response;
    }
}
