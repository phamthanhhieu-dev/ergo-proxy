package com.ergoproxy.platform.service.video;

import java.util.List;

public interface VideoRouter {
    String getSource();

    List<?> search(String query);
}
