package com.ergoproxy.platform.service.subtitle;

import java.util.List;

public interface SubtitleRouter {
    String getSource();

    List<?> search(String movieName, String language, Integer seasonNumber, Integer episodeNumber, Integer year, String type, Integer page, Integer size);
}
