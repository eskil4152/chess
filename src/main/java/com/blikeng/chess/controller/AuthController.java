package com.blikeng.chess.controller;

import com.blikeng.chess.dto.LoginDTO;
import com.blikeng.chess.service.AuthService;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private AuthService authService;
    private Environment environment;

    private final Long maxAge = (long) 24 * 60 * 60;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDTO loginDTO) {
        String token = authService.login(loginDTO);

        ResponseCookie cookie = makeCookie(token, maxAge);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("User logged in successfully");
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody LoginDTO loginDTO) {
        String token = authService.register(loginDTO);

        ResponseCookie cookie = makeCookie(token, maxAge);

        return ResponseEntity
                .status(201)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("User registered successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        ResponseCookie cookie = makeCookie("", 0L);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("User logged out successfully");
    }

    @PostMapping
    public String auth(){
        // TODO
        return "Auth successful";
    }

    private ResponseCookie makeCookie(String token, Long maxAge) {
        boolean isProd = environment.matchesProfiles("prod");

        return ResponseCookie.from("AUTH", token)
                .httpOnly(true)
                .secure(isProd)
                .path("/")
                .sameSite("Strict")
                .maxAge(maxAge)
                .build();
    }
}
