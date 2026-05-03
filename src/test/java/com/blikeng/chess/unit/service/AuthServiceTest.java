package com.blikeng.chess.unit.service;

import com.blikeng.chess.dto.AuthDTO;
import com.blikeng.chess.dto.LoginDTO;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.errorTypes.*;
import com.blikeng.chess.repository.AuthRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.PasswordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.blikeng.chess.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthRepository authRepository;
    @Mock JwtService jwtService;
    @Mock PasswordService passwordService;
    @InjectMocks AuthService authService;

    private UserEntity user;

    @BeforeEach
    void setup() {
        user = new UserEntity("testuser", "hashed");
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    // --- Login ---
    @Test
    void loginShouldReturnTokenOnSuccess() {
        when(authRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(user));
        when(passwordService.checkPassword("pass", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt");

        String token = authService.login(new LoginDTO("testuser", "pass"));
        assertThat(token).isEqualTo("jwt");
    }

    @Test
    void loginShouldThrowWhenUserNotFound() {
        when(authRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginDTO("unknown", "pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginShouldThrowOnWrongPassword() {
        when(authRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(user));
        when(passwordService.checkPassword("wrong", "hashed")).thenReturn(false);
        assertThatThrownBy(() -> authService.login(new LoginDTO("testuser", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }


    // --- Register ---
    @Test
    void registerShouldReturnTokenOnSuccess() {
        when(authRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(passwordService.hashPassword("password1")).thenReturn("hashed");
        when(authRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt");

        String token = authService.register(new LoginDTO("newuser", "password1"));
        assertThat(token).isEqualTo("jwt");
    }

    @Test
    void registerShouldThrowWhenUsernameTooShort() {
        assertThatThrownBy(() -> authService.register(new LoginDTO("ab", "password1")))
                .isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void registerShouldThrowWhenUsernameTooLong() {
        String longName = "a".repeat(33);
        assertThatThrownBy(() -> authService.register(new LoginDTO(longName, "password1")))
                .isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void registerShouldThrowWhenPasswordTooShort() {
        assertThatThrownBy(() -> authService.register(new LoginDTO("validname", "short")))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void registerShouldThrowWhenPasswordTooLong() {
        String longPass = "a".repeat(129);
        assertThatThrownBy(() -> authService.register(new LoginDTO("validname", longPass)))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void registerShouldAcceptMinimumLengthUsername() {
        when(authRepository.existsByUsernameIgnoreCase("abc")).thenReturn(false);
        when(passwordService.hashPassword("password1")).thenReturn("hashed");
        when(authRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt");
        assertThat(authService.register(new LoginDTO("abc", "password1"))).isEqualTo("jwt");
    }

    @Test
    void registerShouldAcceptMaximumLengthUsername() {
        String maxName = "a".repeat(32);
        when(authRepository.existsByUsernameIgnoreCase(maxName)).thenReturn(false);
        when(passwordService.hashPassword("password1")).thenReturn("hashed");
        when(authRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt");
        assertThat(authService.register(new LoginDTO(maxName, "password1"))).isEqualTo("jwt");
    }

    @Test
    void registerShouldAcceptMinimumLengthPassword() {
        when(authRepository.existsByUsernameIgnoreCase("validname")).thenReturn(false);
        when(passwordService.hashPassword("exactly8")).thenReturn("hashed");
        when(authRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt");
        assertThat(authService.register(new LoginDTO("validname", "exactly8"))).isEqualTo("jwt");
    }

    @Test
    void registerShouldAcceptMaximumLengthPassword() {
        String maxPass = "a".repeat(128);
        when(authRepository.existsByUsernameIgnoreCase("validname")).thenReturn(false);
        when(passwordService.hashPassword(maxPass)).thenReturn("hashed");
        when(authRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt");
        assertThat(authService.register(new LoginDTO("validname", maxPass))).isEqualTo("jwt");
    }

    @Test
    void registerShouldThrowWhenUsernameTaken() {
        when(authRepository.existsByUsernameIgnoreCase("taken")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(new LoginDTO("taken", "password1")))
                .isInstanceOf(UsernameTakenException.class);
    }


    // --- Auth ---
    @Test
    void authenticateShouldReturnAuthDTOForValidPrincipal() {
        UUID id = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(id, "user");
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try (MockedStatic<JwtService> ms = mockStatic(JwtService.class)) {
            ms.when(JwtService::getCurrentUser).thenReturn(principal);
            AuthDTO result = authService.authenticate();
            assertThat(result.userId()).isEqualTo(id);
            assertThat(result.username()).isEqualTo("user");
        }
    }

    @Test
    void authenticateShouldThrowWhenPrincipalIsNull() {
        try (MockedStatic<JwtService> ms = mockStatic(JwtService.class)) {
            ms.when(JwtService::getCurrentUser).thenReturn(null);
            assertThatThrownBy(() -> authService.authenticate())
                    .isInstanceOf(InvalidUserException.class);
        }
    }


    // --- Find by ID ---
    @Test
    void findUserByIdShouldDelegateToRepository() {
        UUID id = UUID.randomUUID();
        when(authRepository.findById(id)).thenReturn(Optional.of(user));
        assertThat(authService.findUserById(id)).contains(user);
    }
}