package com.ergoproxy.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ErgoProxyApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ErgoProxyApplication.class);
        application.setHeadless(false);
        application.run(args);
    }
}
