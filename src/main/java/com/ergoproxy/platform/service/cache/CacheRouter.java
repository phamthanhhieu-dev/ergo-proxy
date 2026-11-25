package com.ergoproxy.platform.service.cache;

import java.time.Duration;
import java.util.List;

public interface CacheRouter {

    String getSource();

    void set(String key, Object value, Duration duration);

    <T> T get(String key, Class<T> type);

    void hashSet(String key, String field, Object value, Duration duration);

    <T> T hashGet(String key, String field, Class<T> clazz);

    <T> List<T> hashValues(String key, Class<T> type);

    Boolean delete(String key);

    Boolean exists(String key);
}
