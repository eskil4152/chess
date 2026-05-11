package com.blikeng.chess.unit.controller;

import com.blikeng.chess.config.SecurityConfig;
import com.blikeng.chess.controller.QueueController;
import com.blikeng.chess.exception.types.ExistingGameException;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.service.MatchmakingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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

    @Test
    void shouldJoinQueue() throws Exception {
        mockMvc.perform(post("/api/queue").with(csrf()))
                .andExpect(status().isOk());

        verify(matchmakingService).queuePlayer();
    }

    @Test
    void shouldReturn409WhenAlreadyInGame() throws Exception {
        doThrow(new ExistingGameException()).when(matchmakingService).queuePlayer();

        mockMvc.perform(post("/api/queue").with(csrf()))
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
