package com.ergoproxy.platform.service.cache;

import com.ergoproxy.platform.common.exception.LogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final List<CacheRouter> contracts;

    private CacheRouter getSource(String source) {
        return contracts.stream()
            .filter(contract -> contract.getSource().equals(source))
            .findFirst()
            .orElseThrow(() -> new LogicException("GSC_00001", "host.source.unsupported", source));
    }

    @Override
    public void set(String source, String key, Object value, Duration duration) {
        getSource(source).set(key, value, duration);
    }

    @Override
    public <T> T get(String source, String key, Class<T> clazz) {
        return getSource(source).get(key, clazz);
    }

    @Override
    public void hashSet(String source, String key, String field, Object value, Duration duration) {
        getSource(source).hashSet(key, field, value, duration);
    }

    @Override
    public <T> T hashGet(String source, String key, String field, Class<T> clazz) {
        return getSource(source).hashGet(key, field, clazz);
    }

    @Override
    public <T> List<T> hashValues(String source, String key, Class<T> clazz) {
        return getSource(source).hashValues(key, clazz);
    }

    @Override
    public Boolean delete(String source, String key) {
        return getSource(source).delete(key);
    }

    @Override
    public Boolean exists(String source, String key) {
        return getSource(source).exists(key);
    }
}
