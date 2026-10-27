package com.ergoproxy.platform.service;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.download.DownloadFileResponseDTO;
import com.ergoproxy.platform.dto.download.DownloadRequestDTO;
import com.ergoproxy.platform.dto.download.DownloadResponseDTO;
import com.ergoproxy.platform.dto.download.DownloadVideoTargetResponseDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleFileDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleSaveRequestDTO;
import com.ergoproxy.platform.entity.DownloadJob;
import com.ergoproxy.platform.exception.LogicException;
import com.ergoproxy.platform.util.Messages;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DownloadServiceImpl implements DownloadService {

    private final AnimeSourceService animeSourceService;
    private final OpenSubtitleService openSubtitleService;
    private final Map<UUID, DownloadJob> jobs = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
    @Value("${anime.storage}")
    private String storage;

    private DownloadResponseDTO snapshot(DownloadJob job) throws IOException {
        synchronized (job) {
            List<DownloadFileResponseDTO> files = new ArrayList<>();
            for (Map.Entry<String, Path> entry : job.getPaths().entrySet()) {
                files.add(new DownloadFileResponseDTO(entry.getKey(), entry.getValue().getFileName().toString(), Files.size(entry.getValue())));
            }
            DownloadResponseDTO response = new DownloadResponseDTO(
                job.getId(),
                job.getStatus(),
                job.getCompleted(),
                job.getTotal(),
                job.getCurrent(),
                job.getError(),
                files,
                job.getProgress().view()
            );
            return response;
        }
    }

    private Path storagePath() {
        Path response = Path.of(storage).toAbsolutePath().normalize();
        return response;
    }

    @jakarta.annotation.PostConstruct
    void initializeStorage() throws java.io.IOException {
        Files.createDirectories(storagePath());
    }

    @Override
    public DownloadResponseDTO create(DownloadRequestDTO request) throws Exception {
        DownloadResponseDTO response = create(request, Const.Common.REANIME);
        return response;
    }

    @Override
    public DownloadResponseDTO create(DownloadRequestDTO request, String source) throws Exception {
        AnimeProvider provider = animeSourceService.resolve(source);
        JsonNode available = provider.episodes(request.getSlug());
        Set<Integer> playable = new TreeSet<>();
        for (JsonNode ep : available) {
            if (!ep.path(Const.Common.PLAYABLE).isBoolean() || ep.path(Const.Common.PLAYABLE).asBoolean()) {
                playable.add(ep.path(Const.Common.EPISODE_NUMBER).asInt());
            }
        }
        Set<Integer> selected = new TreeSet<>();
        if (request.isAll()) {
            selected.addAll(playable);
        } else if (request.getEpisodes() != null) {
            selected.addAll(request.getEpisodes());
        }
        if (selected.isEmpty() || !playable.containsAll(selected)) {
            throw new LogicException("CD_00001", "download.episodes.invalid");
        }
        DownloadJob job = DownloadJob.create(storagePath(), selected.size(), request.getSlug());
        jobs.put(job.getId(), job);
        try {
            worker.execute(() -> download(job, request, selected, provider));
        } catch (RejectedExecutionException ex) {
            jobs.remove(job.getId());

            throw new LogicException(ex, "CD_00002", "download.queue.full");
        }
        DownloadResponseDTO response = snapshot(job);
        return response;
    }

    private void download(DownloadJob job, DownloadRequestDTO request, Set<Integer> episodes, AnimeProvider provider) {
        try {
            Files.createDirectories(job.getDirectory());
            synchronized (job) {
                job.setStatus(Const.Common.RUNNING);
            }
            for (int episode : episodes) {
                synchronized (job) {
                    job.setCurrent(episode);
                }
                job.getProgress().start(episode);
                provider.downloadEpisode(request.getSlug(), episode, request.getQuality(), request.getAudio(), job.getDirectory(), job.getProgress()::line);
                try (Stream<Path> paths = Files.walk(job.getDirectory())) {
                    List<Path> videos = new ArrayList<>();
                    Iterator<Path> iterator = paths.iterator();
                    while (iterator.hasNext()) {
                        Path candidate = iterator.next();
                        boolean regularFile = Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS);
                        boolean videoFile = candidate.toString().endsWith(Const.Common.MKV_EXTENSION);
                        if (regularFile && videoFile) {
                            videos.add(candidate);
                        }
                    }
                    synchronized (job) {
                        int previous = job.getPaths().size();
                        for (Path p : videos) {
                            if (!job.getPaths().containsValue(p)) {
                                String fileId = UUID.randomUUID().toString();
                                job.getPaths().put(fileId, p);
                                job.getEpisodeNumbers().put(fileId, episode);
                            }
                        }
                        if (job.getPaths().size() <= previous) {
                            throw new LogicException("DD_00001", "download.video.not.found", episode);
                        }
                        job.setCompleted(job.getCompleted() + 1);
                        job.getProgress().line(Messages.get("download.episode.completed", episode));
                    }
                }
            }
            synchronized (job) {
                job.setStatus(Const.Common.COMPLETED);
            }
        } catch (Exception ex) {
            synchronized (job) {
                job.setStatus(Const.Common.FAILED);
                job.setError(Objects.toString(Messages.fromException(ex), Messages.get("download.status.failed")));
                job.getProgress().line(job.getError());
            }
        }
    }

    private DownloadJob job(UUID id) {
        DownloadJob job = jobs.get(id);
        if (job == null) {
            throw new LogicException("JD_00001", "download.id.not.found");
        }
        return job;
    }

    @Override
    public DownloadResponseDTO get(UUID id) throws Exception {
        DownloadResponseDTO response = snapshot(job(id));
        return response;
    }

    @Override
    public List<DownloadVideoTargetResponseDTO> videoTargets(String slug) {
        List<DownloadVideoTargetResponseDTO> targets = new ArrayList<DownloadVideoTargetResponseDTO>();
        for (DownloadJob job : jobs.values()) {
            synchronized (job) {
                if (!job.getSlug().equals(slug)) {
                    continue;
                }
                for (Map.Entry<String, Integer> entry : job.getEpisodeNumbers().entrySet()) {
                    Path path = job.getPaths().get(entry.getKey());
                    if (path != null && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                        targets.add(new DownloadVideoTargetResponseDTO(job.getId(), entry.getKey(), path.getFileName().toString(), entry.getValue()));
                    }
                }
            }
        }
        return targets;
    }

    @Override
    public DownloadFileResponseDTO saveSubtitle(UUID jobId, String videoId, String language, Supplier<SubtitleFileDTO> content) throws Exception {
        if (!Set.of(Const.Common.VI, Const.Common.EN, Const.Common.JA).contains(language)) {
            throw new LogicException("SSD_00001", "subtitle.language.invalid");
        }
        DownloadJob job = job(jobId);
        Path video = file(jobId, videoId);
        synchronized (job) {
            if (!job.getEpisodeNumbers().containsKey(videoId)) {
                throw new LogicException("SSD_00002", "download.video.not.completed");
            }
        }
        SubtitleFileDTO subtitle = content.get();
        byte[] bytes = subtitle.getBytes();
        String filename = OpenSubtitleServiceImpl.subtitleFilename(subtitle.getFileName(), bytes);
        Path destination = video.resolveSibling(filename);
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            throw new LogicException("SSD_00003", "subtitle.file.already.exist", destination.getFileName());
        }
        try {
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException ex) {
            throw new LogicException(ex, "SSD_00004", "subtitle.file.already.exist.generic");
        }
        String id = UUID.randomUUID().toString();
        synchronized (job) {
            job.getPaths().put(id, destination);
            job.getProgress().line(Messages.get("subtitle.file.saved", destination.getFileName()));
        }
        DownloadFileResponseDTO response = new DownloadFileResponseDTO(id, destination.getFileName().toString(), bytes.length);
        return response;
    }

    @Override
    public Path file(UUID id, String fileId) {
        DownloadJob job = job(id);
        synchronized (job) {
            Path path = job.getPaths().get(fileId);
            if (path == null || !path.normalize().startsWith(job.getDirectory()) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                throw new LogicException("FD_00001", "download.file.not.found");
            }
            return path;
        }
    }

    @PreDestroy
    @Override
    public void stop() {
        worker.shutdownNow();
    }

    @Override
    public ResponseEntity<FileSystemResource> downloadFile(UUID id, String fileId) throws Exception {
        Path path = file(id, fileId);
        String filename = path.getFileName().toString();
        String contentType = Const.Common.VIDEO_MATROSKA;
        String extension = filename.substring(filename.lastIndexOf(Const.Common.DOT) + 1).toLowerCase(Locale.ROOT);
        if (Const.Common.SRT.equals(extension)) {
            contentType = Const.Common.APPLICATION_SUBRIP;
        } else if (Const.Subtitle.SUPPORTED_EXTENSIONS.contains(extension)) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        ContentDisposition.Builder dispositionBuilder = ContentDisposition.attachment();
        dispositionBuilder.filename(filename, StandardCharsets.UTF_8);
        ContentDisposition disposition = dispositionBuilder.build();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        builder.contentType(MediaType.parseMediaType(contentType));
        builder.contentLength(Files.size(path));
        builder.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        FileSystemResource resource = new FileSystemResource(path);
        ResponseEntity<FileSystemResource> response = builder.body(resource);
        return response;
    }

    @Override
    public DownloadFileResponseDTO saveSubtitle(SubtitleSaveRequestDTO request) throws Exception {
        Supplier<SubtitleFileDTO> content = () -> {
            SubtitleFileDTO file = openSubtitleService.download(request.getFileId());
            return file;
        };
        DownloadFileResponseDTO response = saveSubtitle(request.getJobId(), request.getVideoId(), request.getLanguage(), content);
        return response;
    }
}
