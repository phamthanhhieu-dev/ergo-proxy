package com.ergoproxy.platform.service.subtitle;

import java.util.List;

public interface SubtitleService {

    List<?> search(String source, String movieName, String language, Integer seasonNumber, Integer episodeNumber, Integer year, String type, Integer page, Integer size);

}
