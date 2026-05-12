package com.blikeng.chess.unit.controller;

import com.blikeng.chess.bot.BotDefinition;
import com.blikeng.chess.bot.BotDifficulty;
import com.blikeng.chess.bot.BotService;
import com.blikeng.chess.config.SecurityConfig;
import com.blikeng.chess.controller.BotController;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.ExistingGameException;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.UserRole;
import com.blikeng.chess.security.ratelimit.RateLimitingService;
import com.blikeng.chess.service.AuthService;
import com.blikeng.chess.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BotController.class)
@Import(SecurityConfig.class)
class BotControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean BotService botService;
    @MockitoBean GameService gameService;
    @MockitoBean AuthService authService;
    @MockitoBean JwtService jwtService;
    @MockitoBean RateLimitingService rateLimitingService;

    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        UserEntity user = new UserEntity("testUser", "hash");
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);
        when(authService.findUserById(any())).thenReturn(Optional.of(user));
        when(botService.getBot(any())).thenReturn(new BotDefinition(UUID.randomUUID(), "Bot-Easy", BotDifficulty.EASY));
    }

    private RequestPostProcessor jwtAuth() {
        JwtPrincipal principal = new JwtPrincipal(userId, "testUser", UserRole.USER);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    void shouldStartBotGame() throws Exception {
        mockMvc.perform(post("/api/bot/easy").with(jwtAuth()).with(csrf()))
                .andExpect(status().isOk());

        verify(gameService).beginBotGame(any(), any());
    }

    @Test
    void shouldReturn409WhenAlreadyInGame() throws Exception {
        when(gameService.isInGame(any())).thenReturn(true);

        mockMvc.perform(post("/api/bot/easy").with(jwtAuth()).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn500WhenInvalidDifficulty() throws Exception {
        mockMvc.perform(post("/api/bot/invalid").with(jwtAuth()).with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldReturn409WhenUserThrowsExistingGame() throws Exception {
        when(gameService.isInGame(any())).thenThrow(new ExistingGameException());

        mockMvc.perform(post("/api/bot/easy").with(jwtAuth()).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void shouldReturn401WhenAuthenticatedWithoutJwtPrincipal() throws Exception {
        mockMvc.perform(post("/api/bot/easy").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenPrincipalHasNullUserId() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(null, "testUser", UserRole.USER);
        RequestPostProcessor nullUserAuth = authentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        mockMvc.perform(post("/api/bot/easy").with(nullUserAuth).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/bot/easy").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
