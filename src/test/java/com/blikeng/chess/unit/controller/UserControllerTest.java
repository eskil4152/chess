package com.blikeng.chess.unit.controller;

import com.blikeng.chess.config.SecurityConfig;
import com.blikeng.chess.controller.UserController;
import com.blikeng.chess.dto.PasswordDTO;
import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.dto.ProfileEditDTO;
import com.blikeng.chess.exception.types.BadEditException;
import com.blikeng.chess.exception.types.InvalidPasswordException;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.exception.types.UserNotFoundException;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.ratelimit.RateLimitingService;
import com.blikeng.chess.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@WithMockUser
class UserControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;
    @MockitoBean RateLimitingService rateLimitingService;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);
    }

    @Test
    void shouldGetUser() throws Exception {
        when(userService.getUser("someUser"))
                .thenReturn(new ProfileDTO("someUser", "bio text", null, 850, 0, 0, 0, 0, 0, 0, 0, false, null));

        mockMvc.perform(get("/api/user/someUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("someUser"))
                .andExpect(jsonPath("$.bulletElo").value(850));
    }

    @Test
    void shouldFailToGetNonExistentUser() throws Exception {
        when(userService.getUser("nobody")).thenThrow(new UserNotFoundException());

        mockMvc.perform(get("/api/user/nobody"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void unauthenticatedRequestShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/user/someUser"))
                .andExpect(status().isUnauthorized());
    }

    // --- Edit Profile ---

    @Test
    void shouldEditProfile() throws Exception {
        mockMvc.perform(patch("/api/user/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProfileEditDTO("bio", "new bio")))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void editProfileShouldReturn401OnNullUser() throws Exception {
        doThrow(new InvalidUserException()).when(userService).updateUser(any());

        mockMvc.perform(patch("/api/user/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProfileEditDTO("bio", "x")))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void editProfileShouldReturn400OnBlankField() throws Exception {
        doThrow(new BadEditException()).when(userService).updateUser(any());

        mockMvc.perform(patch("/api/user/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProfileEditDTO("", "x")))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editProfileShouldReturn400OnBlankValue() throws Exception {
        doThrow(new BadEditException()).when(userService).updateUser(any());

        mockMvc.perform(patch("/api/user/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProfileEditDTO("bio", "")))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editProfileShouldReturn400OnBadField() throws Exception {
        doThrow(new BadEditException()).when(userService).updateUser(any());

        mockMvc.perform(patch("/api/user/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProfileEditDTO("username", "hacker")))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void editProfileShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/user/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProfileEditDTO("bio", "x")))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // --- Edit Password ---

    @Test
    void shouldEditPassword() throws Exception {
        mockMvc.perform(patch("/api/user/edit-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordDTO("oldPass", "newPass123")))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void editPasswordShouldReturn401OnNullUser() throws Exception {
        doThrow(new InvalidUserException()).when(userService).updatePassword(any());

        mockMvc.perform(patch("/api/user/edit-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordDTO("old", "newPass123")))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void editPasswordShouldReturn400OnWrongOldPassword() throws Exception {
        doThrow(new InvalidPasswordException()).when(userService).updatePassword(any());

        mockMvc.perform(patch("/api/user/edit-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordDTO("wrong", "newPass123")))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editPasswordShouldReturn400OnBadNewPassword() throws Exception {
        doThrow(new BadEditException()).when(userService).updatePassword(any());

        mockMvc.perform(patch("/api/user/edit-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordDTO("old", "short")))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void editPasswordShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/user/edit-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordDTO("old", "newPass123")))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
