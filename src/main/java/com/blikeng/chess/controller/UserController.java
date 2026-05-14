package com.blikeng.chess.controller;

import com.blikeng.chess.dto.PasswordDTO;
import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.dto.ProfileEditDTO;
import com.blikeng.chess.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/edit")
    public ResponseEntity<Void> editUser(
        @RequestBody ProfileEditDTO profileEditDTO
        ){
        userService.updateUser(profileEditDTO);
        return ResponseEntity.ok().build();
    }

    @PutMapping("edit-password")
    public ResponseEntity<Void> editPassword(
        @RequestBody PasswordDTO passwordDTO
    ){
        userService.updatePassword(passwordDTO);
        return ResponseEntity.ok().build();
    }
}
