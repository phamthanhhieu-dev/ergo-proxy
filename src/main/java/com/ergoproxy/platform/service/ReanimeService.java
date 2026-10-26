package com.ergoproxy.platform.service;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import tools.jackson.databind.JsonNode;

public interface ReanimeService extends AnimeProvider {
    JsonNode json(String... args) throws Exception;

    String run(List<String> args, Duration timeout) throws Exception;

    String run(List<String> args, Duration timeout, Consumer<String> onLine) throws Exception;
}
