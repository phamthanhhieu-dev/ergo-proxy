package com.ergoproxy.platform.service;

import com.ergoproxy.platform.config.client.OriginalHttpClient;
import com.ergoproxy.platform.config.client.OriginalHttpClient.Body;
import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.original.OriginalDownloadRequestDTO;
import com.ergoproxy.platform.dto.original.OriginalDownloadResponseDTO;
import com.ergoproxy.platform.entity.OriginalDownloadTicket;
import com.ergoproxy.platform.exception.LogicException;
import com.ergoproxy.platform.util.Messages;
import com.ergoproxy.platform.util.SubtitleDiagnostics;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OriginalDownloadServiceImpl implements OriginalDownloadService {

    private final AnimeSourceService animeSourceService;
    private final OriginalHttpClient originalHttpClient;
    private final Map<UUID, OriginalDownloadTicket> tickets = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore slots = new Semaphore(4);

    static OriginalDownloadTicket parsePage(JsonNode page) throws IOException {
        for (JsonNode node : page.path(Const.Common.NODES)) {
            JsonNode serializedData = node.path(Const.Common.DATA);
            if (!serializedData.isArray() || serializedData.isEmpty()) {
                continue;
            }
            JsonNode root = serializedData.get(0),
                video = ref(serializedData, root, Const.Common.VIDEO),
                download = ref(serializedData, root, Const.Common.DOWNLOAD);
            if (video.isMissingNode() || download.isMissingNode()) {
                continue;
            }
            String name = ref(serializedData, video, Const.Common.FILENAME).asText(),
                fileId = ref(serializedData, video, Const.Common.FILE_ID_CAMEL).asText();
            String base = ref(serializedData, download, Const.Common.BASE).asText(),
                token = ref(serializedData, download, Const.Common.TOKEN).asText();
            URI origin = URI.create(base);
            OriginalHttpClient.validate(origin);
            if (
                !origin.getHost().matches(Const.Regex.FLIXCLOUD_FETCH_HOST) || !fileId.matches(Const.Regex.FILE_IDENTIFIER) || token.isBlank() || name.isBlank()
            ) {
                break;
            }
            name = name.replaceAll(Const.Regex.UNSAFE_FILENAME_CHARACTERS, Const.Common.UNDERSCORE);
            URI file = URI.create(
                base + Const.Path.DOWNLOAD + Const.Common.SLASH + fileId + Const.Common.QUERY_TOKEN_PREFIX + URLEncoder.encode(token, StandardCharsets.UTF_8)
            );
            URI progress = URI.create(
                base +
                    Const.Path.DOWNLOAD +
                    Const.Common.SLASH +
                    fileId +
                    Const.Path.PROGRESS +
                    Const.Common.QUERY_TOKEN_PREFIX +
                    URLEncoder.encode(token, StandardCharsets.UTF_8)
            );
            OriginalDownloadTicket response = new OriginalDownloadTicket(name, file, progress);
            return response;
        }
        throw new LogicException("PPOD_00001", "original.page.data.invalid");
    }

    private static JsonNode ref(JsonNode array, JsonNode object, String key) {
        JsonNode index = object.path(key);
        JsonNode response =
            index.isInt() && index.asInt() >= 0 && index.asInt() < array.size()
                ? array.get(index.asInt())
                : tools.jackson.databind.node.MissingNode.getInstance();
        return response;
    }

    private static String safeError(Exception ex) {
        if (ex instanceof LogicException) {
            String response = Messages.fromException(ex);
            return response;
        }

        if (ex instanceof IOException || ex instanceof IllegalArgumentException) {
            String response = SubtitleDiagnostics.sanitize(Messages.fromException(ex));
            return response;
        }
        String response = Messages.get("original.source.failed", ex.getClass().getSimpleName());
        return response;
    }

    private OriginalDownloadResponseDTO view(OriginalDownloadTicket ticket) {
        OriginalDownloadResponseDTO response = new OriginalDownloadResponseDTO(
            ticket.getId(),
            ticket.getFilename(),
            ticket.getState(),
            ticket.getPercent(),
            ticket.getMessage()
        );
        return response;
    }

    @Override
    public OriginalDownloadResponseDTO prepare(OriginalDownloadRequestDTO request) throws Exception {
        OriginalDownloadResponseDTO response = prepare(request, Const.Common.REANIME);
        return response;
    }

    @Override
    public OriginalDownloadResponseDTO prepare(OriginalDownloadRequestDTO request, String source) throws Exception {
        AnimeProvider provider = animeSourceService.resolve(source);
        Iterator<Map.Entry<UUID, OriginalDownloadTicket>> iterator = tickets.entrySet().iterator();
        Instant now = Instant.now();
        while (iterator.hasNext()) {
            OriginalDownloadTicket ticket = iterator.next().getValue();
            boolean expired = ticket.getExpires().isBefore(now);
            if (expired && ticket.getActive() == null) {
                iterator.remove();
            }
        }
        if (tickets.size() >= 100 || !slots.tryAcquire()) {
            throw new LogicException("POD_00001", "original.queue.full");
        }
        try {
            String page = provider.downloadPage(request);
            URI pageUri = URI.create(page);
            OriginalHttpClient.validate(pageUri);
            if (
                !pageUri.getHost().equals(Const.Common.FLIXCLOUD_HOST) ||
                !pageUri.getPath().matches(Const.Regex.FLIXCLOUD_DOWNLOAD_PATH) ||
                pageUri.getRawQuery() != null
            ) {
                throw new LogicException("POD_00002", "original.page.url.invalid");
            }
            JsonNode data = originalHttpClient.json(URI.create(page.replaceAll(Const.Regex.TRAILING_SLASH, Const.Common.EMPTY) + Const.Path.PAGE_DATA));
            OriginalDownloadTicket ticket = parsePage(data);

            tickets.put(ticket.getId(), ticket);
            ticket.setTask(
                executor.submit(() -> {
                    try {
                        awaitSource(ticket);
                    } catch (Exception exception) {
                        ticket.setState(Const.Common.FAILED);
                        ticket.setMessage(exception instanceof InterruptedException ? Messages.get("original.status.cancelled") : safeError(exception));
                    } finally {
                        slots.release();
                    }
                })
            );
            OriginalDownloadResponseDTO response = view(ticket);
            return response;
        } catch (Exception ex) {
            slots.release();
            if (ex instanceof LogicException existingLogicException) {
                throw existingLogicException;
            }
            throw new LogicException(ex, "POD_00003", "original.request.failed", SubtitleDiagnostics.sanitize(safeError(ex)));
        }
    }

    private void awaitSource(OriginalDownloadTicket ticket) throws Exception {
        Thread.sleep(12000); // Same countdown as the public Download page; never skip it.
        ticket.setState(Const.Common.PREPARING);
        ticket.setMessage(Messages.get("original.source.preparing"));
        try (OriginalHttpClient.Body body = originalHttpClient.open(ticket.getProgress())) {
            ticket.setActive(body);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(body.stream(), StandardCharsets.UTF_8))) {
                String line;
                Instant deadline = Instant.now().plus(Duration.ofMinutes(20));
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted() || Instant.now().isAfter(deadline)) {
                        throw new LogicException("ASOD_00001", "original.source.timeout");
                    }
                    if (!line.startsWith(Const.Common.SSE_DATA_PREFIX)) {
                        continue;
                    }
                    JsonNode event = new ObjectMapper().readTree(line.substring(Const.Common.SSE_DATA_PREFIX.length()).trim());
                    ticket.setPercent(Math.max(0, Math.min(100, event.path(Const.Common.PERCENT).asDouble())));
                    String status = event.path(Const.Common.STATUS).asText();
                    switch (status) {
                        case Const.Common.SOURCE_READY -> {
                            ticket.setState(Const.Common.READY);
                            ticket.setMessage(Messages.get("original.source.ready"));
                            return;
                        }
                        case Const.Common.SOURCE_FAILED -> throw new LogicException("ASOD_00002", "original.source.preparation.failed");
                        default -> {
                            /* Continue receiving progress events. */
                        }
                    }
                }
            }
        } finally {
            ticket.setActive(null);
        }
        throw new LogicException("ASOD_00003", "original.source.disconnected");
    }

    private OriginalDownloadTicket ticket(UUID id) {
        OriginalDownloadTicket ticket = tickets.get(id);
        if (ticket == null || ticket.getExpires().isBefore(Instant.now())) {
            throw new LogicException("TOD_00001", "original.id.not.found");
        }
        return ticket;
    }

    @Override
    public OriginalDownloadResponseDTO status(UUID id) {
        OriginalDownloadResponseDTO response = view(ticket(id));
        return response;
    }

    @Override
    public OriginalHttpClient.Body content(UUID id) throws IOException {
        OriginalDownloadTicket ticket = ticket(id);

        synchronized (ticket) {
            if (!ticket.getState().equals(Const.Common.READY)) {
                throw new LogicException("COD_00001", "original.file.not.ready");
            }
            ticket.setState(Const.Common.DOWNLOADING);
        }
        try {
            OriginalHttpClient.Body body = originalHttpClient.open(ticket.getFile());
            String type = Objects.toString(body.type(), Const.Common.EMPTY).toLowerCase(Locale.ROOT);
            if (type.contains(Const.Common.HTML) || type.contains(Const.Common.JSON)) {
                body.close();
                throw new LogicException("COD_00002", "original.file.invalid");
            }
            ticket.setActive(body);
            return body;
        } catch (Exception ex) {
            ticket.setState(Const.Common.FAILED);
            ticket.setMessage(safeError(ex));
            if (ex instanceof LogicException existingLogicException) {
                throw existingLogicException;
            }
            throw new LogicException(ex, "COD_00003", "original.request.failed", SubtitleDiagnostics.sanitize(ticket.getMessage()));
        }
    }

    @Override
    public void finished(UUID id, boolean success) {
        OriginalDownloadTicket ticket = tickets.get(id);
        if (ticket == null) {
            return;
        }
        ticket.setActive(null);
        ticket.setState(success ? Const.Common.COMPLETED : Const.Common.FAILED);
        ticket.setMessage(success ? Messages.get("original.status.completed") : Messages.get("original.transfer.interrupted"));
    }

    @Override
    public void cancel(UUID id) {
        OriginalDownloadTicket ticket = tickets.get(id);
        if (ticket == null) {
            return;
        }
        ticket.setState(Const.Common.FAILED);
        ticket.setMessage(Messages.get("original.status.cancelled"));
        if (ticket.getTask() != null) {
            ticket.getTask().cancel(true);
        }
        if (ticket.getActive() != null) {
            try {
                ticket.getActive().close();
            } catch (IOException ignored) {}
        }
    }

    @PreDestroy
    @Override
    public void stop() {
        for (UUID id : tickets.keySet()) {
            cancel(id);
        }
        executor.shutdownNow();
    }

    @Override
    public ResponseEntity<StreamingResponseBody> downloadContent(UUID id) throws Exception {
        String name = status(id).getFilename();
        Body body = content(id);
        StreamingResponseBody stream = output -> {
            boolean success = false;
            try (body) {
                long count = body.stream().transferTo(output);
                if (body.length() >= 0 && count != body.length()) {
                    throw new LogicException("DCOD_00001", "original.video.incomplete");
                }
                output.flush();
                success = true;
            } finally {
                finished(id, success);
            }
        };
        ContentDisposition.Builder dispositionBuilder = ContentDisposition.attachment();
        dispositionBuilder.filename(name, StandardCharsets.UTF_8);
        ContentDisposition disposition = dispositionBuilder.build();
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        response.contentType(MediaType.APPLICATION_OCTET_STREAM);
        response.header(HttpHeaders.CACHE_CONTROL, Const.Common.HTTP_NO_STORE);
        response.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        if (body.length() >= 0) {
            response.contentLength(body.length());
        }
        ResponseEntity<StreamingResponseBody> result = response.body(stream);
        return result;
    }
}
