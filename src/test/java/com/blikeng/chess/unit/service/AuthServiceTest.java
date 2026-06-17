package com.blikeng.chess.unit.service;

import com.blikeng.chess.dto.AuthDTO;
import com.blikeng.chess.dto.AuthResult;
import com.blikeng.chess.dto.LoginDTO;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.*;
import com.blikeng.chess.repository.AuthRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.PasswordService;
import com.blikeng.chess.security.UserRole;
import com.blikeng.chess.service.AuthService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
        when(jwtService.generateToken(eq(user), anyBoolean())).thenReturn("jwt");

        AuthResult result = authService.login(new LoginDTO("testuser", "pass", false));
        assertThat(result.token()).isEqualTo("jwt");
        assertThat(result.user().username()).isEqualTo("testuser");
    }

    @Test
    void loginShouldThrowWhenUserNotFound() {
        when(authRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());
        LoginDTO dto = new LoginDTO("unknown", "pass", false);
        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginShouldThrowOnWrongPassword() {
        when(authRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(user));
        when(passwordService.checkPassword("wrong", "hashed")).thenReturn(false);
        LoginDTO dto = new LoginDTO("testuser", "wrong", false);
        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(InvalidCredentialsException.class);
    }


    // --- Register ---
    @Test
    void registerShouldReturnTokenOnSuccess() {
        when(authRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(passwordService.hashPassword("password1")).thenReturn("hashed");
        when(authRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(eq(user), anyBoolean())).thenReturn("jwt");

        AuthResult result = authService.register(new LoginDTO("newuser", "password1", false));
        assertThat(result.token()).isEqualTo("jwt");
        assertThat(result.user().username()).isEqualTo("testuser");
    }

    @Test
    void registerShouldThrowWhenUsernameTooShort() {
        LoginDTO dto = new LoginDTO("ab", "password1", false);
        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void registerShouldThrowWhenUsernameTooLong() {
        String longName = "a".repeat(33);
        LoginDTO dto = new LoginDTO(longName, "password1", false);
        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void registerShouldThrowWhenPasswordTooShort() {
        LoginDTO dto = new LoginDTO("validname", "short", false);
        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void registerShouldThrowWhenPasswordTooLong() {
        String longPass = "a".repeat(129);
        LoginDTO dto = new LoginDTO("validname", longPass, false);
        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void registerShouldAcceptMinimumLengthUsername() {
        when(authRepository.existsByUsernameIgnoreCase("abc")).thenReturn(false);
        when(passwordService.hashPassword("password1")).thenReturn("hashed");
        when(authRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(eq(user), anyBoolean())).thenReturn("jwt");
        assertThat(authService.register(new LoginDTO("abc", "password1", false)).token()).isEqualTo("jwt");
    }

    @Test
    void registerShouldAcceptMaximumLengthUsername() {
        String maxName = "a".repeat(32);
        when(authRepository.existsByUsernameIgnoreCase(maxName)).thenReturn(false);
        when(passwordService.hashPassword("password1")).thenReturn("hashed");
        when(authRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(eq(user), anyBoolean())).thenReturn("jwt");
        assertThat(authService.register(new LoginDTO(maxName, "password1", false)).token()).isEqualTo("jwt");
    }

    @Test
    void registerShouldAcceptMinimumLengthPassword() {
        when(authRepository.existsByUsernameIgnoreCase("validname")).thenReturn(false);
        when(passwordService.hashPassword("exactly8")).thenReturn("hashed");
        when(authRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(eq(user), anyBoolean())).thenReturn("jwt");
        assertThat(authService.register(new LoginDTO("validname", "exactly8", false)).token()).isEqualTo("jwt");
    }

    @Test
    void registerShouldAcceptMaximumLengthPassword() {
        String maxPass = "a".repeat(128);
        when(authRepository.existsByUsernameIgnoreCase("validname")).thenReturn(false);
        when(passwordService.hashPassword(maxPass)).thenReturn("hashed");
        when(authRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(eq(user), anyBoolean())).thenReturn("jwt");
        assertThat(authService.register(new LoginDTO("validname", maxPass, false)).token()).isEqualTo("jwt");
    }

    @Test
    void registerShouldThrowWhenUsernameTaken() {
        when(authRepository.existsByUsernameIgnoreCase("taken")).thenReturn(true);
        LoginDTO dto = new LoginDTO("taken", "password1", false);
        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(UsernameTakenException.class);
    }


    // --- Auth ---
    @Test
    void authenticateShouldReturnAuthDTOForValidPrincipal() {
        UUID id = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(id, "user", UserRole.USER);
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