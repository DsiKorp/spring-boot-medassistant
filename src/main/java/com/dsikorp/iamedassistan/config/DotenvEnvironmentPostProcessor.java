package com.dsikorp.iamedassistan.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotenv = Path.of(".env");
        if (!Files.exists(dotenv)) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        try (var lines = Files.lines(dotenv)) {
            lines.filter(line -> !line.isBlank() && !line.trim().startsWith("#"))
                 .forEach(line -> {
                     int idx = line.indexOf('=');
                     if (idx <= 0) {
                         return;
                     }
                     String key = line.substring(0, idx).trim();
                     String value = unquote(line.substring(idx + 1).trim());
                     properties.put(key, value);
                 });
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load .env file", ex);
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
