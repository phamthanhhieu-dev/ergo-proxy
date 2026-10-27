package com.ergoproxy.platform.service;

import com.ergoproxy.platform.dto.subtitle.PreparedSubtitleArchiveDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;

public interface SubtitleArchiveService {
    PreparedSubtitleArchiveDTO prepare(String payload) throws Exception;

    void write(PreparedSubtitleArchiveDTO prepared, OutputStream output) throws IOException;

    ResponseEntity<StreamingResponseBody> downloadArchive(String payload) throws Exception;
}
