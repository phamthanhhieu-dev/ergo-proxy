package com.ergoproxy.platform.service;

import com.ergoproxy.platform.config.client.OriginalHttpClient;
import com.ergoproxy.platform.config.logging.ApiLogWriter;
import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.original.OriginalDownloadRequestDTO;
import com.ergoproxy.platform.exception.LogicException;
import com.ergoproxy.platform.util.DataUtil;
import com.ergoproxy.platform.util.Messages;
import com.ergoproxy.platform.util.SubtitleDiagnostics;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReanimeServiceImpl implements ReanimeService {

    private static final Pattern ANSI_ESCAPE = Pattern.compile(Const.Regex.ANSI_ESCAPE);
    private final ApiLogWriter apiLogWriter;
    private final OriginalHttpClient originalHttpClient;
    private final ObjectMapper objectMapper;
    @Value("${anime.python}")
    private String python;
    @Value("${reanime.api.url}")
    private String reanimeApiUrl;
    @Value("${flixcloud.api.url}")
    private String flixcloudApiUrl;

    @Override
    public String id() {
        String response = Const.Common.REANIME;
        return response;
    }

    @Override
    public String displayName() {
        String response = Const.Common.REANIME_NAME;
        return response;
    }

    @Override
    public JsonNode search(String query) throws Exception {
        validateQuery(query);
        String animeUrlPrefix = reanimeApiUrl + Const.Path.ANIME;
        if (query.startsWith(animeUrlPrefix)) {
            String slug = query.substring(animeUrlPrefix.length()).replaceAll(Const.Regex.TRAILING_SLASH, Const.Common.EMPTY);
            JsonNode response = animeInfo(slug);
            return response;
        }
        JsonNode response = json(Const.Common.COMMAND_SEARCH, query, Const.Common.OPTION_JSON, Const.Common.OPTION_LIMIT, Const.Python.SEARCH_LIMIT);
        return response;
    }

    @Override
    public JsonNode episodes(String slug) throws Exception {
        validateSlug(slug);
        JsonNode response = json(Const.Common.COMMAND_EPISODES, slug, Const.Common.OPTION_JSON, Const.Common.OPTION_YES);
        return response;
    }

    private JsonNode animeInfo(String slug) throws Exception {
        validateSlug(slug);
        JsonNode response = json(Const.Common.COMMAND_INFO, slug, Const.Common.OPTION_JSON, Const.Common.OPTION_YES);
        return response;
    }

    private void validateQuery(String query) {
        if (DataUtil.isNullOrBlank(query) || query.length() > Const.Python.MAX_QUERY_LENGTH || query.startsWith(Const.Python.OPTION_PREFIX)) {
            throw new LogicException("VQR_00001", "anime.query.invalid");
        }
    }

    private void validateSlug(String slug) {
        if (DataUtil.isNullOrBlank(slug) || !slug.matches(Const.Regex.ANIME_SLUG)) {
            throw new LogicException("VSR_00001", "anime.id.invalid");
        }
    }

    @Override
    public JsonNode json(String... arguments) throws Exception {
        String output = run(List.of(arguments), Const.Python.JSON_TIMEOUT);
        JsonNode response = objectMapper.readTree(output);
        return response;
    }

    @Override
    public String run(List<String> arguments, Duration timeout) throws Exception {
        String response = run(arguments, timeout, null);
        return response;
    }

    @Override
    public String run(List<String> arguments, Duration timeout, Consumer<String> onLine) throws Exception {
        String bridgeScript = loadBridgeScript();
        Process process = createProcessBuilder(arguments).start();
        try (ExecutorService readerExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                Future<String> output = readerExecutor.submit(() -> readProcessOutput(process, onLine));
                writeBridgeScript(process, bridgeScript);
                waitForProcess(process, timeout);
                String response = collectResult(process, output);
                return response;
            } finally {
                stopProcess(process);
            }
        }
    }

    private String loadBridgeScript() throws IOException {
        ClassPathResource bridge = new ClassPathResource(Const.Python.BRIDGE_RESOURCE);
        try (InputStream input = bridge.getInputStream()) {
            String response = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return response;
        }
    }

    private ProcessBuilder createProcessBuilder(List<String> arguments) {
        List<String> command = new ArrayList<>(List.of(python, Const.Python.UNBUFFERED_OPTION, Const.Python.STDIN_SCRIPT));
        command.addAll(arguments);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Map<String, String> environment = builder.environment();
        environment.put(Const.Environment.PYTHON_IO_ENCODING, Const.Common.UTF_8);
        environment.put(Const.Environment.REANIME_API_URL, reanimeApiUrl);
        environment.put(Const.Environment.FLIXCLOUD_API_URL, flixcloudApiUrl);
        environment.put(Const.Environment.API_LOG_ENABLED, Boolean.toString(apiLogWriter.enabled()));
        environment.put(Const.Environment.API_LOG_LIMIT, Integer.toString(apiLogWriter.limit()));
        return builder;
    }

    private void writeBridgeScript(Process process, String script) throws IOException {
        // Pass the script through stdin to avoid Windows command-line quoting problems.
        try (OutputStream input = process.getOutputStream()) {
            input.write(script.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readProcessOutput(Process process, Consumer<String> onLine) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String cleanLine = ANSI_ESCAPE.matcher(line).replaceAll(Const.Common.EMPTY);
                if (handleHttpLog(cleanLine)) {
                    continue;
                }
                publishProgress(cleanLine, onLine);
                appendOutput(output, cleanLine, onLine != null);
            }
            String response = output.toString();
            return response;
        } catch (IOException | RuntimeException exception) {
            stopProcess(process);
            if (exception instanceof LogicException existingLogicException) {
                throw existingLogicException;
            }
            throw new LogicException(exception, "RPOR_00001", "anime.process.failed", Messages.fromException(exception));
        }
    }

    private boolean handleHttpLog(String line) {
        if (!line.startsWith(Const.Common.PYTHON_HTTP_LOG_PREFIX)) {
            boolean response = false;
            return response;
        }
        logPythonHttp(line.substring(Const.Common.PYTHON_HTTP_LOG_PREFIX.length()));
        boolean response = true;
        return response;
    }

    private void publishProgress(String line, Consumer<String> onLine) {
        if (onLine != null && !line.isBlank()) {
            onLine.accept(line);
        }
    }

    private void appendOutput(StringBuilder output, String line, boolean progressEnabled) throws IOException {
        output.append(line).append(Const.Common.NEWLINE);
        if (progressEnabled && output.length() > Const.Python.PROGRESS_BUFFER_LIMIT) {
            output.delete(0, output.length() - Const.Python.PROGRESS_BUFFER_LIMIT);
        }
        if (output.length() > Const.Python.MAX_OUTPUT_CHARACTERS) {
            throw new LogicException("AOR_00001", "anime.process.response.too.large");
        }
    }

    private void waitForProcess(Process process, Duration timeout) throws IOException, InterruptedException {
        if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new LogicException("WFPR_00001", "anime.process.timeout");
        }
    }

    private String collectResult(Process process, Future<String> output) throws Exception {
        String result = output.get(Const.Python.OUTPUT_WAIT_SECONDS, TimeUnit.SECONDS);
        if (process.exitValue() != Const.Python.SUCCESS_EXIT_CODE) {
            String error = result.substring(0, Math.min(result.length(), Const.Python.ERROR_OUTPUT_LIMIT));
            throw new LogicException("CRR_00001", "anime.process.failed", SubtitleDiagnostics.sanitize(error));
        }
        return result;
    }

    private void stopProcess(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        if (!process.isAlive()) {
            return;
        }
        process.destroyForcibly();
        try {
            process.waitFor(Const.Python.STOP_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void logPythonHttp(String event) {
        try {
            JsonNode data = objectMapper.readTree(event);
            URI uri = URI.create(data.path(Const.Common.URI).asText());
            String method = data.path(Const.Common.METHOD).asText();
            String request = formatRequestLog(data);
            String response = formatResponseLog(data);
            long duration = data.path(Const.Common.DURATION_MS).asLong();
            long started = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(duration);
            apiLogWriter.write(uri, method, request, response, started);
        } catch (Exception ignored) {
            // Malformed diagnostic events must not interrupt downloads.
        }
    }

    private String formatRequestLog(JsonNode event) throws IOException {
        byte[] request = objectMapper.writeValueAsBytes(event.path(Const.Common.REQUEST));
        String response = apiLogWriter.body(request, Const.Common.APPLICATION_JSON, false);
        return response;
    }

    private String formatResponseLog(JsonNode event) {
        String status = event.path(Const.Common.STATUS).asText();
        JsonNode error = event.path(Const.Common.ERROR);
        if (error.isTextual()) {
            String response = status + Const.Common.SPACE + error.asText();
            return response;
        }
        byte[] body = event.path(Const.Common.BODY).asText().getBytes(StandardCharsets.UTF_8);
        String contentType = event.path(Const.Common.CONTENT_TYPE).asText();
        boolean truncated = event.path(Const.Common.TRUNCATED).asBoolean();
        String response = status + Const.Common.SPACE + apiLogWriter.body(body, contentType, truncated);
        return response;
    }

    @Override
    public String downloadPage(OriginalDownloadRequestDTO request) throws Exception {
        if (!DataUtil.isNullOrBlank(request.getPageUrl())) {
            String response = request.getPageUrl();
            return response;
        }
        JsonNode servers = loadServers(request);
        URI embed = findEmbed(servers, request);
        OriginalHttpClient.validate(embed);
        if (!Const.Common.FLIXCLOUD_HOST.equals(embed.getHost()) || !embed.getPath().matches(Const.Regex.FLIXCLOUD_EMBED_PATH)) {
            throw new LogicException("DPR_00001", "original.player.link.invalid");
        }
        String path = embed.getPath().replaceFirst(Const.Path.EMBED, Const.Path.DOWNLOAD_PAGE);
        String response = flixcloudApiUrl + path.replaceAll(Const.Regex.TRAILING_SLASH, Const.Common.EMPTY);
        return response;
    }

    @Override
    public String embedUrl(OriginalDownloadRequestDTO request) throws Exception {
        JsonNode servers = loadServers(request);
        URI embed = findEmbed(servers, request);
        OriginalHttpClient.validate(embed);
        if (!Const.Common.FLIXCLOUD_HOST.equals(embed.getHost()) || !embed.getPath().matches(Const.Regex.FLIXCLOUD_EMBED_PATH)) {
            throw new LogicException("EUR_00001", "original.player.link.invalid");
        }
        String response = embed.toString();
        return response;
    }

    private JsonNode loadServers(OriginalDownloadRequestDTO request) throws Exception {
        JsonNode anime = animeInfo(request.getSlug());
        long animeId = anime.path(Const.Common.ANILIST_ID).asLong(anime.path(Const.Common.ANILIST).asLong());
        if (animeId <= 0) {
            throw new LogicException("LSR_00001", "original.anilist.id.is.null.or.empty");
        }
        String endpoint = reanimeApiUrl + Const.Path.API + Const.Path.FLIX + animeId + Const.Common.SLASH + request.getEpisode();
        JsonNode response = originalHttpClient.json(URI.create(endpoint));
        return response;
    }

    private URI findEmbed(JsonNode servers, OriginalDownloadRequestDTO request) throws IOException {
        for (JsonNode server : servers.path(Const.Common.SERVERS)) {
            String serverName = server.path(Const.Common.SERVER_NAME).asText();
            String audio = server.path(Const.Common.DATA_TYPE).asText();
            boolean matchingServer = serverName.equals(request.getServer());
            boolean matchingAudio = audio.equals(request.getAudio()) || audio.equals(Const.Common.SOFTSUB_PREFIX + request.getAudio());
            if (matchingServer && matchingAudio) {
                URI response = URI.create(server.path(Const.Common.DATA_LINK).asText());
                return response;
            }
        }
        throw new LogicException("FER_00001", "original.server.not.found", request.getServer(), request.getEpisode());
    }

    @Override
    public void downloadEpisode(String slug, int episode, int quality, String audio, Path directory, Consumer<String> onLine) throws Exception {
        List<String> arguments = new ArrayList<>();
        arguments.addAll(List.of(Const.Common.DOWNLOAD, slug));
        arguments.addAll(List.of(Const.Python.EPISODE_OPTION, Integer.toString(episode)));
        arguments.addAll(List.of(Const.Python.QUALITY_OPTION, Integer.toString(quality)));
        arguments.addAll(List.of(Const.Common.OPTION_AUDIO, audio));
        arguments.addAll(List.of(Const.Common.OPTION_WORKERS, Const.Python.DOWNLOAD_WORKERS));
        arguments.add(Const.Common.OPTION_YES);
        arguments.addAll(List.of(Const.Python.OUTPUT_OPTION, directory.toString()));
        run(arguments, Const.Python.DOWNLOAD_TIMEOUT, onLine);
    }
}
