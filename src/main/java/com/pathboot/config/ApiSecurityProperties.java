package com.pathboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds the {@code api.security.*} block from {@code application.yml}.
 * Uses @ConfigurationProperties instead of @Value so YAML lists are bound correctly.
 */
@Component
@ConfigurationProperties(prefix = "api.security")
public class ApiSecurityProperties {

    private boolean enabled = true;
    private List<String> apiKeys = new ArrayList<>();
    /** username → password pairs for the login endpoint. */
    private Map<String, String> users = new HashMap<>();

    public boolean isEnabled()                   { return enabled; }
    public void setEnabled(boolean enabled)      { this.enabled = enabled; }

    public List<String> getApiKeys()             { return apiKeys; }
    public void setApiKeys(List<String> apiKeys) { this.apiKeys = apiKeys; }

    public Map<String, String> getUsers()        { return users; }
    public void setUsers(Map<String, String> users) { this.users = users; }
}
