package com.blikeng.chess.controller;

import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<ProfileDTO> getUser(
            @PathVariable String username
    ) {
        return ResponseEntity.ok(userService.getUser(username));
    }
}
