package com.ergoproxy.platform.config.startup;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.util.Messages;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
@RequiredArgsConstructor
public class ApplicationDisplay {

    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        String context = environment.getProperty("server.servlet.context-path", Const.Common.EMPTY);
        String host;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            host = "127.0.0.1";
        }
        String profiles = String.join(", ", environment.getActiveProfiles());
        if (profiles.isBlank()) {
            profiles = Messages.get("application.profile.default");
        }
        LoggerFactory.getLogger(ApplicationDisplay.class).atInfo()
            .log(Messages.get("application.startup.completed", "http://localhost:" + port + context, "http://" + host + ":" + port + context, profiles));
    }
}
