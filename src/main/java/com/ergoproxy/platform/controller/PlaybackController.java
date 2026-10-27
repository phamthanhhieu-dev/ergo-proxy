package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.playback.*;
import com.ergoproxy.platform.service.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> file(@PathVariable UUID id) {
        ResponseEntity<Resource> response = mediaService.file(id);
        return response;
    }
}
