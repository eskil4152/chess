package com.blikeng.chess.unit.controller;

import com.blikeng.chess.config.SecurityConfig;
import com.blikeng.chess.controller.AuthController;
import com.blikeng.chess.dto.AuthDTO;
import com.blikeng.chess.dto.AuthResult;
import com.blikeng.chess.dto.LoginDTO;
import com.blikeng.chess.exception.types.InvalidCredentialsException;
import com.blikeng.chess.security.Blacklist;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.UserRole;
import com.blikeng.chess.security.ratelimit.RateLimitingService;
import com.blikeng.chess.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuthService authService;
    @MockitoBean JwtService jwtService;
    @MockitoBean Environment environment;
    @MockitoBean RateLimitingService rateLimitingService;
    @MockitoBean Blacklist blacklist;

    @BeforeEach
    void setup() {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);
    }

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLogIn() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.login(any(LoginDTO.class)))
                .thenReturn(new AuthResult("jwt-token", new AuthDTO(userId, "user", UserRole.USER)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginDTO("user", "pass", false)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    void shouldFailToLogIn() throws Exception {
        when(authService.login(any(LoginDTO.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginDTO("user", "wrong", false)))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRegister() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.register(any(LoginDTO.class)))
                .thenReturn(new AuthResult("jwt-token", new AuthDTO(userId, "newuser", UserRole.USER)));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginDTO("newuser", "password1", false)))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void shouldLogOut() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldSuccessfullyAuthenticate() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.authenticate()).thenReturn(new AuthDTO(userId, "user", UserRole.USER));

        mockMvc.perform(get("/api/auth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.role").value(UserRole.USER.name()));
    }

    @Test
    void cookieShouldBeSecureWhenProdProfile() throws Exception {
        when(authService.login(any(LoginDTO.class)))
                .thenReturn(new AuthResult("jwt-token", new AuthDTO(UUID.randomUUID(), "user", UserRole.USER)));
        when(environment.matchesProfiles("prod")).thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginDTO("user", "pass", false)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Secure")));
    }
}
