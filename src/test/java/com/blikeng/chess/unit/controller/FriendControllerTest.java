package com.blikeng.chess.unit.controller;

import com.blikeng.chess.config.SecurityConfig;
import com.blikeng.chess.controller.FriendController;
import com.blikeng.chess.dto.FriendDTO;
import com.blikeng.chess.dto.FriendRequest;
import com.blikeng.chess.dto.FriendRequestResponseDTO;
import com.blikeng.chess.dto.FriendRequestsDTO;
import com.blikeng.chess.dto.UsernameDTO;
import com.blikeng.chess.exception.types.AlreadyFriendsException;
import com.blikeng.chess.exception.types.FriendYourselfException;
import com.blikeng.chess.exception.types.InvalidUUIDException;
import com.blikeng.chess.exception.types.NotFoundException;
import com.blikeng.chess.exception.types.RequestExistsException;
import com.blikeng.chess.security.Blacklist;
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
    @MockitoBean Blacklist blacklist;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);
    }

    @Test
    void shouldGetFriends() throws Exception {
        UUID friendId = UUID.randomUUID();
        when(friendService.getFriends())
                .thenReturn(List.of(new FriendDTO(friendId, "alice", "bio", null, false)));

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
    void shouldSendFriendRequest() throws Exception {
        mockMvc.perform(post("/api/friends/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsernameDTO("alice")))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFailToSendRequestWhenAlreadyFriends() throws Exception {
        doThrow(new AlreadyFriendsException()).when(friendService).sendFriendRequest(any());

        mockMvc.perform(post("/api/friends/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsernameDTO("alice")))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldFailToSendRequestWhenRequestAlreadyExists() throws Exception {
        doThrow(new RequestExistsException()).when(friendService).sendFriendRequest(any());

        mockMvc.perform(post("/api/friends/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsernameDTO("alice")))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldFailToSendRequestToYourself() throws Exception {
        doThrow(new FriendYourselfException()).when(friendService).sendFriendRequest(any());

        mockMvc.perform(post("/api/friends/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsernameDTO("me")))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailToSendRequestToNonExistentUser() throws Exception {
        doThrow(new NotFoundException()).when(friendService).sendFriendRequest(any());

        mockMvc.perform(post("/api/friends/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsernameDTO("nobody")))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetFriendRequests() throws Exception {
        when(friendService.getFriendRequests())
                .thenReturn(new FriendRequestsDTO(List.of(new FriendRequest(UUID.randomUUID(), "bob", null))));

        mockMvc.perform(get("/api/friends/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendRequests[0].username").value("bob"));
    }

    @Test
    void shouldReturnEmptyFriendRequests() throws Exception {
        when(friendService.getFriendRequests()).thenReturn(new FriendRequestsDTO(List.of()));

        mockMvc.perform(get("/api/friends/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendRequests").isEmpty());
    }

    @Test
    void shouldAcceptFriendRequest() throws Exception {
        String requestId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/friends/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FriendRequestResponseDTO(requestId, true)))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeclineFriendRequest() throws Exception {
        String requestId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/friends/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FriendRequestResponseDTO(requestId, false)))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenRespondingToNonExistentRequest() throws Exception {
        doThrow(new NotFoundException()).when(friendService).respondToFriendRequest(any());

        mockMvc.perform(post("/api/friends/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FriendRequestResponseDTO(UUID.randomUUID().toString(), true)))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenRespondingWithInvalidUUID() throws Exception {
        doThrow(new InvalidUUIDException()).when(friendService).respondToFriendRequest(any());

        mockMvc.perform(post("/api/friends/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FriendRequestResponseDTO("not-a-uuid", true)))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
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
