package com.blikeng.chess.unit.controller;

import com.blikeng.chess.config.SecurityConfig;
import com.blikeng.chess.controller.QueueController;
import com.blikeng.chess.dto.TimeControlDTO;
import com.blikeng.chess.exception.types.ExistingGameException;
import com.blikeng.chess.security.Blacklist;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.ratelimit.RateLimitingService;
import com.blikeng.chess.service.MatchmakingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueueController.class)
@Import(SecurityConfig.class)
@WithMockUser
class QueueControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MatchmakingService matchmakingService;
    @MockitoBean JwtService jwtService;
    @MockitoBean RateLimitingService rateLimitingService;
    @MockitoBean Blacklist blacklist;

    @BeforeEach
    void setup() {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);
    }

    @Test
    void shouldJoinQueue() throws Exception {
        TimeControlDTO timeControlDTO = new TimeControlDTO("BLITZ_5_0");
        mockMvc.perform(post("/api/queue").with(csrf()).content("{\"timeControl\":\"BLITZ_5_0\"}").header("content-type", "application/json"))
                .andExpect(status().isOk());

        verify(matchmakingService).queuePlayer(timeControlDTO);
    }

    @Test
    void shouldReturn409WhenAlreadyInGame() throws Exception {
        TimeControlDTO timeControlDTO = new TimeControlDTO("BLITZ_5_0");

        doThrow(new ExistingGameException()).when(matchmakingService).queuePlayer(timeControlDTO);

        mockMvc.perform(post("/api/queue")
                .content("{\"timeControl\":\"BLITZ_5_0\"}")
                .header("content-type", "application/json")
                .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldLeaveQueue() throws Exception {
        mockMvc.perform(delete("/api/queue").with(csrf()))
                .andExpect(status().isOk());

        verify(matchmakingService).dequeuePlayer();
    }

    @Test
    @WithAnonymousUser
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/queue").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
