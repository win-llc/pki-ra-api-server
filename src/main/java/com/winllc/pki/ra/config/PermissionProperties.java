package com.winllc.pki.ra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Configuration
@ConfigurationProperties(prefix = "permissions")
public class PermissionProperties {

    private List<String> available;
    private List<String> protectedEntities;

    public List<String> getAvailable() {
        return available;
    }

    public void setAvailable(List<String> available) {
        this.available = available;
    }

    public List<String> getProtectedEntities() {
        return protectedEntities;
    }

    public void setProtectedEntities(List<String> protectedEntities) {
        this.protectedEntities = protectedEntities;
    }
}
