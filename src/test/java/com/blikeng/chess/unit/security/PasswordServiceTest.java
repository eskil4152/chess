package com.blikeng.chess.unit.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.blikeng.chess.security.PasswordService;

class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setup() {
        passwordService = new PasswordService(new BCryptPasswordEncoder());
    }

    @Test
    void hashShouldNotBeEmptyAndBeCorrectFormat() {
        String hash = passwordService.hashPassword("secret");
        assertThat(hash).startsWith("$2a$");
    }

    @Test
    void correctPasswordHashShouldReturnTrue() {
        String hash = passwordService.hashPassword("secret");
        assertThat(passwordService.checkPassword("secret", hash)).isTrue();
    }

    @Test
    void wrongPasswordHashShouldReturnFalse() {
        String hash = passwordService.hashPassword("secret");
        assertThat(passwordService.checkPassword("wrong", hash)).isFalse();
    }

    @Test
    void shouldNotReuseHash() {
        String hash = passwordService.hashPassword("secret");
        String hash2 = passwordService.hashPassword("secret");

        assertThat(hash).isNotEqualTo(hash2);
    }
}
