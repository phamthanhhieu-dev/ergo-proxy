package com.ergoproxy.platform.service;

import com.ergoproxy.platform.dto.subtitle.SubtitleDownloadRequestDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleFileDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleSearchResponseDTO;
import java.util.Map;
import org.springframework.http.ResponseEntity;

public interface OpenSubtitleService {
    Map<String, Boolean> configuration();

    SubtitleSearchResponseDTO search(String query, String language, int season, int episode, String imdb, boolean movie, int page);

    SubtitleFileDTO download(long fileId);

    ResponseEntity<byte[]> nativeDownload(long fileId);

    ResponseEntity<byte[]> downloadResponse(SubtitleDownloadRequestDTO request);
}
