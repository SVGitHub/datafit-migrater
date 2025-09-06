package com.datafit.migrater.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class DebugController {

    @GetMapping("/api/debug/roles")
    public Map<String, Object> roles(Authentication auth) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (auth == null) {
            result.put("authenticated", false);
            return result;
        }

        result.put("authenticated", true);
        result.put("principal", auth.getPrincipal().getClass().getSimpleName());
        result.put("authorities", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());

        if (auth.getPrincipal() instanceof OAuth2User oAuth2User) {
            result.put("email", oAuth2User.getAttribute("email"));
            result.put("name", oAuth2User.getAttribute("name"));
        }

        return result;
    }
}