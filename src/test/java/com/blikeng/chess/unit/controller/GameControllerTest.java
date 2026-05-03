package com.blikeng.chess.unit.controller;

import com.blikeng.chess.config.SecurityConfig;
import com.blikeng.chess.controller.GameController;
import com.blikeng.chess.dto.GameDTO;
import com.blikeng.chess.dto.GamePreviewDTO;
import com.blikeng.chess.exception.errorTypes.GameNotFoundException;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.service.GameHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
@Import(SecurityConfig.class)
@WithMockUser
class GameControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean GameHistoryService gameHistoryService;
    @MockitoBean JwtService jwtService;

    @Test
    void shouldGetGameHistory() throws Exception {
        UUID id = UUID.randomUUID();
        when(gameHistoryService.getGameHistory("someUser"))
                .thenReturn(List.of(new GamePreviewDTO(id, "black", "someUser", GameStatus.WHITE_WIN)));

        mockMvc.perform(get("/api/games/user/someUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].whiteUsername").value("someUser"));
    }

    @Test
    void shouldGetGame() throws Exception {
        UUID id = UUID.randomUUID();
        when(gameHistoryService.getGame(id.toString()))
                .thenReturn(new GameDTO(id, "black", "white", GameStatus.DRAW, List.of()));

        mockMvc.perform(get("/api/games/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAW"));
    }

    @Test
    void shouldFailToFindGame() throws Exception {
        String id = UUID.randomUUID().toString();
        when(gameHistoryService.getGame(id)).thenThrow(new GameNotFoundException());

        mockMvc.perform(get("/api/games/{id}", id))
                .andExpect(status().isNotFound());
    }
}
