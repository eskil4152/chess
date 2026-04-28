package com.blikeng.chess.service;

import com.blikeng.chess.dto.LoginDTO;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.ErrorTypes.InvalidCredentialsException;
import com.blikeng.chess.exception.ErrorTypes.InvalidPasswordException;
import com.blikeng.chess.exception.ErrorTypes.InvalidUsernameException;
import com.blikeng.chess.exception.ErrorTypes.UsernameTakenException;
import com.blikeng.chess.repository.AuthRepository;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.PasswordService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final AuthRepository authRepository;
    private final JwtService jwtService;
    private final PasswordService passwordService;

    public AuthService(
            AuthRepository authRepository,
            JwtService jwtService,
            PasswordService passwordService
    ) {
        this.authRepository = authRepository;
        this.jwtService = jwtService;
        this.passwordService = passwordService;
    }

    public String login(LoginDTO loginDTO) {
        Optional<UserEntity> user = authRepository.findByUsernameIgnoreCase(loginDTO.username());

        if (user.isEmpty()) throw new InvalidCredentialsException();

        if (!passwordService.checkPassword(loginDTO.password(), user.get().getPassword())) throw new InvalidCredentialsException();

        return jwtService.generateToken(user.get());
    }

    public String register(LoginDTO loginDTO) {
        String trimmedUsername = loginDTO.username().trim();
        String trimmedPassword = loginDTO.password().trim();

        if (trimmedUsername.length() > 32 || trimmedUsername.length() < 3) throw new InvalidUsernameException();
        if (trimmedPassword.length() > 128 || trimmedPassword.length() < 8) throw new InvalidPasswordException();

        if (authRepository.existsByUsernameIgnoreCase(trimmedUsername)) throw new UsernameTakenException();

        UserEntity user = authRepository.save(new UserEntity(trimmedUsername, passwordService.hashPassword(trimmedPassword)));

        return jwtService.generateToken(user);
    }
}
