package com.ergoproxy.platform.service;

import com.ergoproxy.platform.config.client.OriginalHttpClient;
import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.bundle.EpisodeBundleRequestDTO;
import com.ergoproxy.platform.dto.bundle.EpisodeSubtitleRequestDTO;
import com.ergoproxy.platform.dto.bundle.PreparedEpisodeBundleDTO;
import com.ergoproxy.platform.dto.original.OriginalDownloadRequestDTO;
import com.ergoproxy.platform.dto.original.OriginalDownloadResponseDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleFileDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleSearchResponseDTO;
import com.ergoproxy.platform.exception.LogicException;
import com.ergoproxy.platform.util.Messages;
import com.ergoproxy.platform.util.SubtitleDiagnostics;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class EpisodeBundleServiceImpl implements EpisodeBundleService {

    private final OriginalDownloadService originalDownloadService;
    private final OpenSubtitleService openSubtitleService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    @Value("${flixcloud.api.url}")
    private String flixcloudApiUrl;

    static String component(String input, String fallback) {
        String name = Objects.toString(input, Const.Common.EMPTY).replaceAll(Const.Regex.UNSAFE_FILENAME_CHARACTERS, Const.Common.UNDERSCORE)
            .replaceAll(Const.Regex.FILENAME_EDGE_SPACES, Const.Common.EMPTY);
        if (name.isBlank() || name.equals(Const.Common.DOT) || name.equals(Const.File.PARENT_DIRECTORY)) {
            name = fallback;
        }
        if (name.matches(Const.Regex.WINDOWS_RESERVED_FILENAME)) {
            name = Const.Common.UNDERSCORE + name;
        }
        return name;
    }

    private static String safe(Exception ex) {
        String message = ex instanceof ResponseStatusException r ? r.getReason() : Messages.fromException(ex);
        String response = SubtitleDiagnostics.sanitize(Objects.toString(message, ex.getClass().getSimpleName()));
        return response;
    }

    private static void putText(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(Objects.toString(text, Const.Common.EMPTY).getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    @Override
    public PreparedEpisodeBundleDTO prepare(String payload) throws Exception {
        PreparedEpisodeBundleDTO response = prepare(payload, Const.Common.REANIME);
        return response;
    }

    @Override
    public PreparedEpisodeBundleDTO prepare(String payload, String providerSource) throws Exception {
        EpisodeBundleRequestDTO request;
        try {
            if (payload.length() > 200000) {
                throw new LogicException("PEB_00001", "bundle.request.invalid");
            }
            request = objectMapper.readValue(payload, EpisodeBundleRequestDTO.class);
            if (!validator.validate(request).isEmpty()) {
                throw new LogicException("PEB_00002", "bundle.request.invalid");
            }
            Set<Integer> episodes = new HashSet<>(request.getEpisodes());
            if (episodes.size() != request.getEpisodes().size()) {
                throw new LogicException("PEB_00003", "bundle.request.invalid");
            }
            Set<Integer> selected = new HashSet<>();
            if (request.getSubtitles() != null) {
                for (EpisodeSubtitleRequestDTO selection : request.getSubtitles()) {
                    if (!episodes.contains(selection.getEpisode()) || !selected.add(selection.getEpisode())) {
                        throw new LogicException("PEB_00004", "bundle.request.invalid");
                    }
                }
            }
            if (
                request.getPageUrl() != null &&
                    !request.getPageUrl().isBlank() &&
                    (episodes.size() != 1 || !request.getPageUrl().matches(Pattern.quote(flixcloudApiUrl) + Const.Regex.FLIXCLOUD_DOWNLOAD_PATH))
            ) {
                throw new LogicException("PEB_00005", "bundle.request.invalid");
            }
        } catch (Exception ex) {
            if (ex instanceof LogicException existingLogicException) {
                throw existingLogicException;
            }
            throw new LogicException(ex, "PEB_00006", "bundle.request.invalid");
        }
        if (!openSubtitleService.configuration().get(Const.Common.CONFIGURED)) {
            throw new LogicException("PEB_00007", "bundle.subtitle.key.is.null.or.empty");
        }
        OriginalDownloadResponseDTO first = originalDownloadService.prepare(source(request, request.getEpisodes().getFirst()), providerSource);
        PreparedEpisodeBundleDTO response = new PreparedEpisodeBundleDTO(
            request,
            first,
            component(request.getTitle(), Const.File.DEFAULT_ANIME_FOLDER),
            providerSource
        );
        return response;
    }

    private OriginalDownloadRequestDTO source(EpisodeBundleRequestDTO request, int episode) {
        OriginalDownloadRequestDTO response = new OriginalDownloadRequestDTO(
            request.getSlug(),
            episode,
            request.getServer(),
            request.getAudio(),
            request.getPageUrl()
        );
        return response;
    }

    @Override
    public void write(PreparedEpisodeBundleDTO prepared, OutputStream output) throws IOException {
        EpisodeBundleRequestDTO request = prepared.getRequest();
        Map<Integer, Long> selected = new HashMap<Integer, Long>();
        if (request.getSubtitles() != null) {
            for (EpisodeSubtitleRequestDTO subtitle : request.getSubtitles()) {
                selected.put(subtitle.getEpisode(), subtitle.getFileId());
            }
        }
        List<String> report = new ArrayList<String>();
        int videoCount = 0,
            subCount = 0;
        UUID activeTicket = prepared.getFirst().getId();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            // Video is already compressed. Level 0 avoids wasting CPU; Java ZIP supports ZIP64.
            zip.setLevel(0);
            putText(zip, prepared.getFolder() + Const.Common.SLASH, Const.Common.EMPTY);
            zip.flush();
            for (int index = 0; index < request.getEpisodes().size(); index++) {
                int episode = request.getEpisodes().get(index);
                String folder = prepared.getFolder() + Const.File.EPISODE_FOLDER_PREFIX + episode + Const.Common.SLASH;
                putText(zip, folder, Const.Common.EMPTY);
                zip.flush();
                OriginalDownloadResponseDTO view = null;
                OriginalHttpClient.Body body = null;
                String videoName = null,
                    error = null;
                try {
                    view = index == 0 ? prepared.getFirst() : originalDownloadService.prepare(source(request, episode), prepared.getSource());
                    activeTicket = view.getId();
                    Instant deadline = Instant.now().plus(Duration.ofMinutes(25));
                    while (!view.getState().equals(Const.Common.READY)) {
                        if (view.getState().equals(Const.Common.FAILED)) {
                            throw new LogicException("WEB_00001", "bundle.source.failed", SubtitleDiagnostics.sanitize(view.getMessage()));
                        }
                        if (Thread.currentThread().isInterrupted() || Instant.now().isAfter(deadline)) {
                            throw new LogicException("WEB_00002", "bundle.source.timeout");
                        }
                        Thread.sleep(250);
                        view = originalDownloadService.status(view.getId());
                    }
                    videoName = component(view.getFilename(), Const.File.EPISODE_PREFIX + episode + Const.Common.MKV_EXTENSION);
                    body = originalDownloadService.content(view.getId());
                } catch (Exception ex) {
                    if (ex instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        if (view != null) {
                            originalDownloadService.cancel(view.getId());
                        }
                        if (ex instanceof LogicException existingLogicException) {
                            throw existingLogicException;
                        }
                        throw new LogicException(ex, "WEB_00003", "bundle.status.cancelled");
                    }
                    error = safe(ex);
                }
                if (body != null) {
                    boolean success = false;
                    try (OriginalHttpClient.Body sourceBody = body) {
                        zip.putNextEntry(new ZipEntry(folder + videoName));
                        long copied = 0;
                        byte[] buffer = new byte[128 * 1024];
                        while (true) {
                            int count;
                            try {
                                count = sourceBody.stream().read(buffer);
                            } catch (IOException ex) {
                                error = Messages.get("bundle.video.disconnected");
                                break;
                            }
                            if (count < 0) {
                                break;
                            }
                            zip.write(buffer, 0, count);
                            copied += count; // Destination errors propagate; stop on browser
                            // cancellation.
                        }
                        zip.closeEntry();
                        zip.flush();
                        if (error == null && sourceBody.length() >= 0 && copied != sourceBody.length()) {
                            error = Messages.get("bundle.video.incomplete");
                        }
                        success = error == null;
                        if (success) {
                            videoCount++;
                        }
                    } finally {
                        originalDownloadService.finished(view.getId(), success);
                    }
                }
                if (error != null) {
                    if (view != null) {
                        originalDownloadService.cancel(view.getId());
                    }
                    report.add(Const.File.EPISODE_PREFIX + episode + Const.Common.COLON_SPACE + error);
                    putText(zip, folder + Const.File.VIDEO_ERROR_REPORT, error);
                }
                // Sub failure must not discard a successfully downloaded video.
                SubtitleFileDTO sub = null;
                String subError = null;
                try {
                    Long fileId = selected.get(episode);
                    if (fileId == null) {
                        SubtitleSearchResponseDTO result = openSubtitleService.search(
                            request.getTitle(),
                            request.getLanguage(),
                            request.getSeason(),
                            episode,
                            Objects.toString(request.getImdb(), Const.Common.EMPTY),
                            request.isMovie(),
                            1
                        );
                        if (!result.getResults().isEmpty()) {
                            fileId = result.getResults().getFirst().getFileId();
                        }
                    }
                    if (fileId == null) {
                        subError =
                            Messages.get("bundle.subtitle.not.found", episode) +
                                Const.Common.OPEN_PARENTHESIS +
                                request.getLanguage() +
                                Const.Common.CLOSE_PARENTHESIS_PERIOD;
                    } else {
                        sub = openSubtitleService.download(fileId);
                    }
                } catch (Exception ex) {
                    subError = safe(ex);
                }
                if (sub != null) {
                    String subtitleName = component(sub.getFileName(), Const.File.DEFAULT_SUBTITLE_FILENAME);
                    zip.putNextEntry(new ZipEntry(folder + subtitleName));
                    zip.write(sub.getBytes());
                    zip.closeEntry();
                    subCount++;
                } else {
                    report.add(Const.File.EPISODE_PREFIX + episode + Const.File.SUBTITLE_ERROR_LABEL + subError);
                    putText(zip, folder + Const.File.SUBTITLE_ERROR_REPORT, subError);
                }
                zip.flush();
                activeTicket = null;
            }
            if (!report.isEmpty()) {
                putText(
                    zip,
                    prepared.getFolder() + Const.File.DOWNLOAD_REPORT_PATH,
                    Messages.get("bundle.report.summary", videoCount, request.getEpisodes().size(), subCount) + String.join(Const.Common.NEWLINE, report)
                );
            }
            zip.finish();
        } finally {
            if (activeTicket != null) {
                originalDownloadService.cancel(activeTicket);
            }
        }
    }

    @Override
    public ResponseEntity<StreamingResponseBody> download(String payload, String source) throws Exception {
        PreparedEpisodeBundleDTO prepared = prepare(payload, source);
        String filename = prepared.getFolder() + Const.Common.ZIP_EXTENSION;
        ContentDisposition.Builder dispositionBuilder = ContentDisposition.attachment();
        dispositionBuilder.filename(filename, StandardCharsets.UTF_8);
        ContentDisposition disposition = dispositionBuilder.build();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        builder.contentType(MediaType.parseMediaType(Const.Common.APPLICATION_ZIP));
        builder.header(HttpHeaders.CACHE_CONTROL, Const.Common.HTTP_NO_STORE);
        builder.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        StreamingResponseBody stream = output -> write(prepared, output);
        ResponseEntity<StreamingResponseBody> response = builder.body(stream);
        return response;
    }
}
