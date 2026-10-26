package com.ergoproxy.platform.entity;

import com.ergoproxy.platform.constant.Const;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DownloadJob {

    private final UUID id = UUID.randomUUID();
    private final Map<String, Path> paths = new LinkedHashMap<>();
    private final Map<String, Integer> episodeNumbers = new HashMap<>();
    private final DownloadProgress progress = new DownloadProgress();
    private Path directory;
    private String status = Const.Common.QUEUED;
    private String error = Const.Common.EMPTY;
    private int completed;
    private int total;
    private int current;
    private String slug;

    public static DownloadJob create(Path root, int total, String slug) {
        DownloadJob job = new DownloadJob();
        job.setDirectory(root.resolve(job.getId().toString()));
        job.setTotal(total);
        job.setSlug(slug);
        return job;
    }
}
