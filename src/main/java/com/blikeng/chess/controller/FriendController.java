package com.blikeng.chess.controller;

import com.blikeng.chess.dto.FriendRequestResponseDTO;
import com.blikeng.chess.dto.FriendRequestsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.blikeng.chess.service.FriendService;
import com.blikeng.chess.dto.UsernameDTO;
import com.blikeng.chess.dto.FriendDTO;
import java.util.List;

/**
 * Endpoints for managing friends (base path {@code /api/friends}).
 *
 * <p>{@code GET} lists the user's friends and {@code GET /requests} lists pending
 * requests. {@code POST /add} sends a request, {@code POST /respond} accepts or declines
 * one, and {@code DELETE /remove} removes a friend.
 */
@Controller
@RequestMapping("/api/friends")
public class FriendController {
    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public ResponseEntity<List<FriendDTO>> getFriends() {
        return ResponseEntity.ok(friendService.getFriends());
    }

    @PostMapping("/add")
    public ResponseEntity<Void> addFriend(
        @RequestBody UsernameDTO usernameDTO
    ){
        friendService.sendFriendRequest(usernameDTO);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/respond")
    public ResponseEntity<Void> respondToFriendRequest(
        @RequestBody FriendRequestResponseDTO friendRequestResponseDTO
    ){
        friendService.respondToFriendRequest(friendRequestResponseDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/requests")
    public ResponseEntity<FriendRequestsDTO> getFriendRequests() {
        return ResponseEntity.ok(friendService.getFriendRequests());
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeFriend(
        @RequestBody UsernameDTO usernameDTO
    ){
        friendService.removeFriend(usernameDTO);
        return ResponseEntity.ok().build();
    }
}
