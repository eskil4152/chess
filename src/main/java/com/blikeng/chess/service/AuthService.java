package com.blikeng.chess.service;

import com.blikeng.chess.dto.AuthDTO;
import com.blikeng.chess.dto.AuthResult;
import com.blikeng.chess.dto.LoginDTO;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.errorTypes.*;
import com.blikeng.chess.repository.AuthRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.PasswordService;
import com.blikeng.chess.security.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthRepository authRepository;
    private final JwtService jwtService;
    private final PasswordService passwordService;

    Logger logger = LoggerFactory.getLogger(AuthService.class);

    public AuthService(
            AuthRepository authRepository,
            JwtService jwtService,
            PasswordService passwordService
    ) {
        this.authRepository = authRepository;
        this.jwtService = jwtService;
        this.passwordService = passwordService;
    }

    public AuthResult login(LoginDTO loginDTO) {
        Optional<UserEntity> user = authRepository.findByUsernameIgnoreCase(loginDTO.username());

        if (user.isEmpty()) throw new InvalidCredentialsException();

        if (!passwordService.checkPassword(loginDTO.password(), user.get().getPassword())) {
            logger.warn("Failed login attempt for username: {}", loginDTO.username());
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.get());
        AuthDTO authDTO = new AuthDTO(
                user.get().getId(), user.get().getUsername(), user.get().getRole()
        );

        return new AuthResult(token, authDTO);
    }

    public AuthResult register(LoginDTO loginDTO) {
        String trimmedUsername = loginDTO.username().trim();
        String trimmedPassword = loginDTO.password().trim();

        if (trimmedUsername.length() > 32 || trimmedUsername.length() < 3) throw new InvalidUsernameException();
        if (trimmedPassword.length() > 128 || trimmedPassword.length() < 8) throw new InvalidPasswordException();

        if (authRepository.existsByUsernameIgnoreCase(trimmedUsername)) throw new UsernameTakenException();

        UserEntity user = authRepository.save(new UserEntity(trimmedUsername, passwordService.hashPassword(trimmedPassword)));

        String token = jwtService.generateToken(user);
        AuthDTO authDTO = new AuthDTO(
                user.getId(), user.getUsername(), user.getRole()
        );

        return new AuthResult(token, authDTO);
    }

    public AuthDTO authenticate(){
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null) throw new InvalidUserException();

        return new AuthDTO(
                principal.userId(),
                principal.username(),
                principal.role()
        );
    }

    public Optional<UserEntity> findUserById(UUID userId){
        return authRepository.findById(userId);
    }
}
