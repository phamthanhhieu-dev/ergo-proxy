package com.ergoproxy.platform.service;

import com.ergoproxy.platform.entity.playback.LibraryFile;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaybackMediaServiceImpl implements PlaybackMediaService {

    private final PlaybackLibraryService libraryService;

    @Override
    public ResponseEntity<Resource> file(UUID id) {
        LibraryFile file = libraryService.find(id);
        String contentType;
        switch (file.extension()) {
            case "mp4", "m4v" -> contentType = "video/mp4";
            case "webm" -> contentType = "video/webm";
            case "mkv" -> contentType = "video/x-matroska";
            case "mov" -> contentType = "video/quicktime";
            case "ts" -> contentType = "video/mp2t";
            case "vtt" -> contentType = "text/vtt; charset=UTF-8";
            case "ass", "ssa", "srt" -> contentType = "text/plain; charset=UTF-8";
            case "ttf" -> contentType = "font/ttf";
            case "otf" -> contentType = "font/otf";
            case "woff" -> contentType = "font/woff";
            case "woff2" -> contentType = "font/woff2";
            default -> contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        Resource resource = new FileSystemResource(file.path());
        ResponseEntity<Resource> response = ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).contentLength(file.size())
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline().filename(file.path().getFileName().toString(), StandardCharsets.UTF_8).build().toString()
            )
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(resource);
        return response;
    }
}
