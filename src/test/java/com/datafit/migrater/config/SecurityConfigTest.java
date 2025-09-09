package com.datafit.migrater.config;

import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {SecurityConfig.class, TestOAuth2Config.class})
@AutoConfigureMockMvc
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityConfig securityConfig;

    @MockBean
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService;

    @Test
    void testLocalAdminLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("admin123"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testSsoUserWithAdminEmailGetsRoleAdmin() {
        OAuth2User fakeUser = new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                Map.of("email", "meetsnehadeep@gmail.com"),
                "email"
        );

        Mockito.when(oauth2UserService.loadUser(Mockito.any())).thenReturn(fakeUser);

        OAuth2User mappedUser = oauth2UserService.loadUser(Mockito.mock(OAuth2UserRequest.class));
        assertThat(mappedUser.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN");
    }

    @Test
    void testSsoUserWithNonAdminEmailGetsRoleUser() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("fake-client")
                .clientSecret("fake-secret")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/google")
                .scope("email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("email")
                .clientName("Google")
                .build();

        OAuth2UserRequest userRequest = new OAuth2UserRequest(
                clientRegistration,
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "dummy", null, null)
        );

        OAuth2User user = new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", "someoneelse@example.com"),
                "email"
        );

        OAuth2User mappedUser = securityConfig.oauth2UserService().loadUser(userRequest);

        assertThat(mappedUser.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER");
    }
}