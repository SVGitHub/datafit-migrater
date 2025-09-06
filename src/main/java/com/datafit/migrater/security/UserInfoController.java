package com.datafit.migrater.security;

import com.datafit.migrater.repo.UserAccessRepository;
import com.datafit.migrater.repo.ProjectRepository;
import com.datafit.migrater.config.AdminSecurityProperties;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

/**
 * Exposes /api/me for frontend SSO usage and returns admin flag
 * based on user_access table + configured admin-emails.
 */
@RestController
@RequestMapping("/api")
public class UserInfoController {

    private final UserAccessRepository accessRepo;
    private final ProjectRepository projectRepo;
    private final AdminSecurityProperties adminProps;

    public UserInfoController(UserAccessRepository accessRepo,
                              ProjectRepository projectRepo,
                              AdminSecurityProperties adminProps) {
        this.accessRepo = accessRepo;
        this.projectRepo = projectRepo;
        this.adminProps = adminProps;
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal OAuth2User user) {
        if (user == null) throw new RuntimeException("Not signed in");
        String email = user.getAttribute("email");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("email", email);
        out.put("name", user.getAttribute("name"));

        // Admin flag comes from either DB role OR YAML configured admin-emails
        boolean isAdmin =
                accessRepo.findByEmail(email).stream()
                        .anyMatch(ua -> "ADMIN".equalsIgnoreCase(ua.getRoleName()))
                        ||
                        adminProps.getAdminEmails().stream()
                                .map(String::toLowerCase)
                                .anyMatch(adminEmail -> adminEmail.equalsIgnoreCase(email));

        out.put("admin", isAdmin);

        // Also include list of project ids user has access to
        List<UUID> projects = accessRepo.findByEmail(email).stream()
                .filter(ua -> ua.getProject() != null)
                .map(ua -> ua.getProject().getId())
                .toList();

        out.put("projects", projects);

        return out;
    }

    @GetMapping("/public/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}