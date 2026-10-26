package com.ergoproxy.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AnimeApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AnimeApplication.class);
        application.setHeadless(false);
        application.run(args);
    }
}
