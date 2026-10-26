package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.original.OriginalDownloadRequestDTO;
import com.ergoproxy.platform.dto.original.OriginalDownloadResponseDTO;
import com.ergoproxy.platform.service.OriginalDownloadService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequiredArgsConstructor
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.ORIGINAL)
public class OriginalController {

    private final OriginalDownloadService originalDownloadService;

    @PostMapping
    public OriginalDownloadResponseDTO prepare(
        @Valid @RequestBody OriginalDownloadRequestDTO request,
        @RequestParam(defaultValue = Const.Common.REANIME) String source
    ) throws Exception {
        OriginalDownloadResponseDTO response = originalDownloadService.prepare(request, source);
        return response;
    }

    @GetMapping("/{id}")
    public OriginalDownloadResponseDTO status(@PathVariable UUID id) {
        OriginalDownloadResponseDTO response = originalDownloadService.status(id);
        return response;
    }

    @DeleteMapping("/{id}")
    public void cancel(@PathVariable UUID id) {
        originalDownloadService.cancel(id);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<StreamingResponseBody> content(@PathVariable UUID id) throws Exception {
        ResponseEntity<StreamingResponseBody> response = originalDownloadService.downloadContent(id);
        return response;
    }
}
