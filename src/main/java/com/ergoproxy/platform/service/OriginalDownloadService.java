package com.ergoproxy.platform.service;

import com.ergoproxy.platform.config.client.OriginalHttpClient;
import com.ergoproxy.platform.dto.original.OriginalDownloadRequestDTO;
import com.ergoproxy.platform.dto.original.OriginalDownloadResponseDTO;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface OriginalDownloadService {
    OriginalDownloadResponseDTO prepare(OriginalDownloadRequestDTO request) throws Exception;

    OriginalDownloadResponseDTO prepare(OriginalDownloadRequestDTO request, String source) throws Exception;

    OriginalDownloadResponseDTO status(UUID id);

    OriginalHttpClient.Body content(UUID id) throws IOException;

    void finished(UUID id, boolean success);

    void cancel(UUID id);

    void stop();

    ResponseEntity<StreamingResponseBody> downloadContent(UUID id) throws Exception;
}
