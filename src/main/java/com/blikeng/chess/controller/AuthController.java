package com.blikeng.chess.controller;

import com.blikeng.chess.dto.AuthDTO;
import com.blikeng.chess.dto.AuthResult;
import com.blikeng.chess.dto.LoginDTO;
import com.blikeng.chess.security.Blacklist;
import com.blikeng.chess.service.AuthService;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final Environment environment;
    private final Blacklist blacklist;

    public AuthController(
            AuthService authService,
            Environment environment,
            Blacklist blacklist
    ) {
        this.authService = authService;
        this.environment = environment;
        this.blacklist = blacklist;
    }

    private static final Long MAX_AGE = 30 * 24 * 60 * 60L;

    @PostMapping("/login")
    public ResponseEntity<AuthDTO> login(@RequestBody LoginDTO loginDTO) {
        AuthResult authResult = authService.login(loginDTO);

        ResponseCookie cookie = makeCookie(authResult.token(), MAX_AGE);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(authResult.user());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDTO> register(@RequestBody LoginDTO loginDTO) {
        AuthResult authResult = authService.register(loginDTO);

        ResponseCookie cookie = makeCookie(authResult.token(), MAX_AGE);

        return ResponseEntity
                .status(201)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(authResult.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
        @CookieValue(value = "AUTH", required = false) String token
    ) {
       if (token == null) return ResponseEntity.ok().body("User already logged out");

       blacklist.add(token);

        ResponseCookie cookie = makeCookie("", 0L);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("User logged out successfully");
    }

    @GetMapping
    public ResponseEntity<AuthDTO> auth(){
        return ResponseEntity.ok(authService.authenticate());
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
