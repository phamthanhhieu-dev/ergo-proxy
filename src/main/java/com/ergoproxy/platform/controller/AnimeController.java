package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.download.DownloadRequestDTO;
import com.ergoproxy.platform.dto.download.DownloadResponseDTO;
import com.ergoproxy.platform.service.AnimeSourceService;
import com.ergoproxy.platform.service.DownloadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.ANIME)
@RequiredArgsConstructor
public class AnimeController {

    private final AnimeSourceService animeSourceService;
    private final DownloadService downloadService;

    @GetMapping("/sources")
    public List<Map<String, String>> sources() {
        List<Map<String, String>> response = animeSourceService.sources();
        return response;
    }

    @GetMapping(Const.Path.SEARCH)
    public JsonNode search(@RequestParam String q, @RequestParam(defaultValue = Const.Common.REANIME) String source) throws Exception {
        JsonNode response = animeSourceService.search(q, source);
        return response;
    }

    @GetMapping("/anime/{slug}/episodes")
    public JsonNode episodes(@PathVariable String slug, @RequestParam(defaultValue = Const.Common.REANIME) String source) throws Exception {
        JsonNode response = animeSourceService.episodes(slug, source);
        return response;
    }

    @PostMapping("/embed")
    public Map<String, String> embed(@Valid @RequestBody com.ergoproxy.platform.dto.original.OriginalDownloadRequestDTO request,
                                     @RequestParam(defaultValue = Const.Common.REANIME) String source) throws Exception {
        Map<String, String> response = animeSourceService.embed(request, source);
        return response;
    }

    @PostMapping("/downloads")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DownloadResponseDTO create(@Valid @RequestBody DownloadRequestDTO request, @RequestParam(defaultValue = Const.Common.REANIME) String source)
        throws Exception {
        DownloadResponseDTO response = downloadService.create(request, source);
        return response;
    }

    @GetMapping("/downloads/{id}")
    public DownloadResponseDTO status(@PathVariable UUID id) throws Exception {
        DownloadResponseDTO response = downloadService.get(id);
        return response;
    }

    @GetMapping("/downloads/{id}/files/{fileId}")
    public ResponseEntity<FileSystemResource> file(@PathVariable UUID id, @PathVariable String fileId) throws Exception {
        ResponseEntity<FileSystemResource> response = downloadService.downloadFile(id, fileId);
        return response;
    }
}
