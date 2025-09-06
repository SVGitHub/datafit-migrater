package com.datafit.migrater.config;

import com.datafit.migrater.domain.UserAccess;
import com.datafit.migrater.repo.UserAccessRepository;
import org.springframework.boot.autoconfigure.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserAccessRepository accessRepo;
    private final List<String> adminEmails;

    public SecurityConfig(UserAccessRepository accessRepo, AdminSecurityProperties properties) {
        this.accessRepo = accessRepo;
        // normalize emails to lowercase
        this.adminEmails = properties.getAdminEmails()
                .stream()
                .map(String::toLowerCase)
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/public/**", "/api/public/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(this.oauth2UserService())
                        )
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        return userRequest -> {
            DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
            OAuth2User oAuth2User = delegate.loadUser(userRequest);

            String email = Optional.ofNullable((String) oAuth2User.getAttribute("email"))
                    .map(e -> e.toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new OAuth2AuthenticationException("Email not found"));

            // Lookup existing roles
            var accessList = accessRepo.findByEmail(email);
            List<String> roles = accessList.stream()
                    .map(UserAccess::getRoleName) // e.g. ADMIN, USER
                    .toList();

            if (roles.isEmpty()) {
                // Assign role based on configured admin list
                String role = adminEmails.contains(email) ? "ADMIN" : "USER";

                UserAccess ua = new UserAccess();
                ua.setEmail(email);
                ua.setRoleName(role);
                accessRepo.save(ua);

                roles = List.of(role);
            }

            return new DefaultOAuth2User(
                    roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList(),
                    oAuth2User.getAttributes(),
                    "email"
            );
        };
    }
}