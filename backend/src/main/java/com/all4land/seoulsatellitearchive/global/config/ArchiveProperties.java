package com.all4land.seoulsatellitearchive.global.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.archive")
public record ArchiveProperties(Path root) {}
