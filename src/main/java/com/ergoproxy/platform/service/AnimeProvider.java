package com.ergoproxy.platform.service;

import com.ergoproxy.platform.dto.original.OriginalDownloadRequestDTO;
import tools.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface AnimeProvider {
    String id();

    String displayName();

    JsonNode search(String query) throws Exception;

    JsonNode episodes(String slug) throws Exception;

    String embedUrl(OriginalDownloadRequestDTO request) throws Exception;

    String downloadPage(OriginalDownloadRequestDTO request) throws Exception;

    void downloadEpisode(String slug, int episode, int quality, String audio, Path directory, Consumer<String> onLine) throws Exception;
}
