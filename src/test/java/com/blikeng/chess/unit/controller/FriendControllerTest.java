package com.blikeng.chess.unit.controller;

import com.blikeng.chess.config.SecurityConfig;
import com.blikeng.chess.controller.FriendController;
import com.blikeng.chess.dto.FriendDTO;
import com.blikeng.chess.dto.UsernameDTO;
import com.blikeng.chess.exception.types.AlreadyFriendsException;
import com.blikeng.chess.exception.types.NotAllowedException;
import com.blikeng.chess.exception.types.NotFoundException;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.ratelimit.RateLimitingService;
import com.blikeng.chess.service.FriendService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendController.class)
@Import(SecurityConfig.class)
@WithMockUser
class FriendControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FriendService friendService;
    @MockitoBean JwtService jwtService;
    @MockitoBean RateLimitingService rateLimitingService;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);
    }

    @Test
    void shouldGetFriends() throws Exception {
        UUID friendId = UUID.randomUUID();
        when(friendService.getFriends())
                .thenReturn(List.of(new FriendDTO(friendId, "alice", "bio", null)));

        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].userId").value(friendId.toString()));
    }

    @Test
    void shouldReturnEmptyListWhenNoFriends() throws Exception {
        when(friendService.getFriends()).thenReturn(List.of());

        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldAddFriend() throws Exception {
        mockMvc.perform(post("/api/friends/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsernameDTO("alice")))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFailToAddAlreadyExistingFriend() throws Exception {
        doThrow(new AlreadyFriendsException()).when(friendService).addFriend(any());

        mockMvc.perform(post("/api/friends/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsernameDTO("alice")))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn403WhenAddingYourself() throws Exception {
        doThrow(new NotAllowedException()).when(friendService).addFriend(any());

        mockMvc.perform(post("/api/friends/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsernameDTO("me")))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldFailToAddNonExistentFriend() throws Exception {
        doThrow(new NotFoundException()).when(friendService).addFriend(any());

        mockMvc.perform(post("/api/friends/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsernameDTO("nobody")))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRemoveFriend() throws Exception {
        mockMvc.perform(delete("/api/friends/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsernameDTO("alice")))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFailToRemoveNonExistentFriend() throws Exception {
        doThrow(new NotFoundException()).when(friendService).removeFriend(any());

        mockMvc.perform(delete("/api/friends/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsernameDTO("nobody")))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void unauthenticatedRequestShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isUnauthorized());
    }
}