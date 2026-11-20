package com.ergoproxy.platform.service.subtitle;

import java.util.List;

public interface SubtitleRouter {
    String getSource();

    List<?> search(String query);
}
