package com.blikeng.chess.service;

import com.blikeng.chess.dto.PasswordDTO;
import com.blikeng.chess.dto.PlayerStatsDTO;
import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.dto.ProfileEditDTO;
import com.blikeng.chess.entity.FriendId;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.BadEditException;
import com.blikeng.chess.exception.types.InvalidPasswordException;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.exception.types.UserNotFoundException;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.timecontrol.TcType;
import com.blikeng.chess.repository.FriendRepository;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.PasswordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final PasswordService passwordService;
    private final GameService gameService;

    public UserService(UserRepository userRepository, FriendRepository friendRepository, PasswordService passwordService, GameService gameService){
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
        this.passwordService = passwordService;
        this.gameService = gameService;
    }

    public ProfileDTO getUser(String username) {
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        if (username == null || username.trim().isBlank()) throw new UserNotFoundException();
        username = username.trim();

        UserEntity user = userRepository.findByUsernameIgnoreCase(username).orElseThrow(UserNotFoundException::new);

        boolean isFriend = false;
        boolean isSelf = username.equals(principal.username());
        if (!isSelf){
            isFriend = friendRepository.existsById(
                FriendId.generate(principal.userId(), user.getId())
            );
        }

        String gameId = gameService.getActiveGame(user.getId())
                .map(g -> g.getId().toString())
                .orElse(null);

        return new ProfileDTO(
            user.getUsername(),
            user.getBio(),
            user.getAvatarUrl(),

            user.getBulletElo(),
            user.getBulletGames(),
            user.getBulletWins(),
            user.getWinPercentage("bullet"),

            user.getBlitzElo(),
            user.getBlitzGames(),
            user.getBlitzWins(),
            user.getWinPercentage("blitz"),

            user.getRapidElo(),
            user.getRapidGames(),
            user.getRapidWins(),
            user.getWinPercentage("rapid"),

            user.getClassicalElo(),
            user.getClassicalGames(),
            user.getClassicalWins(),
            user.getWinPercentage("classical"),

            isFriend,
            gameId
        );
    }

    public void updateUser(ProfileEditDTO profileEditDTO){
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        UserEntity user = userRepository.findById(principal.userId()).orElseThrow(InvalidUserException::new);

        if (profileEditDTO.field().isBlank()) throw new BadEditException();

        switch (profileEditDTO.field()) {
            case "bio" -> user.setBio(profileEditDTO.newValue().trim());
            case "avatarUrl" -> user.setAvatarUrl(profileEditDTO.newValue().trim());
            default -> throw new BadEditException();
        }

        userRepository.save(user);
    }

    public void updatePassword(PasswordDTO passwordDTO){
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        if (
            passwordDTO.newPassword().isBlank() ||
            passwordDTO.newPassword().trim().length() > 128 ||
            passwordDTO.newPassword().trim().length() < 8
        ) throw new BadEditException();

        UserEntity user = userRepository.findById(principal.userId()).orElseThrow(InvalidUserException::new);

        if (!passwordService.checkPassword(passwordDTO.oldPassword(), user.getPassword())) throw new InvalidPasswordException();

        user.setPassword(passwordService.hashPassword(passwordDTO.newPassword()));
        userRepository.save(user);
    }

    public PlayerStatsDTO getPlayerStats(String username, String timeControl){
        UserEntity user = userRepository.findByUsernameIgnoreCase(username).orElseThrow(UserNotFoundException::new);
        TcType type = TcType.valueOf(timeControl.toUpperCase());

        int gamesPlayed = user.getGames(type);
        int wins = user.getWins(type);
        int losses = user.getLosses(type);
        int draws = gamesPlayed - wins - losses;

        List<GameEntity> games = gameService.getAllGames(username, timeControl);

        int winsByCheckmate = 0, winsByFlagging = 0, winsByResignation = 0;
        int lossesByCheckmate = 0, lossesByFlagging = 0, lossesByResignation = 0;
        int drawsByStalemate = 0, drawsByAgreement = 0, drawsByRepetition = 0, drawsBy50MoveRule = 0, drawsByInsufficientMaterial = 0;
        int gamesAsBlack = 0, winsAsBlack = 0, lossesAsBlack = 0;
        int gamesAsWhite = 0, winsAsWhite = 0, lossesAsWhite = 0;

        for (GameEntity game : games) {
            boolean isWhite = game.getWhite().getId().equals(user.getId());
            boolean won = isWhite ? game.getStatus() == GameStatus.WHITE_WIN : game.getStatus() == GameStatus.BLACK_WIN;
            boolean lost = isWhite ? game.getStatus() == GameStatus.BLACK_WIN : game.getStatus() == GameStatus.WHITE_WIN;

            if (isWhite) {
                gamesAsWhite++;
                if (won) winsAsWhite++;
                if (lost) lossesAsWhite++;
            } else {
                gamesAsBlack++;
                if (won) winsAsBlack++;
                if (lost) lossesAsBlack++;
            }

            if (game.getEndedBy() == null) continue;

            if (won) switch (game.getEndedBy()) {
                case CHECKMATE -> winsByCheckmate++;
                case TIMEOUT -> winsByFlagging++;
                case RESIGNATION -> winsByResignation++;
                default -> {}
            }
            else if (lost) switch (game.getEndedBy()) {
                case CHECKMATE -> lossesByCheckmate++;
                case TIMEOUT -> lossesByFlagging++;
                case RESIGNATION -> lossesByResignation++;
                default -> {}
            }
            else switch (game.getEndedBy()) {
                case STALEMATE -> drawsByStalemate++;
                case AGREEMENT -> drawsByAgreement++;
                case REPETITION -> drawsByRepetition++;
                case FIFTY_MOVE_RULE -> drawsBy50MoveRule++;
                case INSUFFICIENT_MATERIAL -> drawsByInsufficientMaterial++;
                default -> {}
            }
        }

        return new PlayerStatsDTO(
            user.getElo(type),
            wins, losses, draws, gamesPlayed,
            winsByCheckmate, winsByFlagging, winsByResignation,
            lossesByCheckmate, lossesByFlagging, lossesByResignation,
            drawsByStalemate, drawsByAgreement, drawsByRepetition, drawsBy50MoveRule, drawsByInsufficientMaterial,
            gamesAsBlack, winsAsBlack, lossesAsBlack,
            gamesAsWhite, winsAsWhite, lossesAsWhite
        );
    }
}
