package com.blikeng.chess.service;

import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.errorTypes.UserNotFoundException;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

    public void updateUserElo(UUID whiteId, UUID blackId, GameStatus status){
        if (status == GameStatus.ONGOING) return;

        if (status == GameStatus.BLACK_WIN) {
            updateElo(blackId, true, false);
            updateElo(whiteId, false, false);
        } else if (status == GameStatus.WHITE_WIN) {
            updateElo(blackId, false, false);
            updateElo(whiteId, true, false);
        } else {
            updateElo(blackId, false, true);
            updateElo(whiteId, false, true);
        }
    }

    private void updateElo(UUID userId, boolean won, boolean draw){
        UserEntity user = userRepository.findById(userId).orElseThrow();

        int elo = user.getElo();

        if (won) elo += 1;
        else if (draw) elo += 0;
        else elo -= 1;

        user.setElo(elo);
        userRepository.save(user);
    }
}
