package com.datafit.migrater.security;

import com.datafit.migrater.repo.UserAccessRepository;
import com.datafit.migrater.repo.ProjectRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.*;

/**
 * Exposes /api/me for frontend SSO usage and returns admin flag based on user_access table.
 */
@RestController
@RequestMapping("/api")
public class UserInfoController {

    private final UserAccessRepository accessRepo;
    private final ProjectRepository projectRepo;

    public UserInfoController(UserAccessRepository accessRepo, ProjectRepository projectRepo){
        this.accessRepo = accessRepo; this.projectRepo = projectRepo;
    }

    @GetMapping("/me")
    public Map<String,Object> me(@AuthenticationPrincipal OAuth2User user){
        if(user==null) throw new RuntimeException("Not signed in");
        String email = user.getAttribute("email");
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("email", email);
        out.put("name", user.getAttribute("name"));
        boolean isAdmin = accessRepo.findByEmail(email).stream().anyMatch(ua->"ADMIN".equalsIgnoreCase(ua.getRoleName()));
        out.put("admin", isAdmin);
        // Also include list of project ids user has access to (empty = all? here we return specific)
        List<java.util.UUID> projects = accessRepo.findByEmail(email).stream().filter(ua->ua.getProject()!=null).map(ua->ua.getProject().getId()).toList();
        out.put("projects", projects);
        return out;
    }

    @GetMapping("/public/health")
    public Map<String,String> health(){ return Map.of("status","ok"); }
}
