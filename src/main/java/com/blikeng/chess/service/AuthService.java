package com.blikeng.chess.service;

import com.blikeng.chess.dto.LoginDTO;
import com.blikeng.chess.repository.AuthRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private AuthRepository authRepository;

    public String login(LoginDTO loginDTO) {
        return "";
    }

    public String register(LoginDTO loginDTO) {
        return "";
    }
}
