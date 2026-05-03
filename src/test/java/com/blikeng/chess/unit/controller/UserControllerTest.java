package com.blikeng.chess.unit.controller;

import com.blikeng.chess.config.SecurityConfig;
import com.blikeng.chess.controller.UserController;
import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.exception.errorTypes.UserNotFoundException;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@WithMockUser
class UserControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;

    @Test
    void shouldGetUser() throws Exception {
        when(userService.getUser("someUser"))
                .thenReturn(new ProfileDTO("someUser", "bio text", null, 850));

        mockMvc.perform(get("/api/user/someUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("someUser"))
                .andExpect(jsonPath("$.elo").value(850));
    }

    @Test
    void shouldFailToGetNonExistentUser() throws Exception {
        when(userService.getUser("nobody")).thenThrow(new UserNotFoundException());

        mockMvc.perform(get("/api/user/nobody"))
                .andExpect(status().isNotFound());
    }
}
