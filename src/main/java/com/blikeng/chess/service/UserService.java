package com.blikeng.chess.service;

import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.exception.ErrorTypes.UserNotFoundException;
import com.blikeng.chess.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public ProfileDTO getUser(String username) {
        if (username == null || username.trim().isBlank()) throw new UserNotFoundException();
        username = username.trim();

        return userRepository.findByUsernameIgnoreCase(username)
                .map(self -> new ProfileDTO(
                        self.getUsername(), self.getBio(), self.getAvatarUrl(), self.getElo()
                ))
                .orElseThrow(UserNotFoundException::new);
    }
}
