package com.ergoproxy.platform.service;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.playback.*;
import com.ergoproxy.platform.entity.playback.LibraryFile;
import com.ergoproxy.platform.exception.LogicException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@lombok.RequiredArgsConstructor
public class PlaybackLibraryServiceImpl implements PlaybackLibraryService {

    private final NativeFilePickerService nativeFilePickerService;

    @Value("${playback.library-root}")
    private String libraryRoot;

    @Value("${playback.max-files:10000}")
    private int maxFiles;

    private final Map<UUID, LibraryFile> files = new ConcurrentHashMap<>();
    private final Map<Path, UUID> paths = new ConcurrentHashMap<>();
    private final Semaphore picker = new Semaphore(1);

    @Override
    public LibrarySnapshotDTO library() {
        LibrarySnapshotDTO response = scanLibrary(Path.of(libraryRoot));
        return response;
    }

    @Override
    public LibrarySnapshotDTO pick(String type) {
        if (!picker.tryAcquire()) {
            throw new LogicException("PPL_00001", "library.picker.busy");
        }
        try {
            Path selected = nativeFilePickerService.choose(type, Path.of(libraryRoot));
            LibrarySnapshotDTO response;
            if (selected == null) {
                response = new LibrarySnapshotDTO(Const.Common.EMPTY, List.of());
            } else {
                response = scanLibrary(selected);
            }
            return response;
        } finally {
            picker.release();
        }
    }

    private synchronized LibrarySnapshotDTO scanLibrary(Path input) {
        try {
            Path selected = input.toRealPath();
            boolean singleFile = Files.isRegularFile(selected);
            Path base = singleFile ? selected.getParent() : selected;
            List<Path> videos = new ArrayList<>();
            if (singleFile) {
                if (!Const.Playback.VIDEO_EXTENSIONS.contains(extension(selected))) {
                    throw new LogicException("SLPL_00001", "library.video.required");
                }
                videos.add(selected);
            } else {
                collect(base, videos, new int[] { 0 }, 0);
            }
            List<PlaybackVideoDTO> entries = new ArrayList<>();
            for (Path video : videos) {
                entries.add(describe(video, base));
            }
            entries.sort(
                Comparator.comparing(PlaybackVideoDTO::series, String.CASE_INSENSITIVE_ORDER).thenComparingInt(this::episodeNumber)
                    .thenComparing(item -> item.video().name())
            );
            LibrarySnapshotDTO response = new LibrarySnapshotDTO(base.getFileName().toString(), entries);
            return response;
        } catch (LogicException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LogicException(exception, "SLPL_00002", "library.scan.failed");
        }
    }

    private void collect(Path directory, List<Path> videos, int[] count, int depth) throws java.io.IOException {
        if (depth > 6) {
            return;
        }
        try (DirectoryStream<Path> children = Files.newDirectoryStream(directory)) {
            for (Path child : children) {
                count[0]++;
                if (count[0] > maxFiles) {
                    throw new LogicException("CPL_00001", "library.scan.limit", maxFiles);
                }
                if (Files.isSymbolicLink(child)) {
                    continue;
                }
                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    collect(child, videos, count, depth + 1);
                } else if (Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS) && Const.Playback.VIDEO_EXTENSIONS.contains(extension(child))) {
                    videos.add(child);
                }
            }
        }
    }

    private PlaybackVideoDTO describe(Path video, Path base) throws java.io.IOException {
        List<PlaybackFileDTO> subtitles = new ArrayList<>();
        List<PlaybackFileDTO> fonts = new ArrayList<>();
        try (DirectoryStream<Path> siblings = Files.newDirectoryStream(video.getParent())) {
            for (Path path : siblings) {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                String type = extension(path);
                if (Const.Playback.SUBTITLE_EXTENSIONS.contains(type)) {
                    subtitles.add(register(path));
                } else if (Const.Playback.FONT_EXTENSIONS.contains(type)) {
                    fonts.add(register(path));
                }
            }
        }
        subtitles.sort(Comparator.comparing(PlaybackFileDTO::name, String.CASE_INSENSITIVE_ORDER));
        String parent = video.getParent().getFileName().toString();
        Matcher episodeMatcher = Pattern.compile(Const.Playback.EPISODE_PATTERN).matcher(parent);
        String episode = video.getFileName().toString();
        String series = base.getFileName().toString();
        if (episodeMatcher.matches()) {
            episode = "EP" + episodeMatcher.group(1);
            series = video.getParent().getParent().getFileName().toString();
        } else if (base.relativize(video).getNameCount() > 1) {
            series = base.relativize(video).getName(0).toString();
        }
        PlaybackVideoDTO response = new PlaybackVideoDTO(register(video), series, episode, subtitles, fonts);
        return response;
    }

    private int episodeNumber(PlaybackVideoDTO item) {
        Matcher matcher = Pattern.compile(Const.Playback.EPISODE_PATTERN).matcher(item.episode());
        int response = Integer.MAX_VALUE;
        if (matcher.matches()) {
            try {
                response = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                /* Tên không phải số tập hợp lệ. */
            }
        }
        return response;
    }

    private PlaybackFileDTO register(Path path) throws java.io.IOException {
        Path actual = path.toRealPath();
        UUID id = paths.get(actual);
        if (id == null) {
            if (files.size() >= maxFiles * 3) {
                throw new LogicException("RPL_00001", "library.registry.limit");
            }
            id = UUID.randomUUID();
            paths.put(actual, id);
        }
        LibraryFile file = new LibraryFile(id, actual, extension(actual), Files.size(actual));
        files.put(id, file);
        PlaybackFileDTO response = new PlaybackFileDTO(id, actual.getFileName().toString(), file.extension(), file.size());
        return response;
    }

    private String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String response = Const.Common.EMPTY;
        if (dot >= 0) {
            response = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        return response;
    }

    @Override
    public LibraryFile find(UUID id) {
        LibraryFile response = files.get(id);
        if (response == null || !Files.isRegularFile(response.path(), LinkOption.NOFOLLOW_LINKS)) {
            throw new LogicException("FPL_00001", "library.file.missing");
        }
        return response;
    }
}
