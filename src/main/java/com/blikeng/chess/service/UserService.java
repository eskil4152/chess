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

        switch (status) {
            case WHITE_WIN -> updateElo(whiteId, blackId, true, false);
            case BLACK_WIN -> updateElo(blackId, whiteId, false, false);
            case DRAW -> updateElo(whiteId, blackId, false, true);
        }
    }

    private void updateElo(UUID whiteId, UUID blackId, boolean whiteWin, boolean draw){
        UserEntity white = userRepository.findById(whiteId).orElseThrow();
        UserEntity black = userRepository.findById(blackId).orElseThrow();

        int whiteElo = calculateNewElo(
                white.getElo(),
                black.getElo(),
                whiteWin ? 1.0 : draw ? 0.5 : 0.0,
                getKFactor(white.getGames(), white.isBeen2400())
        );
        int blackElo = calculateNewElo(
                black.getElo(),
                white.getElo(),
                whiteWin ? 0.0 : draw ? 0.5 : 1.0,
                getKFactor(black.getGames(), black.isBeen2400())
        );

        white.setGames(white.getGames() + 1);
        if (whiteElo > 2399) white.setBeen2400(true);
        white.setElo(whiteElo);

        black.setGames(black.getGames() + 1);
        if (blackElo > 2399) black.setBeen2400(true);
        black.setElo(blackElo);

        userRepository.save(white);
        userRepository.save(black);
    }

    private int calculateNewElo(int playerElo, int opponentElo, double score, int kFactor) {
        double expected = 1.0 / (1 + Math.pow(10, (opponentElo - playerElo) / 400.0));
        return (int) Math.round(playerElo + kFactor * (score - expected));
    }

    private int getKFactor(int gamesPlayed, boolean been2400) {
        if (been2400) return 10;

        return gamesPlayed >= 30 ? 20 : 40;
    }
}
