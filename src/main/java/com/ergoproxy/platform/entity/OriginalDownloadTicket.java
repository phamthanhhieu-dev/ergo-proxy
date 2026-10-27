package com.ergoproxy.platform.entity;

import com.ergoproxy.platform.config.client.OriginalHttpClient;
import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.util.Messages;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Future;

@Getter
@Setter
@RequiredArgsConstructor
public class OriginalDownloadTicket {

    private final UUID id = UUID.randomUUID();
    private final String filename;
    private final URI file;
    private final URI progress;
    private final Instant expires = Instant.now().plus(Duration.ofHours(1));
    private volatile String state = Const.Common.WAITING;
    private volatile String message = Messages.get("original.status.waiting");
    private volatile double percent;
    private volatile Future<?> task;
    private volatile OriginalHttpClient.Body active;
}
