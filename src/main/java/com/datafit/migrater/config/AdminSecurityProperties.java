package com.datafit.migrater.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds security-related custom properties from application.yml
 */
@Component
@ConfigurationProperties(prefix = "app.security")
public class AdminSecurityProperties {

    /**
     * Preconfigured list of admin emails.
     */
    private List<String> adminEmails = new ArrayList<>();

    public List<String> getAdminEmails() {
        return adminEmails;
    }

    public void setAdminEmails(List<String> adminEmails) {
        this.adminEmails = adminEmails;
    }
}