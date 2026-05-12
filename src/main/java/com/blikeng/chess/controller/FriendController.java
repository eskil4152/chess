package com.blikeng.chess.controller;

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
import java.util.UUID;

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
        friendService.addFriend(usernameDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeFriend(
        @RequestBody UsernameDTO usernameDTO
    ){
        friendService.removeFriend(usernameDTO);
        return ResponseEntity.ok().build();
    }
}
