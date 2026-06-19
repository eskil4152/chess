package com.blikeng.chess.unit.security;

import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "test-secret-key-that-is-at-least-64-bytes-long-for-testing-purposes-only-here";

    private JwtService jwtService;
    private UserEntity user;

    @BeforeEach
    void setup() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        jwtService.validateSecret();
        user = new UserEntity("testuser", "hash");
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldThrowIfSecretIsTooShort() {
        JwtService svc = new JwtService();
        ReflectionTestUtils.setField(svc, "secret", "short");
        assertThatThrownBy(svc::validateSecret)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatingGeneratedTokenShouldReturnJwtPrincipal() {
        String token = jwtService.generateToken(user, false);
        assertThat(token).isNotBlank();

        JwtPrincipal principal = jwtService.validateToken(token);
        assertThat(principal).isNotNull();
        assertThat(principal.userId()).isEqualTo(user.getId());
        assertThat(principal.username()).isEqualTo("testuser");
    }

    @Test
    void validatingInvalidTokenShouldReturnNull() {
        JwtPrincipal principal = jwtService.validateToken("not.a.valid.jwt");
        assertThat(principal).isNull();
    }

    @Test
    void validatingTamperedTokenShouldReturnNull() {
        String token = jwtService.generateToken(user, false);
        String tampered = token + "tampered";
        assertThat(jwtService.validateToken(tampered)).isNull();
    }

    @Test
    void getCurrentUserShouldReturnJwtPrincipal() {
        JwtPrincipal principal = new JwtPrincipal(user.getId(), "testuser", UserRole.USER);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            assertThat(JwtService.getCurrentUser()).isEqualTo(principal);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void getCurrentUserWithoutTokenShouldReturnNull() {
        SecurityContextHolder.clearContext();

        assertThat(JwtService.getCurrentUser()).isNull();
    }

    @Test
    void getCurrentUserShouldReturnNullWhenPrincipalIsNotJwtPrincipal() {
        var auth = new UsernamePasswordAuthenticationToken("some-string-principal", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(JwtService.getCurrentUser()).isNull();
    }
}
