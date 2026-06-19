package com.blikeng.chess.unit.controller;

import com.blikeng.chess.config.SecurityConfig;
import com.blikeng.chess.controller.ActiveGameController;
import com.blikeng.chess.dto.GameStateDTO;
import com.blikeng.chess.exception.types.GameNotFoundException;
import com.blikeng.chess.security.Blacklist;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.ratelimit.RateLimitingService;
import com.blikeng.chess.service.game.GameViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActiveGameController.class)
@Import(SecurityConfig.class)
@WithMockUser
class ActiveGameControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean GameViewService gameViewService;
    @MockitoBean JwtService jwtService;
    @MockitoBean RateLimitingService rateLimitingService;
    @MockitoBean Blacklist blacklist;

    @BeforeEach
    void setup() {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);
    }

    @Test
    void shouldGetActiveGameState() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID whiteId = UUID.randomUUID();
        UUID blackId = UUID.randomUUID();
        when(gameViewService.restoreGameState())
                .thenReturn(new GameStateDTO(gameId, whiteId, "white", blackId, "black", List.of(), false, false, 800, 800, 50000, 50000));

        mockMvc.perform(get("/api/games/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whiteUsername").value("white"))
                .andExpect(jsonPath("$.blackUsername").value("black"));
    }

    @Test
    void shouldReturn404WhenNoActiveGame() throws Exception {
        when(gameViewService.restoreGameState()).thenThrow(new GameNotFoundException());

        mockMvc.perform(get("/api/games/active"))
                .andExpect(status().isNotFound());
    }
}