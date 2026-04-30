package com.pathboot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds the {@code api.security.*} block from {@code application.yml}.
 * Uses @ConfigurationProperties instead of @Value so YAML lists/maps are bound correctly.
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "api.security")
public class ApiSecurityProperties{
    private boolean enabled = true;
    private List<String> apiKeys = new ArrayList<>();
    /** username → password pairs for the login endpoint. */
    private Map<String, String> users = new HashMap<>();
    /**
     * Optional per-user API key mapping: username → apiKey.
     * If a username is not present here, the first key in {@code apiKeys} is used as fallback.
     */
    private Map<String, String> userKeys = new HashMap<>();

}
