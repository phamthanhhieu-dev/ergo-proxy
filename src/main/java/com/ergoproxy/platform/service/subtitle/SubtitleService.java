package com.ergoproxy.platform.service.subtitle;

import java.util.List;

public interface SubtitleService {

    List<?> search(String source, String query, String language, int season, int episode, String imdb, boolean movie, int page);

}
