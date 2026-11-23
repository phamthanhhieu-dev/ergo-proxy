package com.ergoproxy.platform.service.cache;

import java.time.Duration;
import java.util.List;

public interface CacheService {

    void set(String source, String key, Object value, Duration duration);

    <T> T get(String source, String key, Class<T> clazz);

    void hashSet(String source, String key, String field, Object value, Duration duration);

    <T> List<T> hashValues(String source, String key, Class<T> clazz);

    Boolean delete(String source, String key);

    Boolean exists(String source, String key);
}
