package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.download.DownloadFileResponseDTO;
import com.ergoproxy.platform.dto.download.DownloadVideoTargetResponseDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleDownloadRequestDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleSaveRequestDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleSearchResponseDTO;
import com.ergoproxy.platform.service.DownloadService;
import com.ergoproxy.platform.service.OpenSubtitleService;
import com.ergoproxy.platform.service.SubtitleArchiveService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequiredArgsConstructor
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.SUBTITLE)
public class SubtitleController {

    private final OpenSubtitleService openSubtitleService;
    private final SubtitleArchiveService subtitleArchiveService;
    private final DownloadService downloadService;

    @PostMapping(value = "/native", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> nativeDownload(@RequestParam long fileId) {
        ResponseEntity<byte[]> response = openSubtitleService.nativeDownload(fileId);
        return response;
    }

    @PostMapping(value = Const.Path.NATIVE_ARCHIVE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<StreamingResponseBody> nativeArchive(@RequestParam String payload) throws Exception {
        ResponseEntity<StreamingResponseBody> response = subtitleArchiveService.downloadArchive(payload);
        return response;
    }

    @GetMapping("/videos")
    public List<DownloadVideoTargetResponseDTO> videos(@RequestParam String slug) {
        List<DownloadVideoTargetResponseDTO> response = downloadService.videoTargets(slug);
        return response;
    }

    @PostMapping("/save")
    public DownloadFileResponseDTO save(@Valid @RequestBody SubtitleSaveRequestDTO request) throws Exception {
        DownloadFileResponseDTO response = downloadService.saveSubtitle(request);
        return response;
    }

    @GetMapping("/configuration")
    public Map<String, Boolean> configuration() {
        Map<String, Boolean> response = openSubtitleService.configuration();
        return response;
    }

    @GetMapping("/search")
    public SubtitleSearchResponseDTO search(
        @RequestParam String q,
        @RequestParam(defaultValue = Const.Common.VI) String language,
        @RequestParam(defaultValue = "1") int season,
        @RequestParam(defaultValue = "1") int episode,
        @RequestParam(defaultValue = Const.Common.EMPTY) String imdb,
        @RequestParam(defaultValue = "false") boolean movie,
        @RequestParam(defaultValue = "1") int page
    ) {
        SubtitleSearchResponseDTO response = openSubtitleService.search(q, language, season, episode, imdb, movie, page);
        return response;
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@Valid @RequestBody SubtitleDownloadRequestDTO request) {
        ResponseEntity<byte[]> response = openSubtitleService.downloadResponse(request);
        return response;
    }
}
