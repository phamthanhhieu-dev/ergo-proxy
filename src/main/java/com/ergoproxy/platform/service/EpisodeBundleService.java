package com.ergoproxy.platform.service;

import com.ergoproxy.platform.dto.bundle.PreparedEpisodeBundleDTO;
import java.io.IOException;
import java.io.OutputStream;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface EpisodeBundleService {
    PreparedEpisodeBundleDTO prepare(String payload) throws Exception;

    PreparedEpisodeBundleDTO prepare(String payload, String source) throws Exception;

    void write(PreparedEpisodeBundleDTO prepared, OutputStream output) throws IOException;

    ResponseEntity<StreamingResponseBody> download(String payload, String source) throws Exception;
}
