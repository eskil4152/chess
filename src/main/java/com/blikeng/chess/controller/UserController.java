package com.blikeng.chess.controller;

import com.blikeng.chess.dto.PasswordDTO;
import com.blikeng.chess.dto.PlayerStatsDTO;
import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.dto.ProfileEditDTO;
import com.blikeng.chess.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User profile endpoints (base path {@code /api/user}).
 *
 * <p>{@code GET /{username}} returns a profile and
 * {@code GET /{username}/stats/{time-control}} returns that user's stats for a time
 * control. {@code PATCH /edit} updates the current user's profile and
 * {@code PATCH /edit-password} changes their password.
 */
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

    @GetMapping("/{username}/stats/{time-control}")
    public ResponseEntity<PlayerStatsDTO> getUserStats(
        @PathVariable String username,
        @PathVariable("time-control") String timeControl
    ) {
        return ResponseEntity.ok(userService.getPlayerStats(username, timeControl));
    }

    @PatchMapping("/edit")
    public ResponseEntity<Void> editUser(
        @RequestBody ProfileEditDTO profileEditDTO
        ){
        userService.updateUser(profileEditDTO);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/edit-password")
    public ResponseEntity<Void> editPassword(
        @RequestBody PasswordDTO passwordDTO
    ){
        userService.updatePassword(passwordDTO);
        return ResponseEntity.ok().build();
    }
}
