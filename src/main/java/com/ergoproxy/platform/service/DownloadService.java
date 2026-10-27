package com.ergoproxy.platform.service;

import com.ergoproxy.platform.dto.download.DownloadFileResponseDTO;
import com.ergoproxy.platform.dto.download.DownloadRequestDTO;
import com.ergoproxy.platform.dto.download.DownloadResponseDTO;
import com.ergoproxy.platform.dto.download.DownloadVideoTargetResponseDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleFileDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleSaveRequestDTO;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public interface DownloadService {
    DownloadResponseDTO create(DownloadRequestDTO request) throws Exception;

    DownloadResponseDTO create(DownloadRequestDTO request, String source) throws Exception;

    DownloadResponseDTO get(UUID id) throws Exception;

    List<DownloadVideoTargetResponseDTO> videoTargets(String slug);

    DownloadFileResponseDTO saveSubtitle(UUID jobId, String videoId, String language, Supplier<SubtitleFileDTO> content) throws Exception;

    Path file(UUID id, String fileId);

    void stop();

    ResponseEntity<FileSystemResource> downloadFile(UUID id, String fileId) throws Exception;

    DownloadFileResponseDTO saveSubtitle(SubtitleSaveRequestDTO request) throws Exception;
}
