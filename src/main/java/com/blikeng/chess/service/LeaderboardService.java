package com.blikeng.chess.service;

import com.blikeng.chess.dto.LeaderboardPlayerDTO;
import com.blikeng.chess.exception.types.InvalidParameterException;
import com.blikeng.chess.exception.types.NotFoundException;
import com.blikeng.chess.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds Elo-ranked leaderboard pages for a given time control (BULLET, BLITZ, RAPID,
 * CLASSICAL).
 */
@Service
public class LeaderboardService {
    private final UserRepository userRepository;

    public LeaderboardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<LeaderboardPlayerDTO> getLeaderboardPlayers(String timeControl, int page){
        if (page < 0) throw new InvalidParameterException();

        return switch (timeControl) {
            case "BULLET" -> userRepository.findAllByOrderByBulletEloDesc(PageRequest.of(page, 25))
                .stream()
                .map(user -> new LeaderboardPlayerDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getBulletGames(),
                    user.getBulletWins(),
                    user.getBulletElo()
                )).toList();

            case "BLITZ" -> userRepository.findAllByOrderByBlitzEloDesc(PageRequest.of(page, 25))
                .stream()
                .map(user -> new LeaderboardPlayerDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getBlitzGames(),
                    user.getBlitzWins(),
                    user.getBlitzElo()
                )).toList();

            case "RAPID" -> userRepository.findAllByOrderByRapidEloDesc(PageRequest.of(page, 25))
                .stream()
                .map(user -> new LeaderboardPlayerDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getRapidGames(),
                    user.getRapidWins(),
                    user.getRapidElo()
                )).toList();

            case "CLASSICAL" -> userRepository.findAllByOrderByClassicalEloDesc(PageRequest.of(page, 25))
                .stream()
                .map(user -> new LeaderboardPlayerDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getClassicalGames(),
                    user.getClassicalWins(),
                    user.getClassicalElo()
                )).toList();

            default -> throw new NotFoundException();
        };
    }
}
