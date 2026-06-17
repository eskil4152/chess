package com.blikeng.chess.service;

import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.UserNotFoundException;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Updates both players' record (win/loss/game counts and the "reached 2400" flag) and
 * recomputes their Elo for a finished game, persisting both users.
 *
 * <p>{@link #updateUserStatsAndReturnNewElos} returns the two new ratings
 * ({@code [whiteElo, blackElo]}), or an empty array if the game was neither decisive nor
 * a draw.
 */
@Service
public class StatsService {
    private final UserRepository userRepository;

    public StatsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public int[] updateUserStatsAndReturnNewElos(TimeControl timeControl, UUID whiteId, UUID blackId, GameStatus status){
        return switch (status) {
            case WHITE_WIN -> updateStatsAndReturnNewElos(timeControl, whiteId, blackId, true, false);
            case BLACK_WIN -> updateStatsAndReturnNewElos(timeControl, whiteId, blackId, false, false);
            case DRAW -> updateStatsAndReturnNewElos(timeControl, whiteId, blackId, false, true);
            default -> new int[0];
        };
    }

    private int[] updateStatsAndReturnNewElos(TimeControl timeControl, UUID whiteId, UUID blackId, boolean whiteWin, boolean draw){
        UserEntity white = userRepository.findById(whiteId).orElseThrow(UserNotFoundException::new);
        UserEntity black = userRepository.findById(blackId).orElseThrow(UserNotFoundException::new);

        double drawScore = draw ? 0.5 : 0.0;
        double whiteScore = whiteWin ? 1.0 : drawScore;
        double blackScore = 1.0 - whiteScore;

        int[] newElos = switch (timeControl.type()) {
            case BULLET -> handleBulletStats(white, black, whiteScore, blackScore);
            case BLITZ -> handleBlitzStats(white, black, whiteScore, blackScore);
            case RAPID -> handleRapidStats(white, black, whiteScore, blackScore);
            case CLASSICAL -> handleClassicalStats(white, black, whiteScore, blackScore);
        };

        userRepository.save(white);
        userRepository.save(black);

        return newElos;
    }

    private int[] handleBulletStats(UserEntity white, UserEntity black, double whiteScore, double blackScore){
        int whiteElo = calculateNewElo(white.getBulletElo(), black.getBulletElo(), whiteScore, getKFactor(white.getBulletGames(), white.isBeen2400Bullet()));
        int blackElo = calculateNewElo(black.getBulletElo(), white.getBulletElo(), blackScore, getKFactor(black.getBulletGames(), black.isBeen2400Bullet()));

        if (whiteScore == 1) {
            white.setBulletWins(white.getBulletWins() + 1);
            black.setBulletLosses(black.getBulletLosses() + 1);
        }

        if (blackScore == 1) {
            black.setBulletWins(black.getBulletWins() + 1);
            white.setBulletLosses(white.getBulletLosses() + 1);
        }

        white.setBulletGames(white.getBulletGames() + 1);
        if (whiteElo > 2399) white.setBeen2400Bullet(true);
        white.setBulletElo(whiteElo);

        black.setBulletGames(black.getBulletGames() + 1);
        if (blackElo > 2399) black.setBeen2400Bullet(true);
        black.setBulletElo(blackElo);

        return new int[]{whiteElo, blackElo};
    }

    private int[] handleBlitzStats(UserEntity white, UserEntity black, double whiteScore, double blackScore){
        int whiteElo = calculateNewElo(white.getBlitzElo(), black.getBlitzElo(), whiteScore, getKFactor(white.getBlitzGames(), white.isBeen2400Blitz()));
        int blackElo = calculateNewElo(black.getBlitzElo(), white.getBlitzElo(), blackScore, getKFactor(black.getBlitzGames(), black.isBeen2400Blitz()));

        if (whiteScore == 1) {
            white.setBlitzWins(white.getBlitzWins() + 1);
            black.setBlitzLosses(black.getBlitzLosses() + 1);
        }

        if (blackScore == 1) {
            black.setBlitzWins(black.getBlitzWins() + 1);
            white.setBlitzLosses(white.getBlitzLosses() + 1);
        }

        white.setBlitzGames(white.getBlitzGames() + 1);
        if (whiteElo > 2399) white.setBeen2400Blitz(true);
        white.setBlitzElo(whiteElo);

        black.setBlitzGames(black.getBlitzGames() + 1);
        if (blackElo > 2399) black.setBeen2400Blitz(true);
        black.setBlitzElo(blackElo);

        return new int[]{whiteElo, blackElo};
    }

    private int[] handleRapidStats(UserEntity white, UserEntity black, double whiteScore, double blackScore){
        int whiteElo = calculateNewElo(white.getRapidElo(), black.getRapidElo(), whiteScore, getKFactor(white.getRapidGames(), white.isBeen2400Rapid()));
        int blackElo = calculateNewElo(black.getRapidElo(), white.getRapidElo(), blackScore, getKFactor(black.getRapidGames(), black.isBeen2400Rapid()));

        if (whiteScore == 1) {
            white.setRapidWins(white.getRapidWins() + 1);
            black.setRapidLosses(black.getRapidLosses() + 1);
        }

        if (blackScore == 1) {
            black.setRapidWins(black.getRapidWins() + 1);
            white.setRapidLosses(white.getRapidLosses() + 1);
        }

        white.setRapidGames(white.getRapidGames() + 1);
        if (whiteElo > 2399) white.setBeen2400Rapid(true);
        white.setRapidElo(whiteElo);

        black.setRapidGames(black.getRapidGames() + 1);
        if (blackElo > 2399) black.setBeen2400Rapid(true);
        black.setRapidElo(blackElo);

        return new int[]{whiteElo, blackElo};
    }

    private int[] handleClassicalStats(UserEntity white, UserEntity black, double whiteScore, double blackScore){
        int whiteElo = calculateNewElo(white.getClassicalElo(), black.getClassicalElo(), whiteScore, getKFactor(white.getClassicalGames(), white.isBeen2400Classical()));
        int blackElo = calculateNewElo(black.getClassicalElo(), white.getClassicalElo(), blackScore, getKFactor(black.getClassicalGames(), black.isBeen2400Classical()));

        if (whiteScore == 1) {
            white.setClassicalWins(white.getClassicalWins() + 1);
            black.setClassicalLosses(black.getClassicalLosses() + 1);
        }

        if (blackScore == 1) {
            black.setClassicalWins(black.getClassicalWins() + 1);
            white.setClassicalLosses(white.getClassicalLosses() + 1);
        }

        white.setClassicalGames(white.getClassicalGames() + 1);
        if (whiteElo > 2399) white.setBeen2400Classical(true);
        white.setClassicalElo(whiteElo);

        black.setClassicalGames(black.getClassicalGames() + 1);
        if (blackElo > 2399) black.setBeen2400Classical(true);
        black.setClassicalElo(blackElo);

        return new int[]{whiteElo, blackElo};
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
