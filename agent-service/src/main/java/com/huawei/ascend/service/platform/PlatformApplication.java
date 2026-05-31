package com.huawei.ascend.service.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal bootstrap entry point for the engine-module development sandbox.
 *
 * <p>This is scaffolding: the only purpose is to give the agent-service
 * Spring Boot module a main class so {@code spring-boot-maven-plugin:repackage}
 * succeeds and {@code mvn verify} passes on the otherwise source-free skeleton.
 * When engine-module code migrates back to the mainline, the real
 * {@code PlatformApplication} (with its full configuration) replaces this stub
 * at the same path with no structural conflict.
 */
@SpringBootApplication
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
