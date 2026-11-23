package com.ergoproxy.platform;

import com.ergoproxy.platform.common.config.configmapping.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class ErgoProxyApplication {

    private final ApplicationProperties applicationProperties;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ErgoProxyApplication.class);
        application.run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        Integer port = applicationProperties.getServerPort();
        List<String> networkAddresses = getNetworkAddresses();

        log.info("========================================");
        log.info("ERGO PROXY APPLICATION STARTED");
        log.info("Local          : http://localhost:{}", port);
        log.info("Local Swagger  : http://localhost:{}/swagger-ui/index.html", port);

        if (networkAddresses.isEmpty()) {
            log.info("Network        : No available IPv4 address");
        } else {
            for (String address : networkAddresses) {
                log.info("Network        : http://{}:{}", address, port);
                log.info("Network Swagger: http://{}:{}/swagger-ui/index.html", address, port);
            }
        }

        log.info("========================================");
    }

    private List<String> getNetworkAddresses() {
        List<String> addresses = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    InetAddress address = inetAddresses.nextElement();

                    if (address instanceof Inet4Address
                        && !address.isLoopbackAddress()
                        && !address.isLinkLocalAddress()) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException exception) {
            log.error("Failed to get network addresses", exception);
        }

        return addresses;
    }
}
