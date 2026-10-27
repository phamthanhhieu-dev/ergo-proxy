package com.ergoproxy.platform.entity;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.download.DownloadProgressResponseDTO;
import com.ergoproxy.platform.util.Messages;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bounded log history and actual HLS segment progress reported by reanimdl.
 */
public class DownloadProgress {

    private static final Pattern SEGMENTS = Pattern.compile("\\]\\s+([\\d.]+)%\\s+(\\d+)/(\\d+)\\s+(.+?)\\s+ETA\\s+(\\S+)");

    private final Deque<String> logs = new ArrayDeque<>();
    private Double percent;
    private int done, total;
    private String speed = Const.Common.EMPTY,
        eta = Const.Common.EMPTY,
        stage = Messages.get("download.status.waiting");
    private Instant updatedAt = Instant.now();

    public synchronized void start(int episode) {
        percent = null;
        done = 0;
        total = 0;
        speed = Const.Common.EMPTY;
        eta = Const.Common.EMPTY;
        stage = Messages.get("download.source.preparing");
        line(Messages.get("download.episode.started", episode));
    }

    public synchronized void line(String line) {
        updatedAt = Instant.now();
        Matcher match = SEGMENTS.matcher(line);
        if (match.find()) {
            percent = Math.max(0, Math.min(100, Double.parseDouble(match.group(1))));
            done = Integer.parseInt(match.group(2));
            total = Integer.parseInt(match.group(3));
            speed = match.group(4);
            eta = match.group(5);
            stage = Messages.get("download.segments.downloading");
        } else if (line.toLowerCase(Locale.ROOT).contains("mux")) {
            stage = Messages.get("download.media.muxing");
        }
        logs.addLast(updatedAt.toString() + "  " + line.substring(0, Math.min(line.length(), 2000)));
        while (logs.size() > 150) {
            logs.removeFirst();
        }
    }

    public synchronized DownloadProgressResponseDTO view() {
        DownloadProgressResponseDTO response = new DownloadProgressResponseDTO(percent, done, total, speed, eta, stage, updatedAt, List.copyOf(logs));
        return response;
    }
}
